package bg.softuni.parkzone.exception;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {

    private final String errorCode;
    private final String errorTitle;

    public ApplicationException(String message, String errorCode, String errorTitle) {
        super(message);
        this.errorCode = errorCode;
        this.errorTitle = errorTitle;
    }
}
