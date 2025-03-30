import java.io.*;
import java.util.*;

public class OrdonnancementProjet {
    public static void main() {
        String nomFichier = "src/table 3.txt";
        List<int[]> contraintes = lireFichierContraintes(nomFichier);

        if (contraintes.isEmpty()) {
            System.out.println("Le fichier est vide ou une erreur s'est produite.");
            return;
        }

        //afficherContraintes(contraintes);
        int[][] matrice = creerMatriceContraintes(contraintes);
        afficherMatrice(matrice);

        if (verifierGraphe(matrice)) {
            System.out.println("\nLe graphe contient un circuit ou des valeurs négatives.");
            return;
        }

        System.out.println("\nLe graphe est valide pour l'ordonnancement.");

        int[] rangs = calculerRangs(matrice);
        afficherRangs(rangs);

        int[] auPlusTot = calculerCalendrierAuPlusTot(matrice);
        int[] auPlusTard = calculerCalendrierAuPlusTard(matrice, auPlusTot);
        int[] marges = calculerMarges(auPlusTot, auPlusTard);

        afficherCalendriers(auPlusTot, auPlusTard, marges);
        afficherCheminCritique(marges);
    }

    // Lecture du fichier
    public static List<int[]> lireFichierContraintes(String nomFichier) {
        List<int[]> contraintes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nomFichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] valeurs = ligne.split("\\s+");
                int[] ligneContraintes = Arrays.stream(valeurs).mapToInt(Integer::parseInt).toArray();
                contraintes.add(ligneContraintes);
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
        return contraintes;
    }

    // Affichage des contraintes
    public static void afficherContraintes(List<int[]> contraintes) {
        System.out.println("\nContraintes lues :");
        for (int[] ligne : contraintes) {
            System.out.println(Arrays.toString(ligne));
        }
    }

    // Création de la matrice d'adjacence
    public static int[][] creerMatriceContraintes(List<int[]> contraintes) {
        int N = contraintes.size();
        int[][] matrice = new int[N + 2][N + 2];

        // Initialisation de la matrice avec -1 (aucun lien)
        for (int[] ligne : matrice) {
            Arrays.fill(ligne, -1);
        }

        // Remplissage des arcs entre les tâches
        for (int[] ligne : contraintes) {
            int tache = ligne[0];

            if (ligne.length == 2) {
                matrice[0][tache] = 0;
            } else {
                for (int i = 2; i < ligne.length; i++) {
                    int predecesseur = ligne[i];
                    matrice[predecesseur][tache] = contraintes.get(predecesseur - 1)[1];
                }
            }
        }

        for (int i = 1; i <= N; i++) {
            boolean estDerniereTache = true;
            for (int[] ligne : contraintes) {
                for (int j = 2; j < ligne.length; j++) {
                    if (ligne[j] == i) {
                        estDerniereTache = false;
                        break;
                    }
                }
            }
            if (estDerniereTache) {
                matrice[i][N + 1] = contraintes.get(i - 1)[1];
            }
        }

        return matrice;
    }


    // Affichage de la matrice d'adjacence
    public static void afficherMatrice(int[][] matrice) {
        System.out.println("\nMatrice d'adjacence :");
        for (int[] ligne : matrice) {
            for (int valeur : ligne) {
                if (valeur == -1) {
                    System.out.print(" . "); // Affichage d'un point pour les absences de lien
                } else {
                    System.out.printf("%2d ", valeur);
                }
            }
            System.out.println();
        }
    }


    // Vérification de la présence d'arcs à valeurs négatives
    public static boolean verifierGraphe(int[][] matrice) {
        for (int[] ints : matrice) {
            for (int anInt : ints) {
                if (anInt < -1) {
                    System.out.println("Valeur négative détectée dans le graphe.");
                    return true;
                }
            }
        }
        return contientCircuit(matrice);
    }

    public static boolean contientCircuit(int[][] matrice) {
        int N = matrice.length;
        int[] degresEntrants = new int[N];

        // Compter les degrés entrants
        for (int[] ints : matrice) {
            for (int j = 0; j < N; j++) {
                if (ints[j] > -1) {
                    degresEntrants[j]++;
                }
            }
        }

        Queue<Integer> file = new LinkedList<>();
        for (int i = 0; i < N; i++) {
            if (degresEntrants[i] == 0) {
                file.add(i);
            }
        }

        int compte = 0;
        while (!file.isEmpty()) {
            int sommet = file.poll();
            compte++;

            for (int i = 0; i < N; i++) {
                if (matrice[sommet][i] > -1) {
                    degresEntrants[i]--;
                    if (degresEntrants[i] == 0) {
                        file.add(i);
                    }
                }
            }
        }

        if (compte != N) {
            System.out.println("Circuit détecté dans le graphe.");
            return true;
        }
        return false;
    }

    public static int[] calculerRangs(int[][] matrice) {
        int N = matrice.length;
        int[] rangs = new int[N]; // Tableau des rangs
        int[] degreEntrant = new int[N]; // Stocke le nombre de prédecesseurs pour chaque sommet
        Queue<Integer> file = new LinkedList<>(); // File pour le tri topologique

        // Calcul du degré entrant de chaque sommet
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (matrice[i][j] > -1) { // Il existe un arc de i vers j
                    degreEntrant[j]++;
                }
            }
        }

        // Ajouter à la file les sommets sans prédécesseurs (degré entrant == 0)
        for (int i = 0; i < N; i++) {
            if (degreEntrant[i] == 0) {
                file.add(i);
            }
        }

        // Traitement des sommets dans l'ordre topologique
        while (!file.isEmpty()) {
            int sommet = file.poll();

            for (int successeur = 0; successeur < N; successeur++) {
                if (matrice[sommet][successeur] > -1) { // Il y a un arc sommet -> successeur
                    rangs[successeur] = Math.max(rangs[successeur], rangs[sommet] + 1);
                    degreEntrant[successeur]--;

                    if (degreEntrant[successeur] == 0) {
                        file.add(successeur);
                    }
                }
            }
        }

        return rangs;
    }

    public static void afficherRangs(int[] rangs) {
        System.out.println("\nRangs des sommets :");
        for (int i = 0; i < rangs.length; i++) {
            System.out.printf("Sommet %d : Rang %d\n", i, rangs[i]);
        }
    }


    // Calcul du calendrier au plus tôt
    public static int[] calculerCalendrierAuPlusTot(int[][] matrice) {
        int N = matrice.length;
        int[] auPlusTot = new int[N];

        for (int i = 1; i < N; i++) {
            for (int j = 0; j < i; j++) {
                if (matrice[j][i] > -1) {
                    auPlusTot[i] = Math.max(auPlusTot[i], auPlusTot[j] + matrice[j][i]);
                }
            }
        }

        return auPlusTot;
    }

    // Calcul du calendrier au plus tard
    public static int[] calculerCalendrierAuPlusTard(int[][] matrice, int[] auPlusTot) {
        int N = matrice.length;
        int[] auPlusTard = new int[N];
        Arrays.fill(auPlusTard, auPlusTot[N - 1]);

        for (int i = N - 2; i >= 0; i--) {
            for (int j = i + 1; j < N; j++) {
                if (matrice[i][j] > -1) {
                    auPlusTard[i] = Math.min(auPlusTard[i], auPlusTard[j] - matrice[i][j]);
                }
            }
        }

        return auPlusTard;
    }

    // Calcul des marges
    public static int[] calculerMarges(int[] auPlusTot, int[] auPlusTard) {
        int N = auPlusTot.length;
        int[] marges = new int[N];

        for (int i = 0; i < N; i++) {
            marges[i] = auPlusTard[i] - auPlusTot[i];
        }

        return marges;
    }

    // Affichage des résultats
    public static void afficherCalendriers(int[] auPlusTot, int[] auPlusTard, int[] marges) {
        System.out.println("\nCalendriers et marges :");
        for (int i = 0; i < auPlusTot.length; i++) {
            System.out.printf("Tâche %d : Tot = %d, Tard = %d, Marge = %d\n", i, auPlusTot[i], auPlusTard[i], marges[i]);
        }
    }

    // Affichage du chemin critique
    public static void afficherCheminCritique(int[] marges) {
        System.out.print("\nChemin critique : ");
        for (int i = 0; i < marges.length; i++) {
            if (marges[i] == 0) {
                System.out.print(i + " ");
            }
        }
        System.out.println();
    }
}