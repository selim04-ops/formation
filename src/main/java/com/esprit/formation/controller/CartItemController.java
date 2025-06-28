package com.esprit.formation.controller;

import com.esprit.formation.dto.CartItemRequest;
import com.esprit.formation.dto.CartItemResponse;
import com.esprit.formation.entities.User;
import com.esprit.formation.services.CartItemService;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "Endpoints for managing shopping cart items")
public class CartItemController {

    private final CartItemService cartItemService;
    private static final Logger LOGGER = LoggerFactory.getLogger(CartItemController.class);

    @Operation(summary = "Add item to cart", description = "Add a formation to the user's shopping cart")
    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody CartItemRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = ((User) authentication.getPrincipal()).getId();
            CartItemResponse response = cartItemService.addToCart(request, userId);
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            LOGGER.error("Error adding item to cart", e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error adding item to cart: " + e.getMessage());
        }
    }

    @Operation(summary = "Remove item from cart", description = "Remove a specific item from the shopping cart")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long id) {

        try {
            cartItemService.removeFromCart(id);
            return ResponseWrapper.success("Item removed successfully");
        } catch (Exception e) {
            LOGGER.error("Error removing item from cart", e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error removing item from cart: " + e.getMessage());
        }
    }

    @Operation(summary = "Clear cart", description = "Remove all items from the user's shopping cart")
    @DeleteMapping
    public ResponseEntity<?> clearCart(@RequestParam(defaultValue = "false") boolean forSessionEvent) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = ((User) authentication.getPrincipal()).getId();
            cartItemService.clearCart(userId, forSessionEvent);
            return ResponseWrapper.success("Cart cleared successfully");
        } catch (Exception e) {
            LOGGER.error("Error clearing cart", e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error clearing cart: " + e.getMessage());
        }
    }

    @Operation(summary = "Get cart contents", description = "Retrieve paginated list of items in the user's shopping cart")
    @GetMapping
    public ResponseEntity<?> getCart(
            @RequestParam(defaultValue = "false") boolean forSessionEvent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = ((User) authentication.getPrincipal()).getId();

            Page<CartItemResponse> response = cartItemService.getUserCart(userId, PageRequest.of(page, size), forSessionEvent);
            BigDecimal totalPrice = cartItemService.calculateCartTotal(response.getContent());

            return ResponseWrapper.success(
                    Map.of(
                            "items", response.getContent(),
                            "totalPrice", totalPrice,
                            "currentPage", response.getNumber(),
                            "totalItems", response.getTotalElements(),
                            "totalPages", response.getTotalPages()
                    )
            );
        } catch (Exception e) {
            LOGGER.error("Error fetching cart items", e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching cart items: " + e.getMessage());
        }
    }

}