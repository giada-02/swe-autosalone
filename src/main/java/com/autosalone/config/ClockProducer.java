package com.autosalone.config;

import java.time.Clock;
import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ClockProducer {

    public static final String TIME_ZONE = "Europe/Rome";
    public static final ZoneId TIME_ZONE_ID = ZoneId.of(TIME_ZONE);

    @Produces
    public Clock clock() {
        return Clock.system(TIME_ZONE_ID);
    }
}
