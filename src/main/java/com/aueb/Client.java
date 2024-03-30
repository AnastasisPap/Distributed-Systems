package com.aueb;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket clientSocket;
    private DataOutputStream out;

    public Client() {
        try {
            clientSocket = new Socket("127.0.0.1", Master.PORT);
            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRoom(Room room) throws IOException{
        System.out.println("Sending room to master");
        out.writeUTF(room.getJSON().toString());
    }

    public void stopConnection() throws IOException {
        out.close();
        clientSocket.close();
    }

    public boolean isOpen() {
        return !clientSocket.isClosed();
    }
}
