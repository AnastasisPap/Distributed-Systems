package com.aueb;

import com.aueb.packets.Packet;
import com.google.common.collect.Range;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Worker extends Thread {
    HashMap<Integer, Room> rooms = new HashMap<>();
    private final int port;

    public Worker(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("[START UP] Started worker at port " + port);

            while (true) {
                Socket connection = server.accept();
                System.out.println("Accepted packet");

                new Thread(() -> {
                    try {
                        handleRequest(connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRequest(Socket connection) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
        Packet request = (Packet) in.readObject();
        Packet response = switch (request.function) {
            case "add_rooms" -> addRooms(request);
            case "book_room" -> bookRoom(request);
            case "show_rooms" -> showRooms(request);
            default -> throw new RuntimeException("Invalid function");
        };

        sendResponseToReducer(response);
    }

    private Packet bookRoom(Packet request) {
        Packet res = new Packet(request);
        Object[] data = (Object[]) request.data;
        int room_id = (int) data[0];
        if (!rooms.containsKey(room_id)) res.output = "Couldn't find room";
        else {
            boolean booked_room = rooms.get(room_id).book((Range<Integer>) data[1]);
            if (booked_room) res.output = "Successfully booked room.";
            else res.output = "Couldn't book room for the given date.";
        }

        return res;
    }

    private Packet showRooms(Packet request) {
        Packet res = new Packet(request);

        res.data = new ArrayList<>(rooms.values());
        return res;
    }

    private Packet addRooms(Packet request) {
        Packet res = new Packet(request);

        for (Room room : (ArrayList<Room>) request.data)
            rooms.put(room.id, room);

        res.output = "Successfully added rooms\n";
        return res;
    }

    private void sendResponseToReducer(Packet response) {
        try {
            Socket reducer_socket = new Socket("127.0.0.1", Reducer.REDUCER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(reducer_socket.getOutputStream());
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
