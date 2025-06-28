package com.esprit.formation.iservices;

import com.esprit.formation.dto.CouponRequest;
import com.esprit.formation.dto.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICouponService {
    CouponResponse createOrUpdateCoupon(CouponRequest request, Long couponId, Long userId);
    CouponResponse applyCoupon(String code, Long userId, Long formationId);
    Page<CouponResponse> getAllCoupons(Pageable pageable);
    CouponResponse getCouponById(Long id);
    void deleteCoupon(Long id);
    boolean validateCoupon(String code, Long formationId);
    CouponResponse addFormationToCoupon(Long couponId, Long formationId);
    CouponResponse getCouponByIdWithFormations(Long id);

    CouponResponse removeFormationFromCoupon(Long couponId, Long formationId);

        //CouponsByUserResponse getCouponsCreatedByUser(Long userId, Pageable pageable);

        // CouponResponse updateCoupon(Long id, CouponRequest request);
   // CouponResponse toggleCouponStatus(Long id, Integer maxUsage);


}
