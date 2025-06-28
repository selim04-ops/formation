package com.esprit.formation.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "landing_page_data")
public class LandingPageData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String section;  // Ex: "Header", "About"
    private String keyName; // Ex: "title", "paragraph", "Gallery[0].title"



    @Lob
    @Column(columnDefinition = "TEXT")
    private String value;    // String representation of the value


}

