package com.aueb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ResponsePayload implements Serializable {
    public Integer id;
    public String output_response;
    public ArrayList<Room> rooms_response = new ArrayList<>();
    public HashMap<Integer, List<Object>> reducer_response;
    public int mapId;

    @Override
    public String toString() {
        String res = "MapID: " + mapId + "\nResponse: " + output_response + "\n";
        if (id != null) res += "ID: " + id + "\n";
        if (!rooms_response.isEmpty()) res += rooms_response.toString();
        return res;
    }
}
