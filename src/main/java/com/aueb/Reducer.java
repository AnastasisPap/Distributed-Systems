package com.aueb;

import com.aueb.handlers.ServicesHandler;
import com.aueb.packets.Packet;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Reducer extends Thread {
    public static void main(String[] args) {
        new Reducer().start();
    }

    // Key: connection ID (similar to Map ID)
    // Value: list of packets (list because we append from each worker)
    private final HashMap<Integer, HashSet<Packet>> connectionOutputs = new HashMap<>();
    public static int REDUCER_PORT = 6969; // Only workers connect to this port

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(REDUCER_PORT);
            System.out.println("Reducer listening at " + REDUCER_PORT);

            // Keep accepting connections until the program stops
            while (true) {
                Socket connection = server.accept();
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                Packet res = (Packet) in.readObject();

                // Start a new thread and handle the request (either append it to the results, or send to master)
                new Thread(() -> handleWorkerResponse(res)).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleWorkerResponse(Packet res) {
        // We need to use synchronized since multiple threads can be writing at the map at the same time, which can
        // lead to race conditions (data loss/exception)
        synchronized (connectionOutputs) {
            if (!connectionOutputs.containsKey(res.connectionId)) {
                connectionOutputs.put(res.connectionId, new HashSet<>());
            }

            connectionOutputs.get(res.connectionId).add(res);
        }

        // Use synchronized for the same reason as before
        int workersLeft;
        synchronized (ServicesHandler.numOfWorkersPerConnection) {
            workersLeft = ServicesHandler.numOfWorkersPerConnection.get(res.connectionId);
            workersLeft--;
            // For the current connection id, reduce the expecting number of workers by 1
            ServicesHandler.numOfWorkersPerConnection.put(res.connectionId, workersLeft);
        }

        // If no more workers are expected, send the results to the master
        if (workersLeft == 0)
            sendToMaster(res);
    }

    private void sendToMaster(Packet packet) {
        try {
            Socket masterConnection = new Socket("127.0.0.1", Master.MASTER_PORT_REDUCER);
            ObjectOutputStream out = new ObjectOutputStream(masterConnection.getOutputStream());
            out.writeObject(prepareResponse(packet));
            out.flush();
            out.close();
            masterConnection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Packet prepareResponse(Packet packet) {
        Packet res = new Packet(packet);

        ArrayList<Packet> packets;
        // Use synchronized to get the newest result
        // We could possibly not include it as for that connection id, no more threads will be writing to the map
        synchronized (connectionOutputs) {
            packets = new ArrayList<>(connectionOutputs.get(res.connectionId));
        }

        if (!packets.isEmpty()) {
            String function = packets.getFirst().function;
            switch (function) {
                case "add_rooms", "book_room", "rate_room":
                    res.data = packets.getFirst().data;
                    break;
                case "show_rooms", "filter":
                    ArrayList<Room> rooms = new ArrayList<>();
                    for (Packet roomsList : packets) {
                        rooms.addAll((ArrayList<Room>) roomsList.data);
                    }
                    res.data = rooms;
                    break;
                case "show_bookings":
                    ArrayList<String> totalBookings = new ArrayList<>();
                    for (Packet currPacket : packets)
                        totalBookings.addAll((ArrayList<String>) currPacket.data);

                    res.data = totalBookings;
                    break;
            }
        }

        return res;
    }
}
