package com.aueb;

import org.json.simple.JSONObject;

import java.io.Serializable;

public class Room implements Serializable {
    private String room_name;
    private String area;
    private String[] availabilityRanges;
    private int num_of_people;
    private float price;
    private int total_rating;
    private int rating_count;

    public Room(String input)
    {
        String[] items = input.split(",");
        this.room_name = items[0];
        this.area = items[1];
        this.num_of_people = Integer.parseInt(items[2].trim());
        this.price = Float.parseFloat(items[3].trim());
        this.total_rating = 0;
        this.rating_count = 0;
    }

    public Room(String room_name, String area, int num_of_people, float price) {
        this.room_name = room_name;
        this.area = area;
        //this.availabilityRanges = availabilityRanges;
        this.num_of_people = num_of_people;
        this.price = price;
        this.total_rating = 0;
        this.rating_count = 0;
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

        return res;
    }

    @Override
    public String toString() {
        return this.getJSON().toString();
    }
}
