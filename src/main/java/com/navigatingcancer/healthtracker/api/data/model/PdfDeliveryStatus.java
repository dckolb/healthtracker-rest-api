package com.navigatingcancer.healthtracker.api.data.model;

public enum PdfDeliveryStatus {
    PENDING_GENERATION,
    GENERATED,
    PENDING_DELIVERY,
    DELIVERED,
    SENT_BY_OTHER_SERVICE,
    ERROR
}