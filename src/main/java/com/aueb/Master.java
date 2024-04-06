package com.aueb;

import com.aueb.handlers.UserConnectionHandler;
import com.aueb.packets.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Master extends Thread {
    public static int MASTER_PORT_USER = 9080;
    public static int MASTER_PORT_REDUCER = 9090;
    private final HashMap<Integer, ObjectOutputStream> out_map = new HashMap<>();

    @Override
    public void run() {
        new Thread(this::handleUserConnection).start();
        new Thread(this::handleReducerConnection).start();
    }

    private void handleUserConnection() {
        int connection_cnt = 0;
        System.out.println("[START UP] Master listening at " + MASTER_PORT_USER + " for users.");

        try {
            ServerSocket user_socket = new ServerSocket(MASTER_PORT_USER);

            while (true) {
                Socket connection = user_socket.accept();
                System.out.println("User with ID " + connection_cnt + " connected.");
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

                synchronized (out_map) {
                    out_map.put(connection_cnt, out);
                }
                UserConnectionHandler connection_handler = new UserConnectionHandler(in, connection_cnt);
                connection_handler.start();
                connection_cnt++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReducerConnection() {
        try {
            System.out.println("Master listening at " + MASTER_PORT_REDUCER + " for reducer.");
            ServerSocket reducer_socket = new ServerSocket(MASTER_PORT_REDUCER);

            while (true) {
                Socket connection = reducer_socket.accept();
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

                Packet res = (Packet) in.readObject();

                synchronized (out_map) {
                    if (out_map.containsKey(res.connection_id))
                        out_map.get(res.connection_id).writeObject(res);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
