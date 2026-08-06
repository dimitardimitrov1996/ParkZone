package bg.softuni.parkzone.exception.billing;

import bg.softuni.parkzone.exception.ApplicationException;

public class BillingServiceUnavailableException extends ApplicationException {

    public BillingServiceUnavailableException() {
        super("Billing service is currently unavailable.", "503", "Billing service unavailable");
    }
}
