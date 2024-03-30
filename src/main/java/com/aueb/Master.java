package com.aueb;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master {
    private JSONParser parser = new JSONParser();
    private ServerSocket server;
    private List<Socket> workers = new ArrayList<>();
    private final int MAX_WORKERS = 5;
    private ExecutorService executor = Executors.newFixedThreadPool(MAX_WORKERS);
    public static final int PORT = 9000;

    private void start() throws IOException, ClassNotFoundException {
        server = new ServerSocket(PORT);
        System.out.println("Server listening at " + PORT);

        while (true) {
            Socket socket = server.accept();

            executor.submit(() -> {
                try {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String input = in.readUTF();
                    if (input.startsWith("WORKER")) {
                        workers.add(socket);
                        System.out.println("Worker added to pool");
                    } else {
                        System.out.println("User connected");
                        dispatchTask(input);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    private void dispatchTask(String taskInput) {
        System.out.println("Task: " + taskInput);
        int workerIdx = 0;
        try {
            String room_name = ((JSONObject) ((JSONObject) parser.parse(taskInput)).get("room")).get("roomName").toString();
            workerIdx = room_name.hashCode() % workers.size();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Socket selectedWorker = workers.get(workerIdx);

        try {
            DataOutputStream out = new DataOutputStream(selectedWorker.getOutputStream());
            out.writeUTF(taskInput);
        } catch (IOException e) { e.printStackTrace(); }
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