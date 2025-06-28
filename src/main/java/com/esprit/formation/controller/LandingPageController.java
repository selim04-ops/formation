package com.esprit.formation.controller;

import com.esprit.formation.dto.landingpage.LandingPageResponse;
import com.esprit.formation.entities.LandingPageData;
import com.esprit.formation.services.LandingPageDataService;
import com.esprit.formation.utils.ImageHandler;
import com.esprit.formation.utils.ResponseWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/landing-page")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "LandingPage-End-Point", description = "Endpoints for managing landing page data")
public class LandingPageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LandingPageController.class);
    private final LandingPageDataService landingPageDataService;
    private final ObjectMapper objectMapper;

    // Inject dependencies via constructor
    public LandingPageController(LandingPageDataService landingPageDataService, ObjectMapper objectMapper) {
        this.landingPageDataService = landingPageDataService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Get all landing page data",
            description = "Retrieve all landing page data in structured format")
    @GetMapping
    public ResponseEntity<?> getAllData() {
        try {
            LandingPageResponse response = landingPageDataService.getStructuredData();
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            LOGGER.error("Error fetching all landing page data: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve data");
        }
    }



    @Operation(summary = "Update section data with images",
            description = "Update existing landing page data with image support")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateDataWithImage(
            @PathVariable Long id,
            @RequestPart("data") LandingPageData landingPageData,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        try {
            // Check if data exists
            LandingPageData existingData = landingPageDataService.getDataById(id);
            if (existingData == null) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "Data not found");
            }

            // Handle images if present
            if (images != null && !images.isEmpty()) {
                List<String> existingImageUrls = existingData.getValue() != null ?
                        List.of(existingData.getValue()) : List.of();

                List<String> imageUrls = ImageHandler.handleImages(images, existingImageUrls);

                if (!imageUrls.isEmpty()) {
                    landingPageData.setValue(imageUrls.get(0));
                }
            }

            LandingPageData updatedData = landingPageDataService.updateData(id, landingPageData);
            return ResponseWrapper.success(updatedData);

        } catch (Exception e) {
            LOGGER.error("Error updating data: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update data");
        }
    }

    @Operation(summary = "Delete landing page data",
            description = "Delete a specific landing page data entry by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteData(@PathVariable Long id) {
        try {
            if (landingPageDataService.getDataById(id) == null) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "Data not found with ID: " + id);
            }
            landingPageDataService.deleteData(id);
            return ResponseWrapper.success("Data deleted successfully");
        } catch (Exception e) {
            LOGGER.error("Error deleting data with ID {}: {}", id, e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete data");
        }
    }

    @Operation(summary = "Delete data by section",
            description = "Delete all landing page data entries for a specific section")
    @DeleteMapping("/section/{section}")
    public ResponseEntity<?> deleteSection(@PathVariable String section) {
        try {
            List<LandingPageData> sectionData = landingPageDataService.getDataBySection(section);
            if (sectionData.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "No data found for section: " + section);
            }
            landingPageDataService.deleteBySection(section);
            return ResponseWrapper.success("Section data deleted successfully");
        } catch (Exception e) {
            LOGGER.error("Error deleting data for section {}: {}", section, e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete section data");
        }
    }

    @Operation(summary = "Get all sections",
            description = "Retrieve a list of all unique sections")
    @GetMapping("/sections")
    public ResponseEntity<?> getAllSections() {
        try {
            List<String> sections = landingPageDataService.getAllSections();
            if (sections.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NO_CONTENT, "No sections found");
            }
            return ResponseWrapper.success(sections);
        } catch (Exception e) {
            LOGGER.error("Error fetching sections: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve sections");
        }
    }





    @Operation(summary = "Create or update section data",
            description = "Create or update landing page data with support for multiple key-value pairs and images")
    @PostMapping(value = "/{section}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createOrUpdateSectionData(
            @PathVariable String section,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "image", required = false) MultipartFile singleImage,
            @RequestPart(value = "images", required = false) MultipartFile[] imagesArray,
            @RequestPart(value = "largeImages", required = false) MultipartFile[] largeImagesArray,
            @RequestPart(value = "smallImages", required = false) MultipartFile[] smallImagesArray) {

        try {
            LOGGER.info("Received request to update section: {}", section);

            // Parse JSON data
            List<Map<String, Object>> dataList = objectMapper.readValue(
                    dataJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Convert array parameters to lists
            List<MultipartFile> images = imagesArray != null ? Arrays.asList(imagesArray) : null;
            List<MultipartFile> largeImages = largeImagesArray != null ? Arrays.asList(largeImagesArray) : null;
            List<MultipartFile> smallImages = smallImagesArray != null ? Arrays.asList(smallImagesArray) : null;

            // First delete all existing data for this section
            landingPageDataService.deleteBySection(section);

            List<LandingPageData> savedItems = new ArrayList<>();
            Map<String, String> imageUrlsMap = new HashMap<>();

            // Handle image uploads using ImageHandler
            switch (section) {
                case "About":
                    if (singleImage != null) {
                        validateImage(singleImage);
                        List<MultipartFile> imageList = Collections.singletonList(singleImage);
                        List<String> imageUrls = ImageHandler.handleImages(imageList, Collections.emptyList());
                        if (!imageUrls.isEmpty()) {
                            savedItems.add(createLandingPageData(section, "img", imageUrls.get(0)));
                        }
                    }
                    break;

                case "Team":
                case "Testimonials":
                    if (images != null) {
                        images.forEach(this::validateImage);
                        List<String> imageUrls = ImageHandler.handleImages(images, Collections.emptyList());
                        for (int i = 0; i < imageUrls.size(); i++) {
                            imageUrlsMap.put("img_" + i, imageUrls.get(i));
                        }
                    }
                    break;

                case "Gallery":
                    if (largeImages != null) {
                        largeImages.forEach(this::validateImage);
                        List<String> largeUrls = ImageHandler.handleImages(largeImages, Collections.emptyList());
                        for (int i = 0; i < largeUrls.size(); i++) {
                            imageUrlsMap.put("largeImage_" + i, largeUrls.get(i));
                        }
                    }
                    if (smallImages != null) {
                        smallImages.forEach(this::validateImage);
                        List<String> smallUrls = ImageHandler.handleImages(smallImages, Collections.emptyList());
                        for (int i = 0; i < smallUrls.size(); i++) {
                            imageUrlsMap.put("smallImage_" + i, smallUrls.get(i));
                        }
                    }
                    break;
            }

            // Process data based on section type
            switch (section) {
                case "About":
                    processAboutSection(dataList, savedItems);
                    break;

                case "Team":
                case "Testimonials":
                    processMultiItemSection(section, dataList, savedItems, imageUrlsMap);
                    break;

                case "Gallery":
                    processGallerySection(dataList, savedItems, imageUrlsMap);
                    break;

                case "Features":
                case "Services":
                    processServicesOrFeatures(section, dataList, savedItems);
                    break;

                case "Header":
                    processHeaderSection(dataList, savedItems);
                    break;
            }

            return ResponseWrapper.success(savedItems);
        } catch (JsonProcessingException e) {
            LOGGER.error("JSON processing error: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Invalid data format");
        } catch (IllegalArgumentException e) {
            LOGGER.error("Validation error: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error updating section data: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update data");
        }
    }

    // ===== Helper Methods ===== //

    private void validateImage(MultipartFile image) {
        if (image.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IllegalArgumentException("Image size too large, maximum is 5MB");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid image file type");
        }
    }

    private void processAboutSection(List<Map<String, Object>> dataList, List<LandingPageData> savedItems) {
        Map<String, Object> aboutData = new HashMap<>();

        // First collect all data into a single map
        for (Map<String, Object> dataItem : dataList) {
            aboutData.putAll(dataItem);
        }

        // Process Why items
        processWhyItems(aboutData, "Why1", savedItems);
        processWhyItems(aboutData, "Why2", savedItems);

        // Process other fields
        aboutData.forEach((key, value) -> {
            if (!key.startsWith("Why")) {
                savedItems.add(createLandingPageData("About", key, value.toString()));
            }
        });
    }

    private void processWhyItems(Map<String, Object> aboutData, String whyPrefix, List<LandingPageData> savedItems) {
        for (int i = 1; i <= 4; i++) {
            String key = whyPrefix + i;
            if (aboutData.containsKey(key)) {
                savedItems.add(createLandingPageData(
                        "About",
                        key,
                        aboutData.get(key).toString()
                ));
            }
        }
    }

    private void processMultiItemSection(String section, List<Map<String, Object>> dataList,
                                         List<LandingPageData> savedItems, Map<String, String> imageUrlsMap) {
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> item = dataList.get(i);
            String imageUrl = imageUrlsMap.get("img_" + i);

            Map<String, Object> itemData = new HashMap<>(item);
            if (imageUrl != null) {
                itemData.put("img", imageUrl);
            }

            try {
                savedItems.add(createLandingPageData(
                        section,
                        section.toLowerCase() + "_" + i,
                        objectMapper.writeValueAsString(itemData)
                ));
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to serialize item data for {}: {}", section, e.getMessage());
                throw new RuntimeException("Failed to process " + section + " data");
            }
        }
    }

    private void processGallerySection(List<Map<String, Object>> dataList,
                                       List<LandingPageData> savedItems, Map<String, String> imageUrlsMap) {
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> item = dataList.get(i);
            String largeImageUrl = imageUrlsMap.get("largeImage_" + i);
            String smallImageUrl = imageUrlsMap.get("smallImage_" + i);

            Map<String, Object> galleryItem = new HashMap<>();
            galleryItem.put("title", item.get("title"));
            if (largeImageUrl != null) {
                galleryItem.put("largeImage", largeImageUrl);
            }
            if (smallImageUrl != null) {
                galleryItem.put("smallImage", smallImageUrl);
            }

            try {
                savedItems.add(createLandingPageData(
                        "Gallery",
                        "gallery_" + i,
                        objectMapper.writeValueAsString(galleryItem))
                );
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to serialize gallery item: {}", e.getMessage());
                throw new RuntimeException("Failed to process gallery data");
            }
        }
    }

    private void processServicesOrFeatures(String section, List<Map<String, Object>> dataList,
                                           List<LandingPageData> savedItems) {
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> item = dataList.get(i);
            savedItems.add(createLandingPageData(section, "icon_" + i, item.get("icon").toString()));
            savedItems.add(createLandingPageData(section, "title_" + i, item.get("title").toString()));
            savedItems.add(createLandingPageData(section, "text_" + i, item.get("text").toString()));
        }
    }

    private void processHeaderSection(List<Map<String, Object>> dataList, List<LandingPageData> savedItems) {
        Map<String, Object> headerData = dataList.get(0);
        savedItems.add(createLandingPageData("Header", "title", headerData.get("title").toString()));
        savedItems.add(createLandingPageData("Header", "paragraph", headerData.get("paragraph").toString()));

        String videoUrl = headerData.containsKey("videoUrl") ?
                headerData.get("videoUrl").toString() :
                "https://youtube.com/embed/...";
        savedItems.add(createLandingPageData("Header", "videoUrl", videoUrl));
    }

    private LandingPageData createLandingPageData(String section, String keyName, String value) {
        return landingPageDataService.createData(
                LandingPageData.builder()
                        .section(section)
                        .keyName(keyName)
                        .value(value)
                        .build()
        );
    }
}