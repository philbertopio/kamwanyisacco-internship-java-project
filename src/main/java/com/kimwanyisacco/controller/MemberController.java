package com.kimwanyisacco.controller;

import com.kimwanyisacco.dto.request.MemberRegistrationRequest;
import com.kimwanyisacco.dto.response.ApiResponse;
import com.kimwanyisacco.dto.response.MemberResponse;
import com.kimwanyisacco.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> register(@Valid @RequestBody MemberRegistrationRequest request) {
        MemberResponse response = memberService.registerMember(request);
        return new ResponseEntity<>(ApiResponse.of("Member registered successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponse>> getById(@PathVariable Long memberId) {
        MemberResponse response = memberService.getMemberById(memberId);
        return ResponseEntity.ok(ApiResponse.of("Member retrieved", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getAll() {
        List<MemberResponse> members = memberService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.of("Members retrieved", members));
    }

    @PatchMapping("/{memberId}/deactivate")
    public ResponseEntity<ApiResponse<MemberResponse>> deactivate(@PathVariable Long memberId) {
        MemberResponse response = memberService.deactivateMember(memberId);
        return ResponseEntity.ok(ApiResponse.of("Member deactivated", response));
    }

    @PatchMapping("/{memberId}/reactivate")
    public ResponseEntity<ApiResponse<MemberResponse>> reactivate(@PathVariable Long memberId) {
        MemberResponse response = memberService.reactivateMember(memberId);
        return ResponseEntity.ok(ApiResponse.of("Member reactivated", response));
    }
}
