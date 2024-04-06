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
            case "show_bookings" -> showBookings(request);
            case "filter" -> filterRooms(request);
            default -> throw new RuntimeException("Invalid function");
        };

        sendResponseToReducer(response);
    }

    private synchronized ArrayList<Room> roomsToArray() {
        return new ArrayList<>(rooms.values());
    }

    private Packet filterRooms(Packet request) {
        ArrayList<Room> filtered_rooms = new ArrayList<>();
        Packet response = new Packet(request);
        Room.RoomFilters filters = (Room.RoomFilters) request.data;

        for (Room room : roomsToArray())
            if (room.satisfiesConditions(filters)) filtered_rooms.add(room);

        response.data = filtered_rooms;
        if (!filtered_rooms.isEmpty()) response.successful = true;
        return response;
    }

    private Packet showBookings(Packet request) {
        Packet response = new Packet(request);
        String username = request.data.toString();
        String bookings = "";

        for (Room room : roomsToArray())
            bookings += room.getBookings(username);
        response.data = bookings;
        if (!bookings.isEmpty()) response.successful = true;

        return response;
    }

    private Packet bookRoom(Packet request) {
        Packet res = new Packet(request);
        Object[] data = (Object[]) request.data;
        int room_id = (int) data[0];

        synchronized (rooms) {
            if (!rooms.containsKey(room_id)) res.output = "Couldn't find room";
            else {
                boolean booked_room;
                booked_room = rooms.get(room_id).book(data[2].toString(), (Range<Integer>) data[1]);
                if (booked_room) res.output = "Successfully booked room.";
                else res.output = "Couldn't book room for the given date.";
                res.successful = true;
            }
        }

        return res;
    }

    private Packet showRooms(Packet request) {
        Packet res = new Packet(request);

        ArrayList<Room> arr = roomsToArray();
        res.data = arr;
        if (!arr.isEmpty()) res.successful = true;
        return res;
    }

    private Packet addRooms(Packet request) {
        Packet res = new Packet(request);

        for (Room room : (ArrayList<Room>) request.data)
            synchronized (rooms) {
                rooms.put(room.id, room);
            }

        res.output = "Successfully added rooms\n";
        res.successful = true;
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
