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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // For each new connection (either CLI user or Worker), open a new thread
            // Each thread corresponds to a CLI user socket or a Worker socket
            executor.submit(() -> {
                try {
                    String input = "";
                    // While loop is used to constantly listen for inputs
                    // Reasoning behind condition on while loop:
                    // If the input initializes worker -> input has WORKER -> only read input back from the Worker at specific times
                    // If the input is from the CLI -> constantly read the input
                    while (!input.startsWith("WORKER")) {
                        input = in.readUTF();
                        System.out.println("[INFO] Received: " + input);
                        if (input.startsWith("WORKER")) {
                            workers.add(socket);
                            System.out.println("[INFO] Worker added");
                        } else dispatchTask(input, socket);
                    }
                } catch (Exception e) {
                    System.out.println("[WARNING] Lost connection because of error: " + e);
                }
            });
        }
    }

    private void dispatchTask(String taskInput, Socket socket) throws IOException, InterruptedException {
        if (taskInput.contains("show_rooms")) handleRequestRooms(taskInput, socket);
        else if (taskInput.contains("filter_rooms")) handleRoomFilter(taskInput, socket);
        else handleUserTask(taskInput);
    }

    private void handleRoomFilter(String taskInput, Socket cliSocket) throws InterruptedException, IOException {
        Set<Integer> filtered_room_ids = new HashSet<>();
        ArrayList<Thread> worker_threads = new ArrayList<>();

        for (Socket socket : workers) {
            Thread worker_thread = new Thread(() -> {
                try {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(taskInput);

                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String response = in.readUTF();
                    JSONParser parser = new JSONParser();
                    JSONObject response_json = (JSONObject) parser.parse(response);
                    for (Object id : (JSONArray) response_json.get("filtered_rooms"))
                        filtered_room_ids.add(Integer.parseInt(id.toString()));

                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
            });

            worker_threads.add(worker_thread);
            worker_thread.start();
        }

        for (Thread worker_thread : worker_threads) worker_thread.join();
        DataOutputStream cli_out = new DataOutputStream(cliSocket.getOutputStream());
        cli_out.writeUTF("------------------------------\nFetched room IDs: " + filtered_room_ids.toString()
                .replace("[", "").replace("]", ""));
    }

    private String formatRoom(JSONObject json_room) {
        String output = "---------------------------------------\n";
        output += "Room name: " + json_room.get("room_name") + " with ID: " + json_room.get("id") + "\n";
        output += "Area: " + json_room.get("area") + "\n";
        output += "Capacity: " + json_room.get("num_of_people") + "\n";
        output += "Average review: " + json_room.get("review") + " (" + json_room.get("num_of_reviews") + " review(s))\n";
        output += "Price per day: " + json_room.get("price") + "\n";
        JSONArray dates = (JSONArray) json_room.get("available_dates");
        for (Object date_range : dates) {
            JSONArray date_interval = (JSONArray) date_range;
            LocalDate start_date = LocalDate.ofEpochDay(Integer.parseInt(date_interval.getFirst().toString()));
            LocalDate end_date = LocalDate.ofEpochDay(Integer.parseInt(date_interval.getLast().toString()));
            output += start_date + " until " + end_date + ", ";
        }
        output += "\n";
        return output;
    }

    private void handleRequestRooms(String taskInput, Socket cliSocket) throws InterruptedException, IOException {
        ArrayList<Thread> worker_threads = new ArrayList<>();

        final String[] output = {""};
        for (Socket workerSocket : workers) {
            Thread worker_thread = new Thread(() -> {
                try {
                    DataOutputStream out = new DataOutputStream(workerSocket.getOutputStream());
                    if (!workerSocket.isClosed()) out.writeUTF(taskInput);

                    DataInputStream in = new DataInputStream(workerSocket.getInputStream());
                    String response = in.readUTF();
                    JSONParser new_parser = new JSONParser();
                    JSONObject response_json = (JSONObject) new_parser.parse(response);
                    for (Object room : (JSONArray) response_json.get("rooms")) {
                        String room_str = formatRoom((JSONObject) room);
                        output[0] += room_str;
                    }

                } catch (IOException | ParseException e) { e.printStackTrace(); }
            });

            worker_threads.add(worker_thread);
            worker_thread.start();
        }

        for (Thread worker_thread : worker_threads)
            worker_thread.join();

        DataOutputStream cli_out = new DataOutputStream(cliSocket.getOutputStream());
        cli_out.writeUTF(output[0]);
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