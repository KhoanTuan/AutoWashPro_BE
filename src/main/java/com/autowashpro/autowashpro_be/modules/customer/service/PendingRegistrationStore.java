package com.autowashpro.autowashpro_be.modules.customer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingRegistrationStore {

    private final long ttlSeconds;
    private final Map<String, PendingRegistration> store = new ConcurrentHashMap<>();

    public PendingRegistrationStore(@Value("${app.otp.ttl-seconds:120}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public void save(String phoneNumber, String fullName, String passwordHash) {
        store.put(phoneNumber, new PendingRegistration(
                fullName,
                passwordHash,
                Instant.now().plusSeconds(ttlSeconds)
        ));
    }

    public Optional<PendingRegistration> find(String phoneNumber) {
        PendingRegistration entry = store.get(phoneNumber);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phoneNumber);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    public boolean hasPending(String phoneNumber) {
        return find(phoneNumber).isPresent();
    }

    public void remove(String phoneNumber) {
        store.remove(phoneNumber);
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public record PendingRegistration(String fullName, String passwordHash, Instant expiresAt) {
    }
}
