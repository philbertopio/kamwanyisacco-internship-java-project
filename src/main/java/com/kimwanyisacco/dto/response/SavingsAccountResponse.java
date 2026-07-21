package com.kimwanyisacco.dto.response;

import com.kimwanyisacco.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsAccountResponse {

    private Long id;
    private Long memberId;
    private BigDecimal balance;
    private BigDecimal minBalance;
    private AccountStatus status;
}