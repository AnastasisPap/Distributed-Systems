package com.aueb.handlers;

import com.aueb.Master;
import com.aueb.Reducer;
import com.aueb.Worker;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class ServicesHandler {
    // Contains the worker ports, static because the Master, Worker and Reducer need to access it
    public static int[] workerPorts;
    // Key: Connection ID
    // Value: Number of workers
    // This Hash Map is needed for the reducer to know how many workers to wait to get a result from. Once
    // the number of workers becomes 0, the reducer stops receiving from workers, reduces the result,
    // and sends it to the master.
    public static HashMap<Integer, Integer> numOfWorkersPerConnection = new HashMap<>();
    public static HashSet<Integer> failedWorkers = new HashSet<>();
    // 0 backups = no extra nodes will be used
    public static Integer numOfBackups = 0;

    /*
    Number of args: 1
     - Path to the config file
     */
    public static void main(String[] args) {
        workerPorts = getWorkerPorts(args[0]);
        new Master().start(); // Start the Master server
        new Reducer().start(); // Start the Reducer server

        /*
        // Start each worker, listening on a specific port that is read from the config file
        for (int port : workerPorts) {
            Worker worker = new Worker(port);
            worker.start();
        } */
    }

    // Input: path to config file
    // Output: int array with port numbers for the workers
    public static int[] getWorkerPorts(String path) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject data =  (JSONObject) parser.parse(new FileReader(path));
            JSONArray portsJSON = (JSONArray) data.get("worker_ports");
            int[] ports = new int[portsJSON.size()];
            for (int i = 0; i < portsJSON.size(); i++) ports[i] = Integer.parseInt(portsJSON.get(i).toString());
            numOfBackups = Integer.parseInt(data.get("num_of_backups").toString());

            return ports;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
