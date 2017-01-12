package com.nikogura.boxpile.exception;

/**
 * Created by nikogura on 9/4/15.
 */
public class DockerRMIException extends Exception {
    public DockerRMIException() {super();}
    public DockerRMIException(String message) {super(message);}
    public DockerRMIException(String message, Throwable cause) {super(message, cause);}
    public DockerRMIException(Throwable cause) {super(cause);}

}
