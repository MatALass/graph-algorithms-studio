# GraphRoute Studio

GraphRoute Studio est une application JavaFX de visualisation interactive des algorithmes de graphes, pensée pour un cas d'usage concret : **réseaux de transport / GPS**.

## Ce que le projet propose
- interface JavaFX moderne en thème sombre
- sélection visuelle source / cible directement sur le graphe
- création, déplacement et suppression de nœuds
- ajout d'arcs pondérés en mode dédié
- surbrillance visuelle du chemin optimal, des arbres couvrants et des cycles
- distances affichées directement sous les nœuds pour les algorithmes de plus court chemin
- animation pas-à-pas avec lecture, pause, navigation et contrôle de vitesse
- import / export JSON
- import de tableaux de contraintes `.txt`
- presets : réseau de transport, réseau de démonstration, graphes aléatoires

## Algorithmes inclus
- Dijkstra
- A*
- Bellman-Ford
- Floyd-Warshall
- Kruskal
- Prim
- Détection de cycles
- Ford-Fulkerson

## Lancement
Prérequis :
- **Java 21+**
- **Maven 3.9+**

```powershell
mvn clean javafx:run
```

## Tests
```powershell
mvn test
```

## Structure
- `src/main/java/com/matalass/graphroutestudio/engine` : modèle graphe (`Graph`, `Node`, `Edge`)
- `algorithms` : implémentations des algorithmes
- `analysis` : métriques et diagnostic du graphe
- `animation` : lecture pas-à-pas des étapes algorithmiques
- `io` : import / export et génération aléatoire
- `presets` : jeux de données de démonstration
- `ui` : interface JavaFX
- `src/test/java` : tests unitaires de base

## Conseils d'utilisation
- **Mode vue** : cliquez sur une source puis une cible, puis exécutez un algorithme.
- **Mode édition** : cliquez dans le vide pour créer un nœud, glissez pour déplacer, double-cliquez pour supprimer.
- **Mode ajout d'arc** : choisissez la source puis la cible, avec le poids défini dans le panneau latéral.

## Nom recommandé du repo
`graphroute-studio`

## Topics GitHub recommandés
`java` `javafx` `maven` `graph-theory` `algorithms` `shortest-path` `dijkstra` `a-star` `bellman-ford` `floyd-warshall` `minimum-spanning-tree` `ford-fulkerson` `visualization`
