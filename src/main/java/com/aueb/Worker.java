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

    public void processTask(String input) {
        try {
            JSONObject json_obj = (JSONObject) parser.parse(input);

            System.out.println("[INFO] Function: " + json_obj.get("function"));
            if (json_obj.get("function").toString().startsWith("add_room")) handleRoom((JSONObject) json_obj.get("room"));
            else if (json_obj.get("function").toString().startsWith("add_availability")) handleAvailability(json_obj);
            else if (json_obj.get("function").toString().startsWith("show_rooms")) showRooms();
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleAvailability(JSONObject json_obj) {
        int id = Integer.parseInt(json_obj.get("id").toString());
        System.out.println("[INFO] Searching " + rooms.size() + " room(s) for room with ID: " + id);
        JSONArray dates_array = (JSONArray) json_obj.get("dates");
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id == id) {
                for (Object date : dates_array) {
                    int epoch = Integer.parseInt(date.toString());
                    rooms.get(i).available_days.add(epoch);
                }

                System.out.println("[INFO] Successfully added, available dates: " + rooms.get(i).available_days);
            }
        }
    }

    public void handleRoom(JSONObject json_obj) {
        Room room = new Room(json_obj);
        rooms.add(room);
        System.out.println("[INFO] Successfully stored room " + json_obj.get("id"));
    }

    public void showRooms() throws IOException {
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        JSONArray rooms_json = new JSONArray();
        for (Room room : rooms)
            rooms_json.add(room.getJSON());
        JSONObject fetched_rooms = Utils.createJSONObject("fetched_rooms");
        fetched_rooms.put("rooms", rooms_json);
        out.writeUTF(fetched_rooms.toString());
    }

    public void connectToMaster() throws IOException {
        clientSocket = new Socket("127.0.0.1", Master.PORT);

        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.writeUTF("WORKER");

        new Thread(this::listenForMasterRequests).start();
    }

}
