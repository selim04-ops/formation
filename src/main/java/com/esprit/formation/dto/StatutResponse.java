package com.esprit.formation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StatutResponse {
    private Long id;
    private String content;
    private UserDTO user;
    private List<String> images;
    private LocalDateTime date;
    private List<CommentaireResponse> commentaires;
    private Boolean activeStatus;

}
