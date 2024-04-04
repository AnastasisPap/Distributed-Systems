package com.aueb;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Scanner;

public class User {
    private final ObjectOutputStream cli_out;
    private final Scanner in = new Scanner(System.in);
    private final Socket master_socket;

    public User() {
        try {
            master_socket = new Socket("127.0.0.1", Master.PORT);
            cli_out = new ObjectOutputStream(master_socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addRoom() throws IOException {
        while (true) {
            System.out.println("Enter room name, area, number of people, price, and id (separated by \",\" in the same order). Type stop to exit");
            String room_str = in.nextLine();
            Room room = new Room(room_str);
            RequestPayload request = new RequestPayload();
            request.data = room;
            request.function = "add_room";
            request.id = room.id;

            cli_out.writeObject(request);
            cli_out.flush();
        }
    }

    private void showRooms() throws IOException {
        RequestPayload request = new RequestPayload();
        request.function = "show_rooms";
        cli_out.writeObject(request);
        cli_out.flush();
    }

    private void addAvailability() throws IOException {
        System.out.println("Enter room ID: ");
        int id = in.nextInt();
        in.nextLine();
        RangeSet<Integer> dates = TreeRangeSet.create();

        while (true) {
            System.out.println("Enter availability range e.g. 1/1/2024-1/1/2025. Type stop to submit: ");
            String input = in.nextLine();
            if (input.equals("stop")) break;
            Range<Integer> date_ranges = Utils.stringToRange(input);
            dates.add(date_ranges);
        }

        RequestPayload request = new RequestPayload();
        request.function = "add_availability";
        request.data = dates;
        request.id = id;
        cli_out.writeObject(request);
    }

    private void bookRoom() throws IOException {
        System.out.println("Enter room ID:");
        int id = in.nextInt();
        in.nextLine();
        System.out.println("Enter date range e.g. 1/1/2024-5/1/2024: ");
        Range<Integer> dates = Utils.stringToRange(in.nextLine());

        RequestPayload request = new RequestPayload();
        request.id = id;
        request.function = "book_room";
        request.data = dates;

        cli_out.writeObject(request);
        cli_out.flush();
    }

    private void filterRoom() throws IOException {
        Room.RoomFilters filters = new Room.RoomFilters();
        boolean finished = false;
        while (!finished) {
            System.out.println("Select filter: \n(1) Room name\n(2) Area\n(3) Price\n(4) Number of people\n(5) Rating\n(6) Date range\n(7) Request rooms");
            int selection = in.nextInt();
            in.nextLine();
            switch (selection) {
                case 1:
                    System.out.println("Give room name: ");
                    filters.room_name = in.nextLine();
                    break;
                case 2:
                    System.out.println("Give area:");
                    filters.area = in.nextLine();
                    break;
                case 3:
                    System.out.println("Give price range (e.g. 10-100):");
                    String[] price_range = in.nextLine().split("-");
                    Float price_low = Float.parseFloat(price_range[0].trim());
                    Float price_high = Float.parseFloat(price_range[1].trim());
                    filters.price = Range.closed(price_low, price_high);
                    break;
                case 4:
                    System.out.println("Give range for number of people (e.g. 2-5):");
                    String[] cap_range = in.nextLine().split("-");
                    Integer cap_low = Integer.parseInt(cap_range[0].trim());
                    Integer cap_high = Integer.parseInt(cap_range[1].trim());
                    filters.num_of_people = Range.closed(cap_low, cap_high);
                    break;
                case 5:
                    System.out.println("Give rating:");
                    filters.rating = in.nextFloat();
                    break;
                case 6:
                    System.out.println("Give date range (e.g. 1/1/2024-5/1/2024):");
                    filters.date_range = Utils.stringToRange(in.nextLine());
                    break;
                case 7:
                    finished = true;
                    break;
                default:
                    System.out.println("Invalid");
            }
        }

        RequestPayload request = new RequestPayload();
        request.function = "filter_rooms";
        request.data = filters;
        cli_out.writeObject(request);
    }

    private void showBookings() throws IOException {
        RequestPayload request = new RequestPayload();
        request.function = "show_bookings";

        cli_out.writeObject(request);
        cli_out.flush();
    }

    private void selection() throws IOException, ClassNotFoundException {
        System.out.println("Select the number to choose function: ");
        System.out.println("(1) Add rooms\n(2) Show rooms\n(3) Add availability to room\n(4) Book room\n(5) Filter room\n(6) Show bookings");
        int selection = in.nextInt();
        in.nextLine();
        switch (selection) {
            case 1: addRoom(); break;
            case 2: showRooms(); break;
            case 3: addAvailability(); break;
            case 4: bookRoom(); break;
            case 5: filterRoom(); break;
            case 6: showBookings(); break;
            default:
                System.out.println("Wrong selection");
        }
    }

    public static void main(String[] args) {
        User user = new User();
        try {
            user.selection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + e);
        }
    }
}
