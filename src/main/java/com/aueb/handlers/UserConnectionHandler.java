package com.aueb.handlers;

import com.aueb.Utils;
import com.aueb.packets.Packet;
import com.aueb.packets.UserRequest;
import com.aueb.Room;
import com.google.common.collect.Range;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/*
This is called when a DummyUser connects to the Master. It converts the UserRequest object to a Packet request. This
packet will be sent to one or more workers. This handler decides to which workers it will send requests.
 */
public class UserConnectionHandler extends Thread {
    private final ObjectInputStream in;
    private final int connection_id;
    private final int n; // Number of workers

    public UserConnectionHandler(ObjectInputStream in, int connection_id) {
        this.connection_id = connection_id;
        this.in = in;
        this.n = ServicesHandler.worker_ports.length;
    }

    @Override
    public void run() {
        try {
            // Read the user request that comes from the Dummy User
            JSONParser parser = new JSONParser();
            Object obj = in.readObject();
            UserRequest user_request;
            if (obj instanceof UserRequest)
                user_request = (UserRequest) obj;
            else
                user_request = new UserRequest((JSONObject) parser.parse(obj.toString()));
            Packet request = new Packet();
            request.connection_id = this.connection_id;
            request.function = user_request.requested_function;
            /*
                Packets Hash Map:
                Key: worker index
                Value: Packet object that contains all the needed data that will be sent to the Worker
            */
            // Route task based on the function
            HashMap<Integer, Packet> packets_map = switch (request.function) {
                case "add_rooms" -> handleAddRooms(user_request, request);
                case "book_room" -> handleBookRoom(user_request, request);
                case "show_rooms" -> sendToAll(request);
                case "show_bookings" -> handleShowBookings(user_request, request);
                case "filter" -> handleRoomsFilter(user_request, request);
                default -> throw new RuntimeException("Can't find function: " + request.function);
            };

            // Send the data to the worker(s)
            sendToWorkers(request.connection_id, packets_map);
        } catch (IOException | ClassNotFoundException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Input: connection id which is similar to Map ID, packets map that maps worker idx to packet
    // Sends the object to the workers
    private void sendToWorkers(int connection_id, HashMap<Integer, Packet> packets_map) throws IOException {
        for (Map.Entry<Integer, Packet> entry : packets_map.entrySet()) {
            // Start a new thread for each worker
            new Thread(() -> {
                Packet request = entry.getValue();
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

        /*
        The array of keys of the map contains all the workers that will do work for this connection. Update the
        hash map so the reducer will know when to stop expecting responses from workers
         */
        synchronized (ServicesHandler.num_of_workers_per_connection) {
            ServicesHandler.num_of_workers_per_connection.put(connection_id, packets_map.size());
        }
    }

    // Sends the same request to all workers, so creates a map that contains all worker indices and map
    // to the same request
    private HashMap<Integer, Packet> sendToAll(Packet request) {
        HashMap<Integer, Packet> packets_map = new HashMap<>();

        for (int i = 0; i < n; i++)
            packets_map.put(ServicesHandler.worker_ports[i] % n, request);

        return packets_map;
    }

    // Input: request from which we get the room filters as JSON
    // Output: hash map that contains all workers and the request contains the filtering
    private HashMap<Integer, Packet> handleRoomsFilter(UserRequest user_request, Packet request) {
        JSONObject filter_json = user_request.data;
        request.data = new Room.RoomFilters(filter_json);

        return sendToAll(request);
    }

    // Input: request from which we get the username
    // Output: hashmap that contains all workers and the request contains the username
    private HashMap<Integer, Packet> handleShowBookings(UserRequest user_request, Packet packet) {
        Packet request = new Packet(packet);
        request.data = user_request.username;

        return sendToAll(request);
    }

    // Input: request from which we get the date range, username, and room id
    // Output: hash map that will be sent to all workers and contains the room id, date range, and username
    // We send it to all workers since we don't know which one holds the room with that ID.
    // We could only send it to one if the hashing was done based on room ID (or the booking was done based on room name)
    private HashMap<Integer, Packet> handleBookRoom(UserRequest user_request, Packet request) {
        Range<Long> date_range = Utils.stringToRange(user_request.data.get("date_range").toString());
        int room_id = Integer.parseInt(user_request.data.get("room_id").toString());
        Packet worker_request = new Packet(request);
        worker_request.data = new Object[]{room_id, date_range, user_request.username};

        return sendToAll(worker_request);
    }

    // Input: request from which we get the room information as JSON
    // Output: map that contains the workers for which the rooms hash to (not all or only one necessarily) and an array
    // that contains the rooms
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