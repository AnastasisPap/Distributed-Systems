package com.aueb;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.checkerframework.checker.units.qual.A;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Worker extends Thread {
    private final HashMap<Integer, ArrayList<Room>> rooms_map = new HashMap<>();
    private ServerSocket socket;
    private Socket connection;
    public int PORT;

    public Worker(int PORT) { this.PORT = PORT; }

    public void run() {
        try {
            socket = new ServerSocket(PORT);
            System.out.println("Worker listening at " + PORT);

            while (true) {
                connection = socket.accept();

                new Thread(() -> { handleTasks(connection); }).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleTasks(Socket connection) {
        try {
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            RequestPayload request = (RequestPayload) in.readObject();
            ResponsePayload res = new ResponsePayload();
            if (request.function.equals("add_rooms")) {
                res = addRoom(request);
                System.out.println(res);
            }
            else if (request.function.equals("show_rooms")) showRooms(request);
            else if (request.function.equals("add_availability")) {
                res = addAvailability(request);
                System.out.println(res);
            }
            else if (request.function.equals("book_room")) {
                res = bookRoom(request);
                System.out.println(res);
            }
            else if (request.function.equals("filter_rooms")) filterRooms(request);
            else if (request.function.equals("show_bookings")) showBookings(request);

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponsePayload bookRoom(RequestPayload request) {
        ResponsePayload res = new ResponsePayload();

        boolean booked_room = false;
        for (Map.Entry<Integer, ArrayList<Room>> entry : rooms_map.entrySet()) {
            for (Room room : entry.getValue()) {
                if (room.id == request.room_id) {
                    Range<Integer> date = (Range<Integer>) request.data;
                    boolean booked = room.book(date);
                    if (booked) booked_room = true; break;
                }
            }
            if (booked_room) break;
        };

        if (booked_room) res.output_response = "Successfully Booked Room";
        else res.output_response = "Failed to Book Room";
        return res;
    }

    private ResponsePayload addAvailability(RequestPayload request) {
        ResponsePayload res = new ResponsePayload();
        for (Room room : rooms_map.get(request.user_id)) {
            if (room.id == request.room_id) {
                RangeSet<Integer> dates = (RangeSet<Integer>) request.data;
                room.addMultipleRanges(dates);
                res.output_response = "All available days:\n" + room.getAvailableDates();
                return res;
            }
        }

        res.output_response = "Can't find room with that ID";

        return res;
    }

    private ResponsePayload addRoom(RequestPayload request) {
        ResponsePayload res = new ResponsePayload();
        if (!rooms_map.containsKey(request.user_id)) rooms_map.put(request.user_id, new ArrayList<>());

        try {
            rooms_map.get(request.user_id).add((Room) request.data);
            res.output_response = "Successfully added room with ID " + request.room_id;
            System.out.println(rooms_map);
        } catch (Exception e) {
            res.output_response = "Error: " + e.getLocalizedMessage();
        }

        return res;
    }

    private void showRooms(RequestPayload request) throws IOException {
        ResponsePayload res = new ResponsePayload();
        for (Map.Entry<Integer, ArrayList<Room>> entry : rooms_map.entrySet()) {
            res.rooms_response.addAll(entry.getValue());
        }
        res.map_id = request.map_id;

        sendToReducer(res);
    }

    private void showBookings(RequestPayload request) throws IOException {
        ResponsePayload res = new ResponsePayload();
        res.map_id = request.map_id;
        String output_str = "";
        for (Room room : rooms_map.get(request.user_id))
            if (!room.bookings.isEmpty())
                output_str += "Room with ID=" + room.id + " has bookings on: " + room.getBookings() + "\n";

        if (!output_str.isEmpty()) res.output_response = output_str;
        System.out.println(res);
        sendToReducer(res);
    }

    private void sendToReducer(ResponsePayload res) throws IOException {
        Socket reducer = new Socket("127.0.0.1", Reducer.REDUCER_PORT);
        ObjectOutputStream reducer_out = new ObjectOutputStream(reducer.getOutputStream());
        reducer_out.writeObject(res);
    }

    private void filterRooms(RequestPayload request) throws IOException {
        ResponsePayload res = new ResponsePayload();
        res.map_id = request.map_id;
        Room.RoomFilters filters = (Room.RoomFilters) request.data;

        int i = 0;
        for (Map.Entry<Integer, ArrayList<Room>> entry : rooms_map.entrySet()) {
            for (Room room : entry.getValue()) {
                if (room.satisfiesConditions(filters)) {
                    res.rooms_response.add(room);
                    i++;
                }
            }
        }
        res.output_response = "Found " + i + " rooms satisfying filters.";

        sendToReducer(res);
    }

    public static void main(String[] args) {
        // System.out.println("Give the path for the config file: ");
        // Scanner in = new Scanner(System.in);
        // String path = in.nextLine();
        String path = "/Users/anastasispap/IdeaProjects/Distributed-Systems/src/main/resources/config.json";
        JSONParser parser = new JSONParser();
        List<Thread> workers = new ArrayList<>();
        try {
            JSONObject config = (JSONObject) parser.parse(new FileReader(path));
            int num_of_workers = Integer.parseInt(config.get("num_of_workers").toString());
            JSONArray ports = (JSONArray) config.get("worker_ports");
            for (int i = 0; i < num_of_workers; i++) {
                Thread worker = new Worker(Integer.parseInt(ports.get(i).toString()));
                worker.start();
                workers.add(worker);
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}