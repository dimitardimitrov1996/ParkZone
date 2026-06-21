document.addEventListener("DOMContentLoaded", function () {
    const parkingLotSelect = document.getElementById("parkingLotId");
    const parkingSpotSelect = document.getElementById("parkingSpotId");

    if (!parkingLotSelect || !parkingSpotSelect) {
        return;
    }

    const originalSpotOptions = Array.from(parkingSpotSelect.querySelectorAll("option"))
        .filter(option => option.value !== "")
        .map(option => option.cloneNode(true));

    function filterParkingSpots() {
        const selectedParkingLotId = parkingLotSelect.value;
        const selectedParkingSpotId = parkingSpotSelect.value;

        parkingSpotSelect.innerHTML = "";

        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Select parking spot";
        parkingSpotSelect.appendChild(placeholder);

        if (!selectedParkingLotId) {
            parkingSpotSelect.value = "";
            return;
        }

        originalSpotOptions
            .filter(option => option.dataset.parkingLotId === selectedParkingLotId)
            .forEach(option => {
                parkingSpotSelect.appendChild(option.cloneNode(true));
            });

        const selectedSpotStillExists = Array.from(parkingSpotSelect.options)
            .some(option => option.value === selectedParkingSpotId);

        parkingSpotSelect.value = selectedSpotStillExists ? selectedParkingSpotId : "";
    }

    parkingLotSelect.addEventListener("change", filterParkingSpots);

    filterParkingSpots();
});