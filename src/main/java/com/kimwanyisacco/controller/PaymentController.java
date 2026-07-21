package com.kimwanyisacco.controller;

import com.kimwanyisacco.dto.request.CardPaymentRequest;
import com.kimwanyisacco.dto.request.MobileMoneyPaymentRequest;
import com.kimwanyisacco.dto.response.ApiResponse;


import com.kimwanyisacco.dto.response.PaymentResponse;
import com.kimwanyisacco.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mobile-money")
    public ResponseEntity<ApiResponse<PaymentResponse>> payWithMobileMoney(
            @Valid @RequestBody MobileMoneyPaymentRequest request,
            @RequestParam Long payingMemberId) {
        return ResponseEntity.ok(ApiResponse.of("Mobile money payment processed",
                paymentService.payWithMobileMoney(request, payingMemberId)));
    }

    @PostMapping("/card")
    public ResponseEntity<ApiResponse<PaymentResponse>> payWithCard(
            @Valid @RequestBody CardPaymentRequest request,
            @RequestParam Long payingMemberId) {
        return ResponseEntity.ok(ApiResponse.of("Card payment processed",
                paymentService.payWithCard(request, payingMemberId)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> history(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.of("Payment history retrieved",
                paymentService.getPaymentHistory(memberId)));
    }
}
