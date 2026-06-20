document.addEventListener("DOMContentLoaded", function () {
    const reservationType = document.getElementById("reservationType");
    const startDate = document.getElementById("startDate");
    const endDate = document.getElementById("endDate");

    if (!reservationType || !startDate || !endDate) {
        return;
    }

    function updateEndDate() {
        if (!reservationType.value || !startDate.value) {
            return;
        }

        const start = new Date(startDate.value);
        const end = new Date(start);

        if (reservationType.value === "MONTHLY") {
            end.setMonth(end.getMonth() + 1);
            endDate.value = formatDateTime(end);
            endDate.readOnly = true;
        } else if (reservationType.value === "YEARLY") {
            end.setFullYear(end.getFullYear() + 1);
            endDate.value = formatDateTime(end);
            endDate.readOnly = true;
        } else {
            endDate.readOnly = false;
        }
    }

    function formatDateTime(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hours = String(date.getHours()).padStart(2, "0");
        const minutes = String(date.getMinutes()).padStart(2, "0");

        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }

    reservationType.addEventListener("change", updateEndDate);
    startDate.addEventListener("change", updateEndDate);

    updateEndDate();
});