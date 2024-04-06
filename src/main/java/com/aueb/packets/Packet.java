package com.aueb.packets;

import java.io.Serializable;

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

    @Override
    public String toString() {
        return "[Packet]={ connection_id=" + connection_id + ", function=" + function + ", data=" + data + ", output=" + output + "}";
    }
}
