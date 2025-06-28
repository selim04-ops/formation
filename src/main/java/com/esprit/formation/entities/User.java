package com.esprit.formation.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"motDePasse", "statuts", "commentaires", "formationsAsParticipant", "formationsAsFormateur", "eligibleCoupons", "createdCoupons"})
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    private Boolean isActive = true;
    private String googleId;
    private String facebookId;
    private String nomEtPrenom;
    private String cin;
    private String imgUrl;
    private String email;
    private String motDePasse;
    private Long phoneNumber;


    @Builder.Default
    @ManyToMany(mappedBy = "eligibleUsers")
    @JsonIgnoreProperties("eligibleUsers")
    private Set<Coupon> eligibleCoupons = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "participants")
    private Set<Formation> formationsAsParticipant = new HashSet<>();
    @Builder.Default
    @ManyToMany(mappedBy = "formateurs")
    private Set<Formation> formationsAsFormateur = new HashSet<>();


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Statut> statuts; // List of statuses created by the user

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commentaire> commentaires; // List of comments made by the user


    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role= Role.PARTICIPANT; // ADMIN PARTICIPANT FORMATEUR

    @OneToMany(fetch = FetchType.EAGER)
    private List<PasswordResetToken> passwordResetTokens;
    @Override
    public boolean isEnabled() {
        return isActive;
    }

    @Builder.Default
    @OneToMany(mappedBy = "createdBy", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnoreProperties("createdBy")
    private Set<Coupon> createdCoupons = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }



    public void addFormationAsParticipant(Formation formation) {
        this.formationsAsParticipant.add(formation);
        formation.getParticipants().add(this);
    }

    public void removeFormationAsParticipant(Formation formation) {
        this.formationsAsParticipant.remove(formation);
        formation.getParticipants().remove(this);
    }

    public void addFormationAsFormateur(Formation formation) {
        this.formationsAsFormateur.add(formation);
        formation.getFormateurs().add(this);
    }

    public void removeFormationAsFormateur(Formation formation) {
        this.formationsAsFormateur.remove(formation);
        formation.getFormateurs().remove(this);
    }

    public void addCreatedCoupon(Coupon coupon) {
        this.createdCoupons.add(coupon);
        coupon.setCreatedBy(this);
    }

    public void removeCreatedCoupon(Coupon coupon) {
        this.createdCoupons.remove(coupon);
        coupon.setCreatedBy(null);
    }
}