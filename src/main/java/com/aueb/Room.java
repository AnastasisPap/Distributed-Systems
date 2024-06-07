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
    public final String roomName;
    public final String area;
    public final int numOfPeople;
    public final float price;
    public final int id;
    public int ratingCount;
    public boolean isBackup;
    public float rating;
    public final String imageURL;
    public RangeSet<Long> availableDays = TreeRangeSet.create();
    public HashMap<String, RangeSet<Long>> bookings = new HashMap<>();
    public int portBackup;
    public String ownerUsername;

    // Construct a Room object directly from JSON
    public Room(JSONObject json_obj) {
        this.roomName = json_obj.get("room_name").toString();
        this.numOfPeople = Integer.parseInt(json_obj.get("num_of_people").toString());
        this.area = json_obj.get("area").toString();
        this.price = Float.parseFloat(json_obj.get("price").toString());
        if (json_obj.containsKey("num_of_reviews"))
            this.ratingCount = Integer.parseInt(json_obj.get("num_of_reviews").toString());
        else
            this.ratingCount = 0;
        if (json_obj.containsKey("review"))
            this.rating = Float.parseFloat(json_obj.get("review").toString());
        else
            this.rating = 0;

        this.id = Integer.parseInt(json_obj.get("id").toString());
        this.imageURL = json_obj.get("room_image").toString();
        if (json_obj.containsKey("is_backup"))
            this.isBackup = Boolean.parseBoolean(json_obj.get("is_backup").toString());
        if (json_obj.containsKey("is_backup"))
            this.portBackup = Integer.parseInt(json_obj.get("port_backup").toString());
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
        if (filter.numOfPeople != null && this.numOfPeople < filter.numOfPeople) return false;
        return filter.dateRange == null || this.availableDays.encloses(filter.dateRange);
    }

    // Adds date range in the available dates
    // Input: Range object
    public void addDateRange(Range<Long> date_range) {
        availableDays.add(date_range);
        mergeSets();
    }

    private void mergeSets() {
        // The .add() function "unions" the range and the current available date ranges,
        // For example: [1, 5].add([7, 9]) = {[1, 5], [7, 9]}, [1, 5].add([4, 9]) = {[1, 9]}
        // But if we have [1, 5].add([6, 9]) then we have {[1, 5], [6, 9]} instead of [1, 9]
        // Solution: we order ranges and check if one range ends 1 day before the start of the next range (e.g. one
        // ends in 5 the next starts at 6) then we want to merge them
        ArrayList<Range<Long>> rangesList = new ArrayList<>(availableDays.asDescendingSetOfRanges());
        for (int i = 1; i < rangesList.size(); i++) {
            Range<Long> prev = rangesList.get(i);
            Range<Long> curr = rangesList.get(i - 1);
            if (prev.upperEndpoint() == curr.lowerEndpoint() - 1) availableDays.add(Range.closed(
                    prev.upperEndpoint(), curr.lowerEndpoint()));
        }
    }

    public String getBookings(ArrayList<Range<Long>> dateRanges) {
        ArrayList<String> bookingsList = new ArrayList<>();
        for (String username : bookings.keySet()) {
            for (Range<Long> range : bookings.get(username).asRanges())
                for (Range<Long> overallDateRange : dateRanges) {
                    if (overallDateRange.encloses(range))
                        bookingsList.add(Utils.dateRangeToString(range));
                }
        }

        String res = "";
        for (int i = 0; i < bookingsList.size(); i++) {
            res += bookingsList.get(i);
            if (i < bookingsList.size() - 1) res += ", ";
        }
        return res;
    }

    public float addRating(float rating) {
        float currTotal = this.rating * ratingCount;
        ratingCount++;
        currTotal += rating;
        this.rating = currTotal / ratingCount;

        return this.rating;
    }

    // Input: JSON array with the first time = start date and second item = end date
    // Output: true if the room can be booked, false otherwise
    public boolean book(String username, Range<Long> date) {
        if (!availableDays.encloses(date)) return false;

        Range<Long> dateToRemove = Range.open(date.lowerEndpoint() - 1, date.upperEndpoint() + 1);
        this.availableDays.remove(dateToRemove);

        if (!bookings.containsKey(username)) bookings.put(username, TreeRangeSet.create());
        bookings.get(username).add(date);

        return true;
    }

    // Converts Room object to JSON
    public JSONObject getJSON() {
        JSONObject res = new JSONObject();
        res.put("room_name", this.roomName);
        res.put("area", this.area);
        res.put("num_of_people", this.numOfPeople);
        res.put("price", this.price);
        res.put("review", this.rating);
        res.put("num_of_reviews", this.ratingCount);
        res.put("id", this.id);
        res.put("room_image", this.imageURL);
        res.put("is_backup", this.isBackup);
        res.put("port_backup", this.portBackup);
        res.put("owner_username", this.ownerUsername);

        JSONArray dates = new JSONArray();
        for (Range<Long> range : availableDays.asRanges())
            dates.add(Utils.dateRangeToString(range));
        res.put("available_dates", dates);

        return res;
    }

    // hashCode() and equals() are used to find the worker idx and also used for the hashmap
    @Override
    public int hashCode() {
        return id;
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
        public Integer numOfPeople;
        public Float rating;
        public Range<Long> dateRange;

        public RoomFilters(JSONObject json) {
            if (json.containsKey("area")) area = json.get("area").toString();
            if (json.containsKey("price")) {
                JSONArray priceArr = (JSONArray) json.get("price");
                price = Range.closed(Float.parseFloat(priceArr.get(0).toString()), Float.parseFloat(priceArr.get(1).toString()));
            }
            if (json.containsKey("numberOfPeople")) numOfPeople = Integer.parseInt(json.get("numberOfPeople").toString());
            if (json.containsKey("rating")) rating = Float.parseFloat(json.get("rating").toString());
            if (json.containsKey("dates")) {
                String[] datesArr = json.get("dates").toString().split("-");
                dateRange = Range.closed(Long.parseLong(datesArr[0]), Long.parseLong(datesArr[1]));
            }
        }
    }
}