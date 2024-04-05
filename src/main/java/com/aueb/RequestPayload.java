package com.aueb;

import java.io.Serializable;

public class RequestPayload implements Serializable {
    public String function;
    public Object data;
    public Integer room_id;
    public Integer user_id;
    public int map_id;

    @Override
    public String toString() {
        return "User ID: " + user_id + "\nMap ID: " + map_id + "\nFunction: " + function + "\nRoom ID: " + room_id + "\nData: " + data;
    }
}
