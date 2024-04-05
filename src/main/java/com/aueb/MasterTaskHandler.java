package com.aueb;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class MasterTaskHandler extends Thread {
    private final ObjectInputStream cli_in;
    private final DataOutputStream out;
    private final Socket connection;
    private final int[] ports;

    public MasterTaskHandler(Socket connection, int[] ports) {
        this.ports = ports;
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
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            ResponsePayload cli_res = new ResponsePayload();
            boolean is_response = false;
            while (!is_response) {
                Object payload = cli_in.readObject();
                if (payload instanceof RequestPayload) {
                    cli_res = handleTask((RequestPayload) payload);
                } else if (payload instanceof ResponsePayload) {
                    cli_res = handleResponse((ResponsePayload) payload);
                    is_response = true;
                }

                out.writeObject(cli_res);
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

    private ResponsePayload handleResponse(ResponsePayload response) throws IOException {
        return response;
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

    private ResponsePayload handleTask(RequestPayload request) throws IOException, ClassNotFoundException, InterruptedException {
        request.map_id = request.user_id;
        if (request.function.equals("add_rooms")) {
            ArrayList<Room> rooms = (ArrayList<Room>) request.data;
            for (Room room : rooms) {
                request.data = room;
                request.room_id = room.id;

                sendToWorker(request);
            }
        } else if (request.room_id == null) requestAllWorkers(request);
        else sendToWorker(request);

        ResponsePayload res = new ResponsePayload();
        res.output_response = "SUCCESSFULLY FINISHED";

        return res;
    }

    private void sendToWorker(RequestPayload request) throws IOException {
        int workerIdx = String.valueOf(request.room_id).hashCode() % ports.length;

        int worker_port = ports[workerIdx];
        Socket socket = new Socket("127.0.0.1", worker_port);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(request);
    }
}
