package com.unimage.controller;

import com.unimage.dto.ScreenshotDto;
import com.unimage.exception.CreateBackUpFileException;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.IIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 스크린샷 파일의 저장, 조회, 수정, 삭제와 같은 작업을 처리하는 API를 제공합니다.
 *
 * <p>주요 기능:</p>
 *
 * <ul>
 *     <li>전달된 한 장의 스크린샷 저장.</li>
 *     <li>저장된 모든 스크린샷 조회.</li>
 *     <li>선택한 하나의 스크린샷 조회.</li>
 *     <li>스크린샷의 파일명 수정.</li>
 *     <li>리스트 단위로 선택된 스크린샷 삭제, 실패 시 자동 복원.</li>
 * </ul>
 */
@RestController
@RequestMapping("/screenshot")
public class ScreenshotController {

  private static final Logger logger = LoggerFactory.getLogger(ScreenshotController.class);
  private static final String UPLOAD_DIR = "C:/Server/Unimage/screenshot/";
  private static final String BACKUP_DIR = UPLOAD_DIR + "backup/";

  /**
   * 전달된 스크린샷 파일을 저장합니다.
   *
   * @param file 저장할 스크린샷 파일
   * @return 성공 시 응답으로 201 CREATED와 생성된 {@link ScreenshotDto} 반환, 실패 시 응답으로 상태 코드와 에러 메시지 반환
   */
  @PostMapping("/upload")
  public ResponseEntity<Map<String, Object>> uploadScreenshot(
      @RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Screenshot file is empty"));
    }

    String filename = file.getOriginalFilename();
    if (filename == null || filename.isEmpty()) {
      filename = UUID.randomUUID().toString();
      filename += ".jpeg";
    }

    String imageType = filename.substring(filename.lastIndexOf(".") + 1);
    if (!imageType.equals("jpg") && !imageType.equals("jpeg")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Only 'jpg' or 'jpeg' image type supported"));
    }

    if (new File(UPLOAD_DIR + filename).exists()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", filename + " already exists"));
    }

    File uploadDir = new File(UPLOAD_DIR);
    if (!uploadDir.exists()) {
      uploadDir.mkdirs();
    }

    try {
      file.transferTo(new File(UPLOAD_DIR + filename));
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("data", new ScreenshotDto(filename)));
    } catch (IIOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "File is damaged of not supported"));
    } catch (ClosedChannelException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "File stream is closed"));
    } catch (FileSystemException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "File needs permission or is locked"));
    } catch (SocketException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("message", "Network error occurred"));
    } catch (InterruptedByTimeoutException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
          .body(Map.of("message", "Timeout occurred"));
    } catch (InterruptedIOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("message", "Interrupt occurred: try it again"));
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Unexpected error " + e.getMessage()));
    } catch (IllegalStateException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "File already stored"));
    }
  }

  /**
   * 저장된 모든 파일들을 불러옵니다.
   *
   * @return 성공 시 응답으로 200 OK와 {@link List<ScreenshotDto>} 반환, 실패 시 응답으로 상태 코드와 에러 메시지 반환
   */
  @GetMapping("/all")
  public ResponseEntity<Map<String, Object>> getAllScreenshots() {
    File[] files = new File(UPLOAD_DIR).listFiles();

    if (files == null) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Can't load screenshots"));
    }

    List<ScreenshotDto> screenshotList = Arrays.stream(files)
        .filter(File::isFile)
        .map(file -> new ScreenshotDto(file.getName()))
        .collect(Collectors.toList());
    return ResponseEntity.status(HttpStatus.OK).body(Map.of("data", screenshotList));
  }

  /**
   * 지정된 파일 정보를 불러옵니다.
   *
   * @param filename 반환할 파일의 파일명
   * @return 성공 시 응답으로 200 OK와 {@link ScreenshotDto} 반환, 실패 시 응답으로 상태 코드와 에러 메시지 반환
   */
  @GetMapping("/{filename}")
  public ResponseEntity<Map<String, Object>> getScreenshot(@PathVariable String filename) {
    if (filename == null || filename.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "File name needs at least 1 character"));
    }

    if (!new File(UPLOAD_DIR + filename).exists()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("message", filename + " does not exist"));
    }

    return ResponseEntity.status(HttpStatus.OK).body(Map.of("data", new ScreenshotDto(filename)));
  }

  /**
   * 지정된 파일의 파일명을 수정합니다.
   *
   * @param filename    기존 파일명
   * @param newFilename 새 파일명
   * @return 성공 시 응답으로 200 OK와 {@link ScreenshotDto} 반환, 실패 시 응답으로 상태 코드와 에러 메시지 반환
   */
  @PutMapping("/modify")
  public ResponseEntity<Map<String, Object>> modifyScreenshot(
      @RequestParam("filename") String filename,
      @RequestParam("newFilename") String newFilename) {
    if (filename == null || filename.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "File name needs at least 1 character"));
    }

    if (newFilename == null || newFilename.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "New file name needs at least 1 character"));
    }

    File originalFile = new File(UPLOAD_DIR + filename);
    File newFile = new File(UPLOAD_DIR + newFilename);
    if (!originalFile.exists()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("message", filename + " does not exist"));
    }
    if (newFile.exists()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", newFilename + " already exists"));
    }

    if (originalFile.renameTo(newFile)) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(Map.of("data", new ScreenshotDto(newFilename)));
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", filename + " is locked"));
    }
  }

  /**
   * 전달된 파일 리스트를 삭제합니다. 삭제 중 오류 발생 시 삭제된 파일을 복원합니다.
   *
   * @param fileList 삭제할 파일명을 담고있는 리스트
   * @return 성공 시 응답으로 204 NO_CONTENT 반환, 실패 시 응답으로 상태 코드와 에러 메시지 반환
   */
  @DeleteMapping("/delete")
  public ResponseEntity<Map<String, String>> deleteScreenshot(
      @RequestParam("fileList") List<String> fileList) {
    if (fileList == null || fileList.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "File list needs at least 1 file"));
    }

    // 리스트 내의 파일 이름의 유효성 검사
    for (int i = 0; i < fileList.size(); i++) {
      String filename = fileList.get(i);
      if (filename == null || filename.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            Map.of("message", "Index " + i + " data in file list needs at least 1 character"));
      }

      if (!new File(UPLOAD_DIR + filename).exists()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", filename + " doesn't exist"));
      }
    }

    // 파일 백업
    List<File> backUpFiles = new ArrayList<>();
    try {
      createBackUpFiles(fileList, backUpFiles);
    } catch (CreateBackUpFileException e) {
      e.printStackTrace();
      deleteBackupFiles(backUpFiles);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Error occurred while creating back up files"));
    }

    // 파일 제거 실패 시, 삭제 파일 복구 후 백업 파일 제거
    for (String filename : fileList) {
      if (!new File(UPLOAD_DIR + filename).delete()) {
        restoreDeletedFiles(filename, backUpFiles);
        deleteBackupFiles(backUpFiles);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message",
            "Failed to delete " + filename + ": file in use or insufficient permissions"));
      }
    }

    deleteBackupFiles(backUpFiles);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
  }

  private void restoreDeletedFiles(String filename, List<File> backUpFiles) {
    for (File backUpFile : backUpFiles) {
      if (backUpFile.getName().equals(filename)) {
        break;
      }
      try {
        Files.copy(backUpFile.toPath(), new File(UPLOAD_DIR + backUpFile.getName()).toPath(),
            StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createBackUpFiles(List<String> fileList, List<File> backupFiles)
      throws CreateBackUpFileException {
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
        throw new CreateBackUpFileException("Read-only file error during back up: " + filename, e);
      } catch (SocketException e) {
        throw new CreateBackUpFileException("Network error during back up: " + filename, e);
      } catch (InterruptedByTimeoutException e) {
        throw new CreateBackUpFileException("Timeout error during backup: " + filename, e);
      } catch (IOException e) {
        throw new CreateBackUpFileException("Unexpected I/O error during back up: " + filename, e);
      }
    }
  }

  private void deleteBackupFiles(List<File> backupFiles) {
    for (File backupFile : backupFiles) {
      if (!backupFile.delete()) {
        logger.error("Failed to delete back up file while deleting all back up files: {}",
            backupFile.getName());
      }
    }
  }
}
