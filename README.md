# Graph Algorithms Studio

[![CI](https://github.com/MatALass/graph-algorithms-studio/actions/workflows/ci.yml/badge.svg)](https://github.com/MatALass/graph-algorithms-studio/actions)
![Java](https://img.shields.io/badge/Java-21-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-green)
![Maven](https://img.shields.io/badge/Maven-Build-red)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

Interactive JavaFX application for visualizing and simulating graph algorithms on transport networks.

## Overview

Graph Algorithms Studio is a desktop application built with JavaFX that enables interactive creation, manipulation, and analysis of graphs. It is designed for algorithm visualization, education, and transport network simulation.

The application provides a structured and visual approach to understanding graph algorithms through real-time interaction and step-by-step execution.

## Features

### Graph Manipulation
- Add and remove nodes and edges
- Weighted graphs support
- Drag and drop nodes
- Directed and undirected graphs

### Algorithms
- Dijkstra (shortest path)
- A* (heuristic search)
- Bellman-Ford (negative weights)
- Floyd-Warshall (all-pairs shortest paths)
- Kruskal and Prim (minimum spanning tree)
- Cycle detection (DFS / Union-Find)
- Ford-Fulkerson (maximum flow)

### Visualization
- Highlighted shortest paths
- Node distance display
- Step-by-step execution
- Playback controls for algorithm animation

### Analysis
- Graph density
- Connectivity detection
- Degree metrics
- Weight distribution insights

## Installation

### Prerequisites
- Java 21+
- Maven 3.9+

### Run locally

git clone https://github.com/MatALass/graph-algorithms-studio
cd graph-algorithms-studio
mvn clean javafx:run

## Tests

mvn test

## Project Structure

src/main/java/com/matalass/graphroutestudio
├── algorithms   # Algorithm implementations
├── engine       # Core graph model
├── ui           # JavaFX UI layer
├── animation    # Step-by-step visualization
├── analysis     # Graph metrics
├── io           # Import / export
├── presets      # Sample graphs

## Design Principles

- Separation of concerns between UI, algorithms, and core engine
- Deterministic algorithm implementations
- Extensible architecture for adding new algorithms
- Clear data flow for visualization and animation

## Use Cases

- Graph theory learning and teaching
- Algorithm visualization
- Transport network simulation
- Decision-support exploration

## Future Improvements

- Real-world map integration
- CSV / API data import
- Performance benchmarking
- Multi-algorithm comparison
- Export visualizations

## Author

Mathieu Alassoeur  
https://github.com/MatALass

## License

MIT License
