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
import java.util.Map;

public class Room implements Serializable {
    public final String room_name;
    public final String area;
    public final int num_of_people;
    public final float price;
    public final int id;
    public int rating_count;
    public float rating;
    public RangeSet<Integer> available_days = TreeRangeSet.create();

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
            JSONArray date_range = (JSONArray) date_obj;
            int start = Integer.parseInt(date_range.getFirst().toString());
            int end = Integer.parseInt(date_range.getLast().toString());
            addDateRange(Range.closed(start, end));
        }
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

    public void rateRoom(float rating) {
        this.rating = (this.rating * this.rating_count + rating) / (this.rating_count + 1);
        this.rating_count++;
    }

    public boolean satisfiesConditions(JSONObject filter) {
        if (filter.containsKey("area") && !filter.get("area").toString().equals(this.area)) return false;
        if (filter.containsKey("rating") && Float.parseFloat(filter.get("rating").toString()) < this.rating) return false;
        if (filter.containsKey("price")) {
            JSONObject price = (JSONObject) filter.get("price");

            if (this.price < Float.parseFloat(price.get("min_price").toString()) ||
                this.price > Float.parseFloat(price.get("max_price").toString())) return false;
        }
        if (filter.containsKey("capacity")) {
            JSONObject capacity = (JSONObject) filter.get("capacity");

            if (this.num_of_people < Integer.parseInt(capacity.get("min_cap").toString()) ||
                this.num_of_people > Integer.parseInt(capacity.get("max_cap").toString())) return false;
        }
        if (filter.containsKey("dates")) {
            JSONArray dates = (JSONArray) filter.get("dates");

            for (Object date_range_obj : dates) {
                JSONArray date_range = (JSONArray) date_range_obj;
                int start_date = Integer.parseInt(date_range.get(0).toString());
                int end_date = Integer.parseInt(date_range.get(1).toString());
                if (!available_days.encloses(Range.closed(start_date, end_date))) return false;
            }
        }

        return true;
    }

    public void addDateRange(Range<Integer> date_range) {
        available_days.add(date_range);

        ArrayList<Range<Integer>> ranges_list = new ArrayList<>(available_days.asDescendingSetOfRanges());
        for (int i = 1; i < ranges_list.size(); i++) {
            Range<Integer> prev = ranges_list.get(i);
            Range<Integer> curr = ranges_list.get(i-1);
            if (prev.upperEndpoint() == curr.lowerEndpoint()-1) available_days.add(Range.closed(
                    prev.upperEndpoint(), curr.lowerEndpoint()));
        }
    }

    public void addRange(JSONArray date_range) {
        int start = Integer.parseInt(date_range.getFirst().toString());
        int end = Integer.parseInt(date_range.getLast().toString());
        Range<Integer> date = Range.closed(start, end);
        addDateRange(date);
    }

    public String getAvailableDates() {
        String dates_str = "";
        for (Range<Integer> date_range : available_days.asRanges()) {
            LocalDate start_date = LocalDate.ofEpochDay(date_range.lowerEndpoint());
            LocalDate end_date = LocalDate.ofEpochDay(date_range.upperEndpoint());
            dates_str += start_date + " to " + end_date + ",";
        }

        return dates_str;
    }

    public boolean book(JSONArray date_range_json) {
        int start_date = Integer.parseInt(date_range_json.get(0).toString());
        int end_date = Integer.parseInt(date_range_json.get(1).toString());

        if (!available_days.encloses(Range.closed(start_date, end_date))) return false;

        this.available_days.remove(Range.closed(start_date-1, end_date+1));
        return true;
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
        String ranges_str = (new ArrayList<>(available_days.asDescendingSetOfRanges())).toString().replace("..", ",");
        JSONParser parser = new JSONParser();

        try {
            res.put("available_dates", (JSONArray) parser.parse(ranges_str));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return res;
    }

    @Override
    public String toString() {
        return this.getJSON().toString();
    }
}
