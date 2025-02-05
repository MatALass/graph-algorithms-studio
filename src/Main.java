import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String nomFichier = "src/contraintes.txt";
        List<int[]> contraintes = OrdonnancementProjet.lireFichierContraintes(nomFichier);

        if (contraintes.isEmpty()) {
            System.out.println("Le fichier est vide ou une erreur s'est produite.");
            return;
        }

        OrdonnancementProjet.afficherContraintes(contraintes);
        int[][] matrice = OrdonnancementProjet.creerMatriceAdjacence(contraintes);
        OrdonnancementProjet.afficherMatrice(matrice);

        if (!OrdonnancementProjet.verifierGraphe(matrice)) {
            System.out.println("\nLe graphe contient un circuit ou des valeurs négatives.");
            return;
        }

        System.out.println("\nLe graphe est valide pour l'ordonnancement.");

        int[] rangs = OrdonnancementProjet.calculerRangs(matrice);
        OrdonnancementProjet.afficherRangs(rangs);

        int[] auPlusTot = OrdonnancementProjet.calculerCalendrierAuPlusTot(matrice);
        int[] auPlusTard = OrdonnancementProjet.calculerCalendrierAuPlusTard(matrice, auPlusTot);
        int[] marges = OrdonnancementProjet.calculerMarges(auPlusTot, auPlusTard);

        OrdonnancementProjet.afficherCalendriers(auPlusTot, auPlusTard, marges);
        OrdonnancementProjet.afficherCheminCritique(marges);
    }
}