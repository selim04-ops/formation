package com.esprit.formation.dto;

import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Builder
public class CommentaireResponse {
    private Long id;
    private String content;
    private UserDTO user;
    private LocalDateTime date;
    private Long statutId;
    private Boolean activeCommentaire;
}