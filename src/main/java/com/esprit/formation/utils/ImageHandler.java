package com.esprit.formation.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Collectors;

public final class ImageHandler {
    private static final String UPLOAD_DIR = "uploads";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    static {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            setDirectoryPermissions(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    private ImageHandler() {}

    public static String saveImage(MultipartFile file) throws IOException {
        validateImage(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID() + extension;
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFilename).toAbsolutePath().normalize();

        // Security check
        if (!filePath.startsWith(Paths.get(UPLOAD_DIR).toAbsolutePath().normalize())) {
            throw new IOException("Attempt to save file outside upload directory");
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        setFilePermissions(filePath);

        return "/uploads/" + uniqueFilename;
    }

    public static List<String> saveImages(List<MultipartFile> files) throws IOException {
        if (files == null) return new ArrayList<>();

        return files.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .map(file -> {
                    try {
                        return saveImage(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to save image: " + e.getMessage(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    public static List<String> handleImages(List<MultipartFile> newImages, List<String> existingUrls) {
        // If new images are provided (even empty list), it means we want to replace all images
        if (newImages != null) {
            try {
                // First delete all existing images if we're doing a replacement
                deleteImages(existingUrls);

                // Now save the new images (empty list means we're removing all)
                return saveImages(newImages);
            } catch (IOException e) {
                throw new RuntimeException("Failed to handle images: " + e.getMessage(), e);
            }
        }

        // If no new images provided, keep existing ones
        return existingUrls != null ? new ArrayList<>(existingUrls) : new ArrayList<>();
    }

    private static void deleteImages(List<String> imageUrls) throws IOException {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String url : imageUrls) {
            try {
                String filename = url.substring(url.lastIndexOf('/') + 1);
                Path filePath = Paths.get(UPLOAD_DIR, filename).toAbsolutePath().normalize();

                // Security check
                if (!filePath.startsWith(Paths.get(UPLOAD_DIR).toAbsolutePath().normalize())) {
                    continue;
                }

                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                // Log but continue with other deletions
                System.err.println("Failed to delete image: " + url + " - " + e.getMessage());
            }
        }
    }

    private static void validateImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds limit");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!isExtensionAllowed(extension)) {
            throw new IllegalArgumentException("Invalid file type");
        }
    }

    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private static boolean isExtensionAllowed(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private static void setDirectoryPermissions(Path path) throws IOException {
        if (IS_WINDOWS) {
            path.toFile().setReadable(true, false);
            path.toFile().setWritable(true, false);
            path.toFile().setExecutable(true, false);
        } else {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static void setFilePermissions(Path path) throws IOException {
        if (IS_WINDOWS) {
            path.toFile().setReadable(true, false);
            path.toFile().setWritable(true, false);
        } else {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-r--r--");
            Files.setPosixFilePermissions(path, permissions);
        }
    }
}