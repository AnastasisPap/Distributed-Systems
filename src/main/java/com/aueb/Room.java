package com.aueb;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Serializable;
import java.time.LocalDate;
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
    public RangeSet<Integer> available_days = TreeRangeSet.create();
    public HashMap<String, RangeSet<Integer>> bookings = new HashMap<>();

    // Construct a Room object directly from JSON
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

        for (Object date_obj : dates)
            addDateRange(Utils.stringToRange(date_obj.toString()));
    }

    // Dates should be in the form of DD/MM/YYYY (e.g. 25/10/2022)
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

    // Returns true if it satisfies all the filters provided in the JSON Object
    // Input: JSON Object that contains the filters. Each key is an attribute of the Room (e.g. room_name, area, price,...)
    public boolean satisfiesConditions(RoomFilters filter) {
        if (filter.area != null && !filter.area.equals(this.area)) return false;
        if (filter.room_name != null && !filter.room_name.equals(this.room_name)) return false;
        if (filter.rating != null && this.rating < filter.rating) return false;
        if (filter.price != null && !filter.price.contains(this.price)) return false;
        if (filter.num_of_people != null && !filter.num_of_people.contains(this.num_of_people)) return false;
        if (filter.date_range != null && !this.available_days.encloses(filter.date_range)) return false;

        return true;
    }

    // Adds date range in the available dates
    // Input: Range object
    public void addDateRange(Range<Integer> date_range) {
        available_days.add(date_range);
        mergeSets();
    }

    private void mergeSets() {
        // The .add() function "unions" the range and the current available date ranges,
        // For example: [1, 5].add([7, 9]) = {[1, 5], [7, 9]}, [1, 5].add([4, 9]) = {[1, 9]}
        // But if we have [1, 5].add([6, 9]) then we have {[1, 5], [6, 9]} instead of [1, 9]
        // Solution: we order ranges and check if one range ends 1 day before the start of the next range (e.g. one
        // ends in 5 the next starts at 6) then we want to merge them
        ArrayList<Range<Integer>> ranges_list = new ArrayList<>(available_days.asDescendingSetOfRanges());
        for (int i = 1; i < ranges_list.size(); i++) {
            Range<Integer> prev = ranges_list.get(i);
            Range<Integer> curr = ranges_list.get(i-1);
            if (prev.upperEndpoint() == curr.lowerEndpoint()-1) available_days.add(Range.closed(
                    prev.upperEndpoint(), curr.lowerEndpoint()));
        }
    }

    public void addMultipleRanges(RangeSet<Integer> dates) {
        available_days.addAll(dates);
        mergeSets();
    }

    public String getAvailableDates() {
        return convertToDatesString(this.available_days);
    }

    public String getBookings(String username) {
        if (!bookings.containsKey(username)) return "";
        return bookings.get(username).toString();
    }

    private String convertToDatesString(RangeSet<Integer> dates) {
        String dates_str = "";
        for (Range<Integer> date_range : dates.asRanges()) {
            // Convert epochs (i.e. how many days since a specific point which is an int) to a Date
            LocalDate start_date = LocalDate.ofEpochDay(date_range.lowerEndpoint());
            LocalDate end_date = LocalDate.ofEpochDay(date_range.upperEndpoint());
            dates_str += start_date + " to " + end_date + ",";
        }

        return dates_str;
    }

    // Input: JSON array with the first time = start date and second item = end date
    // Output: true if the room can be booked, false otherwise
    public boolean book(String username, Range<Integer> date) {
        if (!available_days.encloses(date)) return false;

        Range<Integer> date_to_remove = Range.open(date.lowerEndpoint() - 1, date.upperEndpoint() + 1);
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

        return res;
    }

    @Override
    public String toString() {
        return this.getJSON().toString();
    }

    public static class RoomFilters implements Serializable {
        public String room_name;
        public String area;
        public Range<Float> price;
        public Range<Integer> num_of_people;
        public Float rating;
        public Range<Integer> date_range;
    }

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
}
