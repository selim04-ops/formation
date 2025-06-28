package com.esprit.formation.services;

import com.esprit.formation.dto.landingpage.*;
import com.esprit.formation.entities.LandingPageData;
import com.esprit.formation.iservices.ILandingPageDataService;
import com.esprit.formation.repository.LandingPageDataRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
@Service
@Transactional
public class LandingPageDataService implements ILandingPageDataService {

    private final LandingPageDataRepository repository;

    public LandingPageDataService(LandingPageDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LandingPageData> getAllData() {
        return repository.findAll();
    }

    @Override
    public List<LandingPageData> getDataBySection(String section) {
        return repository.findBySection(section);
    }

    @Override
    public LandingPageData getDataById(Long id) {
        Optional<LandingPageData> data = repository.findById(id);
        return data.orElse(null);
    }

    @Override
    public LandingPageData createData(LandingPageData landingPageData) {
        return repository.save(landingPageData);
    }

    @Override
    public LandingPageData updateData(Long id, LandingPageData landingPageData) {
        if (repository.existsById(id)) {
            landingPageData.setId(id);
            return repository.save(landingPageData);
        }
        return null;
    }

    @Override
    public void deleteData(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteBySection(String section) {
        repository.deleteBySection(section);
    }

    @Override
    public List<String> getAllSections() {
        return repository.findDistinctSection();
    }

@Override
    public LandingPageResponse getStructuredData() {
        List<LandingPageData> allData = repository.findAll();

        LandingPageResponse response = new LandingPageResponse();

        // Process each section
        response.setHeader(processHeader(allData));
        response.setAbout(processAbout(allData));
        response.setGallery(processGallery(allData));
        response.setServices(processService(allData));
        response.setTestimonials(processTestimonials(allData));
        response.setTeam(processTeam(allData));
        response.setContact(processContact(allData));
        response.setFeatures(processFeatures(allData));

        return response;
    }

    private Header processHeader(List<LandingPageData> data) {
        Header header = new Header();
        data.stream()
                .filter(d -> "Header".equals(d.getSection()))
                .forEach(d -> {
                    switch (d.getKeyName()) {
                        case "title": header.setTitle(d.getValue()); break;
                        case "paragraph": header.setParagraph(d.getValue()); break;
                        case "videoUrl": header.setVideoUrl(d.getValue()); break;
                    }
                });
        return header;
    }

    private About processAbout(List<LandingPageData> data) {
        About about = new About();
        List<LandingPageData> aboutData = data.stream()
                .filter(d -> "About".equals(d.getSection()))
                .collect(Collectors.toList());

        aboutData.forEach(d -> {
            switch (d.getKeyName()) {
                case "img": about.setImg(d.getValue()); break;
                case "paragraph": about.setParagraph(d.getValue()); break;
            }
        });

        // Process Why lists
        about.setWhy(aboutData.stream()
                .filter(d -> d.getKeyName().startsWith("Why["))
                .sorted(Comparator.comparing(d -> d.getKeyName()))
                .map(LandingPageData::getValue)
                .collect(Collectors.toList()));

        about.setWhy2(aboutData.stream()
                .filter(d -> d.getKeyName().startsWith("Why2["))
                .sorted(Comparator.comparing(d -> d.getKeyName()))
                .map(LandingPageData::getValue)
                .collect(Collectors.toList()));

        return about;
    }

    private List<GalleryItem> processGallery(List<LandingPageData> data) {
        Map<Integer, GalleryItem> galleryMap = new HashMap<>();

        data.stream()
                .filter(d -> d.getSection().startsWith("Gallery["))
                .forEach(d -> {
                    // Extract index from section name (e.g., "Gallery[0]" -> 0)
                    int index = Integer.parseInt(d.getSection().substring(8, d.getSection().length() - 1));
                    GalleryItem item = galleryMap.computeIfAbsent(index, k -> new GalleryItem());

                    switch (d.getKeyName()) {
                        case "title": item.setTitle(d.getValue()); break;
                        case "largeImage": item.setLargeImage(d.getValue()); break;
                        case "smallImage": item.setSmallImage(d.getValue()); break;
                    }
                });

        return galleryMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<Services> processService(List<LandingPageData> data) {
        Map<Integer, Services> servicesMap = new HashMap<>();

        data.stream()
                .filter(d -> d.getSection().startsWith("Services["))
                .forEach(d -> {
                    int index = Integer.parseInt(d.getSection().substring(9, d.getSection().length() - 1));
                    Services service = servicesMap.computeIfAbsent(index, k -> new Services());

                    switch (d.getKeyName()) {
                        case "icon": service.setIcon(d.getValue()); break;
                        case "name": service.setName(d.getValue()); break;
                        case "text": service.setText(d.getValue()); break;
                    }
                });

        return servicesMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<Testimonial> processTestimonials(List<LandingPageData> data) {
        Map<Integer, Testimonial> testimonialsMap = new HashMap<>();

        data.stream()
                .filter(d -> d.getSection().startsWith("Testimonials["))
                .forEach(d -> {
                    int index = Integer.parseInt(d.getSection().substring(12, d.getSection().length() - 1));
                    Testimonial testimonial = testimonialsMap.computeIfAbsent(index, k -> new Testimonial());

                    switch (d.getKeyName()) {
                        case "img": testimonial.setImg(d.getValue()); break;
                        case "text": testimonial.setText(d.getValue()); break;
                        case "name": testimonial.setName(d.getValue()); break;
                    }
                });

        return testimonialsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<TeamMember> processTeam(List<LandingPageData> data) {
        Map<Integer, TeamMember> teamMap = new HashMap<>();

        data.stream()
                .filter(d -> d.getSection().startsWith("Team["))
                .forEach(d -> {
                    int index = Integer.parseInt(d.getSection().substring(5, d.getSection().length() - 1));
                    TeamMember member = teamMap.computeIfAbsent(index, k -> new TeamMember());

                    switch (d.getKeyName()) {
                        case "img": member.setImg(d.getValue()); break;
                        case "name": member.setName(d.getValue()); break;
                        case "job": member.setJob(d.getValue()); break;
                    }
                });

        return teamMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private Contact processContact(List<LandingPageData> data) {
        Contact contact = new Contact();
        data.stream()
                .filter(d -> "Contact".equals(d.getSection()))
                .forEach(d -> {
                    switch (d.getKeyName()) {
                        case "address": contact.setAddress(d.getValue()); break;
                        case "phone": contact.setPhone(d.getValue()); break;
                        case "email": contact.setEmail(d.getValue()); break;
                        case "facebook": contact.setFacebook(d.getValue()); break;
                        case "twitter": contact.setTwitter(d.getValue()); break;
                        case "youtube": contact.setYoutube(d.getValue()); break;
                    }
                });
        return contact;
    }

    private List<Feature> processFeatures(List<LandingPageData> data) {
        Map<Integer, Feature> featuresMap = new HashMap<>();

        data.stream()
                .filter(d -> d.getSection().startsWith("Features["))
                .forEach(d -> {
                    int index = Integer.parseInt(d.getSection().substring(9, d.getSection().length() - 1));
                    Feature feature = featuresMap.computeIfAbsent(index, k -> new Feature());

                    switch (d.getKeyName()) {
                        case "icon": feature.setIcon(d.getValue()); break;
                        case "title": feature.setTitle(d.getValue()); break;
                        case "text": feature.setText(d.getValue()); break;
                    }
                });

        return featuresMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}