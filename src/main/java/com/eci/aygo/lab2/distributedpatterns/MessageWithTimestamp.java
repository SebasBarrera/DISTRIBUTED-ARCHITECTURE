package com.eci.aygo.lab2.distributedpatterns;

import java.io.Serializable;
import java.time.Instant;

public class MessageWithTimestamp implements Serializable {
    private final String message;
    private final Instant timestamp;

    public MessageWithTimestamp(String message) {
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + message;
    }
}

