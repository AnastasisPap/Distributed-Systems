package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class User {
    private Socket clientSocket;
    private final Scanner in = new Scanner(System.in);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");

    public void selection() {
        System.out.println("Choose (1) for user, (2) for manager.");

        int input = in.nextInt();
        try {
            clientSocket = new Socket("127.0.0.1", Master.PORT);
            if (input == 1) user();
            else manager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void user() {
        System.out.print("""
                Type the number to choose function:
                (1) Book room
                (2) Filter room
                (3) Rate room
                """);
        int selection = in.nextInt();

        try {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            if (selection == 1) bookRoom(out);
            else if (selection == 2) filterRooms(out);
            else if (selection == 3) rateRoom(out);
            else System.out.println("Invalid choice");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void manager() {
        System.out.print("""
                Type the number to choose function:
                (1) Add room
                (2) Add availability for room
                (3) Show your rooms
                (4) Add rooms from config file
                """);
        int selection = in.nextInt();

        try {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            if (selection == 1) addRoom(out);
            else if (selection == 2) addAvailability(out);
            else if (selection == 3) showRooms(out);
            else if (selection == 4) initializeFromConfig(out);
            else System.out.println("Invalid choice");
        } catch (Exception e) { e.printStackTrace(); }
    }

    /*
    ---------------------------- FILTERING ----------------------------
     */

    // Show prompt for the user to select filters and show results once read from the Master
    private void filterRooms(DataOutputStream out) throws Exception {
        JSONObject json_obj = Utils.createJSONObject("filter_rooms");
        JSONObject filter = new JSONObject();
        while (true) {
            System.out.println("Select one of the following filters");
            System.out.print("""
                    (1) Area
                    (2) Dates
                    (3) Number of people
                    (4) Price
                    (5) Rating
                    (6) Exit program
                    """);
            int selection = in.nextInt();
            in.nextLine();
            if (selection == 1) filterArea(filter);
            else if (selection == 2) filterDates(filter);
            else if (selection == 3) filterCapacity(filter);
            else if (selection == 4) filterPrice(filter);
            else if (selection == 5) filterRating(filter);
            else if (selection == 6) break;
        }

        json_obj.put("filters", filter);
        String response = makeRequest(out, json_obj.toJSONString());
        System.out.println(response);
    }

    private void filterRating(JSONObject filter) {
        System.out.print("Enter the minimum rating: ");
        float min_rating = in.nextFloat();
        in.nextLine();
        filter.put("rating", min_rating);
        System.out.println();
    }

    private void filterPrice(JSONObject filter) {
        float max_price;
        float min_price;
        while (true) {
            System.out.print("Enter the minimum price: ");
            min_price = in.nextFloat();
            System.out.print("Enter the maximum price: ");
            max_price = in.nextFloat();

            if (min_price > max_price) System.out.println("Incorrect range");
            else break;
        }
        in.nextLine();
        JSONObject price = new JSONObject();
        price.put("min_price", min_price);
        price.put("max_price", max_price);
        filter.put("price", price);
        System.out.println();
    }

    private void filterCapacity(JSONObject filter) {
        int min_cap;
        int max_cap;
        while (true) {
            System.out.print("Enter the minimum capacity: ");
            min_cap = in.nextInt();
            System.out.print("Enter the maximum capacity: ");
            max_cap = in.nextInt();

            if (min_cap > max_cap) System.out.println("Incorrect range");
            else break;
        }
        in.nextLine();
        JSONObject capacity = new JSONObject();
        capacity.put("min_cap", min_cap);
        capacity.put("max_cap", max_cap);
        filter.put("capacity", capacity);
        System.out.println();
    }

    private void filterArea(JSONObject filter) {
        System.out.println("Please type the area:");
        String area = in.nextLine();
        filter.put("area", area.toLowerCase());
        System.out.println();
    }

    private void filterDates(JSONObject filter) throws Exception {
        JSONArray dates = new JSONArray();

        while (true) {
            System.out.println("Enter date range divided by - in the form of DD/MM/YYYY (e.g. 1/1/2024-5/1/2024) or type stop to stop input");
            String input = in.nextLine();
            if (input.startsWith("stop")) break;

            JSONArray date_ranges = handleDateRange(input);
            dates.add(date_ranges);
        }

        filter.put("dates", dates);
        System.out.println();
    }

    /*
    ---------------------------- RATING ----------------------------
     */

    // Ask the user to give the Room ID and a rating in 1-5 and send it to the Master
    private void rateRoom(DataOutputStream out) {
        System.out.println("Enter room ID");
        int id = in.nextInt();
        in.nextLine();
        System.out.println("Enter the rating in the range of [1-5]");
        float rating = in.nextFloat();
        in.nextLine();
        try {
            if (rating < 1 || rating > 5) throw new RuntimeException("Rating out of range");
            JSONObject json_obj = Utils.createJSONObject("rate_room");
            json_obj.put("id", id);
            json_obj.put("rating", rating);
            out.writeUTF(json_obj.toJSONString());
        } catch (Exception e) {
            System.out.println("Please re-enter the rating: " + e);
        }
    }

    /*
    ---------------------------- BOOK ----------------------------
     */

    // Ask the user to provide the Room ID and a date range to book a room and send the request to the Master
    private void bookRoom(DataOutputStream out) {
        System.out.println("Enter room ID");
        int id = in.nextInt();
        in.nextLine();
        System.out.println("Enter date range in the form of DD/MM/YYYY (e.g. 25/10/2024-29/10/2024)");
        String date_range = in.nextLine();
        try {
            JSONArray range = handleDateRange(date_range);
            JSONObject json_obj = Utils.createJSONObject("book_room");
            json_obj.put("id", id);
            json_obj.put("date_range", range);
            out.writeUTF(json_obj.toJSONString());
        } catch (Exception e) {
            System.out.println("Please re-enter the date: " + e);
        }
    }

    /*
    ---------------------------- SHOW ROOMS ----------------------------
     */

    private void showRooms(DataOutputStream out) throws IOException {
        JSONObject json_obj = Utils.createJSONObject("show_rooms");
        String response = makeRequest(out, json_obj.toJSONString());
        System.out.println(response);
    }

    /*
    ---------------------------- ADD ROOMS ----------------------------
     */

    // Ask the user to provide information about the room (doesn't include availability dates)
    private void addRoom(DataOutputStream out) throws IOException {
        in.nextLine();
        while (true) {
            System.out.println("Enter room name, area, number of people, price, and id (separated by \",\" in the same order). Type stop to exit");
            String room_str = in.nextLine();
            if (room_str.startsWith("stop")) break;
            Room room = new Room(room_str);
            sendRoom(out, room);
        }
    }

    // Instead of asking the user to provide through prompting, ask the user to provide information in a JSON file
    private void initializeFromConfig(DataOutputStream out) throws IOException, ParseException {
        in.nextLine();
        System.out.println("Give full path to json file: ");
        String path = in.nextLine();
        JSONParser parser = new JSONParser();

        JSONArray rooms = (JSONArray) parser.parse(new FileReader(path));
        for (Object room_obj : rooms) {
            JSONObject room_json = (JSONObject) room_obj;
            Room room = new Room(room_json);
            sendRoom(out, room);
        }
    }

    // Send the room information to the Master as a JSON file
    private void sendRoom(DataOutputStream out, Room room) throws IOException {
        JSONObject json_obj = Utils.createJSONObject("add_room");
        json_obj.put("room", room.getJSON());
        json_obj.put("id", room.id);
        out.writeUTF(json_obj.toJSONString());
    }

    /*
    ---------------------------- ADD DATES ----------------------------
     */

    // Ask the manager to provide date availabilities for a room
    // each time a date range is typed, it's instantly sent to the Master
    private void addAvailability(DataOutputStream out) {
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
                JSONObject json_obj = Utils.createJSONObject("add_availability");
                JSONArray arr = handleDateRange(input);
                json_obj.put("date_range", arr);
                json_obj.put("id", id);
                out.writeUTF(json_obj.toJSONString());
            } catch (Exception e) {
                System.out.println("Please re-enter the date: " + e.getLocalizedMessage());
            }
        }
    }

    // Check if the input is correct and format it as JSON
    private JSONArray handleDateRange(String input) throws Exception {
        String[] dates = input.split("-");
        if (dates.length != 2) throw new Exception("Wrong format");

        int date_start = (int) LocalDate.parse(dates[0].trim(), formatter).toEpochDay();
        int date_end = (int) LocalDate.parse(dates[1].trim(), formatter).toEpochDay();
        if (date_start >= date_end) throw new Exception("Wrong range");

        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse("[" + date_start + "," + date_end + "]");
    }

    // Sends the task to the Master and waits for the response
    private String makeRequest(DataOutputStream out, String request) throws IOException {
        out.writeUTF(request);

        DataInputStream master_in = new DataInputStream(clientSocket.getInputStream());
        return master_in.readUTF();
    }

    public static void main(String[] args) {
        User user = new User();
        user.selection();
    }

}