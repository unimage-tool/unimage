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

    // 사용자의 모든 스크린샷 날짜순 조회 및 경로 전달하여 다운로드 가능
    @GetMapping("/all")
    public ResponseEntity<List<ScreenshotDto>> getAllScreenshots(
            @RequestParam("email") String email) {
        File uploadDir = new File(UPLOAD_DIR);
        File[] files = uploadDir.listFiles();

        if (files == null || files.length == 0) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<ScreenshotDto> screenshotList = Arrays.stream(files)
                .filter(File::isFile)
                .map(file -> new ScreenshotDto(
                        "fileName",
                        "C:/Server/Unimage~~",
                        "2024-09-23"
                        // fileName, 생성시 저장한 파일 명
                        // filePath, 생성시 저장해놓은 경로
                        // date , 생성 날짜 연,월,일
                ))
                .sorted(Comparator.comparing(ScreenshotDto::getDate).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(screenshotList);
    }

    // 사용자가 선택한 스크린샷 조회
    @GetMapping("/{filename}")
    public ResponseEntity<ScreenshotDto> getScreenshot(
            @RequestParam("email") String email,
            @PathVariable String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) {
            ScreenshotDto error = new ScreenshotDto(
                    "no " + filename,
                    "",
                    null
            );
            return ResponseEntity.badRequest().body(error);
        }

        ScreenshotDto screenshotDto = new ScreenshotDto(
                file.getName(),
                file.getAbsolutePath(),
                "2024-09-23"
        );
        return ResponseEntity.ok(screenshotDto);
    }

    // 찍은 스크린샷 프론트에서 공유 작업하면 링크 전달
    @GetMapping("/{filename}")
    public ResponseEntity<String> getScreenshotLink(
            @RequestParam("email") String email,
            @PathVariable String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) {
            return ResponseEntity.badRequest().body("file doesn't exist");
        }
        return ResponseEntity.ok(file.getAbsolutePath());
    }

    // 파일명 수정
    @PutMapping("/modify")
    public ResponseEntity<String> modifyScreenshot(
            @RequestParam("email") String email,
            @RequestParam("filename") String filename,
            @RequestParam("newFilename") String newFilename) {

        if (filename == null || filename.isEmpty()) {
            return ResponseEntity.badRequest().body("filename invalid");
        }

        if (newFilename == null || newFilename.isEmpty()) {
            return ResponseEntity.badRequest().body("newFilename invalid");
        }

        File originalFile = new File(UPLOAD_DIR + filename);
        File newFile = new File(UPLOAD_DIR + newFilename);
        if (!originalFile.exists()) {
            return ResponseEntity.badRequest().body("originalFile not found");
        }
        if (newFile.exists()) {
            return ResponseEntity.badRequest().body("newFile already exists");
        }

        try {
            if (originalFile.renameTo(newFile)) {
                return ResponseEntity.ok("modifying filename successful");
            } else {
                return ResponseEntity.status(500).body("modifying " + filename + " to " + newFilename + " failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("modifying " + filename + " to " + newFilename + " error occurred");
        }
    }

    // 1개 이상의 파일 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteScreenshot(
            @RequestParam("email") String email,
            @RequestParam("fileList") List<String> fileList) {
        if (fileList == null || fileList.isEmpty()) {
            return ResponseEntity.badRequest().body("fileList invalid");
        }

        for (String filename : fileList) {
            if (filename == null || filename.isEmpty()) {
                return ResponseEntity.badRequest().body("filename invalid");
            }

            File deleteFile = new File(UPLOAD_DIR + filename);
            if (!deleteFile.exists()) {
                return ResponseEntity.badRequest().body(filename + " doesn't exist");
            }

            try {
                if (!deleteFile.delete()) {
                    return ResponseEntity.status(500).body("deleting " + filename + " failed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("deleting " + filename + " error occurred");
            }
        }
        return ResponseEntity.ok("delete successful");
    }
}