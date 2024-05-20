package com.aueb;

import com.aueb.handlers.ServicesHandler;
import com.aueb.handlers.UserConnectionHandler;
import com.aueb.packets.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Master extends Thread {
    public static void main(String[] args) {
        new Master().start();
    }

    public static int MASTER_PORT_USER = 5050; // Only dummy user will connect to this port
    public static int MASTER_PORT_REDUCER = 5060; // Only the reducer will connect to this port
    // Key: connection ID (similar to map ID)
    // Value: OutputStream
    // It's used when we get the result from the reducer, and we need to find which socket to send the response to
    private final HashMap<Integer, ObjectOutputStream> out_map = new HashMap<>();

    @Override
    public void run() {
        // Start one thread for listening to users and one thread for listening to reducer
        new Thread(this::handleUserConnection).start();
        new Thread(this::handleReducerConnection).start();
    }

    // Called for dummy user connections
    private void handleUserConnection() {
        // Used as Connection ID (similar to map ID)
        int connection_cnt = 0;
        System.out.println("[START UP] Master listening at " + MASTER_PORT_USER + " for users.");

        try {
            ServerSocket user_socket = new ServerSocket(MASTER_PORT_USER);

            // Keep accepting connections until the program stops
            while (true) {
                Socket connection = user_socket.accept();
                System.out.println("User with ID " + connection_cnt + " connected.");
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

                // No need to use synchronized since only this thread will write to the map (no race conditions)
                out_map.put(connection_cnt, out);
                // Start new thread and handle the user request
                UserConnectionHandler connection_handler = new UserConnectionHandler(in, connection_cnt);
                connection_handler.start();
                connection_cnt++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called for reducer connections
    private void handleReducerConnection() {
        try {
            System.out.println("Master listening at " + MASTER_PORT_REDUCER + " for reducer.");
            ServerSocket reducer_socket = new ServerSocket(MASTER_PORT_REDUCER);

            while (true) {
                Socket connection = reducer_socket.accept();
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

                Packet res = (Packet) in.readObject();

                // No need to use synchronized since only one thread writes at the map (no race condition)
                if (out_map.containsKey(res.connection_id)) {
                    out_map.get(res.connection_id).writeObject(res.toString());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
