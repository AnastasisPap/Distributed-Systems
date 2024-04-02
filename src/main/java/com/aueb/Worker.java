package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {
    private Socket clientSocket;
    private final ArrayList<Room> rooms = new ArrayList<>();
    private final JSONParser parser = new JSONParser();
    private final int MAX_THREADS = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    public static void main(String[] args) {
        Worker worker = new Worker();
        try {
            worker.connectToMaster();
        } catch (IOException e) { throw new RuntimeException("Connection to Master refused"); }
    }

    // Socket constantly open and listens for requests from the master.
    // When a new task arrives, a new thread opens and the task is processed
    public void listenForMasterRequests() {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            while (true) {
                String input = in.readUTF();
                executor.submit(() -> processTask(input));
            }
        } catch (IOException e) {
            throw new RuntimeException("Connection to Master lost");
        }
    }

    // Routes the tasks depending on which function is requested to be performed
    public void processTask(String input) {
        try {
            JSONObject json_obj = (JSONObject) parser.parse(input);

            System.out.println("[INFO] Function: " + json_obj.get("function"));
            if (json_obj.get("function").toString().startsWith("add_room")) handleRoom((JSONObject) json_obj.get("room"));
            else if (json_obj.get("function").toString().startsWith("add_availability")) handleAvailability(json_obj);
            else if (json_obj.get("function").toString().startsWith("show_rooms")) showRooms();
            else if (json_obj.get("function").toString().startsWith("book_room")) bookRoom(json_obj);
            else if (json_obj.get("function").toString().startsWith("rate_room")) rateRoom(json_obj);
            else if (json_obj.get("function").toString().startsWith("filter_rooms")) filterRooms((JSONObject) json_obj.get("filters"));
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Checks which rooms satisfy all the conditions and sends them as a JSON Array to the Master
    private void filterRooms(JSONObject json_obj) throws IOException {
        JSONArray filtered_rooms = new JSONArray();
        for (int i = 0; i < rooms.size(); i++) {
            // If room i satisfies conditions, add it to the list
            if (rooms.get(i).satisfiesConditions(json_obj))
                filtered_rooms.add(rooms.get(i).id);
        }

        JSONObject rooms = new JSONObject();
        rooms.put("filtered_rooms", filtered_rooms);
        // Send it to the Master
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.writeUTF(rooms.toJSONString());
    }

    // Adds a rating to the room
    private void rateRoom(JSONObject json_obj) {
        int id = Integer.parseInt(json_obj.get("id").toString());
        float rating = Float.parseFloat(json_obj.get("rating").toString());
        System.out.println("[INFO] Searching " + rooms.size() + " room(s) for room with ID: " + id);

        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id == id) {
                rooms.get(i).rateRoom(rating);
                System.out.println("Rated room with ID " + id + " with " + rating + " stars.");
            }
        }
    }

    // Books room
    private void bookRoom(JSONObject json_obj) {
        int id = Integer.parseInt(json_obj.get("id").toString());
        System.out.println("[INFO] Searching " + rooms.size() + " room(s) for room with ID: " + id);
        JSONArray date_range = (JSONArray) json_obj.get("date_range");

        // Iterates over all rooms to find the index of the selected room
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id == id) {
                // Convert int representation of date to string date
                LocalDate start_date = LocalDate.ofEpochDay(Integer.parseInt(date_range.get(0).toString()));
                LocalDate end_date = LocalDate.ofEpochDay(Integer.parseInt(date_range.get(1).toString()));
                // If the room can be booked for the specific date range, book it
                if (rooms.get(i).book(date_range)) {
                    System.out.println("Successfully booked room with ID: " + id + " for " + start_date + " until " +
                            end_date + ". Current available days:");
                    System.out.println(rooms.get(i).getAvailableDates());
                } else { // Room is unavailable for at least one day in the given range
                    System.out.println("Couldn't book room with ID " + id + " for " + start_date + " until " + end_date
                            + ". Available days:");
                    System.out.println(rooms.get(i).getAvailableDates());
                }
            }
        }
    }

    // Add date range to availability
    private void handleAvailability(JSONObject json_obj) {
        int id = Integer.parseInt(json_obj.get("id").toString());
        System.out.println("[INFO] Searching " + rooms.size() + " room(s) for room with ID: " + id);
        JSONArray date_range = (JSONArray) json_obj.get("date_range");
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id == id) {
                rooms.get(i).addRange(date_range);

                System.out.println("[INFO] Successfully added, available dates: " + rooms.get(i).getAvailableDates());
            }
        }
    }

    // Store room in the memory of the Worker
    private void handleRoom(JSONObject json_obj) {
        Room room = new Room(json_obj);
        rooms.add(room);
        System.out.println("[INFO] Successfully stored room " + json_obj.get("id"));
    }

    // Get all the stored rooms, format them as JSON, return them as an Array to the Master
    private void showRooms() throws IOException {
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        JSONArray rooms_json = new JSONArray();
        for (Room room : rooms)
            rooms_json.add(room.getJSON());
        JSONObject fetched_rooms = Utils.createJSONObject("fetched_rooms");
        fetched_rooms.put("rooms", rooms_json);
        out.writeUTF(fetched_rooms.toString());
    }

    // Request to connect with the Master
    public void connectToMaster() throws IOException {
        clientSocket = new Socket("127.0.0.1", Master.PORT);

        // send information to the master that the worker connected and is a worker
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.writeUTF("WORKER");

        new Thread(this::listenForMasterRequests).start();
    }

}
