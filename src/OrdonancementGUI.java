import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;


public class OrdonancementGUI {
    private JFrame frame;
    private JTextArea contraintesArea;
    private JPanel matricePanel;
    private JTextArea rangsArea;
    private JTextArea calendriersArea;
    private JTextArea cheminCritiqueArea;
    private String fichierCourant;

    public OrdonancementGUI() {
        setupFrame();
        setupComponents();
    }

    private void setupFrame() {
        frame = new JFrame("Ordonnancement de Tâches");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
    }
    private void setupComponents() {
        // Layout principal
        frame.setLayout(new BorderLayout(10, 10));

        // Grande Section
        JPanel mainPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialisation
        contraintesArea = createStyledTextArea();
        matricePanel = createMatricePanel();
        rangsArea = createStyledTextArea();
        calendriersArea = createStyledTextArea();
        cheminCritiqueArea = createStyledTextArea();

        // Sections création
        mainPanel.add(createSectionPanel("Contraintes", contraintesArea));
        mainPanel.add(createSectionPanel("Matrice de contraintes", matricePanel));
        mainPanel.add(createSectionPanel("Rangs des sommets", rangsArea));

        // Section calendriers & chemin critique
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        bottomPanel.add(new JScrollPane(calendriersArea));
        bottomPanel.add(new JScrollPane(cheminCritiqueArea));
        mainPanel.add(createSectionPanel("Calendriers et Chemin Critique", bottomPanel));

        JToolBar toolBar = createToolBar();

        frame.add(toolBar, BorderLayout.NORTH);
        frame.add(mainPanel, BorderLayout.CENTER);
    }
    private JPanel createSectionPanel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 0));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(content), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        return panel;
    }


    private JTextArea createStyledTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return textArea;
    }

    private JPanel createMatricePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(300, 200));
        return panel;
    }


    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton btnChoisirFichier = new JButton("Choisir fichier");
        JButton btnCalculer = new JButton("Calculer");

        btnChoisirFichier.addActionListener(_ -> choisirFichier());
        btnCalculer.addActionListener(_ -> calculerEtAfficher());

        toolBar.add(btnChoisirFichier);
        toolBar.addSeparator();
        toolBar.add(btnCalculer);

        return toolBar;
    }


    private void choisirFichier() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            fichierCourant = fileChooser.getSelectedFile().getAbsolutePath();
            contraintesArea.setText("Fichier sélectionné : " + fichierCourant);
            // Réinitialiser les autres zones
            matricePanel.removeAll();
            matricePanel.revalidate();
            matricePanel.repaint();
            rangsArea.setText("");
            calendriersArea.setText("");
            cheminCritiqueArea.setText("");
        }
    }

    private void calculerEtAfficher() {
        if (fichierCourant == null) {
            JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un fichier", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);

            // Lire contraintes
            List<int[]> contraintes = OrdonnancementProjet.lireFichierContraintes(fichierCourant);

            // Contraintes affichage
            OrdonnancementProjet.afficherContraintes(contraintes);
            contraintesArea.setText(baos.toString());
            baos.reset();

            // Matrice adjacente création
            int[][] matrice = OrdonnancementProjet.creerMatriceContraintes(contraintes);

            // Matrice affichage
            afficherMatriceDansGrille(matrice);

            if (OrdonnancementProjet.verifierGraphe(matrice)) {
                JOptionPane.showMessageDialog(frame, "Le graphe contient un circuit ou des valeurs négatives", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Rangs calcul
            int[] rangs = OrdonnancementProjet.calculerRangs(matrice);
            baos.reset();
            OrdonnancementProjet.afficherRangs(rangs);
            rangsArea.setText(baos.toString());

            // Calendriers calcul
            int[] auPlusTot = OrdonnancementProjet.calculerCalendrierAuPlusTot(matrice);
            int[] auPlusTard = OrdonnancementProjet.calculerCalendrierAuPlusTard(matrice, auPlusTot);
            int[] marges = OrdonnancementProjet.calculerMarges(auPlusTot, auPlusTard);

            // Calendriers affichage
            baos.reset();
            OrdonnancementProjet.afficherCalendriers(auPlusTot, auPlusTard, marges);
            calendriersArea.setText(baos.toString());

            // Chemin critique affichage
            baos.reset();
            OrdonnancementProjet.afficherCheminCritique(marges);
            cheminCritiqueArea.setText(baos.toString());

            System.setOut(oldOut);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Erreur lors du calcul : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }



    private void afficherMatriceDansGrille(int[][] matrice) {
        matricePanel.removeAll();
        matricePanel.setLayout(new GridLayout(matrice.length + 1, matrice[0].length + 1));
        // NAMES
        matricePanel.add(new JLabel(""));
        for (int j = 0; j < matrice[0].length; j++) {
            matricePanel.add(new JLabel(String.valueOf(j), SwingConstants.CENTER));
        }

        // DATA
        for (int i = 0; i < matrice.length; i++) {
            matricePanel.add(new JLabel(String.valueOf(i), SwingConstants.CENTER));
            for (int j = 0; j < matrice[i].length; j++) {
                JLabel label = new JLabel(matrice[i][j] == -1 ? "." : String.valueOf(matrice[i][j]), SwingConstants.CENTER);
                matricePanel.add(label);
            }
        }
        matricePanel.revalidate();
        matricePanel.repaint();
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OrdonancementGUI().show());
    }

}