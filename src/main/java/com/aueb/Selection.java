package com.aueb;

import java.io.IOException;
import java.util.Scanner;

public class Selection {
    Scanner in;
    Client client;

    public Selection(String ip, int port) {
        System.out.println("Choose (1) for user, (2) for manager.");
        in = new Scanner(System.in);
        client = new Client();

        int input = in.nextInt();
        if (input == 1) {
            user();
        } else manager();
    }

    private void user() {
        System.out.println("""
                Type the number to choose function:
                (1) Filter rooms
                (2) Book room
                (3) Rate room
                (4) exit
                """);
        int selection = in.nextInt();
        System.out.println(selection);
    }

    private void manager() {
        int selection = 0;
        while (selection != 4) {
            System.out.println("""
                    Type the number to choose function:
                    (1) Add room
                    (2) Add availability for room
                    (3) Show your rooms
                    (4) Exit
                    """);
            in.nextInt();
            selection = in.nextInt();
            if (selection == 1) {
                System.out.println("Enter room name, area, number of people, and price (separated by \",\" in the same order)");
                String room_str = in.nextLine();
                Room room = new Room(room_str);
                try {
                    client.sendRoom(room);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
