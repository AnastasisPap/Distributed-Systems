package com.aueb.handlers;

import java.io.ObjectInputStream;

public class ReducerConnectionHandler extends Thread {
    private ObjectInputStream in;
    private final int connection_id;

    public ReducerConnectionHandler(ObjectInputStream in, int connection_id) {
        this.connection_id = connection_id;
        this.in = in;
    }

    @Override
    public void run() {
        System.out.println("Sending to worker");
    }
}