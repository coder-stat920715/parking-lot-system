package com.parking.payment.web;

import com.parking.payment.domain.PaymentStatus;
import com.parking.payment.dto.PaymentRequest;
import com.parking.payment.dto.PaymentResponse;
import com.parking.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 200 on success, 402 when the gateway declined (the body still describes the failed attempt). */
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.pay(request);
        HttpStatus status = response.status() == PaymentStatus.FAILED ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{paymentReference}")
    public PaymentResponse get(@PathVariable String paymentReference) {
        return paymentService.get(paymentReference);
    }
}
