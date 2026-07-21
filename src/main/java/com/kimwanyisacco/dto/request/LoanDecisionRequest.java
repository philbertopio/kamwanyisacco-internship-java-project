package com.kimwanyisacco.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDecisionRequest {

    @NotNull(message = "Admin ID is required")
    private Long adminId;

    @NotNull(message = "Approval decision is required")
    private Boolean approve; // true = approve, false = reject
}
