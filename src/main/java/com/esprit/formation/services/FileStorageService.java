package com.esprit.formation.services;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() throws IOException {
        // Define the directory where files will be stored
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    public String storeFile(MultipartFile file) {
        try {
            // Generate a unique file name to avoid conflicts
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

            // Resolve the file path
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            // Save the file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the relative path or full URL
            return "/uploads/" + uniqueFileName; // Adjust this to match your server's file serving setup
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file", ex);
        }
    }
}
