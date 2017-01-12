package com.nikogura.boxpile.exception;

/**
 * Created by nikogura on 9/4/15.
 */
public class DockerBuildException extends Exception {
    public DockerBuildException() {super();}
    public DockerBuildException(String message) {super(message);}
    public DockerBuildException(String message, Throwable cause) {super(message, cause);}
    public DockerBuildException(Throwable cause) {super(cause);}

}
