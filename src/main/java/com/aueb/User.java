package com.aueb;

import org.json.simple.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class User {
    public void selection(String ip, int port) {
        System.out.println("Choose (1) for user, (2) for manager.");
        Scanner in = new Scanner(System.in);
        Client client = new Client();

        int input = in.nextInt();
        if (input == 1) {
            System.out.println("Selected user");
        } else manager(in, client);
    }

    public void manager(Scanner in, Client client) {
        /*
        System.out.println("""
                Type the number to choose function:
                (1) Add room
                (2) Add availability for room
                (3) Show your rooms
                """);
        int selection = in.nextInt();
        if (selection == 1) {*/
        in.nextLine();
        while (true) {
            System.out.println("Enter room name, area, number of people, and price (separated by \",\" in the same order)");
            String room_str = in.nextLine();
            Room room = new Room(room_str);
            try {
                Socket clientSocket = new Socket("127.0.0.1", Master.PORT);
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                JSONObject json_obj = new JSONObject();
                json_obj.put("function", "add_room");
                json_obj.put("room", room.getJSON());
                out.writeUTF(json_obj.toJSONString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        User user = new User();
        user.selection("127.0.0.1", 1234);
    }

}