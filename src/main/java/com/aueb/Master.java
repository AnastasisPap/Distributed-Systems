package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master {
    private final JSONParser parser = new JSONParser();
    private final List<Socket> workers = new ArrayList<>();
    private final int MAX_WORKERS = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_WORKERS);
    public static final int PORT = 9000;

    private void start() throws IOException, ClassNotFoundException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[INFO] Server listening at " + PORT);

        while (true) {
            Socket socket = server.accept();

            executor.submit(() -> {
                try {
                    while (true) {
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        String input = in.readUTF();
                        System.out.println("[INFO] Received: " + input);
                        if (input.startsWith("WORKER")) workers.add(socket);
                        else dispatchTask(input);
                    }
                } catch (Exception e) {
                    System.out.println("[WARNING] Lost connection because of error: " + e);
                }
            });
        }
    }

    private void dispatchTask(String taskInput) throws IOException, ParseException {
        if (taskInput.contains("show_rooms")) handleRequestRooms(taskInput);
        else if (taskInput.contains("fetched_rooms")) handleFetchedRooms(taskInput);
        else handleUserTask(taskInput);
    }

    private void handleFetchedRooms(String taskInput) throws ParseException {
        JSONParser new_parser = new JSONParser();
        JSONObject json_obj = (JSONObject) new_parser.parse(taskInput);
        JSONArray dates = (JSONArray) json_obj.get("rooms");
        for (Object room_obj : (JSONArray) json_obj.get("rooms"))
            showRoom((JSONObject) room_obj);
    }

    private void showRoom(JSONObject json_room) {
        String output = "---------------------------------------\n";
        output += "Room name: " + json_room.get("room_name") + " with ID: " + json_room.get("id") + "\n";
        output += "Area: " + json_room.get("area") + "\n";
        output += "Capacity: " + json_room.get("num_of_people") + "\n";
        output += "Average review: " + json_room.get("review") + " (" + json_room.get("num_of_reviews") + " review(s))\n";
        output += "Price per day: " + json_room.get("price") + "\n";
        JSONArray dates = (JSONArray) json_room.get("available_dates");
        output += "Available days (" + (dates.size()) + "):\n";
        for (Object date_epoch : dates) {
            LocalDate date = LocalDate.ofEpochDay(Integer.parseInt(date_epoch.toString()));
            output += date + ", ";
        }
        output += "\n";
        System.out.println(output);
    }

    private void handleRequestRooms(String taskInput) throws IOException {
        DataOutputStream out;
        for (Socket workerSocket : workers) {
            out = new DataOutputStream(workerSocket.getOutputStream());
            if (!workerSocket.isClosed()) {
                System.out.println(taskInput);
                out.writeUTF(taskInput);
            }
        }
    }

    private void handleUserTask(String taskInput) throws IOException {
        int workerIdx = 0;
        try {
            String id = ((JSONObject) parser.parse(taskInput)).get("id").toString();
            workerIdx = id.hashCode() % workers.size();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Socket selectedWorker = workers.get(workerIdx);

        DataOutputStream out = new DataOutputStream(selectedWorker.getOutputStream());
        out.writeUTF(taskInput);
    }

    public static void main(String[] args) {
        Master server = new Master();
        try {
            server.start();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}