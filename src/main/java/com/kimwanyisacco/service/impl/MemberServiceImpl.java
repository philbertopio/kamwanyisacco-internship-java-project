package com.kimwanyisacco.service.impl;

import com.kimwanyisacco.dto.request.MemberRegistrationRequest;
import com.kimwanyisacco.dto.response.MemberResponse;
import com.kimwanyisacco.exception.DuplicateResourceException;
import com.kimwanyisacco.exception.ResourceNotFoundException;
import com.kimwanyisacco.model.entity.Member;
import com.kimwanyisacco.model.entity.SavingsAccount;
import com.kimwanyisacco.model.entity.User;
import com.kimwanyisacco.model.enums.AccountStatus;
import com.kimwanyisacco.model.enums.UserRole;
import com.kimwanyisacco.repository.MemberRepository;
import com.kimwanyisacco.repository.SavingsAccountRepository;
import com.kimwanyisacco.repository.UserRepository;
import com.kimwanyisacco.service.MemberService;
import com.kimwanyisacco.utils.SaccoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemberServiceImpl implements MemberService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public MemberServiceImpl(UserRepository userRepository,
                             MemberRepository memberRepository,
                             SavingsAccountRepository savingsAccountRepository) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    @Override
    @Transactional
    public MemberResponse registerMember(MemberRegistrationRequest request) {
        // Guard uniqueness before hitting the DB constraint, so we can
        // return a friendly, specific error instead of a raw SQL exception.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (memberRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("A member with this National ID already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.MEMBER) // enforces admin/member separation at creation time
                .active(true)
                .build();
        user = userRepository.save(user);

        Member member = Member.builder()
                .user(user)
                .fullName(request.getFullName())
                .nationalId(request.getNationalId())
                .membershipNumber(generateMembershipNumber())
                .phone(request.getPhone())
                .address(request.getAddress())
                .dateJoined(LocalDate.now())
                .status(AccountStatus.ACTIVE)
                .build();
        member = memberRepository.save(member);

        // Every member automatically gets exactly one savings account.
        SavingsAccount account = SavingsAccount.builder()
                .member(member)
                .balance(java.math.BigDecimal.ZERO)
                .minBalance(SaccoConstants.MIN_SAVINGS_BALANCE)
                .dateOpened(LocalDate.now())
                .status(AccountStatus.ACTIVE)
                .build();
        savingsAccountRepository.save(account);

        return toResponse(member);
    }

    @Override
    public MemberResponse getMemberById(Long memberId) {
        return toResponse(findMemberOrThrow(memberId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberResponse> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse deactivateMember(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        member.setStatus(AccountStatus.DEACTIVATED); // never deleted, per business rules
        return toResponse(memberRepository.save(member));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse reactivateMember(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        member.setStatus(AccountStatus.ACTIVE);
        return toResponse(memberRepository.save(member));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
    }

    private String generateMembershipNumber() {
        return "KIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private MemberResponse toResponse(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .nationalId(member.getNationalId())
                .membershipNumber(member.getMembershipNumber())
                .phone(member.getPhone())
                .dateJoined(member.getDateJoined())
                .status(member.getStatus())
                .build();
    }
}
