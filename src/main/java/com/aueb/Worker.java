package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {
    private Socket clientSocket;
    private DataInputStream in;
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
            if (json_obj.get("function").toString().startsWith("add_room")) {
                json_obj = (JSONObject) json_obj.get("room");
                try {
                    storeRoom(json_obj);
                    System.out.println("Storing room " + json_obj.get("roomName").toString() + "...");
                    Thread.sleep(20000);
                    System.out.println("Successfully stored room " + json_obj.get("roomName"));
                } catch (IOException | ParseException | InterruptedException e) { e.printStackTrace(); }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectToMaster() throws IOException {
        clientSocket = new Socket("127.0.0.1", Master.PORT);

        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.writeUTF("WORKER");

        new Thread(this::listenForMasterRequests).start();
    }

    private void storeRoom(JSONObject json_obj) throws IOException, ParseException, InterruptedException {
        File file = new File(System.getProperty("user.dir") + "/src/main/resources/rooms.json");
        JSONArray json_array;
        if (file.exists())
        {
            json_array = (JSONArray) parser.parse(new FileReader(file));
        } else {
            json_array = new JSONArray();
        }

        json_array.add(json_obj);
        try {
            FileWriter file_writer = new FileWriter(file);
            file_writer.write(json_array.toJSONString());
            file_writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
