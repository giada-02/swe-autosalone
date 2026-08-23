package com.autosalone.dtos.requests;

import java.util.Set;
import java.util.UUID;

public record CatalogItemIdsRequest(
        Set<UUID> catalogItemIds) {
}
