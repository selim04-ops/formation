package com.esprit.formation.controller;

import com.esprit.formation.dto.TransactionResponse;
import com.esprit.formation.entities.PaymentMethode;
import com.esprit.formation.entities.Transaction;
import com.esprit.formation.entities.User;
import com.esprit.formation.repository.TransactionRepository;
import com.esprit.formation.services.FormationService;
import com.esprit.formation.services.PaymentService;
import com.esprit.formation.utils.ResponseWrapper;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for handling payment transactions")
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final FormationService formationService;

  /*  @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;*/

    @PostMapping()
    @Operation(summary = "Create transaction from cart",
            description = "Creates a transaction from user's cart items and sends confirmation email. " +
                    "Set forSessionEvent=true to process session event cart items")
    public ResponseEntity<?> createTransaction(@RequestParam(defaultValue = "false") boolean forSessionEvent) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = ((User) authentication.getPrincipal()).getId();

            Transaction transaction = paymentService.createTransaction(userId, forSessionEvent);
            return ResponseWrapper.success(transaction);
        } catch (EntityNotFoundException e) {
            LOGGER.error("Entity not found: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            LOGGER.error("Invalid request: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error creating transaction: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating transaction");
        }
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Confirm a payment",
            description = "Confirms a pending payment transaction")
    public ResponseEntity<?> confirmPayment(
            @Parameter(description = "ID of the transaction to confirm", required = true)
            @PathVariable Long id,

            @Parameter(description = "Payment method used (e.g., CASH, BANK_TRANSFER, CREDIT_CARD)",
                    required = true,
                    example = "BANK_TRANSFER")
            @RequestParam PaymentMethode paymentMethod,

            @Parameter(description = "Optional admin notes")
            @RequestParam(required = false) String adminNotes) {
        try {
            Transaction transaction = paymentService.confirmPayment(id, paymentMethod, adminNotes);

            return ResponseWrapper.success(transaction);
        } catch (EntityNotFoundException e) {
            LOGGER.error("Transaction not found: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (StripeException e) {
            LOGGER.error("Stripe error: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Payment processing error");
        } catch (Exception e) {
            LOGGER.error("Error confirming payment: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error confirming payment");
        }
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Issue a refund",
            description = "Issues a refund for a confirmed payment")
    public ResponseEntity<?> refundPayment(
            @Parameter(description = "ID of the transaction to refund", required = true)
            @PathVariable Long id,

            @Parameter(description = "Optional refund notes")
            @RequestParam(required = false) String adminNotes) {
        try {
            Transaction transaction = paymentService.refundPayment(id, adminNotes);
            return ResponseWrapper.success(transaction);
        } catch (EntityNotFoundException e) {
            LOGGER.error("Transaction not found: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error processing refund: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing refund");
        }
    }



    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @Operation(
            summary = "Get paginated transactions",
            description = "Retrieve all transactions with pagination support. Accessible by ADMIN and SUPER_ADMIN only.")
    public ResponseEntity<?> getAllTransactions(
            @Parameter(hidden = true) @RequestParam(defaultValue = "0") int page,
            @Parameter(hidden = true) @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @RequestParam(defaultValue = "createdAt,desc") String sort) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
            Page<TransactionResponse> transactions = paymentService.getAllTransactions(pageable);
            return ResponseWrapper.success(transactions);
        } catch (Exception e) {
            LOGGER.error("Error fetching transactions: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching transactions");
        }
    }

    /*@PostMapping("/stripe-webhook")
    @Operation(summary = "Handle Stripe webhook",
            description = "Endpoint for Stripe payment webhook notifications",
            hidden = true)
    public ResponseEntity<?> handleStripeWebhook(
            @Parameter(description = "Stripe event payload", required = true)
            @RequestBody String payload,

            @Parameter(description = "Stripe signature header for verification",
                    required = true,
                    in = ParameterIn.HEADER)
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getData().getObject();
                String transactionId = session.getMetadata().get("transaction_id");

                Transaction transaction = transactionRepository.findById(Long.parseLong(transactionId))
                        .orElseThrow(() -> new RuntimeException("Transaction not found"));
                transaction.setTransactionStatus(TransactionStatus.CONFIRMED);
                transactionRepository.save(transaction);

                paymentService.sendPaymentConfirmation(transaction.getUser(), transaction.getFormations());
            }
            return ResponseWrapper.success("Webhook processed successfully");
        } catch (Exception e) {
            LOGGER.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Webhook processing failed");
        }
    }*/
}