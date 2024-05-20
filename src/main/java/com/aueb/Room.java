package com.aueb;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.abs;

public class Room implements Serializable {
    public final String room_name;
    public final String area;
    public final int num_of_people;
    public final float price;
    public final int id;
    public int rating_count;
    public float rating;
    public final String imageURL;
    public RangeSet<Long> available_days = TreeRangeSet.create();
    public HashMap<String, RangeSet<Long>> bookings = new HashMap<>();

    // Construct a Room object directly from JSON
    public Room(JSONObject json_obj) {
        this.room_name = json_obj.get("room_name").toString();
        this.num_of_people = Integer.parseInt(json_obj.get("num_of_people").toString());
        this.area = json_obj.get("area").toString();
        this.rating = Float.parseFloat(json_obj.get("review").toString());
        this.price = Float.parseFloat(json_obj.get("price").toString());
        this.rating_count = Integer.parseInt(json_obj.get("num_of_reviews").toString());
        this.id = Integer.parseInt(json_obj.get("id").toString());
        this.imageURL = json_obj.get("room_image").toString();
        JSONArray dates = (JSONArray) json_obj.get("available_dates");

        for (Object date_obj : dates)
            addDateRange(Utils.stringToRange(date_obj.toString()));
    }

    // Returns true if it satisfies all the filters provided in the JSON Object
    // Input: JSON Object that contains the filters. Each key is an attribute of the Room (e.g. room_name, area, price,...)
    public boolean satisfiesConditions(RoomFilters filter) {
        if (filter.area != null && !filter.area.equalsIgnoreCase(this.area)) return false;
        if (filter.rating != null && this.rating < filter.rating) return false;
        if (filter.price != null && !filter.price.contains(this.price)) return false;
        if (filter.num_of_people != null && this.num_of_people < filter.num_of_people) return false;
        return filter.date_range == null || this.available_days.encloses(filter.date_range);
    }

    // Adds date range in the available dates
    // Input: Range object
    public void addDateRange(Range<Long> date_range) {
        available_days.add(date_range);
        mergeSets();
    }

    private void mergeSets() {
        // The .add() function "unions" the range and the current available date ranges,
        // For example: [1, 5].add([7, 9]) = {[1, 5], [7, 9]}, [1, 5].add([4, 9]) = {[1, 9]}
        // But if we have [1, 5].add([6, 9]) then we have {[1, 5], [6, 9]} instead of [1, 9]
        // Solution: we order ranges and check if one range ends 1 day before the start of the next range (e.g. one
        // ends in 5 the next starts at 6) then we want to merge them
        ArrayList<Range<Long>> ranges_list = new ArrayList<>(available_days.asDescendingSetOfRanges());
        for (int i = 1; i < ranges_list.size(); i++) {
            Range<Long> prev = ranges_list.get(i);
            Range<Long> curr = ranges_list.get(i - 1);
            if (prev.upperEndpoint() == curr.lowerEndpoint() - 1) available_days.add(Range.closed(
                    prev.upperEndpoint(), curr.lowerEndpoint()));
        }
    }

    public String getBookings(String username) {
        if (!bookings.containsKey(username)) return "";
        return bookings.get(username).toString();
    }

    // Input: JSON array with the first time = start date and second item = end date
    // Output: true if the room can be booked, false otherwise
    public boolean book(String username, Range<Long> date) {
        if (!available_days.encloses(date)) return false;

        Range<Long> date_to_remove = Range.open(date.lowerEndpoint() - 1, date.upperEndpoint() + 1);
        this.available_days.remove(date_to_remove);

        if (!bookings.containsKey(username)) bookings.put(username, TreeRangeSet.create());
        bookings.get(username).add(date);
        System.out.println(bookings);

        return true;
    }

    // Converts Room object to JSON
    public JSONObject getJSON() {
        JSONObject res = new JSONObject();
        res.put("room_name", this.room_name);
        res.put("area", this.area);
        res.put("num_of_people", this.num_of_people);
        res.put("price", this.price);
        res.put("review", this.rating);
        res.put("num_of_reviews", this.rating_count);
        res.put("id", this.id);
        res.put("available_dates", available_days);
        res.put("room_image", this.imageURL);

        return res;
    }

    // hashCode() and equals() are used to find the worker idx and also used for the hashmap
    @Override
    public int hashCode() {
        return abs(room_name.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Room room) {
            return room.id == this.id;
        } else if (obj instanceof Integer) {
            int id = Integer.parseInt(obj.toString());
            return id == this.id;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getJSON().toString();
    }

    public static class RoomFilters implements Serializable {
        public String area;
        public Range<Float> price;
        public Integer num_of_people;
        public Float rating;
        public Range<Long> date_range;

        public RoomFilters(JSONObject json) {
            if (json.containsKey("area")) area = json.get("area").toString();
            if (json.containsKey("price")) {
                JSONArray price_arr = (JSONArray) json.get("price");
                price = Range.closed(Float.parseFloat(price_arr.get(0).toString()), Float.parseFloat(price_arr.get(1).toString()));
            }
            if (json.containsKey("numberOfPeople")) num_of_people = Integer.parseInt(json.get("numberOfPeople").toString());
            if (json.containsKey("rating")) rating = Float.parseFloat(json.get("rating").toString());
            if (json.containsKey("dates")) {
                String[] datesArr = json.get("dates").toString().split("-");
                date_range = Range.closed(Long.parseLong(datesArr[0]), Long.parseLong(datesArr[1]));
            }
        }
    }
}