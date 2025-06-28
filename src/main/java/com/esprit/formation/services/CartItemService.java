package com.esprit.formation.services;

import com.esprit.formation.dto.CartItemRequest;
import com.esprit.formation.dto.CartItemResponse;
import com.esprit.formation.entities.*;
import com.esprit.formation.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final FormationRepository formationRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final SessionEventRepository sessionEventRepository;

    @Transactional
    public CartItemResponse addToCart(CartItemRequest request, Long userId) {
        Formation formation = null;
        SessionEvent sessionEvent = null;

        // Fetch Formation (if provided)
        if (request.getFormationId() != null) {
            formation = formationRepository.findById(request.getFormationId())
                    .orElseThrow(() -> new EntityNotFoundException("Formation not found"));
        }

        // Validate Coupon (if provided)
        if (request.getCouponId() != null) {
            couponRepository.findByCode(request.getCouponId())
                    .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));
        }

        // Fetch SessionEvent (if provided)
        if (request.getSessionEventId() != null) {
            sessionEvent = sessionEventRepository.findById(request.getSessionEventId())
                    .orElseThrow(() -> new EntityNotFoundException("SessionEvent not found"));
        }

        // Set date (from Formation or SessionEvent)
        if (formation != null && formation.getDateDebut() != null) {
            request.setDateDebut(formation.getDateDebut());
        } else if (sessionEvent != null) {
            request.setDateDebut(sessionEvent.getDateDebut());
        } else {
            throw new IllegalArgumentException("Neither Formation nor SessionEvent has a valid date");
        }

        // Save CartItem
        CartItem cartItem = mapToCartItem(request, userId);
        CartItem savedItem = cartItemRepository.save(cartItem);

        // Build Response DTO directly (no separate mapping method needed)
        return CartItemResponse.builder()
                .id(savedItem.getId())
                .formationId(savedItem.getFormationId())
                .formationName(formation != null ? formation.getTitre() : null)
                .sessionEventId(savedItem.getSessionEventId())
                .sessionEventTitre(sessionEvent != null ? sessionEvent.getTitre() : null)
                .originalPrice(savedItem.getOriginalPrice())
                .finalPrice(savedItem.getDiscountedPrice())
                .build();
    }

    // Remove single item from cart
    public void removeFromCart(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(() -> new EntityNotFoundException("CartItem not found"));
        if (cartItem.getAppliedCouponId() != null) {
            User user = userRepository.findById(cartItem.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            Coupon coupon = couponRepository.findByCode(cartItem.getAppliedCouponId())
                    .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));

            // Remove the bidirectional relationship
            coupon.removeEligibleUser(user);
            couponRepository.save(coupon);
            userRepository.save(user);
        }

        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void clearCart(Long userId, boolean forSessionEvent) {
        List<CartItem> cartItems = forSessionEvent
                ? cartItemRepository.findAllByUserIdAndSessionEventIdIsNotNull(userId)
                : cartItemRepository.findAllByUserIdAndSessionEventIdIsNull(userId);

        if (cartItems != null) {
            // First handle coupons for all cart items
            for (CartItem cartItem : cartItems) {
                if (cartItem.getAppliedCouponId() != null) {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found"));
                    Coupon coupon = couponRepository.findByCode(cartItem.getAppliedCouponId())
                            .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));

                    // Remove the bidirectional relationship
                    coupon.removeEligibleUser(user);
                    couponRepository.save(coupon);
                    userRepository.save(user);
                }
            }

            // Then delete all cart items
            cartItemRepository.deleteAll(cartItems);
        }
    }


    // Get all cart items for user
   /* public List<CartItemResponse> getAllCartUser(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findCartItemsByUserId(userId);
        return mapToCartItemResponse(cartItems);
    }*/


    @Transactional(readOnly = true)
    public Page<CartItemResponse> getUserCart(Long userId, Pageable pageable, boolean forSessionEvent) {
        return cartItemRepository.findUserCartWithDetails(userId, forSessionEvent, pageable);
    }

    private CartItem mapToCartItem(CartItemRequest request, Long userId) {
        return CartItem.builder()
                .userId(userId)
                .sessionEventId(request.getSessionEventId())
                .formationId(request.getFormationId())
                .appliedCouponId(request.getCouponId())
                .dateDebut(request.getDateDebut())
                .originalPrice(request.getOriginalPrice())
                .discountedPrice(request.getDiscountedPrice())
                .build();
    }


    public BigDecimal calculateCartTotal(List<CartItemResponse> cartItems) {
        return cartItems.stream()
                .map(CartItemResponse::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

   /* public static BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, BigDecimal discountPercent) {
        if (originalPrice == null || discountPercent == null) {
            throw new IllegalArgumentException("Price and discount cannot be null");
        }

        // Ensure the percentage is within 0–100
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }

        BigDecimal discount = originalPrice.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return originalPrice.subtract(discount);
    }*/
}
