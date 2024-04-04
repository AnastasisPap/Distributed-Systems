package com.aueb;

import com.google.common.collect.Range;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Utils {
    public static <T> JSONObject createJSONObject(T value) {
        JSONObject json_obj = new JSONObject();
        json_obj.put("function", value);

        return json_obj;
    }

    public static JSONObject getConfig(String path) {
        JSONObject config = new JSONObject();
        try {
            JSONParser parser = new JSONParser();
            config = (JSONObject) parser.parse(new FileReader(path));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return config;
    }

    public static Range<Integer> stringToRange(String dates) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        String[] dates_arr = dates.split("-");
        LocalDate start_date = LocalDate.parse(dates_arr[0].trim(), formatter);
        LocalDate end_date = LocalDate.parse(dates_arr[1].trim(), formatter);
        return Range.closed((int) start_date.toEpochDay(), (int) end_date.toEpochDay());
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Give dates divided by \",\" to convert to int (e.g. 14/2/2002, 1/2/2024): ");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        LocalDate date;
        for (String date_str : in.nextLine().split(",")) {
            date_str = date_str.trim();
            date = LocalDate.parse(date_str, formatter);
            System.out.print(date.toEpochDay() + ", ");
        }
    }
}