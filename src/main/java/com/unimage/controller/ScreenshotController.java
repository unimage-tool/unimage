package com.unimage.controller;

import com.unimage.dto.ScreenshotDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/screenshot")
public class ScreenshotController {

    private static final String UPLOAD_DIR = "C:/Server/Unimage/screenshot/";

    // 스크린샷, 스크린샷 파일명 전달로 스크린샷 저장
    @PostMapping("/upload")
    public ResponseEntity<String> uploadScreenshot(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("screenshot empty");
        }

        if (filename == null || filename.isEmpty()) {
            filename = UUID.randomUUID().toString();
        }

        File checkFileExist = new File(UPLOAD_DIR + filename);
        if (checkFileExist.exists()) {
            return ResponseEntity.badRequest().body("screenshot already exists");
        }

        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File destinationFile = new File(UPLOAD_DIR + filename);
            file.transferTo(destinationFile);

            return ResponseEntity.ok("Uploading screenshot successful");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Uploading screenshot error occurred");
        }
    }
}