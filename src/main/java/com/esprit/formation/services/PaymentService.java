package com.esprit.formation.services;


import com.esprit.formation.dto.TransactionFormation;
import com.esprit.formation.dto.TransactionResponse;
import com.esprit.formation.dto.TransactionSessionEvent;
import com.esprit.formation.dto.TransactionUser;
import com.esprit.formation.entities.*;
import com.esprit.formation.repository.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service

public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FormationRepository formationRepository;
    private final JavaMailSenderImpl mailSender;
    private final CartItemRepository cartItemRepository;
    private final SessionEventRepository sessionEventRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Autowired
    public PaymentService(TransactionRepository transactionRepository, UserRepository userRepository, FormationRepository formationRepository, JavaMailSenderImpl mailSender, CartItemRepository cartItemRepository, SessionEventRepository sessionEventRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.formationRepository = formationRepository;
        this.mailSender = mailSender;
        this.cartItemRepository = cartItemRepository;
        this.sessionEventRepository = sessionEventRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }



    @Transactional
    public Transaction createTransaction(Long userId, boolean forSessionEvent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<CartItem> cartItems = forSessionEvent
                ? cartItemRepository.findAllByUserIdAndSessionEventIdIsNotNull(userId)
                : cartItemRepository.findAllByUserIdAndSessionEventIdIsNull(userId);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        BigDecimal totalPrice = cartItems.stream()
                .map(item -> item.getDiscountedPrice() != null ?
                        item.getDiscountedPrice() : item.getOriginalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionFormation> formations = cartItems.stream()
                .map(item -> {
                    Formation formation = formationRepository.findById(item.getFormationId())
                            .orElseThrow(() -> new EntityNotFoundException("Formation not found"));
                    return TransactionFormation.builder()
                            .formationId(formation.getId())
                            .dateDebut(formation.getDateDebut())
                            .titre(formation.getTitre())
                            .oldPrice(formation.getPrix())
                            .newPrice(item.getDiscountedPrice())
                            .codeCoupon(item.getAppliedCouponId())
                            .build();
                })
                .toList();

        // Modified: Deduplicate session events while keeping all formations
        List<TransactionSessionEvent> sessionEvents = forSessionEvent ?
                cartItems.stream()
                        .collect(Collectors.toMap(
                                CartItem::getSessionEventId,
                                item -> {
                                    SessionEvent sessionEvent = sessionEventRepository.findById(item.getSessionEventId())
                                            .orElseThrow(() -> new EntityNotFoundException("Session or event not found"));
                                    return TransactionSessionEvent.builder()
                                            .sessionEventId(sessionEvent.getId())
                                            .dateDebut(sessionEvent.getDateDebut())
                                            .titre(sessionEvent.getTitre())
                                            .dateFin(sessionEvent.getDateFin())
                                            .lieu(sessionEvent.getLieu())
                                            .type(sessionEvent.getType())
                                            .build();
                                },
                                (existing, replacement) -> existing
                        ))
                        .values().stream()
                        .toList()
                : null;

        Transaction transaction = Transaction.builder()
                .user(TransactionUser.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nomEtPrenom(user.getNomEtPrenom())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .formations(formations)
                .sessionEvents(sessionEvents)
                .totalPrice(totalPrice)
                .currency("DT")
                .transactionStatus(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(forSessionEvent ?
                        getEarliestSessionEventDate(sessionEvents) :
                        getEarliestFormationDate(formations))
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (forSessionEvent) {
            cartItemRepository.deleteByUserIdAndSessionEventIdIsNotNull(userId);
        } else {
            cartItemRepository.deleteByUserId(userId);
        }

        sendPendingPaymentEmail(user, cartItems, formations, sessionEvents, totalPrice);

        return savedTransaction;
    }

    private LocalDate getEarliestFormationDate(List<TransactionFormation> formations) {
        return formations.stream()
                .map(f -> formationRepository.findById(f.getFormationId())
                        .orElseThrow().getDateDebut())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now().plusDays(7));
    }

    private LocalDate getEarliestSessionEventDate(List<TransactionSessionEvent> sessionEvents) {
        return sessionEvents.stream()
                .map(TransactionSessionEvent::getDateDebut)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now().plusDays(7));
    }

    private void sendPendingPaymentEmail(User user,
                                         List<CartItem> cartItems,
                                         List<TransactionFormation> formations,
                                         List<TransactionSessionEvent> sessionEvents,
                                         BigDecimal totalPrice) {
        String subject = "Your Participation is Pending";

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(user.getNomEtPrenom()).append(",\n\n");
        body.append("Your participation is pending for:\n\n");

        // Group formations by their session event
        Map<Long, List<TransactionFormation>> formationsByEvent = new HashMap<>();
        Map<Long, TransactionSessionEvent> eventMap = sessionEvents.stream()
                .collect(Collectors.toMap(TransactionSessionEvent::getSessionEventId, e -> e));

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            TransactionFormation formation = formations.get(i);
            TransactionSessionEvent event = eventMap.get(item.getSessionEventId());

            formationsByEvent.computeIfAbsent(item.getSessionEventId(), k -> new ArrayList<>()).add(formation);
        }

        // Display each session event with its formations
        for (Map.Entry<Long, List<TransactionFormation>> entry : formationsByEvent.entrySet()) {
            TransactionSessionEvent event = eventMap.get(entry.getKey());
            body.append("- ").append(event.getType()).append(": ").append(event.getTitre())
                    .append(" (").append(event.getDateDebut()).append(" to ").append(event.getDateFin()).append("):\n");

            for (TransactionFormation formation : entry.getValue()) {
                body.append("      Formation: ").append(formation.getTitre())
                        .append(" (").append(formation.getNewPrice() != null ? formation.getNewPrice() : formation.getOldPrice())
                        .append(" DT)");

                if (formation.getCodeCoupon() != null) {
                    body.append(" [Coupon: ").append(formation.getCodeCoupon()).append("]");
                }
                body.append("\n");
            }
            body.append("\n");
        }

        body.append("Total: ").append(totalPrice).append(" DT\n");
        body.append("Please complete payment via bank transfer or cash at our site.\n\n");
        body.append("Best regards,\nEsprit Entreprise Team");

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject(subject);
        email.setText(body.toString());
        email.setFrom("nhoucem44@zohomail.com");
        mailSender.send(email);
    }

    @Transactional
    public Transaction confirmPayment(Long transactionId, PaymentMethode paymentMethod, String adminNotes)
            throws StripeException {
        // 1. Fetch the transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 2. Update transaction status and details
        transaction.setTransactionStatus(TransactionStatus.CONFIRMED);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setConfirmedAt(LocalDateTime.now());
        transaction.setAdminNotes(adminNotes);

        // 3. Create SetupIntent for audit purposes (if Stripe is configured)
        if (stripeSecretKey != null && !transaction.getFormations().isEmpty()) {
            String description = "OFFLINE PAYMENT - " +
                    transaction.getFormations().stream()
                            .map(TransactionFormation::getTitre)
                            .collect(Collectors.joining(", "));

            SetupIntent intent = SetupIntent.create(
                    SetupIntentCreateParams.builder()
                            .setDescription(description.length() > 300 ?
                                    description.substring(0, 300) : description)
                            .putMetadata("transaction_id", transaction.getId().toString())
                            .putMetadata("payment_method", paymentMethod.name())
                            .putMetadata("user_id", transaction.getUser().getUserId().toString())
                            .putMetadata("amount", String.valueOf(transaction.getTotalPrice()))
                            .putMetadata("currency", transaction.getCurrency()) // Assuming you have currency field
                            .build());

            transaction.setStripePaymentId(intent.getId());
        }

        // 4. Save the transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 5. Send confirmation email
        sendConfirmationEmail(savedTransaction);

        // 6. Process confirmed transaction (add user to formations)
        if (savedTransaction.getTransactionStatus() == TransactionStatus.CONFIRMED) {
            processConfirmedTransaction(savedTransaction);
        }

        return savedTransaction;
    }

    private void processConfirmedTransaction(Transaction transaction) {
        // 1. Get user
        User user = userRepository.findById(transaction.getUser().getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + transaction.getUser().getUserId()));

        // 2. Get formation IDs
        List<Long> formationIds = transaction.getFormations().stream()
                .map(TransactionFormation::getFormationId)
                .collect(Collectors.toList());

        if (formationIds.isEmpty()) {
            throw new RuntimeException("No formations found in the transaction");
        }

        // 3. Get and validate formations
        List<Formation> formations = formationRepository.findAllById(formationIds);
        if (formations.isEmpty() || formations.size() != formationIds.size()) {
            throw new RuntimeException("Some formations not found with the provided IDs");
        }

        // 4. Add user to each formation
        formations.forEach(formation -> {
            formation.addParticipant(user);
            formationRepository.save(formation);
        });

        // 5. Save user
        userRepository.save(user);
    }

        private void sendConfirmationEmail(Transaction transaction) {
            TransactionUser user = transaction.getUser();
            String subject = "Payment Confirmed";

            StringBuilder body = new StringBuilder();
            body.append("Dear ").append(user.getNomEtPrenom()).append(",\n\n");
            body.append("Your payment for:\n\n");

            transaction.getFormations().forEach(f -> {
                body.append("- ").append(f.getTitre()).append("\n");
            });

            body.append("\nhas been confirmed. Thank you!\n\n");
            body.append("Best regards,\nEsprit Entreprise Team");

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(user.getEmail());
            email.setSubject(subject);
            email.setText(body.toString());
            email.setFrom("nhoucem44@zohomail.com");

            mailSender.send(email);
        }

    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAll(pageable);

        return transactions.map(transaction -> {
            TransactionResponse response = new TransactionResponse(transaction);

            // Fetch user details including avatar
            userRepository.findById(transaction.getUser().getUserId())
                    .ifPresent(user -> {
                        response.setUserAvatar(user.getImgUrl());
                        response.setUserEmail(user.getEmail());
                        response.setUserPhone(user.getPhoneNumber());
                    });

            return response;
        });
    }


    // SuperAdmin issues refund
    @Transactional
    public Transaction refundPayment(Long transactionId, String adminNotes) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setTransactionStatus(TransactionStatus.REFUNDED);
        transaction.setAdminNotes(adminNotes);
        return transactionRepository.save(transaction);
    }

    public PaymentIntent createPaymentIntent(Long transactionId) throws StripeException {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        // Validate transaction status
        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in PENDING state");
        }

        // Convert amount to cents safely
        long amountInCents;
        try {
            amountInCents = transaction.getTotalPrice()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount conversion error - value too large");
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(transaction.getCurrency().toLowerCase()) // Ensure lowercase
                .setDescription("Payment for formation: " + transaction.getFormations().stream()
                        .map(TransactionFormation::getTitre)
                        .collect(Collectors.joining(", ")))
                .putMetadata("transaction_id", transaction.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // Update transaction with payment intent ID
        transaction.setStripePaymentId(intent.getId());
        transactionRepository.save(transaction);

        return intent;
    }

    @Transactional
    public String createMockStripeSession(Long transactionId) throws StripeException {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        // Validate transaction status
        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in PENDING state");
        }

        List<Object> lineItems = transaction.getFormations().stream()
                .map(formation -> Map.of(
                        "price_data", Map.of(
                                "currency", transaction.getCurrency().toLowerCase(),
                                "product_data", Map.of(
                                        "name", formation.getTitre(),
                                        "description", formation.getTitre() // Add if available
                                ),
                                "unit_amount", 0 // €0 for mock
                        ),
                        "quantity", 1
                ))
                .collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types", List.of("card"));
        params.put("line_items", lineItems);
        params.put("mode", "payment");
        params.put("success_url", "https://your-app.com/payment/success?session_id={CHECKOUT_SESSION_ID}");
        params.put("cancel_url", "https://your-app.com/payment/cancel");
        params.put("metadata", Map.of(
                "transaction_id", transactionId.toString(),
                "is_mock_payment", "true"
        ));

        Session session = Session.create(params);

        // Update transaction with session ID
        transaction.setStripePaymentId(session.getId());
        transactionRepository.save(transaction);

        return session.getUrl();
    }


    public void sendPaymentConfirmation(TransactionUser user, List<TransactionFormation> formations) {
        // Calculate total price
        BigDecimal totalPrice = formations.stream()
                .map(f -> f.getNewPrice() != null ? f.getNewPrice() : f.getOldPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build email subject with count of formations
        String subject = "Payment Confirmation for " + formations.size() + " Formation(s)";

        // Build email body
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(user.getNomEtPrenom()).append(",\n\n");
        body.append("Your payment for the following formations has been confirmed:\n\n");

        // Add details for each formation
        for (TransactionFormation formation : formations) {
            body.append("- ").append(formation.getTitre())
                    .append(": ").append(formation.getNewPrice() != null ?
                            formation.getNewPrice() : formation.getOldPrice())
                    .append(" DT");

            if (formation.getCodeCoupon() != null) {
                body.append(" (with coupon: ").append(formation.getCodeCoupon()).append(")");
            }
            body.append("\n");
        }

        // Add total and closing
        body.append("\nTotal Amount: ").append(totalPrice).append(" DT\n\n");
        body.append("Thank you for your payment.\n\n");
        body.append("Best regards,\n");
        body.append("The Esprit Entreprise Team");

        // Construct and send email
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject(subject);
        email.setText(body.toString());
        email.setFrom("nhoucem44@zohomail.com");
        mailSender.send(email);
    }

    private SimpleMailMessage constructEmail(String subject, String body, TransactionUser user) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setFrom("nhoucem44@zohomail.com");
        email.setText(body);
        email.setTo(user.getEmail());
        return email;
    }
}

