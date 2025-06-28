package com.esprit.formation.iservices;

import com.esprit.formation.dto.CartItemRequest;
import com.esprit.formation.dto.CartItemResponse;
import com.esprit.formation.dto.TransactionResponse;
import org.springframework.stereotype.Repository;

@Repository
public interface ICartItemService {
    CartItemResponse addItemToCart(Long userId, CartItemRequest request);
    CartItemResponse applyCouponToItem(Long userId, Long itemId, String couponCode);
    CartItemResponse removeItemFromCart(Long userId, Long itemId);
    void clearCart(Long userId);
    TransactionResponse checkout(Long userId);

    //CheckoutResponse checkout(Long userId, PaymentMethodRequest paymentMethod);
}
