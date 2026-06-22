document.addEventListener("DOMContentLoaded", function () {
    const reservationTypeSelect = document.getElementById("reservationType");
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    if (!reservationTypeSelect || !startDateInput || !endDateInput) {
        return;
    }

    function formatDateTimeLocal(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hours = String(date.getHours()).padStart(2, "0");
        const minutes = String(date.getMinutes()).padStart(2, "0");

        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }

    function calculateEndDate() {
        if (!startDateInput.value) {
            return;
        }

        const startDate = new Date(startDateInput.value);
        const reservationType = reservationTypeSelect.value;

        if (reservationType === "MONTHLY") {
            startDate.setMonth(startDate.getMonth() + 1);
            endDateInput.value = formatDateTimeLocal(startDate);
            endDateInput.readOnly = true;
            return;
        }

        if (reservationType === "YEARLY") {
            startDate.setFullYear(startDate.getFullYear() + 1);
            endDateInput.value = formatDateTimeLocal(startDate);
            endDateInput.readOnly = true;
            return;
        }

        endDateInput.readOnly = false;
    }

    function updateEndDateModeWithoutChangingExistingValue() {
        const reservationType = reservationTypeSelect.value;

        if (reservationType === "MONTHLY" || reservationType === "YEARLY") {
            endDateInput.readOnly = true;
            return;
        }

        endDateInput.readOnly = false;
    }

    reservationTypeSelect.addEventListener("change", calculateEndDate);
    startDateInput.addEventListener("change", calculateEndDate);

    updateEndDateModeWithoutChangingExistingValue();
});