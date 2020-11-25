import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntBinaryOperator;

import javax.swing.JButton;
import javax.swing.JFrame;

public final class A3Main {
  private static volatile boolean running = true;

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    // Spielstärken einlesen
    int n = sc.nextInt();
    int[] players = new int[n];
    int max = -1;
    int strongest = -1;
    for (int i = 0; i < n; ++i) {
      players[i] = sc.nextInt();
      if (players[i] > max) {
        max = players[i];
        strongest = i;
      }
    }
    sc.close();
    Random rnd = new Random();
    // Zufallsfunktionen für das Match
    IntBinaryOperator singleMatch = (a, b) -> nextBoolean(rnd, a, b) ? 1 : 0;
    IntBinaryOperator x5Match = (a, b) -> {
      int w = 0;
      for (int i = 0; i < 5; ++i) if (nextBoolean(rnd, a, b)) ++w;
      return w > 2 ? 1 : 0;
    };
    // Simulationen und Gewinne zählen
    int num;
    int divCount = 0;
    int divX5Count = 0;
    int koCount = 0;
    int koX5Count = 0;
    int[] copy = new int[players.length];
    final int buffer = 1 << 13;
    // Fenster erstellen
    JFrame frame = new JFrame("Simulator");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        running = false;
      }
    });
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    JButton btn = new JButton(Long.toUnsignedString(-1));
    btn.addActionListener((e) -> {
      running = false;
      frame.dispose();
    });
    frame.add(btn);
    frame.pack();
    frame.setVisible(true);
    btn.setText("0");
    // Simulationen
    for (int i = 0; i < copy.length; ++i) copy[i] = i;
    for (num = 0; running; num += buffer) {
      for (int i = 0; i < buffer; ++i) {
        // Liga simulieren
        if (simulateDivision(singleMatch, players) == strongest) ++divCount;
        if (simulateDivision(x5Match, players) == strongest) ++divX5Count;
        // zufällige Startverteilung für K. O. erstellen
        for (int j = 0; j < copy.length; ++j) {
          int k = nextInt(rnd, copy.length);
          int tmp = copy[j];
          copy[j] = copy[k];
          copy[k] = tmp;
        }
        // K. O. simulieren
        if (simulateKO(singleMatch, players, copy) == strongest) ++koCount;
        if (simulateKO(x5Match, players, copy) == strongest) ++koX5Count;
      }
      btn.setText(Integer.toString(num));
    }
    // Ergebnis ausgeben
    double numD = num;
    System.out.println("Anzahl:  " + num + " Simulationen");
    System.out.println("Liga:    " + divCount / numD);
    System.out.println("Liga x5: " + divX5Count / numD);
    System.out.println("K.O.:    " + koCount / numD);
    System.out.println("K.O. x5: " + koX5Count / numD);
  }

  // Liga simulieren
  public static int simulateDivision(IntBinaryOperator rnd, int[] players) {
    int[] score = new int[players.length]; // Punktzahlen
    int max = 0;
    int index = 0; // Gewinner laufend ermitteln
    for (int a = 0; a < players.length; ++a) for (int b = a + 1; b < players.length; ++b) { // für alle Paare
      int winner = rnd.applyAsInt(players[a], players[b]) != 0 ? a : b;
      ++score[winner]; // Gewinner bekommt 1 Punkt
      if (score[winner] > max || (score[winner] == max && winner < index)) {
        max = score[winner]; // Gewinner mit höchster Punktzahl aktualisieren
        index = winner;
      }
    }
    return index;
  }

  // KO simulieren, rnd: „Zufallsoperator“ mit Wahrscheinlichkeiten, der bestimmt, ob 1 oder 5 Matches
  // gespielt werden
  public static int simulateKO(IntBinaryOperator rnd, int[] players, int[] startDistribution) {
    int numPlayers = Integer.highestOneBit(players.length); // 2er-Potenz erzwingen
    // binärer Baum
    int[] koRounds = new int[numPlayers * 2];
    for (int i = 0; i < numPlayers; ++i) {
      koRounds[i + numPlayers] = startDistribution[i]; // initialisieren mit Startverteilung
    }
    for (int nP = numPlayers; nP > 1; nP /= 2) { // für alle Runden
      for (int i = nP / 2; i < nP; ++i) { // für alle Einzelmatches
        int a = koRounds[i * 2]; // Spieler 1
        int b = koRounds[i * 2 + 1]; // Spieler 2
        koRounds[i] = rnd.applyAsInt(players[a], players[b]) != 0 ? a : b; // Gewinner kommt in die nächste
                                                                           // Runde
      }
    }
    return koRounds[1];
  }

  // nextBoolean() für nicht-gleiche Wahrscheinlichkeitsverteilung
  private static boolean nextBoolean(Random rnd, int probTrue, int probFalse) {
    return nextInt(rnd, probTrue + probFalse) < probTrue;
  }

  // für exakte Wahrscheinlichkeitsverteilung:
  // Random kann nur 1:1 Wahrscheinlichkeiten generieren, also exakt gleiche Verteilungen nur bei Bounds der
  // Form 2^n.
  private static int nextInt(Random rnd, int bound) {
    int eb = Integer.highestOneBit(bound);
    if (eb < bound) eb <<= 1;
    // 2^n Bound-Wert für gleiche Verteilung
    int rs;
    do {
      rs = rnd.nextInt(eb);
      // nur Werte < bound werden akzeptiert
    } while (rs >= bound);
    return rs;
  }
}