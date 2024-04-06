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

    /* Args contains: username, function, json file path that contains data */
    public static void main(String[] args) {
        DummyUser user = new DummyUser();
        String path;
        if (args.length < 3) path = "";
        else path = args[2];
        user.sendUserPayload(args[0], args[1], path);
    }

    public void sendUserPayload(String username, String function, String json_path) {
        try {
            JSONObject data = new JSONObject();
            if (!json_path.isEmpty()) {
                JSONParser parser = new JSONParser();
                data = (JSONObject) parser.parse(new FileReader(json_path));
            }

            UserRequest user_payload = new UserRequest(username, function, data);

            Socket master_socket = new Socket("127.0.0.1", Master.MASTER_PORT_USER);
            ObjectOutputStream user_out = new ObjectOutputStream(master_socket.getOutputStream());
            user_out.writeObject(user_payload);
            user_out.flush();

            ObjectInputStream user_in = new ObjectInputStream(master_socket.getInputStream());
            Packet response = (Packet) user_in.readObject();
            System.out.println(response.output);

            master_socket.close();
            user_in.close();
            user_out.close();
        } catch (IOException | ParseException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
