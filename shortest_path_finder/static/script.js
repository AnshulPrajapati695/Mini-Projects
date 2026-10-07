// Create map
var map = L.map('map').setView([30.9010, 75.8573], 13);


// Add OpenStreetMap
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);


// Store markers
var markers = {};


// Store route line
var routeLine = null;


// Find Shortest Path button
document.getElementById("findPath").addEventListener("click", function () {

    var source = document.getElementById("source").value;
    var destination = document.getElementById("destination").value;


    // Check selections
    if (source === "" || destination === "") {

        alert("Please select both source and destination.");

        return;
    }


    // Same location
    if (source === destination) {

        alert("Source and destination cannot be the same.");

        return;
    }


    // Send request to Flask
    fetch("/find-path", {

        method: "POST",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify({
            source: source,
            destination: destination
        })

    })

    .then(response => response.json())

    .then(data => {

        console.log(data);


        // Display results
        document.getElementById("distance").textContent =
            data.distance + " km";

        document.getElementById("nodes").textContent =
            data.visited;

        document.getElementById("time").textContent =
            data.execution_time + " seconds";


        // Remove previous route
        if (routeLine !== null) {

            map.removeLayer(routeLine);

        }


        // Create coordinates for shortest path
        var routeCoordinates = [];


        data.path.forEach(function(location) {

            var coordinate = data.locations[location];

            routeCoordinates.push(coordinate);

        });


        // Draw shortest path
        routeLine = L.polyline(routeCoordinates, {
            weight: 5
        }).addTo(map);


        // Add markers
        data.path.forEach(function(location) {

            var coordinate = data.locations[location];

            var marker = L.marker(coordinate)
                .addTo(map)
                .bindPopup(location);

            markers[location] = marker;

        });


        // Zoom map to route
        map.fitBounds(routeLine.getBounds());

    })

    .catch(error => {

        console.error("Error:", error);

        alert("Something went wrong.");

    });

});