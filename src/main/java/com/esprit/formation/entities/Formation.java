package com.esprit.formation.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "formations")

public class Formation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    private Boolean active = true;

    private String titre;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore // Add this to prevent serialization of the description field
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateFin;
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    private EtatFormation etatFormation;

    @Enumerated(EnumType.STRING)
    private NiveauFormation niveau;

    @Enumerated(EnumType.STRING)
    private Categorie categorie;   //  DEVLLOPPEMENT, DATA_SCIENCE, RESEAU, IA, CLOUD, AUTRES

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "coupon_id")
   @JsonIgnoreProperties("applicableFormations") // Add this
   private Coupon coupon;


    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "formation_images", joinColumns = @JoinColumn(name = "formation_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> images= new ArrayList<>();


    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "formation_participants",
            joinColumns = @JoinColumn(name = "formation_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private Set<User> participants = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "formation_formateurs",
            joinColumns = @JoinColumn(name = "formation_id"),
            inverseJoinColumns = @JoinColumn(name = "formateur_id")
    )
    private Set<User> formateurs = new HashSet<>();

    public void updateEtatFormation() {
        LocalDate now = LocalDate.now();
        if (now.isBefore(dateDebut)) {
            this.etatFormation = EtatFormation.A_VENIR;
        } else if (now.isAfter(dateFin)) {
            this.etatFormation = EtatFormation.TERMINEE;
        } else {
            this.etatFormation = EtatFormation.EN_COURS;
        }
    }

    @AssertTrue(message = "La date de début doit être avant la date de fin")
    public boolean isDateDebutBeforeDateFin() {
        return dateDebut.isBefore(dateFin);
    }

    public void addParticipant(User user) {
        this.participants.add(user);
        user.getFormationsAsParticipant().add(this);
    }

    public void removeParticipant(User user) {
        this.participants.remove(user);
        user.getFormationsAsParticipant().remove(this);
    }

    public void addFormateur(User user) {
        this.formateurs.add(user);
        user.getFormationsAsFormateur().add(this);
    }

    public void removeFormateur(User user) {
        this.formateurs.remove(user);
        user.getFormationsAsFormateur().remove(this);
    }


    public void setCoupon(Coupon coupon) {
        // First, clear the existing coupon relationship if any
        if (this.coupon != null) {
            this.coupon.getApplicableFormations().remove(this);
        }

        // Set the new coupon
        this.coupon = coupon;

        // Add this formation to the new coupon's applicable formations if not null
        if (coupon != null) {
            coupon.getApplicableFormations().add(this);
        }
    }


    public void removeCoupon() {
        if (this.coupon != null) {
            this.coupon.getApplicableFormations().remove(this);
            this.coupon = null;
        }
    }


}
