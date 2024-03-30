package com.aueb;

import org.json.simple.JSONObject;

import javax.swing.text.DateFormatter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class User {
    private final Scanner in = new Scanner(System.in);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");

    public void selection() {
        System.out.println("Choose (1) for user, (2) for manager.");

        int input = in.nextInt();
        if (input == 1) {
            System.out.println("Selected user");
        } else manager();
    }

    public void manager() {
        System.out.print("""
                Type the number to choose function:
                (1) Add room
                (2) Add availability for room
                (3) Show your rooms
                """);
        int selection = in.nextInt();

        try {
            Socket clientSocket = new Socket("127.0.0.1", Master.PORT);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            if (selection == 1) addRoom(out);
            else if (selection == 2) addAvailability(out);
            else if (selection == 3) showRooms(out);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showRooms(DataOutputStream out) throws IOException {
        JSONObject json_obj = Utils.createJSONObject("show_rooms");
        out.writeUTF(json_obj.toJSONString());
    }

    private void addRoom(DataOutputStream out) throws IOException {
        in.nextLine();
        while (true) {
            System.out.println("Enter room name, area, number of people, price, and id (separated by \",\" in the same order)");
            String room_str = in.nextLine();
            Room room = new Room(room_str);
            JSONObject json_obj = Utils.createJSONObject("add_room");
            json_obj.put("room", room.getJSON());
            json_obj.put("id", room.id);
            out.writeUTF(json_obj.toJSONString());
        }
    }

    private void addAvailability(DataOutputStream out) throws IOException {
        in.nextLine();
        String input = "";
        System.out.println("Enter the ID of the room:");
        int id = in.nextInt();
        in.nextLine();
        System.out.println("Selected room with ID " + id);
        ArrayList<Integer> dates_epochs = new ArrayList<>();

        while (true) {
            System.out.println("Enter date of availability in the form of DD/MM/YYYY (e.g. 25/10/2024). Type stop to stop input");
            input = in.nextLine();
            if (input.equalsIgnoreCase("stop")) break;

            try {
                LocalDate date = LocalDate.parse(input, formatter);
                System.out.println("Added date: " + date);
                dates_epochs.add(((int)date.toEpochDay()));
            } catch (DateTimeParseException e) {
                System.out.println("Please re-enter the date: " + e.getLocalizedMessage());
            }
        }

        JSONObject json_obj = Utils.createJSONObject("add_availability");
        json_obj.put("dates", dates_epochs);
        json_obj.put("id", id);
        out.writeUTF(json_obj.toJSONString());
    }

    public static void main(String[] args) {
        User user = new User();
        user.selection();
    }

}