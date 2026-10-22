package com.hotel.qrorder.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Dish ki photo upload. Sirf asli PNG/JPEG/WEBP (file ke andar ka signature check hota hai), max 2 MB. */
@Service
public class UploadService {
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw bad("File khali hai");
        if (file.getSize() > 2L * 1024 * 1024) throw bad("Photo 2 MB se chhoti honi chahiye");
        byte[] h;
        try (InputStream in = file.getInputStream()) { h = in.readNBytes(12); }
        String ext = detect(h);
        if (ext == null) throw bad("Sirf PNG, JPG ya WEBP photo chalegi");

        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) { Files.copy(in, dir.resolve(name)); }
        return "/uploads/" + name;
    }

    private static String detect(byte[] h) {
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        return null;
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
}
