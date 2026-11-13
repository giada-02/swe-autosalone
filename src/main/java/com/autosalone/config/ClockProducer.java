package com.autosalone.config;

import java.time.Clock;
import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ClockProducer {

    @Produces
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Rome"));
    }
}
