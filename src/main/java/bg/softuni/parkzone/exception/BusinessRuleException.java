package bg.softuni.parkzone.exception;

public class BusinessRuleException extends IllegalArgumentException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
