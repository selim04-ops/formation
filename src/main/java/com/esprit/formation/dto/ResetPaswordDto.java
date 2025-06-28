package com.esprit.formation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPaswordDto {
    String token;
    String newpassword;

}
