package com.eci.aygo.lab2.distributedpatterns;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

public class SimpleChat implements Receiver {

    JChannel channel;
    String user_name = System.getProperty("user.name", "n/a");
    final List<String> state = new LinkedList<>();

    private void start() throws Exception {
        //channel = new JChannel().setReceiver(this).connect("ChatCluster");
        channel = new JChannel().setReceiver(this);
        channel.connect("ChatCluster");
        channel.getState(null, 10000);
        eventLoop();
        channel.close();
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
                line = "[" + user_name + "] " + line;
                Message msg = new ObjectMessage(null, line);
                channel.send(msg);
            } catch (Exception e) {
            }
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
        List<String> list;
        list = (List<String>) Util.objectFromStream(new DataInputStream(input));
        synchronized (state) {
            state.clear();
            state.addAll(list);
        }
        System.out.println(list.size() + " messages in chat history):");
        list.forEach(System.out::println);
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    @Override
    public void receive(Message msg) {
        //System.out.println(msg.getSrc() + ": " + msg.getObject());
        String line = msg.getSrc() + ": " + msg.getObject();
        System.out.println(line);
        synchronized (state) {
            state.add(line);
        }
    }

    public static void main(String[] args) throws Exception {
        new SimpleChat().start();
    }
}
//crear un balanceador de cargas que cree mensajes y los distribuya en n nodos en el back, 
//el problema es saber a cuantos se tiene que distribuir
//creando dos grupos, uno para el back de registros donde cada que un nodo se sube
//