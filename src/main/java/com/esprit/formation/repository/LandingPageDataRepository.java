package com.esprit.formation.repository;

import com.esprit.formation.entities.LandingPageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface LandingPageDataRepository extends JpaRepository<LandingPageData, Long> {

  List<LandingPageData> findBySection(String section);

  @Query("SELECT DISTINCT l.section FROM LandingPageData l")
  List<String> findDistinctSection();




  void deleteBySection(String section);


}

