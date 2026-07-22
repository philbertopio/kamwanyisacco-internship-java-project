package com.kimwanyisacco.model.enums;

public enum PaymentStatus {
    INITIATED,   // Payment record created locally, not yet sent to PesaPal
    PENDING,     // Submitted to PesaPal, awaiting member to pay on hosted page
    SUCCESSFUL,  // PesaPal confirmed COMPLETED
    FAILED,      // Gateway reported failure or error
    CANCELLED    // Payment reversed or abandoned by member
}
