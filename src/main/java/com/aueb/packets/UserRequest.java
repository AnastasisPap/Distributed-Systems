package com.aueb.packets;

import org.json.simple.JSONObject;

import java.io.Serializable;

public class UserRequest implements Serializable {
    public String username;
    public String requestedFunction;
    public JSONObject data;

    public UserRequest(JSONObject json_data) {
        this.username = json_data.get("username").toString();
        this.requestedFunction = json_data.get("function").toString();
        this.data = (JSONObject) json_data.get("data");
    }

    public UserRequest(String username, String function, JSONObject data) {
        this.username = username;
        this.requestedFunction = function;
        this.data = data;
    }

    @Override
    public String toString() {
        return "[UserPayload]:\nUsername: " + username + "\nRequested Function: " + requestedFunction + "\nData: " + data;
    }
}
