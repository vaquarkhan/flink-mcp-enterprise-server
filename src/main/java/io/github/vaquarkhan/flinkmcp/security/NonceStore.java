package io.github.vaquarkhan.flinkmcp.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NonceStore {

    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();

    public boolean useOnce(String nonce, long expMillis) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < now) {
                it.remove();
            }
        }
        return seen.putIfAbsent(nonce, expMillis) == null;
    }
}
