package com.aueb;

import com.aueb.packets.Packet;
import com.aueb.packets.UserRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class DummyUser {

    /*
    Number of args: 2 (optionally 3)
     1. username
     2. function
     3. Path to json file (if required)
     */
    public static void main(String[] args) {
        DummyUser user = new DummyUser();
        String path;
        if (args.length < 3) path = "";
        else path = args[2];
        user.sendUserRequest(args[0], args[1], path);
    }

    public void sendUserRequest(String username, String function, String json_path) {
        try {
            // Read the JSON data if provided
            JSONObject data = new JSONObject();
            if (!json_path.isEmpty()) {
                JSONParser parser = new JSONParser();
                data = (JSONObject) parser.parse(new FileReader(json_path));
            }

            // Create User request object and send it to the master
            UserRequest user_payload = new UserRequest(username, function, data);

            Socket master_socket = new Socket("127.0.0.1", Master.MASTER_PORT_USER);
            ObjectOutputStream user_out = new ObjectOutputStream(master_socket.getOutputStream());
            user_out.writeObject(user_payload);
            user_out.flush();

            // Keep connection open until the master returns a result
            ObjectInputStream user_in = new ObjectInputStream(master_socket.getInputStream());
            JSONParser parser = new JSONParser();
            JSONObject response = (JSONObject) parser.parse(user_in.readObject().toString());
            System.out.println(response.get("data"));

            master_socket.close();
            user_in.close();
            user_out.close();
        } catch (IOException | ParseException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
