# GraphRoute Studio

Application JavaFX de visualisation d'algorithmes de graphes orientée réseau de transport / GPS.

## Fonctionnalités
- dessin interactif de graphes
- sélection source / cible par clic droit
- Dijkstra, A*, Bellman-Ford, Floyd-Warshall
- Kruskal / Prim
- détection de cycles
- flot maximal Ford-Fulkerson
- import / export JSON
- import de tableaux de contraintes
- animation pas-à-pas
- presets de démonstration

## Lancement

Prérequis :
- Java 21+ (Java 21 LTS recommandé)
- Maven 3.9+

```powershell
mvn clean javafx:run
```

## Structure
- `src/main/java/com/matalass/graphroutestudio/engine` : modèle graphe
- `algorithms` : algorithmes
- `ui` : JavaFX
- `io` : import / export
- `analysis` : métriques
- `animation` : lecture pas-à-pas
- `presets` : graphes de démonstration
