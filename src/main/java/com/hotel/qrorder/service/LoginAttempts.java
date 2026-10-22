package com.hotel.qrorder.service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** 5 galat password ke baad us username ko 10 minute ke liye lock kar deta hai. */
@Component
public class LoginAttempts {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 10 * 60_000L;

    private record State(int fails, long lockedUntil) {}

    private final Map<String, State> map = new ConcurrentHashMap<>();

    public boolean isLocked(String user) {
        State s = map.get(key(user));
        return s != null && s.lockedUntil() > System.currentTimeMillis();
    }

    public void fail(String user) {
        long now = System.currentTimeMillis();
        if (map.size() > 10_000) map.clear();   // random usernames se memory na bhare
        map.compute(key(user), (k, old) -> {
            boolean fresh = old == null || (old.lockedUntil() > 0 && old.lockedUntil() <= now);
            int fails = fresh ? 1 : old.fails() + 1;
            return new State(fails, fails >= MAX_FAILS ? now + LOCK_MS : 0);
        });
    }

    public void reset(String user) {
        map.remove(key(user));
    }

    private static String key(String u) {
        return u == null ? "" : u.toLowerCase(Locale.ROOT);
    }
}
