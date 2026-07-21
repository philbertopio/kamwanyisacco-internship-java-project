package com.kimwanyisacco.service;

import com.kimwanyisacco.dto.request.MemberRegistrationRequest;
import com.kimwanyisacco.dto.response.MemberResponse;

import java.util.List;

public interface MemberService {

    MemberResponse registerMember(MemberRegistrationRequest request);

    MemberResponse getMemberById(Long memberId);

    List<MemberResponse> getAllMembers();

    MemberResponse deactivateMember(Long memberId);

    MemberResponse reactivateMember(Long memberId);
}
