package com.kimwanyisacco.dto.response;

import com.kimwanyisacco.model.enums.MemberTransactionApprovalStatus;
import com.kimwanyisacco.model.enums.MemberTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTransactionApprovalResponse {
    private Long id;
    private Long memberId;
    private String membershipNumber;
    private Long savingsAccountId;
    private Long loanId;
    private MemberTransactionType transactionType;
    private BigDecimal amount;
    private MemberTransactionApprovalStatus status;
    private LocalDateTime requestedAt;
}
