package com.aueb.packets;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Packet implements Serializable {
    public int connection_id;
    public String function;
    public Object data;
    public String output;
    public boolean successful = false;

    public Packet() {
    }

    public Packet(Packet p) {
        this.connection_id = p.connection_id;
        this.function = p.function;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("connection_id", connection_id);
        json.put("function", function);
        json.put("data", data);
        json.put("output", output);
        return json;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
