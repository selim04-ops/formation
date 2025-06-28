package com.esprit.formation.dto.landingpage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LandingPageResponse {
    private Header header;
    private About about;
    private List<GalleryItem> gallery;
    private List<Services> services;
    private List<Testimonial> testimonials;
    private List<TeamMember> team;
    private Contact contact;
    private List<Feature> features;
}

