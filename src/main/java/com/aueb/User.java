package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showRooms(DataOutputStream out) throws IOException {
        JSONObject json_obj = Utils.createJSONObject("show_rooms");
        out.writeUTF(json_obj.toJSONString());
    }

    private void addRoom(DataOutputStream out) throws IOException {
        in.nextLine();
        while (true) {
            System.out.println("Enter room name, area, number of people, price, and id (separated by \",\" in the same order). Type stop to exit");
            String room_str = in.nextLine();
            if (room_str.startsWith("stop")) break;
            Room room = new Room(room_str);
            JSONObject json_obj = Utils.createJSONObject("add_room");
            json_obj.put("room", room.getJSON());
            json_obj.put("id", room.id);
            out.writeUTF(json_obj.toJSONString());
        }
    }

    private void addAvailability(DataOutputStream out) throws Exception {
        in.nextLine();
        String input = "";
        System.out.println("Enter the ID of the room:");
        int id = in.nextInt();
        in.nextLine();
        System.out.println("Selected room with ID " + id);

        while (true) {
            System.out.println("Enter date range of availabilities in the form of DD/MM/YYYY (e.g. 25/10/2024 - 29/10/2024). Type stop to exit");
            input = in.nextLine();
            if (input.equalsIgnoreCase("stop")) break;

            try {
                int[] formatted_dates = handleDateRange(input);
                JSONObject json_obj = Utils.createJSONObject("add_availability");
                JSONParser parser = new JSONParser();
                JSONArray arr = (JSONArray) parser.parse("[" + formatted_dates[0] + "," + formatted_dates[1] + "]");
                json_obj.put("date_range", arr);
                json_obj.put("id", id);
                out.writeUTF(json_obj.toJSONString());
            } catch (Exception e) {
                System.out.println("Please re-enter the date: " + e.getLocalizedMessage());
            }
        }
    }

    private int[] handleDateRange(String input) throws Exception {
        String[] dates = input.split("-");
        if (dates.length != 2) throw new Exception("Wrong format");

        int date_start = (int) LocalDate.parse(dates[0].trim(), formatter).toEpochDay();
        int date_end = (int) LocalDate.parse(dates[1].trim(), formatter).toEpochDay();
        if (date_start >= date_end) throw new Exception("Wrong range");

        return new int[]{date_start, date_end};
    }

    public static void main(String[] args) {
        User user = new User();
        user.selection();
    }

}