package com.kimwanyisacco.dto.response;

import com.kimwanyisacco.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {

    private Long id;
    private String fullName;
    private String nationalId;
    private String membershipNumber;
    private String phone;
    private LocalDate dateJoined;
    private AccountStatus status;
}