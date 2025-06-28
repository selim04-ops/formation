package com.esprit.formation.dto.landingpage;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class About {
    private String img;
    private String paragraph;
    private List<String> why;
    private List<String> why2;
}
