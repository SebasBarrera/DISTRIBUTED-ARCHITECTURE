package com.eci.aygo.lab2.distributedpatterns;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

public class SimpleChat implements Receiver {

    public static final int HEARTBEAT_INTERVAL = 1000; // 1 segundo
    public static final int HEARTBEAT_TIMEOUT = 3000;

    private JChannel channel;
    private JChannel heartbeatChannel;
    private ScheduledExecutorService heartbeatScheduler;
    String userName = System.getProperty("user.name", "n/a");
    final HashMap<String, MessageWithTimestamp> state = new HashMap<>();

    private void start() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter your name: ");
        userName = reader.readLine().trim();
        if (userName.isEmpty()) {
            userName = "Anonymous";
        }
        channel = new JChannel().setReceiver(this);
        channel.connect("ChatCluster");
        channel.getState(null, 10000);
        heartbeatChannel = new JChannel().setReceiver(new HeartbeatReceiver());
        heartbeatChannel.connect("HeartbeatChannel");
        heartbeatScheduler = Executors.newScheduledThreadPool(1);
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        eventLoop();
        heartbeatScheduler.shutdown();
        channel.close();
        heartbeatChannel.close();
    }

    private void eventLoop() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("> ");
                System.out.flush();
                String line = in.readLine().toLowerCase();
                if (line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }
                line = "[" + userName + "] " + line;
                Message msg = new ObjectMessage(null, line);
                channel.send(msg);
            } catch (Exception e) {
            }
        }
    }

    private void sendHeartbeat() {
        try {
            Message heartbeat = new ObjectMessage(null, "heartbeat");
            heartbeatChannel.send(heartbeat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        HashMap<String, MessageWithTimestamp> map;
        map = (HashMap<String, MessageWithTimestamp>) Util.objectFromStream(new DataInputStream(input));
        synchronized (state) {
            state.clear();
            state.putAll(map);
        }
        System.out.println("Chat history with timestamps:");
        map.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
        new_view.getMembers().forEach(member -> {
            System.out.println("Member: " + member);
        });
    }

    @Override
    public void receive(Message msg) {
        String user = msg.getSrc().toString();
        String message = msg.getObject().toString();
        MessageWithTimestamp messageWithTimestamp = new MessageWithTimestamp(message);
        System.out.println(user + ": " + messageWithTimestamp);
        synchronized (state) {
            state.put(user, messageWithTimestamp);
        }
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org.jgroups.util.SuppressLog").setLevel(Level.SEVERE);
        Logger.getLogger("org.jgroups.protocols.pbcast.GMS").setLevel(Level.WARNING);
        Logger.getLogger("org.jgroups.protocols.TP").setLevel(Level.WARNING);
        new SimpleChat().start();
    }
}
