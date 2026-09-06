package com.pds.pfe.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUploadService {

    public String uploadPdfFile(File pdfFile, String uploadDirectory) {
        try {
            // Créer le répertoire de destination s'il n'existe pas
            File uploadDir = new File(uploadDirectory);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Générer un nom de fichier unique
            String fileName = System.currentTimeMillis() + "_" + pdfFile.getName();
            File destination = new File(uploadDir, fileName);

            // Copier le fichier
            Files.copy(pdfFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return destination.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'upload du fichier", e);
        }
    }

    public boolean isPdfFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".pdf");
    }

    public long getFileSizeInMB(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        return file.length() / (1024 * 1024); // Convertir en MB
    }
}