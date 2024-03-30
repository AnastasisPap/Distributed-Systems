package com.aueb;

import org.json.simple.JSONObject;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class Room implements Serializable {
    private final String room_name;
    private final String area;
    private final int num_of_people;
    private final float price;
    private final int total_rating;
    private final int rating_count;
    private final int id;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
    private Set<Integer> available_days = new HashSet<>();

    public Room(String input)
    {
        String[] items = input.split(",");
        this.room_name = items[0];
        this.area = items[1];
        this.num_of_people = Integer.parseInt(items[2].trim());
        this.price = Float.parseFloat(items[3].trim());
        this.id = Integer.parseInt(items[4].trim());
        this.total_rating = 0;
        this.rating_count = 0;
    }

    // Dates should be in the form of DD/MM/YYYY (e.g. 25/10/2022)
    public Room(String room_name, String area, int num_of_people, float price, int id, String[] dates) {
        this.room_name = room_name;
        this.area = area;
        this.available_days = convertToEpochs(dates);
        this.num_of_people = num_of_people;
        this.price = price;
        this.total_rating = 0;
        this.rating_count = 0;
        this.id = id;
    }

    private Set<Integer> convertToEpochs(String[] dates) {
        Set<Integer> dates_epochs = new HashSet<>();

        for (String date : dates) {
            int epoch = (int) LocalDate.parse(date, formatter).toEpochDay();
            dates_epochs.add(epoch);
        }

        return dates_epochs;
    }

    public boolean isAvailable(LocalDate date) {
        int epoch = (int) date.toEpochDay();

        return this.available_days.contains(epoch);
    }

    public void book(LocalDate date) {
        if (!isAvailable(date)) return;

        this.available_days.remove(((int) date.toEpochDay()));
    }

    public float getAverageRating() { return rating_count == 0 ? 0 : (float) total_rating / rating_count; }

    public JSONObject getJSON() {
        JSONObject res = new JSONObject();
        res.put("roomName", this.room_name);
        res.put("area", this.area);
        res.put("numOfPeople", this.num_of_people);
        res.put("price", this.price);
        res.put("totalRating", this.total_rating);
        res.put("numOfRatings", this.rating_count);
        res.put("id", this.id);

        return res;
    }

    @Override
    public String toString() {
        return this.getJSON().toString();
    }
}
