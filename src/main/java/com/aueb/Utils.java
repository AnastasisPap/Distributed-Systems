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
    public static Range<Integer> stringToRange(String dates) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        String[] dates_arr = dates.split("-");
        LocalDate start_date = LocalDate.parse(dates_arr[0].trim(), formatter);
        LocalDate end_date = LocalDate.parse(dates_arr[1].trim(), formatter);
        return Range.closed((int) start_date.toEpochDay(), (int) end_date.toEpochDay());
    }
}