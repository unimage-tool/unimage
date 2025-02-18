package com.unimage.exception;

public class CreateBackUpFileException extends Exception {

  public enum Cause {
    READ_ONLY_BACK_UP_FILE,
    NETWORK_TIMEOUT,
    ASYNCHRONOUS_TIMEOUT,
    UNSPECIFIED
  }

  public CreateBackUpFileException(Cause exceptionType, String message, Throwable cause) {
    super(message, cause);
  }

}
