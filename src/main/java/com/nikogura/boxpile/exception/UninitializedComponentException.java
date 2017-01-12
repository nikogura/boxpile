package com.nikogura.boxpile.exception;

/**
 * Created by nikogura on 9/4/15.
 */
public class UninitializedComponentException extends Exception {
    public UninitializedComponentException() {super();}
    public UninitializedComponentException(String message) {super(message);}
    public UninitializedComponentException(String message, Throwable cause) {super(message, cause);}
    public UninitializedComponentException(Throwable cause) {super(cause);}

}
