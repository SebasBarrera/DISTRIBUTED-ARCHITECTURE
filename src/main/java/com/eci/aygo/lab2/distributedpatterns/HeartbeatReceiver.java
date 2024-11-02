package com.eci.aygo.lab2.distributedpatterns;

import static com.eci.aygo.lab2.distributedpatterns.SimpleChat.HEARTBEAT_TIMEOUT;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jgroups.Message;
import org.jgroups.Receiver;

public class HeartbeatReceiver implements Receiver {
    private final Map<String, Long> lastHeartbeatTimes = new ConcurrentHashMap<>();

    @Override
    public void receive(Message msg) {
        String sender = msg.getSrc().toString();
        lastHeartbeatTimes.put(sender, System.currentTimeMillis());
        checkNodes();
    }

    public void checkNodes() {
        long currentTime = System.currentTimeMillis();
        lastHeartbeatTimes.forEach((var node, var lastTime) -> {
            if (currentTime - lastTime > HEARTBEAT_TIMEOUT) {
                System.out.println("Node " + node + " is down.");
                lastHeartbeatTimes.remove(node);
            }
        });
    }
}
