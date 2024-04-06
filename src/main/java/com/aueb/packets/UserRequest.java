package com.aueb.packets;

import org.json.simple.JSONObject;

import java.io.Serializable;

public class UserRequest implements Serializable {
    public String username;
    public String requested_function;
    public JSONObject data;

    public UserRequest(JSONObject json_data) {
        this.username = json_data.get("username").toString();
        this.requested_function = json_data.get("function").toString();
        this.data = (JSONObject) json_data.get("data");
    }

    public UserRequest(String username, String function, JSONObject data) {
        this.username = username;
        this.requested_function = function;
        this.data = data;
    }

    @Override
    public String toString() {
        return "[UserPayload]:\nUsername: " + username + "\nRequested Function: " + requested_function + "\nData: " + data;
    }
}
