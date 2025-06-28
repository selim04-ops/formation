package com.esprit.formation.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "coupon")
public class Coupon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(unique = true)
  private String code;
  private Double discount;
  private Integer maxUsage;

  @Builder.Default
  private Integer usageCount = 0;

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  @OnDelete(action = OnDeleteAction.SET_NULL)  // Hibernate annotation
  private User createdBy;

  @Builder.Default
  @JsonFormat(pattern = "dd-MM-yyyy")
  private LocalDate expireAt = LocalDate.now().plusDays(5);


  //  every coupon has multipul formation (you can apply coupon in just one formation with your choice)
  @Builder.Default
  @OneToMany(mappedBy = "coupon", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JsonIgnoreProperties("coupon")
  private Set<Formation> applicableFormations = new HashSet<>();

  // Relation Many-to-Many avec User
  @Builder.Default
  @ManyToMany
  @JoinTable(
          name = "coupon_eligible_users",
          joinColumns = @JoinColumn(name = "coupon_id"),
          inverseJoinColumns = @JoinColumn(name = "user_id")
  )
  @JsonIgnoreProperties("eligibleCoupons")
  private Set<User> eligibleUsers = new HashSet<>();

  public boolean isValidFor(Formation formation) {
    if (!isActive()) {
      return false;
    }
    if (!applicableFormations.contains(formation)) {
      return false;
    }
    return true;
  }


  public void applyUsage() {
    if (usageCount >= maxUsage) {
      throw new IllegalStateException("Ce coupon a atteint sa limite d'utilisation");
    }
    usageCount++;
  }

  public boolean isActive() {
    return LocalDate.now().isBefore(expireAt) && maxUsage > usageCount;
  }

  public void disable() {
    this.maxUsage = 0;
    this.usageCount = 0;
  }


  public void addFormation(Formation formation) {
    if (!this.applicableFormations.contains(formation)) {
      this.applicableFormations.add(formation);
      formation.setCoupon(this);
    }
  }

  public void removeFormation(Formation formation) {
    this.applicableFormations.remove(formation);
    formation.setCoupon(null);
  }

  public void addEligibleUser(User user) {
    this.eligibleUsers.add(user);
    user.getEligibleCoupons().add(this);
  }

  public void removeEligibleUser(User user) {
    this.eligibleUsers.remove(user);
    user.getEligibleCoupons().remove(this);
  }


}