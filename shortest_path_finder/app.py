from flask import Flask, render_template, request, jsonify
from graph import graph, locations, dijkstra

app = Flask(__name__)


@app.route("/")
def home():
    return render_template("index.html")


@app.route("/find-path", methods=["POST"])
def find_path():

    data = request.get_json()

    source = data["source"]
    destination = data["destination"]

    distance, path, visited, execution_time = dijkstra(
        graph,
        source,
        destination
    )

    return jsonify({
    "distance": distance,
    "path": path,
    "visited": visited,
    "execution_time": execution_time,
    "locations": locations
})


if __name__ == "__main__":
    app.run(debug=True)