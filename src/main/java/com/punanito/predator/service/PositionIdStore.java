package com.punanito.predator.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PositionIdStore {
    private static class Entry {
        final long value;
        final long expiresAt;

        Entry(long value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private final List<Entry> values = new CopyOnWriteArrayList<>();
    private final long ttlMillis = 60000;

    public void add(long value) {
        long now = System.currentTimeMillis();
        values.add(new Entry(value, now + ttlMillis));
    }

    public List<Long> getActiveValues() {
        long now = System.currentTimeMillis();
        values.removeIf(e -> e.expiresAt < now);

        List<Long> result = new ArrayList<>();
        for (Entry e : values) {
            result.add(e.value);
        }
        return result;
    }
}