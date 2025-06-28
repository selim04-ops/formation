package com.esprit.formation.iservices;

import com.esprit.formation.dto.landingpage.LandingPageResponse;
import com.esprit.formation.entities.LandingPageData;
import java.util.List;

public interface ILandingPageDataService {
    List<LandingPageData> getAllData();
    List<LandingPageData> getDataBySection(String section);
    LandingPageData getDataById(Long id);
    LandingPageData createData(LandingPageData landingPageData);
    LandingPageData updateData(Long id, LandingPageData landingPageData);
    void deleteData(Long id);
    void deleteBySection(String section);
    List<String> getAllSections();
    LandingPageResponse getStructuredData();
    }