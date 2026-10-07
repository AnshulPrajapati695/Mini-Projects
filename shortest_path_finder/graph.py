import time


graph = {

    "Railway Station": {
        "Junction 1": 1.0
    },

    "Junction 1": {
        "Railway Station": 1.0,
        "Clock Tower": 1.5,
        "Junction 2": 1.2
    },

    "Clock Tower": {
        "Junction 1": 1.5,
        "Junction 2": 1.0
    },

    "Junction 2": {
        "Junction 1": 1.2,
        "Clock Tower": 1.0,
        "Bus Stand": 1.3,
        "Junction 3": 1.0
    },

    "Bus Stand": {
        "Junction 2": 1.3,
        "Junction 3": 1.1
    },

    "Junction 3": {
        "Junction 2": 1.0,
        "Bus Stand": 1.1,
        "Ferozepur Road": 1.4
    },

    "Ferozepur Road": {
        "Junction 3": 1.4
    }
}

locations = {

    "Railway Station": [30.9009, 75.8573],

    "Junction 1": [30.9030, 75.8540],

    "Clock Tower": [30.9080, 75.8513],

    "Junction 2": [30.9050, 75.8500],

    "Bus Stand": [30.9040, 75.8460],

    "Junction 3": [30.9000, 75.8440],

    "Ferozepur Road": [30.8950, 75.8410]
}


def dijkstra(graph, source, destination):

    # Start execution timer
    start_time = time.perf_counter()

    # Distance from source to every vertex
    distance = {
        vertex: float('inf')
        for vertex in graph
    }

    # Keep track of visited vertices
    visited = {
        vertex: False
        for vertex in graph
    }

    # Store previous vertex
    previous = {
        vertex: None
        for vertex in graph
    }

    # Distance from source to itself = 0
    distance[source] = 0

    # Count visited vertices
    visited_count = 0

    # Dijkstra algorithm
    for _ in range(len(graph)):

        # Find unvisited vertex with minimum distance
        current = None
        minimum_distance = float('inf')

        for vertex in graph:

            if not visited[vertex] and distance[vertex] < minimum_distance:
                minimum_distance = distance[vertex]
                current = vertex

        # No reachable vertex remains
        if current is None:
            break

        # Mark current vertex as visited
        visited[current] = True
        visited_count += 1

        # Destination reached
        if current == destination:
            break

        # Update neighboring vertices
        for neighbor, weight in graph[current].items():

            new_distance = distance[current] + weight

            if new_distance < distance[neighbor]:

                distance[neighbor] = new_distance

                previous[neighbor] = current

    # Reconstruct shortest path
    path = []

    current = destination

    while current is not None:

        path.append(current)

        current = previous[current]

    path.reverse()

    # If destination cannot be reached
    if path[0] != source:
        path = []

    # Calculate execution time
    execution_time = time.perf_counter() - start_time

    return distance[destination], path, visited_count, execution_time


# Test the algorithm
if __name__ == "__main__":

    source = "Railway Station"
    destination = "Ferozepur Road"

    distance, path, visited, execution_time = dijkstra(
        graph,
        source,
        destination
    )

    print("Shortest Distance:", distance, "km")
    print("Shortest Path:", " -> ".join(path))
    print("Nodes Visited:", visited)
    print("Execution Time:", execution_time, "seconds")