package com.esprit.formation.dto;

import com.esprit.formation.entities.Role;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter
public class UserDTO {
    private Long id;
    private String email;
    private String nomEtPrenom;
    private String imgUrl;
    private Role role;
    private String password;
    private Boolean isActive;
    private String cin;
    private Long phoneNumber;

}