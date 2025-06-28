package com.esprit.formation.dto.landingpage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GalleryItem {
    private String title;
    private String largeImage;
    private String smallImage;
}
