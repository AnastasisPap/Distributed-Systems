package com.aueb;

import java.io.Serializable;

public class RequestPayload implements Serializable {
    public String function;
    public Object data;
    public Integer id;
    public int mapId;

    @Override
    public String toString() {
        return "MapID: " + mapId + "\nFunction: " + function + "\nID: " + id + "\nData: " + data;
    }
}
