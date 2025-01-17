package com.unimage.controller;

import com.unimage.dto.ApiResponse;
import com.unimage.dto.ScreenshotDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/screenshot")
public class ScreenshotController {
    private static final String UPLOAD_DIR = "C:/Server/Unimage/screenshot/";
    private static final String BACKUP_DIR = UPLOAD_DIR + "backup/";

    /**
     * 사용자가 찍은 스크린샷을 저장합니다.
     *
     * @param email    사용자를 식별하는데 쓰이고, null일 수 없습니다.
     * @param file     스크린샷을 저장하는데 쓰이며, null일 수 없습니다.
     * @param filename 스크린샷의 파일명을 지정하는데 쓰이며, null로 전달될 경우 임시 UUID가 부여됩니다.
     * @return {@link ApiResponse}를 통해 성공/실패 여부를 반환합니다.
     * @throws IIOException                  파일이 손상됐거나 전달된 이미지 형식을 지원하지 않는 경우
     * @throws ClosedChannelException        파일 스트림이 닫혀 있는 경우
     * @throws FileNotFoundException         이미지를 전송하는 파일 경로가 존재하지 않는 경우
     * @throws FileSystemException           파일이 잠겼거나 파일 접근 권한이 필요한 경우
     * @throws SocketException               이미지 저장 중에 네트워크가 끊긴 경우
     * @throws InterruptedByTimeoutException 이미지 저장 요청 시간이 초과된 경우
     * @throws InterruptedIOException        파일 전송 중 중단된 경우
     * @throws IOException                   I/O 예외 중 예상치 못한 예외가 발생한 경우
     * @throws IllegalStateException         저장 경로에 파일이 이미 저장되어 있는 경우
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> uploadScreenshot(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename) {
        if (file == null || file.isEmpty()) {
            String message = "screenshot file is empty";
            return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
        }

        if (filename == null || filename.isEmpty()) {
            filename = UUID.randomUUID().toString();
        }

        File checkFileExist = new File(UPLOAD_DIR + filename);
        if (checkFileExist.exists()) {
            String message = filename + " already exists";
            return new ResponseEntity<>(ApiResponse.error(409, message), HttpStatus.CONFLICT);
        }

        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File destinationFile = new File(UPLOAD_DIR + filename);
            file.transferTo(destinationFile);

            return new ResponseEntity<>(ApiResponse.success(200, null), HttpStatus.OK);
        } catch (IIOException e) {
            e.printStackTrace();
            String message = "file is damaged or not supported";
            return new ResponseEntity<>(ApiResponse.error(400, message), HttpStatus.BAD_REQUEST);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
            String message = "file stream is closed";
            return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            String message = "destination path doesn't exist";
            return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FileSystemException e) {
            e.printStackTrace();
            String message = "file needs permission or is locked";
            return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SocketException e) {
            e.printStackTrace();
            String message = "network error occurred";
            return new ResponseEntity<>(ApiResponse.error(503, message), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (InterruptedByTimeoutException e) {
            e.printStackTrace();
            String message = "timeout occurred";
            return new ResponseEntity<>(ApiResponse.error(504, message), HttpStatus.GATEWAY_TIMEOUT);
        } catch (InterruptedIOException e) {
            e.printStackTrace();
            String message = "interrupt occurred: try it again";
            return new ResponseEntity<>(ApiResponse.error(503, message), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (IOException e) {
            e.printStackTrace();
            String message = "unexpected error " + e.getMessage();
            return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            String message = "file already stored";
            return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 날짜순으로 저장된 스크린샷을 전부 불러옵니다.
     *
     * @param email 사용자를 식별하는데 쓰이고, null일 수 없습니다.
     * @return {@link ApiResponse}를 통해 {@link ScreenshotDto} 리스트를 반환합니다.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ScreenshotDto>>> getAllScreenshots(
            @RequestParam("email") String email) {
        File uploadDir = new File(UPLOAD_DIR);
        File[] files = uploadDir.listFiles();

        if (files == null) {
            String message = "can't load screenshots";
            return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<ScreenshotDto> screenshotList = Arrays.stream(files)
                .filter(File::isFile)
                .map(file -> new ScreenshotDto(
                        "fileName",
                        "C:/Server/Unimage~~",
                        "2024-09-23"
                ))
                .sorted(Comparator.comparing(
                        (ScreenshotDto screenshot) -> LocalDate.parse(screenshot.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                ).reversed())
                .collect(Collectors.toList());
        return new ResponseEntity<>(ApiResponse.success(200, screenshotList), HttpStatus.OK);
    }

    /**
     * 지정된 스크린샷 정보를 불러옵니다.
     *
     * @param email    사용자를 식별하는데 쓰이고, null일 수 없습니다.
     * @param filename 파일을 식별하는데 사용되고, null일 수 없습니다.
     * @return {@link ApiResponse}를 통해 {@link ScreenshotDto} 객체를 반환합니다.
     */
    @GetMapping("/{filename}")
    public ResponseEntity<ApiResponse<ScreenshotDto>> getScreenshot(
            @RequestParam("email") String email,
            @PathVariable String filename) {
        if (filename == null || filename.isEmpty()) {
            String message = "filename needs at least 1 character";
            return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
        }

        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) {
            String message = filename + " does not exist";
            return new ResponseEntity<>(ApiResponse.error(404, message), HttpStatus.NOT_FOUND);
        }

        ScreenshotDto screenshotDto = new ScreenshotDto(
                file.getName(),
                file.getAbsolutePath(),
                "2024-09-23"
        );
        return new ResponseEntity<>(ApiResponse.success(200, screenshotDto), HttpStatus.OK);
    }

    /**
     * 스크린샷 링크를 불러옵니다.
     *
     * @param email    사용자를 식별하는데 쓰이고, null일 수 없습니다.
     * @param filename 파일을 식별하는데 사용되고, null일 수 없습니다.
     * @return {@link ApiResponse}를 통해 성공/실패 여부를 반환합니다.
     */
    @GetMapping("/{filename}")
    public ResponseEntity<ApiResponse<String>> getScreenshotLink(
            @RequestParam("email") String email,
            @PathVariable String filename) {
        if (filename == null || filename.isEmpty()) {
            String message = "filename needs at least 1 character";
            return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
        }

        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) {
            String message = filename + " does not exist";
            return new ResponseEntity<>(ApiResponse.error(404, message), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ApiResponse.success(200, file.getAbsolutePath()), HttpStatus.OK);
    }

    /**
     * 지정된 파일을 파일명을 수정합니다.
     *
     * @param email       사용자를 식별하는데 쓰이고, null일 수 없습니다.
     * @param filename    수정될 파일의 기존 파일명이고, null일 수 없습니다.
     * @param newFilename 수정될 파일의 새 파일명이고, null일 수 없습니다.
     * @return {@link ApiResponse}를 통해 성공/실패 여부를 반환합니다.
     */
    @PutMapping("/modify")
    public ResponseEntity<ApiResponse<String>> modifyScreenshot(
            @RequestParam("email") String email,
            @RequestParam("filename") String filename,
            @RequestParam("newFilename") String newFilename) {
        if (filename == null || filename.isEmpty()) {
            String message = "filename needs at least 1 character";
            return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
        }

        if (newFilename == null || newFilename.isEmpty()) {
            String message = "newFilename needs at least 1 character";
            return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
        }

        File originalFile = new File(UPLOAD_DIR + filename);
        File newFile = new File(UPLOAD_DIR + newFilename);
        if (!originalFile.exists()) {
            String message = filename + " does not exist";
            return new ResponseEntity<>(ApiResponse.error(404, message), HttpStatus.NOT_FOUND);
        }
        if (newFile.exists()) {
            String message = newFilename + " already exists";
            return new ResponseEntity<>(ApiResponse.error(409, message), HttpStatus.CONFLICT);
        }

        if (originalFile.renameTo(newFile)) {
            return new ResponseEntity<>(ApiResponse.success(200, null), HttpStatus.OK);
        } else {
            String message = filename + " is locked";
            return new ResponseEntity<>(ApiResponse.error(403, message), HttpStatus.FORBIDDEN);
        }
    }

    // 스크린샷 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<List<String>>> deleteScreenshot(
            @RequestParam("email") String email,
            @RequestParam("fileList") List<String> fileList) {
        if (fileList == null || fileList.isEmpty()) {
            String message = "fileList needs at least 1 file";
            return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
        }

        // 리스트 내의 파일 이름의 유효성 검사
        for (int i = 0; i < fileList.size(); i++) {
            String filename = fileList.get(i);
            if (filename == null || filename.isEmpty()) {
                String message = "index " + i + " data in fileList needs at least 1 character";
                return new ResponseEntity<>(ApiResponse.error(406, message), HttpStatus.NOT_ACCEPTABLE);
            }

            File file = new File(UPLOAD_DIR + filename);
            if (!file.exists()) {
                String message = filename + "doesn't exist";
                return new ResponseEntity<>(ApiResponse.error(404, message), HttpStatus.NOT_FOUND);
            }
        }

        // 파일 백업
        List<File> backupFiles = new ArrayList<>();
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        for (String filename : fileList) {
            File originalFile = new File(UPLOAD_DIR + filename);
            File backupFile = new File(BACKUP_DIR + filename);

            try {
                Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                backupFiles.add(backupFile);
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                String message = "Failed to make backup file: " + filename + " is read-only";
                return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
                String message = "Failed to make backup file: not enough storage";
                return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // 파일 삭제
        for (int i = 0; i < fileList.size(); i++) {
            String filename = fileList.get(i);
            File file = new File(UPLOAD_DIR + filename);

            try {
                if (!file.delete()) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                for (int j = 0; j < i; j++) {
                    File backupFile = backupFiles.get(j);
                    try {
                        Files.copy(backupFile.toPath(), new File(UPLOAD_DIR + backupFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (UnsupportedOperationException ex) {
                        e.printStackTrace();
                        String message = "Failed to restore file: " + filename + " is read-only";
                        return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
                    } catch (IOException ex) {
                        e.printStackTrace();
                        String message = "Failed to restore file: not enough storage";
                        return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                String message = filename + " not deleted: file using or need permission";
                return new ResponseEntity<>(ApiResponse.error(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(ApiResponse.success(200, null), HttpStatus.OK);
    }
}
