package com.unimage.controller;

import com.unimage.dto.ApiResponse;
import com.unimage.dto.ScreenshotDto;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.IIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 스크린샷 파일의 저장, 조회, 수정, 삭제와 같은 작업을 처리하는 API를 제공합니다.
 *
 * <p>주요 기능:
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
   * 전달된 스크린샷 파일을 지정된 파일명으로 저장합니다. 파일명이 null로 전달될 경우 UUID를 생성하여 저장합니다.
   *
   * @param email    사용자 고유 식별자
   * @param file     저장할 스크린샷 파일
   * @param filename 저장할 파일명 (null일 경우 UUID 부여)
   * @return {@link ApiResponse}를 통해 성공 시 200 OK 반환, 실패 시 상태 코드와 에러 메시지 반환
   */
  @PostMapping("/upload")
  public ApiResponse<Void> uploadScreenshot(
      @RequestParam("email") String email,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "filename", required = false) String filename) {
    if (file == null || file.isEmpty()) {
      return ApiResponse.error("screenshot file is empty", HttpStatus.NOT_ACCEPTABLE);
    }

    if (filename == null || filename.isEmpty()) {
      filename = UUID.randomUUID().toString();
    }

    if (new File(UPLOAD_DIR + filename).exists()) {
      return ApiResponse.error(filename + " already exists", HttpStatus.CONFLICT);
    }

    File uploadDir = new File(UPLOAD_DIR);
    if (!uploadDir.exists()) {
      uploadDir.mkdirs();
    }

    try {
      file.transferTo(new File(UPLOAD_DIR + filename));
      return ApiResponse.success();
    } catch (IIOException e) {
      e.printStackTrace();
      return ApiResponse.error("file is damaged or not supported", HttpStatus.BAD_REQUEST);
    } catch (ClosedChannelException e) {
      e.printStackTrace();
      return ApiResponse.error("file stream is closed", HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (FileSystemException e) {
      e.printStackTrace();
      return ApiResponse.error("file needs permission or is locked",
          HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (SocketException e) {
      e.printStackTrace();
      return ApiResponse.error("network error occurred", HttpStatus.SERVICE_UNAVAILABLE);
    } catch (InterruptedByTimeoutException e) {
      e.printStackTrace();
      return ApiResponse.error("timeout occurred", HttpStatus.GATEWAY_TIMEOUT);
    } catch (InterruptedIOException e) {
      e.printStackTrace();
      return ApiResponse.error("interrupt occurred: try it again", HttpStatus.SERVICE_UNAVAILABLE);
    } catch (IOException e) {
      e.printStackTrace();
      return ApiResponse.error("unexpected error " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (IllegalStateException e) {
      e.printStackTrace();
      return ApiResponse.error("file already stored", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 저장된 파일들을 날짜순으로 전부 불러옵니다.
   *
   * @param email 사용자 고유 식별자
   * @return {@link ApiResponse}를 통해 성공 시 200 OK와 {@link ScreenshotDto<List>} 반환, 실패 시 상태코드와 에러 메시지
   * 반환 {@link ScreenshotDto}는 스크린샷의 파일명, 생성일자를 포함
   */
  @GetMapping("/all")
  public ApiResponse<List<ScreenshotDto>> getAllScreenshots(
      @RequestParam("email") String email) {
    File[] files = new File(UPLOAD_DIR).listFiles();

    if (files == null) {
      return ApiResponse.error("can't load screenshots", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    List<ScreenshotDto> screenshotList = Arrays.stream(files)
        .filter(File::isFile)
        .map(file -> new ScreenshotDto(file.getName()))
        .collect(Collectors.toList());
    return ApiResponse.success(screenshotList);
  }

  /**
   * 지정된 파일 정보를 불러옵니다.
   *
   * @param email    사용자 고유 식별자
   * @param filename 반환할 파일의 파일명
   * @return {@link ApiResponse}를 통해 성공 시 200 OK와 {@link ScreenshotDto} 반환, 실패 시 상태 코드와 에러 메시지 반환
   * {@link ScreenshotDto}는 스크린샷의 파일명, 생성일자를 포함
   */
  @GetMapping("/{filename}")
  public ApiResponse<ScreenshotDto> getScreenshot(
      @RequestParam("email") String email,
      @PathVariable String filename) {
    if (filename == null || filename.isEmpty()) {
      return ApiResponse.error("filename needs at least 1 character", HttpStatus.NOT_ACCEPTABLE);
    }

    File file = new File(UPLOAD_DIR + filename);
    if (!file.exists()) {
      return ApiResponse.error(filename + " does not exist", HttpStatus.NOT_FOUND);
    }

    ScreenshotDto screenshotDto = new ScreenshotDto(file.getName());
    return ApiResponse.success(screenshotDto);
  }

  /**
   * 지정된 파일의 파일명을 수정합니다.
   *
   * @param email       사용자 고유 식별자
   * @param filename    기존 파일명
   * @param newFilename 새 파일명
   * @return {@link ApiResponse}를 통해 성공 시 200 OK 반환, 실패 시 상태 코드와 에러 메시지 반환
   */
  @PutMapping("/modify")
  public ApiResponse<Void> modifyScreenshot(
      @RequestParam("email") String email,
      @RequestParam("filename") String filename,
      @RequestParam("newFilename") String newFilename) {
    if (filename == null || filename.isEmpty()) {
      return ApiResponse.error("filename needs at least 1 character", HttpStatus.NOT_ACCEPTABLE);
    }

    if (newFilename == null || newFilename.isEmpty()) {
      return ApiResponse.error("newFilename needs at least 1 character", HttpStatus.NOT_ACCEPTABLE);
    }

    File originalFile = new File(UPLOAD_DIR + filename);
    File newFile = new File(UPLOAD_DIR + newFilename);
    if (!originalFile.exists()) {
      return ApiResponse.error(filename + " does not exist", HttpStatus.NOT_FOUND);
    }
    if (newFile.exists()) {
      return ApiResponse.error(newFilename + " already exists", HttpStatus.CONFLICT);
    }

    if (originalFile.renameTo(newFile)) {
      return ApiResponse.success();
    } else {
      return ApiResponse.error(filename + " is locked", HttpStatus.FORBIDDEN);
    }
  }

  /**
   * 전달된 파일 리스트를 삭제합니다. 삭제 중 오류 발생 시 모든 파일을 복원합니다.
   *
   * @param email    사용자 고유 식별자
   * @param fileList 삭제할 파일명을 담고있는 리스트
   * @return {@link ApiResponse}를 통해 성공 시 200 OK 반환, 실패 시 상태 코드와 에러 메시지 반환
   */
  @DeleteMapping("/delete")
  public ApiResponse<Void> deleteScreenshot(
      @RequestParam("email") String email,
      @RequestParam("fileList") List<String> fileList) {
    if (fileList == null || fileList.isEmpty()) {
      return ApiResponse.error("fileList needs at least 1 file", HttpStatus.NOT_ACCEPTABLE);
    }

    // 리스트 내의 파일 이름의 유효성 검사
    for (int i = 0; i < fileList.size(); i++) {
      String filename = fileList.get(i);
      if (filename == null || filename.isEmpty()) {
        return ApiResponse.error("index " + i + " data in fileList needs at least 1 character",
            HttpStatus.NOT_ACCEPTABLE);
      }

      if (!new File(UPLOAD_DIR + filename).exists()) {
        return ApiResponse.error(filename + "doesn't exist", HttpStatus.NOT_FOUND);
      }
    }

    // 파일 백업
    File backupDir = new File(BACKUP_DIR);
    if (!backupDir.exists()) {
      backupDir.mkdirs();
    }

    List<File> backupFiles = new ArrayList<>();
    for (String filename : fileList) {
      File originalFile = new File(UPLOAD_DIR + filename);
      File backupFile = new File(BACKUP_DIR + filename);

      try {
        Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        backupFiles.add(backupFile);
      } catch (UnsupportedOperationException e) {
        e.printStackTrace();
        logger.error("Backup failed file: {}. Reason: {} read-only. Exception: {}", filename,
            backupFile.getName(), e.getMessage());
      } catch (SocketException e) {
        e.printStackTrace();
        logger.error("Backup failed file: {}. Reason: Network error. Exception: {}", filename,
            e.getMessage());
      } catch (InterruptedByTimeoutException e) {
        e.printStackTrace();
        logger.error("Backup failed file: {}. Reason: Timeout. Exception: {}", filename,
            e.getMessage());
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Backup failed file: {}. Reason: Unexpected. Exception: {}", filename,
            e.getMessage());
      }
    }
    if (backupFiles.size() != fileList.size()) {
      deleteBackupFiles(backupFiles);
      return ApiResponse.error("Error occurred making backup file. Try it later",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 파일 제거 실패 시, 삭제했던 파일 복구 후 복구 성공한 백업 파일은 제거
    for (int i = 0; i < fileList.size(); i++) {
      String filename = fileList.get(i);

      if (!new File(UPLOAD_DIR + filename).delete()) {
        for (int j = 0; j < i; j++) {
          boolean isCopySuccessful = false;
          File backupFile = backupFiles.get(j);
          File originalFile = new File(UPLOAD_DIR + backupFile.getName());

          try {
            Files.copy(backupFile.toPath(), originalFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            isCopySuccessful = true;
          } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: {} read-only. Exception: {}", filename,
                originalFile.getName(), e.getMessage());
          } catch (SocketException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: Network error. Exception: {}", filename,
                e.getMessage());
          } catch (InterruptedByTimeoutException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: Timeout. Exception: {}", filename,
                e.getMessage());
          } catch (IOException e) {
            e.printStackTrace();
            logger.error("Restore failed file: {}. Reason: Unexpected. Exception: {}", filename,
                e.getMessage());
          } finally {
            if (backupFile.exists() && isCopySuccessful) {
              if (!backupFile.delete()) {
                logger.error("Failed to delete restored file in backup: {}", backupFile.getName());
              }
            }
          }
        }
        logger.error("Failed to delete file: {}", filename);
        return ApiResponse.error("Error occurred deleting file. Try it later",
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    deleteBackupFiles(backupFiles);

    return ApiResponse.success();
  }

  private void deleteBackupFiles(List<File> backupFiles) {
    for (File backupFile : backupFiles) {
      if (!backupFile.delete()) {
        logger.error("Failed to delete backup file: {}", backupFile.getName());
      }
    }
  }
}
