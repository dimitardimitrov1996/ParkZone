package bg.softuni.parkzone.exception;

public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException(String message) {
        super(message, "400", "Business rule violation");
    }
}
