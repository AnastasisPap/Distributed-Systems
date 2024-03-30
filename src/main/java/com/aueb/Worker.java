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
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void listenForMasterRequests() {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            while (true) {
                String input = in.readUTF();
                executor.submit(() -> processTask(input));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processTask(String input) {
        try {
            JSONObject json_obj = (JSONObject) parser.parse(input);
            System.out.println("Function: " + json_obj.get("function"));
            if (json_obj.get("function").toString().startsWith("add_room")) handleRoom((JSONObject) json_obj.get("room"));
            else if (json_obj.get("function").toString().startsWith("add_availability")) handleAvailability(json_obj);
            else if (json_obj.get("function").toString().startsWith("show_rooms")) showRooms();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleAvailability(JSONObject json_obj) {
        int id = Integer.parseInt(json_obj.get("id").toString());
        System.out.println("Searching " + rooms.size() + " room(s) for room with ID: " + id);
        JSONArray dates_array = (JSONArray) json_obj.get("dates");
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id == id) {
                for (Object date : dates_array) {
                    int epoch = Integer.parseInt(date.toString());
                    rooms.get(i).available_days.add(epoch);
                }

                System.out.println("Successfully added, available dates: " + rooms.get(i).available_days);
            }
        }
    }

    public void handleRoom(JSONObject json_obj) {
        System.out.println("Adding room: " + json_obj);
        Room room = new Room(json_obj);
        rooms.add(room);
        System.out.println("Successfully stored room " + json_obj.get("id"));
    }

    public void showRooms() {
        for (Room room : rooms) {
            System.out.println("----------------------------------------------");
            System.out.println("Room " + room.room_name + " (ID: " + room.id + ") located at " + room.area +
                    " can house " + room.num_of_people + " and has a rating of " + room.rating + ". Available dates:");
            for (int epoch : room.available_days) {
                LocalDate date = LocalDate.ofEpochDay(epoch);
                System.out.print(date + ", ");
            }
            System.out.println();
        }
    }

    public void connectToMaster() throws IOException {
        clientSocket = new Socket("127.0.0.1", Master.PORT);

        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.writeUTF("WORKER");

        new Thread(this::listenForMasterRequests).start();
    }

}
