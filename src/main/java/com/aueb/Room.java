package com.aueb;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Room implements Serializable {
    public final String room_name;
    public final String area;
    public final int num_of_people;
    public final float price;
    public final float rating;
    public final int rating_count;
    public final int id;
    public Set<Integer> available_days = new HashSet<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");

    public Room(JSONObject json_obj)
    {
        this.room_name = json_obj.get("room_name").toString();
        this.num_of_people = Integer.parseInt(json_obj.get("num_of_people").toString());
        this.area = json_obj.get("area").toString();
        this.rating = Float.parseFloat(json_obj.get("review").toString());
        this.price = Float.parseFloat(json_obj.get("price").toString());
        this.rating_count = Integer.parseInt(json_obj.get("num_of_reviews").toString());
        this.id = Integer.parseInt(json_obj.get("id").toString());
        JSONArray dates = (JSONArray) json_obj.get("available_dates");

        for (Object date_obj : dates) {
            JSONObject date = (JSONObject) date_obj;
            available_days.add(Integer.parseInt(date.toString()));
        }
    }

    public Room(String input)
    {
        String[] items = input.split(",");
        this.room_name = items[0];
        this.area = items[1];
        this.num_of_people = Integer.parseInt(items[2].trim());
        this.price = Float.parseFloat(items[3].trim());
        this.id = Integer.parseInt(items[4].trim());
        this.rating = 0.0f;
        this.rating_count = 0;
    }

    // Dates should be in the form of DD/MM/YYYY (e.g. 25/10/2022)
    public Room(String room_name, String area, int num_of_people, float price, int id, String[] dates) {
        this.room_name = room_name;
        this.area = area;
        this.available_days = convertToEpochs(dates);
        this.num_of_people = num_of_people;
        this.price = price;
        this.rating = 0;
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

    public JSONObject getJSON() {
        JSONObject res = new JSONObject();
        res.put("room_name", this.room_name);
        res.put("area", this.area);
        res.put("num_of_people", this.num_of_people);
        res.put("price", this.price);
        res.put("review", this.rating);
        res.put("num_of_reviews", this.rating_count);
        res.put("id", this.id);
        ArrayList<Integer> days = new ArrayList<>(available_days);
        res.put("available_dates", days);

        return res;
    }

    @Override
    public String toString() {
        return this.getJSON().toString();
    }
}
