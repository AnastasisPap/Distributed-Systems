package com.aueb;

import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Utils {
    public static <T> JSONObject createJSONObject(T value) {
        JSONObject json_obj = new JSONObject();
        json_obj.put("function", value);

        return json_obj;
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