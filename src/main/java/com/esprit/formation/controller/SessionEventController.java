package com.esprit.formation.controller;

import com.esprit.formation.dto.SessionEventDTO;
import com.esprit.formation.entities.FeedbackSession;
import com.esprit.formation.entities.SessionEvent;
import com.esprit.formation.iservices.ISessionEventService;
import com.esprit.formation.utils.ImageHandler;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/session-events")
@RequiredArgsConstructor
@Tag(name = "SessionEvent-End-Point", description = "Endpoints pour la gestion des session et evenement")
public class SessionEventController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionEventController.class);

    private final ISessionEventService sessionEventService;

   /* @Operation(summary = "Create a new session event",
            description = "Creates a new session event with the provided details")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSessionEvent(@RequestBody SessionEventDTO sessionEventDTO, @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {

            List<String> imageUrls = ImageHandler.handleImages(images, sessionEventDTO.getImages());
            sessionEventDTO.setImages(imageUrls);
            SessionEvent sessionEvent = sessionEventService.toEntity(sessionEventDTO);
            SessionEvent created = sessionEventService.createSessionEvent(sessionEvent);

            return ResponseWrapper.success(SessionEventDTO.fromEntity(created));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error creating session event");
        }
    }

    @Operation(summary = "Update a session event",
            description = "Updates an existing session event with the provided details")
    @PutMapping(value ="/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSessionEvent(@PathVariable Long id, @RequestBody SessionEventDTO sessionEventDTO, @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            List<String> imageUrls = ImageHandler.handleImages(images, sessionEventDTO.getImages());
            sessionEventDTO.setImages(imageUrls);

            SessionEvent sessionEvent = sessionEventService.toEntity(sessionEventDTO);
            SessionEvent updated = sessionEventService.updateSessionEvent(id, sessionEvent);
            return ResponseWrapper.success(SessionEventDTO.fromEntity(updated));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error updating session event");
        }
    }*/



    @Operation(summary = "Create a new session event",
            description = "Creates a new session event with the provided details")

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSessionEvent(
            @RequestPart("sessionEventDTO") SessionEventDTO sessionEventDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        try {

            LOGGER.info("create sessionEventDTO {}", sessionEventDTO);
            // Convert DTO to entity
            SessionEvent sessionEvent = sessionEventService.toEntity(sessionEventDTO);

            // Handle image uploads
            List<String> imageUrls = ImageHandler.handleImages(images, sessionEventDTO.getImages());
            sessionEvent.setImages(imageUrls);

            // Save the session event with associations
            SessionEvent created = sessionEventService.createSessionEventWithAssociations(
                    sessionEvent,
                    sessionEventDTO.getFormateurIds(),
                    sessionEventDTO.getFormationIds()
            );

            return ResponseWrapper.success(SessionEventDTO.fromEntity(created));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error creating session event");
        }
    }
    @Operation(summary = "Update a session event",
            description = "Updates an existing session event with the provided details")

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSessionEvent(
            @PathVariable Long id,
            @RequestPart("sessionEventDTO") SessionEventDTO sessionEventDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        try {
            LOGGER.info("Updating session {}", id);

            // Handle images
            List<String> imageUrls = ImageHandler.handleImages(images, sessionEventDTO.getImages());
            sessionEventDTO.setImages(imageUrls);
            sessionEventDTO.setId(id);

            // Convert to entity - this now handles associations via toEntity()
            SessionEvent sessionEvent = sessionEventService.toEntity(sessionEventDTO);

            // Perform the update
            SessionEvent updated = sessionEventService.updateSessionEvent(id, sessionEvent);
            return ResponseWrapper.success(SessionEventDTO.fromEntity(updated));
        } catch (Exception e) {
            LOGGER.error("Error updating session event", e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST,
                    "Error updating session event: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete a session event",
            description = "Deletes a session event by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSessionEvent(@PathVariable Long id) {
        try {
            sessionEventService.deleteSessionEvent(id);
            return ResponseWrapper.success("Session event deleted successfully");
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error deleting session event");
        }
    }

    @Operation(summary = "Get session event by ID",
            description = "Retrieves a session event by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getSessionEventById(@PathVariable Long id) {
        try {
            SessionEventDTO sessionEvent = sessionEventService.getSessionEventById(id);
            return ResponseWrapper.success(sessionEvent);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, "Session event not found");
        }
    }

    @Operation(summary = "Get all session events by type", description = "Retrieves session events by type")
    @GetMapping
    public ResponseEntity<?> getAllSessionEvents(@RequestParam String type) {
        try {
            List<SessionEventDTO> sessionEvents = sessionEventService.getAllSessionEvents(type);
            LOGGER.info("this is the sessionEvent {}", sessionEvents);
            return ResponseWrapper.success(sessionEvents);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching session events");
        }
    }

    @Operation(summary = "Add formations to session",
            description = "Adds multiple formations to a session event")
    @PostMapping("/{sessionId}/formations")
    public ResponseEntity<?> addFormationsToSession(@PathVariable Long sessionId, @RequestBody List<Long> formationIds) {
        try {
            LOGGER.info("formation Ids {}", formationIds);
            SessionEvent session = sessionEventService.addFormationsToSession(sessionId, formationIds);
            return ResponseWrapper.success(SessionEventDTO.fromEntity(session));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error adding formations to session");
        }
    }

    @Operation(summary = "Add formateurs to session",
            description = "Adds multiple formateurs to a session event")
    @PostMapping("/{sessionId}/formateurs")
    public ResponseEntity<?> addFormateursToSession(@PathVariable Long sessionId, @RequestBody List<Long> formateurIds) {
        try {
            LOGGER.info("formateur Ids {}", formateurIds);
            SessionEvent session = sessionEventService.addFormateursToSession(sessionId, formateurIds);
            return ResponseWrapper.success(SessionEventDTO.fromEntity(session));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error adding formateurs to session");
        }
    }

    @Operation(summary = "Add participants to session",
            description = "Adds multiple participants to a session event")
    @PostMapping("/{sessionId}/participants")
    public ResponseEntity<?> addParticipantsToSession(@PathVariable Long sessionId, @RequestBody List<Long> participantIds) {
        try {
            LOGGER.info("participant Ids {}", participantIds);
            SessionEvent session = sessionEventService.addParticipantsToSession(sessionId, participantIds);
            return ResponseWrapper.success(SessionEventDTO.fromEntity(session));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error adding participants to session");
        }
    }

    @Operation(summary = "Add feedback to session",
            description = "Adds a feedback to a session event")
    @PostMapping("/{sessionId}/feedbacks")
    public ResponseEntity<?> addFeedbackToSession(@PathVariable Long sessionId, @RequestBody FeedbackSession feedback) {
        try {
            FeedbackSession createdFeedback = sessionEventService.addFeedbackToSession(sessionId, feedback);
            return ResponseWrapper.success(createdFeedback);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Error adding feedback to session");
        }
    }

    @Operation(summary = "Get session feedbacks",
            description = "Retrieves all feedbacks for a session event")
    @GetMapping("/{sessionId}/feedbacks")
    public ResponseEntity<?> getSessionFeedbacks(@PathVariable Long sessionId) {
        try {
            List<FeedbackSession> feedbacks = sessionEventService.getSessionFeedbacks(sessionId);
            return ResponseWrapper.success(feedbacks);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching session feedbacks");
        }
    }
}