package com.esprit.formation.iservices;

import com.esprit.formation.dto.ParticipationStatsDTO;
import com.esprit.formation.dto.StatsResponse;
import com.esprit.formation.dto.UserExpenseDTO;

import java.util.List;

public interface IStatService {

    StatsResponse getPlatformStats();
    ParticipationStatsDTO getParticipationStats();
    List<UserExpenseDTO> getUserExpenseStats();



}
