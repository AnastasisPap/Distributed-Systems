package com.aueb;

import java.io.*;
import java.net.Socket;

public class MasterTaskHandler extends Thread {
    private final ObjectInputStream cli_in;
    private final DataOutputStream out;
    private final Socket connection;
    private final int mapId;
    private final int[] ports;

    public MasterTaskHandler(Socket connection, int[] ports) {
        this.ports = ports;
        this.mapId = connection.getPort();
        this.connection = connection;

        try {
            cli_in = new ObjectInputStream(connection.getInputStream());
            out = new DataOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
            while (true) {
                Object payload = cli_in.readObject();
                if (payload instanceof RequestPayload) {
                    handleTask((RequestPayload) payload);
                } else if (payload instanceof ResponsePayload) {
                    handleResponse((ResponsePayload) payload);
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        } catch (IOException e) {
            System.out.println("Connection closed");
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted: " + e.getLocalizedMessage());
        } finally {
            try {
                cli_in.close();
            } catch (IOException e) {
                System.out.println("Lost connection: " + e);
            }
        }
    }

    private void handleResponse(ResponsePayload response) throws IOException {
        System.out.println("[REDUCER RES]: " + response.reducer_response);
    }

    private void requestAllWorkers(RequestPayload request) {
        for (int i = 0; i < ports.length; i++) {
            final int j = i;
            Thread thread = new Thread(() -> {
                try {
                    Socket socket = new Socket("127.0.0.1", ports[j]);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(request);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            thread.start();
        }
    }

    private void handleTask(RequestPayload request) throws IOException, ClassNotFoundException, InterruptedException {
        request.mapId = this.mapId;
        if (request.id == null) {
            requestAllWorkers(request);
        } else {
            int workerIdx = String.valueOf(request.id).hashCode() % ports.length;

            int worker_port = ports[workerIdx];
            Socket socket = new Socket("127.0.0.1", worker_port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(request);
        }
    }
}
