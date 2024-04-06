package com.aueb.handlers;

import com.aueb.Master;
import com.aueb.Reducer;
import com.aueb.Worker;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ServicesHandler {
    // Contains the worker ports, static because the Master, Worker and Reducer need to access it
    public static int[] worker_ports;
    // Key: Connection ID
    // Value: Number of workers
    // This Hash Map is needed for the reducer to know how many workers to wait to get a result from. Once
    // the number of workers becomes 0, the reducer stops receiving from workers, reduces the result,
    // and sends it to the master.
    public static HashMap<Integer, Integer> num_of_workers_per_connection = new HashMap<>();

    /*
    Number of args: 1
     - Path to the config file
     */
    public static void main(String[] args) {
        worker_ports = getWorkerPorts(args[0]);
        new Master().start(); // Start the Master server
        new Reducer().start(); // Start the Reducer server

        // Start each worker, listening on a specific port that is read from the config file
        for (int port : worker_ports) {
            Worker worker = new Worker(port);
            worker.start();
        }
    }

    // Input: path to config file
    // Output: int array with port numbers for the workers
    private static int[] getWorkerPorts(String path) {
        try {
            JSONParser parser = new JSONParser();
            JSONArray ports_json = (JSONArray) parser.parse(new FileReader(path));
            int[] ports = new int[ports_json.size()];
            for (int i = 0; i < ports_json.size(); i++) ports[i] = Integer.parseInt(ports_json.get(i).toString());

            return ports;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
