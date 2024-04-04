package com.aueb;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Reducer {
    public static int REDUCER_PORT = 8080;
    private final ServerSocket server;
    private JSONObject config;

    public Reducer() {
        // System.out.println("Give the path to the config file: ");
        // Scanner in = new Scanner(System.in);
        // String path = in.nextLine();
        String path = "/Users/anastasispap/IdeaProjects/Distributed-Systems/src/main/resources/config.json";
        this.config = Utils.getConfig(path);

        try {
            server = new ServerSocket(REDUCER_PORT);
            System.out.println("[INFO] Reducer listening at " + REDUCER_PORT);
            start();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void start() throws IOException, InterruptedException {
        while (true) {
            HashMap<Integer, List<Object>> results_map = new HashMap<>();
            int total_responses = 0;
            List<Thread> threads = new ArrayList<>();
            int num_of_workers = Integer.parseInt(config.get("num_of_workers").toString());
            while (total_responses < num_of_workers) {
                Socket worker = server.accept();
                System.out.println("Accepted: " + worker.getPort());

                total_responses++;
                Thread thread = new Thread(() -> {
                    try {
                        ObjectInputStream in = new ObjectInputStream(worker.getInputStream());
                        ResponsePayload res = (ResponsePayload) in.readObject();
                        if (!results_map.containsKey(res.mapId)) {
                            results_map.put(res.mapId, new ArrayList<>());
                        }

                        if (!res.rooms_response.isEmpty()) {
                            synchronized (results_map) {
                                results_map.get(res.mapId).addAll(res.rooms_response);
                            }
                        } else if (res.output_response != null) {
                            synchronized (results_map) {
                                results_map.get(res.mapId).add(res.output_response);
                            }
                        }
                        in.close();
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
                threads.add(thread);
            }

            for (Thread thread : threads) thread.join();

            System.out.println(results_map);
            Socket master_connection = new Socket("127.0.0.1", Master.PORT);
            ResponsePayload res = new ResponsePayload();
            res.reducer_response = results_map;

            ObjectOutputStream out = new ObjectOutputStream(master_connection.getOutputStream());
            out.writeObject(res);
            out.close();
        }
    }

    public static void main(String[] args) {
        Reducer reducer = new Reducer();
    }
}
