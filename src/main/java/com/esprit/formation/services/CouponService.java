package com.esprit.formation.services;

import com.esprit.formation.dto.*;
import com.esprit.formation.entities.Coupon;
import com.esprit.formation.entities.Formation;
import com.esprit.formation.entities.NiveauFormation;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.ICouponService;
import com.esprit.formation.repository.CouponRepository;
import com.esprit.formation.repository.FormationRepository;
import com.esprit.formation.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CouponService implements ICouponService {

    private final CouponRepository couponRepository;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public CouponService(CouponRepository couponRepository, FormationRepository formationRepository, UserRepository userRepository, UserService userService) {
        this.couponRepository = couponRepository;
        this.formationRepository = formationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }


    @Transactional
    public CouponResponse removeFormationFromCoupon(Long couponId, Long formationId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found"));

        // Use the proper method to remove the relationship
        formation.removeCoupon();

        formationRepository.save(formation);
        return mapToCouponResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse createOrUpdateCoupon(CouponRequest request, Long couponId, Long userId) {
        Coupon coupon = (couponId != null) ?
                couponRepository.findByIdWithFormations(couponId)
                        .orElseThrow(() -> new EntityNotFoundException("Coupon not found")) :
                new Coupon();

        // Validate code uniqueness for new coupons
        if (couponId == null && couponRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }

 Optional<User> user = userService.getUserById(userId);
        coupon.setCode(request.getCode());
        coupon.setDiscount(request.getDiscount());
        coupon.setMaxUsage(request.getMaxUsage());
        coupon.setExpireAt(request.getExpireAt());
        user.ifPresent(coupon::setCreatedBy);


        if (request.getFormationIds() != null && !request.getFormationIds().isEmpty()) {
            Set<Formation> formations = new HashSet<>(
                    formationRepository.findAllById(request.getFormationIds())
            );

            if (formations.size() < request.getFormationIds().size()) {
                throw new EntityNotFoundException("Some formations not found");
            }

            // Clear existing formations first to properly handle updates
            coupon.getApplicableFormations().forEach(f -> f.setCoupon(null));
            coupon.getApplicableFormations().clear();

            // Add new formations with proper bidirectional relationship
            formations.forEach(formation -> {
                // First remove from any existing coupon
                if (formation.getCoupon() != null) {
                    formation.getCoupon().getApplicableFormations().remove(formation);
                }
                // Then establish new relationship
                formation.setCoupon(coupon);
                coupon.getApplicableFormations().add(formation);
            });
        } else {
            // If no formations provided, clear existing ones
            coupon.getApplicableFormations().forEach(f -> f.setCoupon(null));
            coupon.getApplicableFormations().clear();
        }

        Coupon savedCoupon = couponRepository.save(coupon);
        return mapToCouponResponse(savedCoupon);
    }
    @Transactional
    public CouponResponse applyCoupon(String code, Long userId, Long formationId) {
        // Fetch all required entities first
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found"));
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));
        // Validate coupon applicability
        if (!coupon.isValidFor(formation)) {
            throw new IllegalStateException("Coupon not valid for this formation");
        }
        // Check user eligibility
        if (coupon.getEligibleUsers().contains(user)) {
            throw new IllegalStateException("User not eligible for this coupon");
        }
        // Apply changes
        coupon.applyUsage();
        user.getEligibleCoupons().add(coupon);
        coupon.getEligibleUsers().add(user);
        couponRepository.save(coupon);
        userRepository.save(user);
        return mapToCouponResponse(coupon);
    }

    @Transactional(readOnly = true)
    @Override
    public CouponResponse addFormationToCoupon(Long couponId, Long formationId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found"));

        coupon.addFormation(formation);
        return mapToCouponResponse(couponRepository.save(coupon));
    }

    @Override
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        return couponRepository.findAllWithFormations(pageable)
                .map(this::mapToCouponResponse);
    }

    @Override
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon non trouvé"));
        return mapToCouponResponse(coupon);
    }


    @Transactional
    @Override
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Coupon not found"));
        Formation formation = formationRepository.findFormationByCoupon(coupon);
        if (formation != null) {
            formation.setCoupon(null);
        }
        coupon.disable();
    }

    @Override
    public boolean validateCoupon(String code, Long formationId) {
        return couponRepository.findByCode(code)
                .map(coupon ->
                        coupon.getApplicableFormations().stream()
                                .anyMatch(f -> f.getId().equals(formationId)) &&
                                coupon.getUsageCount() < coupon.getMaxUsage()
                )
                .orElse(false);
    }
    @Override
    public CouponResponse getCouponByIdWithFormations(Long id) {
        Coupon coupon = couponRepository.findByIdWithFormations(id)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found"));
        return mapToCouponResponse(coupon);
    }


    private CouponResponse mapToCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discount(coupon.getDiscount())
                .maxUsage(coupon.getMaxUsage())
                .usageCount(coupon.getUsageCount())
                .expireAt(coupon.getExpireAt())
                .createdAt(coupon.getCreatedAt())

                .formations(
                        coupon.getApplicableFormations().stream()
                                .map(this::mapToFormationResponse)
                                .collect(Collectors.toSet())
                )
                .build();
    }

    private FormationResponse mapToFormationResponse(Formation formation) {
        return FormationResponse.builder()
                .id(formation.getId())
                .titre(formation.getTitre())
                .type(formation.getType() != null ? formation.getType().name() : null)
                .niveau(formation.getNiveau() != null ? NiveauFormation.valueOf(formation.getNiveau().name()) : null)
                .dateDebut(formation.getDateDebut())
                .dateFin(formation.getDateFin())
                .build();
    }
}