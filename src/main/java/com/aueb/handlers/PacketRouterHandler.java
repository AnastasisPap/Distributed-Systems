package com.aueb.handlers;

import com.aueb.packets.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class PacketRouterHandler extends Thread {
    private final Map.Entry<Integer, Packet> data;
    private final Integer numOfBackups = ServicesHandler.numOfBackups;
    private final Integer n = ServicesHandler.workerPorts.length;

    public PacketRouterHandler(Map.Entry<Integer, Packet> entry) {
        this.data = entry;
    }

    @Override
    public void run() {
        Packet request = data.getValue();
        int i = 0;
        while (i <= numOfBackups) {
            int idx = (data.getKey() + i) % n;
            try {
                sendRequest(request, idx);
                break;
            } catch (IOException e) {
                i++;
                request.failedWorkers.add(ServicesHandler.workerPorts[idx]);
            }
        }
        if (i > numOfBackups) {
            synchronized (ServicesHandler.numOfWorkersPerConnection) {
                int workersLeft = ServicesHandler.numOfWorkersPerConnection.get(request.connectionId);
                workersLeft--;
                ServicesHandler.numOfWorkersPerConnection.put(request.connectionId, workersLeft);
            }
        }
    }

    private void sendRequest(Packet request, int portIdx) throws IOException {
        Socket socket = new Socket("127.0.0.1", ServicesHandler.workerPorts[portIdx]);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(request);

        out.close();
        socket.close();
    }
}
