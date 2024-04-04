package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Master {
    public static final int PORT = 9090;
    private final JSONObject config;

    private void start() {
        try {
            int num_of_workers = Integer.parseInt(config.get("num_of_workers").toString());
            int[] ports = new int[num_of_workers];
            for (int i = 0; i < ports.length; i++)
                ports[i] = Integer.parseInt(((JSONArray) config.get("worker_ports")).get(i).toString());

            ServerSocket server = new ServerSocket(PORT);
            System.out.println("[INFO] Master listening at " + PORT);

            while (true) {
                Socket connection = server.accept();

                Thread thread = new MasterTaskHandler(connection, ports);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Master() {
        // System.out.println("Give the path to the config file: ");
        // Scanner in = new Scanner(System.in);
        // String path = in.nextLine();
        String path = "/Users/anastasispap/IdeaProjects/Distributed-Systems/src/main/resources/config.json";
        this.config = Utils.getConfig(path);
    }

    public static void main(String[] args) {
        Master master = new Master();
        master.start();
    }
}
