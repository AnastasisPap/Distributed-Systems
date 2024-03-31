package com.aueb;

import org.json.simple.JSONObject;

public class Utils {
    public static <T> JSONObject createJSONObject(T value) {
        JSONObject json_obj = new JSONObject();
        json_obj.put("function", value);

        return json_obj;
    }
}