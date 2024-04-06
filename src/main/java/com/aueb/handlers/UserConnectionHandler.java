package com.aueb.handlers;

import com.aueb.Utils;
import com.aueb.packets.Packet;
import com.aueb.packets.UserRequest;
import com.aueb.Room;
import com.google.common.collect.Range;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class UserConnectionHandler extends Thread {
    private final ObjectInputStream in;
    private final int connection_id;
    private final int n;

    public UserConnectionHandler(ObjectInputStream in, int connection_id) {
        this.connection_id = connection_id;
        this.in = in;
        this.n = ServicesHandler.worker_ports.length;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
            UserRequest user_request = (UserRequest) in.readObject();
            Packet request = new Packet();
            request.connection_id = this.connection_id;
            request.function = user_request.requested_function;
            HashMap<Integer, Packet> packets_map = switch (request.function) {
                case "add_rooms" -> handleAddRooms(user_request, request);
                case "book_room" -> handleBookRoom(user_request, request);
                case "show_rooms" -> sendToAll(request);
                case "show_bookings" -> handleShowBookings(user_request, request);
                case "filter" -> handleRoomsFilter(user_request, request);
                default -> throw new RuntimeException("Can't find function: " + request.function);
            };

            sendToWorkers(request.connection_id, packets_map);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendToWorkers(int connection_id, HashMap<Integer, Packet> packets_map) throws IOException {
        for (Map.Entry<Integer, Packet> entry : packets_map.entrySet()) {
            new Thread(() -> {
                int worker_idx = entry.getKey();
                Packet request = entry.getValue();
                System.out.println("Sending " + request + " to worker with idx: " + worker_idx);
                try {
                    Socket socket = new Socket("127.0.0.1", ServicesHandler.worker_ports[entry.getKey()]);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(request);

                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        synchronized (ServicesHandler.num_of_workers_per_connection) {
            ServicesHandler.num_of_workers_per_connection.put(connection_id, packets_map.size());
        }
    }

    private HashMap<Integer, Packet> sendToAll(Packet request) {
        HashMap<Integer, Packet> packets_map = new HashMap<>();

        for (int i = 0; i < n; i++)
            packets_map.put(ServicesHandler.worker_ports[i] % n, request);

        return packets_map;
    }

    private HashMap<Integer, Packet> handleRoomsFilter(UserRequest user_request, Packet request) {
        JSONObject filter_json = user_request.data;
        request.data = new Room.RoomFilters(filter_json);

        return sendToAll(request);
    }

    private HashMap<Integer, Packet> handleShowBookings(UserRequest user_request, Packet packet) {
        Packet request = new Packet(packet);
        request.data = user_request.username;

        return sendToAll(request);
    }

    private HashMap<Integer, Packet> handleBookRoom(UserRequest user_request, Packet request) {
        Range<Integer> date_range = Utils.stringToRange(user_request.data.get("date_range").toString());
        int room_id = Integer.parseInt(user_request.data.get("room_id").toString());
        Packet worker_request = new Packet(request);
        worker_request.data = new Object[]{room_id, date_range, user_request.username};

        return sendToAll(worker_request);
    }

    private HashMap<Integer, Packet> handleAddRooms(UserRequest userRequest, Packet request) {
        JSONArray rooms_json = (JSONArray) userRequest.data.get("rooms");
        ArrayList<Room> rooms = new ArrayList<>();

        for (Object room_json : rooms_json) {
            Room room = new Room((JSONObject) room_json);
            rooms.add(room);
        }

        HashMap<Integer, Packet> packets_map = new HashMap<>();

        for (Room room : rooms) {
            int idx = room.hashCode() % n;
            if (!packets_map.containsKey(idx)) {
                Packet packet = new Packet(request);
                packet.data = new ArrayList<Room>();
                packets_map.put(idx, packet);
            }

            ((ArrayList<Room>) packets_map.get(idx).data).add(room);
        }

        return packets_map;
    }
}