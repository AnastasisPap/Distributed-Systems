package com.aueb;

import com.aueb.handlers.ServicesHandler;
import com.aueb.packets.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Reducer extends Thread {
    private final HashMap<Integer, ArrayList<Packet>> connection_outputs = new HashMap<>();
    public static int REDUCER_PORT = 6969;

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(REDUCER_PORT);
            System.out.println("[START UP] Reducer listening at " + REDUCER_PORT);

            while (true) {
                Socket connection = server.accept();
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                Packet res = (Packet) in.readObject();

                new Thread(() -> handleWorkerResponse(res)).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleWorkerResponse(Packet res) {
        synchronized (connection_outputs) {
            if (!connection_outputs.containsKey(res.connection_id)) {
                connection_outputs.put(res.connection_id, new ArrayList<>());
            }

            if (res.successful) {
                connection_outputs.get(res.connection_id).add(res);
            }
        }

        int workers_left;
        synchronized (ServicesHandler.num_of_workers_per_connection) {
            workers_left = ServicesHandler.num_of_workers_per_connection.get(res.connection_id);
            workers_left--;
            ServicesHandler.num_of_workers_per_connection.put(res.connection_id, workers_left);
        }

        if (workers_left == 0) {
            sendToMaster(res);
        }
    }

    private void sendToMaster(Packet packet) {
        Packet res = new Packet(packet);
        synchronized (connection_outputs) {
            res.output = connection_outputs.get(res.connection_id).toString();
        }

        try {
            Socket master_connection = new Socket("127.0.0.1", Master.MASTER_PORT_REDUCER);
            ObjectOutputStream out = new ObjectOutputStream(master_connection.getOutputStream());
            out.writeObject(res);
            out.flush();
            out.close();
            master_connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
