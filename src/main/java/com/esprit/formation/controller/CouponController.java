package com.esprit.formation.controller;

import com.esprit.formation.dto.CouponRequest;
import com.esprit.formation.dto.CouponResponse;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.ICouponService;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
@Tag(name = "Coupon-End-Point", description = "Endpoints pour la gestion des coupons")
public class CouponController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponController.class);


    private final ICouponService couponService;

    @Operation(summary = "Créer ou mettre à jour un coupon", description = "Permet aux administrateurs de créer ou mettre à jour un coupon")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createOrUpdateCoupon(
            @RequestParam(required = false) Long couponId,
            @RequestBody CouponRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            LOGGER.info("this is the principal {}", authentication.getPrincipal());
            Long userId = ((User) authentication.getPrincipal()).getId();

            CouponResponse response = couponService.createOrUpdateCoupon(request, couponId, userId);
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            LOGGER.error("Error in createOrUpdateCoupon", e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "Appliquer un coupon", description = "Permet aux utilisateurs d'appliquer un coupon")
    @PostMapping("/apply")
    public ResponseEntity<?> applyCoupon(
            @RequestParam @NotBlank String code,
            @RequestParam @Min(1) Long userId,
            @RequestParam @Min(1) Long formationId) {
        try {
            CouponResponse response = couponService.applyCoupon(code, userId, formationId);
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "Valider un coupon", description = "Vérifie si un coupon est valide")
    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(
            @RequestParam String code,
            @RequestParam Long formationId) {
        try {
            boolean isValid = couponService.validateCoupon(code, formationId);
            return ResponseWrapper.success(Map.of("isValid", isValid));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "Remove formation from coupon", description = "Removes a formation from a coupon's applicable formations")
    @PutMapping("/{couponId}/remove-formation")
    public ResponseEntity<?> removeFormationFromCoupon(
            @PathVariable Long couponId,
            @RequestBody Map<String, Long> request) {
        try {
            Long formationId = request.get("formationId");
            if (formationId == null) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "formationId is required");
            }
            CouponResponse response = couponService.removeFormationFromCoupon(couponId, formationId);
            return ResponseWrapper.success(response);
        } catch (EntityNotFoundException e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "Voir tous les coupons avec leurs formations", description = "Permet aux administrateurs de voir tous les coupons avec leurs formations associées")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<CouponResponse> response = couponService.getAllCoupons(PageRequest.of(page, size));
            return ResponseWrapper.success(
                    Map.of(
                            "data", response.getContent(),
                            "currentPage", response.getNumber(),
                            "totalItems", response.getTotalElements(),
                            "totalPages", response.getTotalPages()
                    )
            );
        } catch (Exception e) {
            LOGGER.error("Error fetching coupons", e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching coupons: " + e.getMessage());
        }
    }

    @Operation(summary = "Ajouter une formation à un coupon", description = "Ajoute une formation aux formations applicables d'un coupon")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{couponId}/add-formation")
    public ResponseEntity<?> addFormationToCoupon(
            @PathVariable Long couponId,
            @RequestBody Map<String, Long> request) {
        try {
            Long formationId = request.get("formationId");
            CouponResponse response = couponService.addFormationToCoupon(couponId, formationId);
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "Supprimer un coupon", description = "Permet aux administrateurs de supprimer un coupon")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        try {


            couponService.deleteCoupon(id);
            return ResponseWrapper.success("Coupon supprimé avec succès");
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Obtenir un coupon par ID", description = "Permet d'obtenir les détails d'un coupon spécifique avec ses formations")
    @GetMapping("/{id}")
    public ResponseEntity<?> getCouponById(@PathVariable Long id) {
        try {
            CouponResponse response = couponService.getCouponByIdWithFormations(id);
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}