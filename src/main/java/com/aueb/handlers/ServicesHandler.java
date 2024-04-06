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
    public static int[] worker_ports;
    public static HashMap<Integer, Integer> num_of_workers_per_connection = new HashMap<>();

    public static void main(String[] args) {
        worker_ports = getWorkerPorts(args[0]);
        new Master().start();
        new Reducer().start();

        for (int port : worker_ports) {
            Worker worker = new Worker(port);
            worker.start();
        }
    }

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
