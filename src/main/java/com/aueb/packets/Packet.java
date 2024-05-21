package com.aueb.packets;

import org.json.simple.JSONObject;

import java.io.Serializable;

public class Packet implements Serializable {
    public int connectionId;
    public String function;
    public Object data;
    public String output;
    public boolean successful = false;

    public Packet() {
    }

    public Packet(Packet p) {
        this.connectionId = p.connectionId;
        this.function = p.function;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("connection_id", connectionId);
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
