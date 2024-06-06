package com.aueb.packets;

import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashSet;

public class Packet implements Serializable {
    public int connectionId;
    public String function;
    public Object data;
    public boolean returnOutput = false;
    public HashSet<Integer> failedWorkers = new HashSet<>();

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
        json.put("return_output", returnOutput);
        return json;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;
        Packet packet = (Packet) o;
        if (connectionId != packet.connectionId) return false;
        return packet.data.equals(this.data);
    }

    @Override
    public int hashCode() {
        return data.toString().hashCode();
    }
}
