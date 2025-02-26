package com.unimage.exception;

public class CreateBackUpFileException extends Exception {

  public final Cause cause;
  public final String filename;

  public enum Cause {
    READ_ONLY_BACK_UP_FILE,
    NETWORK_TIMEOUT,
    ASYNCHRONOUS_TIMEOUT,
    UNSPECIFIED
  }

  public CreateBackUpFileException(Cause cause, String filename, Throwable e) {
    super(e);
    this.cause = cause;
    this.filename = filename;
  }
}
