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
    // Stores the sockets of all the workers
    private final List<Socket> workers = new ArrayList<>();
    private final int MAX_WORKERS = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_WORKERS);
    public static final int PORT = 9000;

    private void start() throws IOException, ClassNotFoundException {
        // Start the server socket
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[INFO] Server listening at " + PORT);

        while (true) {
            // Listen for requests until the program is stopped
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
                        // If input contains WORKER then that means the connected client is a worker
                        // add the socket to the worker sockets list
                        if (input.startsWith("WORKER")) {
                            workers.add(socket);
                            System.out.println("[INFO] Worker added");
                        } else dispatchTask(input, socket); // if it doesn't contain WORKER it means it's a request
                    }
                } catch (Exception e) {
                    System.out.println("[WARNING] Lost connection because of error: " + e);
                }
            });
        }
    }


    // Handles requests sent either by workers or the user/manager
    private void dispatchTask(String taskInput, Socket socket) throws IOException, InterruptedException {
        // Use different function since show_rooms and filter_rooms do not contain room ID to hash it to a specific worker
        // so they are tasks each worker must do.
        if (taskInput.contains("show_rooms")) handleRequestRooms(taskInput, socket);
        else if (taskInput.contains("filter_rooms")) handleRoomFilter(taskInput, socket);
        else handleUserTask(taskInput);
    }

    // Handles requests to filter rooms
    private void handleRoomFilter(String taskInput, Socket cliSocket) throws InterruptedException, IOException {
        // stores the returned IDs that match the filter criteria
        Set<Integer> filtered_room_ids = new HashSet<>();
        ArrayList<Thread> worker_threads = new ArrayList<>();

        for (Socket socket : workers) {
            // Start a thread for each worker
            Thread worker_thread = new Thread(() -> {
                try {
                    // Send the request to the worker
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(taskInput);

                    // Wait for the response of the worker
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String response = in.readUTF();
                    JSONParser parser = new JSONParser();
                    JSONObject response_json = (JSONObject) parser.parse(response);
                    // Read the response JSON and add the IDs to the set
                    for (Object id : (JSONArray) response_json.get("filtered_rooms"))
                        filtered_room_ids.add(Integer.parseInt(id.toString()));

                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
            });

            worker_threads.add(worker_thread);
            worker_thread.start();
        }

        // Wait for all threads to finish
        for (Thread worker_thread : worker_threads) worker_thread.join();
        // Send the results to the user/manager
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

    // Request all the rooms from all the workers
    private void handleRequestRooms(String taskInput, Socket cliSocket) throws InterruptedException, IOException {
        ArrayList<Thread> worker_threads = new ArrayList<>();

        final String[] output = {""};
        for (Socket workerSocket : workers) {
            Thread worker_thread = new Thread(() -> {
                try {
                    // send the request to the workers
                    DataOutputStream out = new DataOutputStream(workerSocket.getOutputStream());
                    out.writeUTF(taskInput);

                    // Wait for the response of the Worker
                    DataInputStream in = new DataInputStream(workerSocket.getInputStream());
                    String response = in.readUTF();
                    JSONParser new_parser = new JSONParser();
                    JSONObject response_json = (JSONObject) new_parser.parse(response);
                    // The response is an array of rooms formatted in JSON
                    for (Object room : (JSONArray) response_json.get("rooms")) {
                        // Convert JSON representation of the Room object to a string and add it to the output
                        String room_str = formatRoom((JSONObject) room);
                        output[0] += room_str;
                    }

                } catch (IOException | ParseException e) { e.printStackTrace(); }
            });

            worker_threads.add(worker_thread);
            worker_thread.start();
        }

        // Wait for all threads to finish
        for (Thread worker_thread : worker_threads) worker_thread.join();

        // Send the results to the user/manager
        DataOutputStream cli_out = new DataOutputStream(cliSocket.getOutputStream());
        cli_out.writeUTF(output[0]);
    }

    // These function handles task that only a specific worker will execute
    private void handleUserTask(String taskInput) throws IOException {
        int workerIdx = 0;
        try {
            // Get the ID of the requested room and hash it to get the worker index
            String id = ((JSONObject) parser.parse(taskInput)).get("id").toString();
            workerIdx = id.hashCode() % workers.size();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        // use the worker socket of the worker with the appropriate room
        Socket selectedWorker = workers.get(workerIdx);

        // send the results to the worker
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