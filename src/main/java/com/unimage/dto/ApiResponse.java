package com.unimage.dto;

/**
 * API 응답을 나타내는 클래스입니다.
 *
 * @param <T> 응답 데이터의 타입
 */
public class ApiResponse<T> {

  /** 응답 메시지 */
  public final String message;

  /** 응답 데이터 */
  public final T data;

  /**
   * ApiResponse 생성자
   *
   * @param message 응답 메시지
   * @param data    응답 데이터
   */
  private ApiResponse(String message, T data) {
    this.message = message;
    this.data = data;
  }

  /**
   * 성공적인 응답을 생성하는 static 메서드
   *
   * @param <T>  응답 데이터 유형
   * @param data 응답 데이터
   * @return 성공적인 응답 객체
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(null, data);
  }

  /**
   * 실패 응답을 생성하는 static 메서드
   *
   * @param message 실패 메시지
   * @return 실패 응답 객체
   */
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(message, null);
  }
}
