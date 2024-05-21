package com.aueb;

import com.google.common.collect.Range;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class Utils {
    // Converts String date range in the form of 10/10/2000-20/10/2000
    // to a Range Object of integers. The lower bound is the epoch of the start date and the upper the epoch of the
    // end date
    public static Range<Long> stringToRange(String dates) {
        String[] datesArr = dates.split("-");
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.FRANCE);
        Date startDate, endDate;
        try {
            startDate = dateFormat.parse(datesArr[0]);
            endDate = dateFormat.parse(datesArr[1]);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return Range.closed(startDate.getTime(), endDate.getTime());
    }

    public static String dateRangeToString(Range<Long> range) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.FRANCE);

        String startDate = dateFormat.format(range.lowerEndpoint());
        String endDate = dateFormat.format(range.upperEndpoint());
        return startDate + "-" + endDate;
    }
}