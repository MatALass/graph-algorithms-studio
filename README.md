# Graph Algorithms Studio

[![CI](https://github.com/MatALass/graph-algorithms-studio/actions/workflows/ci.yml/badge.svg)](https://github.com/MatALass/graph-algorithms-studio/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/MatALass/graph-algorithms-studio)](https://github.com/MatALass/graph-algorithms-studio/releases)
![Java](https://img.shields.io/badge/Java-21-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-green)
![Maven](https://img.shields.io/badge/Maven-3.9+-red)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

Interactive JavaFX application for visualizing and simulating graph algorithms on transport networks.

## Overview

Graph Algorithms Studio is a desktop JavaFX application designed to create, edit, visualize, and analyze weighted graphs in an interactive way. The project targets graph theory learning, algorithm visualization, and transport network simulation, with a focus on clarity, reproducibility, and maintainable architecture.

The application combines:
- an interactive graph editor,
- classic graph algorithms,
- visual step-by-step execution,
- and a clean separation between UI, engine, and algorithm layers.

## Core Features

### Interactive graph editing
- Add, move, and remove nodes
- Add and remove weighted edges
- Support for directed and undirected graphs
- Preset graph loading for quick demos

### Algorithms
- Dijkstra
- A*
- Bellman-Ford
- Floyd-Warshall
- Kruskal
- Prim
- Cycle detection
- Ford-Fulkerson

### Visualization
- Result path highlighting
- Distance display on nodes
- Step-by-step execution controls
- Visual feedback during traversal and updates

### Analysis
- Graph density
- Connectivity checks
- Degree metrics
- Weight-related indicators

## Technology Stack

- Java 21
- JavaFX
- Maven
- JUnit 5
- GitHub Actions

## Project Structure

```text
src/main/java/com/matalass/graphroutestudio
├── algorithms
├── analysis
├── animation
├── engine
├── io
├── presets
└── ui
```

## Local Setup

### Prerequisites

- Java 21 or higher
- Maven 3.9 or higher

### Run locally

```bash
git clone https://github.com/MatALass/graph-algorithms-studio
cd graph-algorithms-studio
mvn clean javafx:run
```

### Run tests

```bash
mvn test
```

### Build the project

```bash
mvn clean package
```

## Continuous Integration

This repository is configured with GitHub Actions.

### CI workflow
The CI workflow:
- checks out the repository,
- installs Java 21,
- caches Maven dependencies,
- runs `mvn clean verify`,
- uploads test reports and generated JAR artifacts.

Workflow file:
```text
.github/workflows/ci.yml
```

### Release workflow
The release workflow runs when a Git tag starting with `v` is pushed, for example:

```bash
git tag v1.0.0
git push origin v1.0.0
```

It:
- builds the project,
- collects generated JAR files,
- creates a GitHub Release,
- attaches the build artifacts.

Workflow file:
```text
.github/workflows/release.yml
```

## Release and Distribution Notes

For a JavaFX desktop project, a plain JAR is useful as a build artifact, but it is not always the best end-user distribution format. If you want a stronger desktop delivery later, the next recommended step is to package a runtime image or native installer with `jlink` or `jpackage`.

## Demo

A GIF or short video demo is strongly recommended for the repository front page.

Recommended capture sequence:
1. Load a transport preset
2. Select source and target nodes
3. Run Dijkstra
4. Show highlighted path and total distance
5. Start step-by-step animation
6. Switch to another algorithm such as A* or Kruskal

Suggested output:
- `docs/demo.gif`
- first screenshot in `assets/screenshots/`

I did not generate a real demo GIF here because that requires running the UI and capturing the application visually on your machine.

## Design Principles

- Separation of concerns
- Testable algorithm layer
- UI isolated from core graph logic
- Extensible project structure
- Portfolio-ready engineering standards

## Author

Mathieu Alassoeur  

## License

MIT License
