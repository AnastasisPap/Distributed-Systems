package com.aueb;

import com.aueb.handlers.ServicesHandler;
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
    // Key: room ID
    // Value: Room object
    HashMap<Integer, Room> rooms = new HashMap<>();
    HashMap<Integer, Room> backupRoomsPerID = new HashMap<>();
    HashMap<Integer, ArrayList<Room>> backupRoomsPerPort = new HashMap<>();
    private final int port;

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        Worker worker = new Worker(port);
        worker.start();
    }

    public Worker(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Started worker at port " + port);

            // Worker accepts connections until program stops
            while (true) {
                Socket connection = server.accept();
                System.out.println("Accepted packet");

                // Open a new thread on new connection
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

    // Routes the input based on the requested function, gets the response object and sends it to the reducer
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

        if (request.returnOutput) {
            System.out.println("Worker " + port + " sending to reducer.");
            sendResponseToReducer(response);
        }
    }

    // We use synchronized to get the latest value as multiple threads could be updating the map
    private synchronized ArrayList<Room> roomsToArray() {
        ArrayList<Room> allRooms = new ArrayList<>(rooms.values());

        for (int port : backupRoomsPerPort.keySet()) {
            synchronized (ServicesHandler.failedWorkers) {
                if (ServicesHandler.failedWorkers.contains(port))
                    allRooms.addAll(backupRoomsPerPort.get(port));
            }
        }

        return allRooms;
    }

    // The input packet contains a RoomFilters object
    // The output packet contains a list of rooms that satisfy all conditions on the filter
    private Packet filterRooms(Packet request) {
        ArrayList<Room> filteredRooms = new ArrayList<>();
        Packet response = new Packet(request);
        Room.RoomFilters filters = (Room.RoomFilters) request.data;
        ArrayList<Room> roomsToFilter = new ArrayList<>(roomsToArray());

        for (int port : backupRoomsPerPort.keySet()) {
            if (request.failedWorkers.contains(port))
                roomsToFilter.addAll(backupRoomsPerPort.get(port));
        }

        for (Room room : roomsToFilter)
            if (room.satisfiesConditions(filters)) filteredRooms.add(room);

        response.data = filteredRooms;
        return response;
    }

    // The input packet contains the username
    // The output packet contains a string of all the bookings for the rooms of this worker
    // (empty if no bookings on this worker)
    private Packet showBookings(Packet request) {
        Packet response = new Packet(request);
        String username = request.data.toString();
        ArrayList<String> bookings = new ArrayList<>();

        for (Room room : roomsToArray()) {
            String currBookings = room.getBookings(username);
            if (!currBookings.isEmpty()) bookings.add(room.roomName + ": " + currBookings);
        }
        response.data = bookings;

        return response;
    }

    // The input packet contains the date range, username, and room ID to book room
    // The output packet contains a string that says if it was successful, unsuccessful, or the room ID not found
    private Packet bookRoom(Packet request) {
        Packet res = new Packet(request);
        Object[] data = (Object[]) request.data;
        int room_id = (int) data[0];

        // Use synchronized because the thread that adds the room might not have finished adding it yet if at race
        // condition
        synchronized (rooms) {
            if (!rooms.containsKey(room_id) && !backupRoomsPerID.containsKey(room_id)) res.data = "Couldn't find room";
            else {
                boolean bookedRoom;
                if (rooms.containsKey(room_id))
                    bookedRoom = rooms.get(room_id).book(data[2].toString(), (Range<Long>) data[1]);
                else
                    bookedRoom = backupRoomsPerID.get(room_id).book(data[2].toString(), (Range<Long>) data[1]);
                if (bookedRoom) {
                    res.data = "Successfully booked room.";
                    System.out.println("Successfully booked room.");
                } else {
                    res.data = "Couldn't book room for the given date.";
                    System.out.println("Couldn't book room for the given date.");
                }
            }
        }

        return res;
    }

    // Input packet contains only the function
    // The output packet contains a string that has all the rooms of this worker (or empty is no rooms)
    private Packet showRooms(Packet request) {
        Packet res = new Packet(request);

        res.data = roomsToArray();

        return res;
    }

    // The input packet contains a list of room objects
    // The output packet contains a string that confirms that rooms were added successfully
    private Packet addRooms(Packet request) {
        Packet res = new Packet(request);

        for (Room room : (ArrayList<Room>) request.data) {
            // use synchronized as multiple threads can be writing at the object at the same time
            if (room.isBackup) {
                synchronized (backupRoomsPerID) {
                    backupRoomsPerID.put(room.id, room);
                }
                synchronized (backupRoomsPerPort) {
                    int port = room.portBackup;
                    if (!backupRoomsPerPort.containsKey(port)) backupRoomsPerPort.put(port, new ArrayList<>());
                    backupRoomsPerPort.get(port).add(room);
                }
            } else {
                synchronized (rooms) {
                    rooms.put(room.id, room);
                }
            }
        }

        res.data = "Successfully added rooms";
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
