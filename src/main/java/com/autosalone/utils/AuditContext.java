package com.autosalone.utils;

import java.util.UUID;

public class AuditContext {
    public static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    public static void setCurrentUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getCurrentUserId() {
        UUID userId = currentUserId.get();
        return (userId != null) ? userId : SYSTEM_ID;
    }

    public static void clear() {
        currentUserId.remove();
    }
}
