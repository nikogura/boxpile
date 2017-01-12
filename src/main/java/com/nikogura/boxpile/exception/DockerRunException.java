package com.nikogura.boxpile.exception;

/**
 * Created by nikogura on 9/21/15.
 */
public class DockerRunException extends Exception {
    public DockerRunException() {super();}
    public DockerRunException(String message) {super(message);}
    public DockerRunException(String message, Throwable cause) {super(message, cause);}
    public DockerRunException(Throwable cause) {super(cause);}
}
