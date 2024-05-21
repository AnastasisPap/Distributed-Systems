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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/*
This is called when a DummyUser connects to the Master. It converts the UserRequest object to a Packet request. This
packet will be sent to one or more workers. This handler decides to which workers it will send requests.
 */
public class UserConnectionHandler extends Thread {
    private final ObjectInputStream in;
    private final int connectionId;
    private final int n; // Number of workers

    public UserConnectionHandler(ObjectInputStream in, int connection_id) {
        this.connectionId = connection_id;
        this.in = in;
        this.n = ServicesHandler.workerPorts.length;
    }

    @Override
    public void run() {
        try {
            // Read the user request that comes from the Dummy User
            JSONParser parser = new JSONParser();
            Object obj = in.readObject();
            UserRequest userRequest;
            if (obj instanceof UserRequest)
                userRequest = (UserRequest) obj;
            else
                userRequest = new UserRequest((JSONObject) parser.parse(obj.toString()));
            Packet request = new Packet();
            request.connectionId = this.connectionId;
            request.function = userRequest.requestedFunction;
            /*
                Packets Hash Map:
                Key: worker index
                Value: Packet object that contains all the needed data that will be sent to the Worker
            */
            // Route task based on the function
            HashMap<Integer, Packet> packetsMap = switch (request.function) {
                case "add_rooms" -> handleAddRooms(userRequest, request);
                case "book_room" -> handleBookRoom(userRequest, request);
                case "show_rooms" -> sendToAll(request);
                case "show_bookings" -> handleShowBookings(userRequest, request);
                case "filter" -> handleRoomsFilter(userRequest, request);
                default -> throw new RuntimeException("Can't find function: " + request.function);
            };

            // Send the data to the worker(s)
            sendToWorkers(request.connectionId, packetsMap);
        } catch (IOException | ClassNotFoundException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Input: connection id which is similar to Map ID, packets map that maps worker idx to packet
    // Sends the object to the workers
    private void sendToWorkers(int connectionId, HashMap<Integer, Packet> packetsMap) throws IOException {
        for (Map.Entry<Integer, Packet> entry : packetsMap.entrySet()) {
            // Start a new thread for each worker
            new Thread(() -> {
                Packet request = entry.getValue();
                try {
                    Socket socket = new Socket("127.0.0.1", ServicesHandler.workerPorts[entry.getKey()]);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(request);

                    out.close();
                    socket.close();
                } catch (IOException e) {
                    int num_of_workers = ServicesHandler.numOfWorkersPerConnection.get(connectionId);
                    ServicesHandler.numOfWorkersPerConnection.put(connectionId, num_of_workers - 1);
                    System.out.println("Port " + ServicesHandler.workerPorts[entry.getKey()] +
                            " isn't open or refused connection.");
                }
            }).start();
        }

        /*
        The array of keys of the map contains all the workers that will do work for this connection. Update the
        hash map so the reducer will know when to stop expecting responses from workers
         */
        synchronized (ServicesHandler.numOfWorkersPerConnection) {
            ServicesHandler.numOfWorkersPerConnection.put(connectionId, packetsMap.size());
        }
    }

    // Sends the same request to all workers, so creates a map that contains all worker indices and map
    // to the same request
    private HashMap<Integer, Packet> sendToAll(Packet request) {
        HashMap<Integer, Packet> packets_map = new HashMap<>();

        for (int i = 0; i < n; i++)
            packets_map.put(ServicesHandler.workerPorts[i] % n, request);

        return packets_map;
    }

    private HashMap<Integer, Packet> sendToMainAndBackups(Packet request, int roomId) {
        HashMap<Integer, Packet> packetsMap = new HashMap<>();

        // if numOfBackups > 0 (e.g. 1) then 2 iterations will happen:
        //  - The first iteration is the main node
        //  - The second iteration will be the backup node
        for (int i = 0; i <= ServicesHandler.numOfBackups; i++)
            packetsMap.put((roomId + i) % n, request);

        return packetsMap;
    }


    // Input: request from which we get the room filters as JSON
    // Output: hash map that contains all workers and the request contains the filtering
    private HashMap<Integer, Packet> handleRoomsFilter(UserRequest userRequest, Packet request) {
        JSONObject filter_json = userRequest.data;
        request.data = new Room.RoomFilters(filter_json);

        return sendToAll(request);
    }

    // Input: request from which we get the username
    // Output: hashmap that contains all workers and the request contains the username
    private HashMap<Integer, Packet> handleShowBookings(UserRequest userRequest, Packet packet) {
        Packet request = new Packet(packet);
        request.data = userRequest.username;

        return sendToAll(request);
    }

    // Input: request from which we get the date range, username, and room id
    // Output: hash map that will be sent to all workers and contains the room id, date range, and username
    // We send it to all workers since we don't know which one holds the room with that ID.
    // We could only send it to one if the hashing was done based on room ID (or the booking was done based on room name)
    private HashMap<Integer, Packet> handleBookRoom(UserRequest userRequest, Packet request) {
        Range<Long> dateRange = Utils.stringToRange(userRequest.data.get("date_range").toString());
        int roomId = Integer.parseInt(userRequest.data.get("room_id").toString());
        Packet workerRequest = new Packet(request);
        workerRequest.data = new Object[]{roomId, dateRange, userRequest.username};

        return sendToMainAndBackups(workerRequest, roomId);
    }

    // Input: request from which we get the room information as JSON
    // Output: map that contains the workers for which the rooms hash to (not all or only one necessarily) and an array
    // that contains the rooms
    private HashMap<Integer, Packet> handleAddRooms(UserRequest userRequest, Packet request) {
        JSONArray roomsJSON = (JSONArray) userRequest.data.get("rooms");
        ArrayList<Room> rooms = new ArrayList<>();

        for (Object room_json : roomsJSON) {
            Room room = new Room((JSONObject) room_json);
            rooms.add(room);
        }

        HashMap<Integer, Packet> packetsMap = new HashMap<>();

        for (Room room : rooms) {
            for (int i = 0; i <= ServicesHandler.numOfBackups; i++) {
                int idx = (room.id + i) % n;
                if (!packetsMap.containsKey(idx)) {
                    Packet packet = new Packet(request);
                    packet.data = new ArrayList<Room>();
                    packetsMap.put(idx, packet);
                }

                ((ArrayList<Room>) packetsMap.get(idx).data).add(room);
            }
        }

        return packetsMap;
    }
}