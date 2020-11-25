import java.io.PrintStream;
import java.util.*;

public final class A1Main {
  public static void main(String[] args) {
    List<Gap> sentenceOrderGapList = new ArrayList<>();
    List<Gap> gapList = new ArrayList<>();
    List<String> wordList = new ArrayList<>();
    {
      // Einleseprozedur
      Scanner sc = new Scanner(System.in);
      String textString = sc.nextLine();
      String wordString = sc.nextLine();
      sc.close();
      // Lücken lesen
      StringBuilder builder = new StringBuilder();
      boolean first = true; // Satzanfang?
      boolean word = true; // Wort oder Satzzeichen?
      int length = 0; // Wortlänge
      int[] positions = new int[1];
      char[] chars = new char[1]; // Lückendaten
      int charCount = 0;
      for (int i = 0; i < textString.length(); ++i) {
        char value = textString.charAt(i);
        boolean w = value == '_' || Character.isAlphabetic(value);
        if (w && !word) { // wenn vorher Satzzeichen, jetzt Wort: Lücke initialisieren und Daten zurücksetzen
          gapList.add(new Gap(first, length, Arrays.copyOf(positions, charCount),
              Arrays.copyOf(chars, charCount), builder.toString()));
          first = builder.indexOf(".") != -1;
          length = 0;
          charCount = 0;
          builder.setLength(0);
        }
        if (w) { // wenn Wort: Zeichen hinzufügen
          if (value != '_') {
            if (positions.length <= charCount) { // Array nötigenfalls erweitern
              positions = Arrays.copyOf(positions, positions.length << 1);
              chars = Arrays.copyOf(chars, chars.length << 1);
            }
            positions[charCount] = length;
            chars[charCount] = value;
            ++charCount;
          }
          ++length;
        } else builder.append(value);
        word = w;
      }
      if (length != 0) gapList.add(new Gap(first, length, Arrays.copyOf(positions, charCount),
          Arrays.copyOf(chars, charCount), builder.toString())); // letzte Lücke hinzufügen
      // Wörter lesen
      StringTokenizer strTok = new StringTokenizer(wordString);
      while (strTok.hasMoreTokens()) wordList.add(strTok.nextToken());
    }
    // Satzordnung der Lücken merken
    sentenceOrderGapList.addAll(gapList);
    // Wörter und Lücken nach Länge sortieren
    gapList.sort((a, b) -> {
      int lc = Integer.compare(a.length, b.length);
      return lc == 0 ? a.gapString.compareToIgnoreCase(b.gapString) : lc;
    });
    wordList.sort((a, b) -> {
      int lc = Integer.compare(a.length(), b.length());
      return lc == 0 ? a.compareToIgnoreCase(b) : lc;
    });
    // Mehrfachvorkommen von Wörtern anders speichern
    List<Word> words = new ArrayList<Word>();
    for (int i = 0; i < wordList.size(); ++i) {
      if (!words.isEmpty() && wordList.get(i).contentEquals(words.get(words.size() - 1).word)) {
        ++words.get(words.size() - 1).count; // jedes Wort nur einmal, dafür zählen
      } else {
        words.add(new Word(wordList.get(i), 1));
      }
    }
    // bipartiter Graph
    int gapIndexOffset = words.size(); // Startindex der Lücken im Array
    int[][] graph = new int[gapList.size() + words.size()][1];
    int[][] otherEdgeIndex = new int[graph.length][1]; // Index der Kante beim anderen Knoten
    int[] graphSize = new int[graph.length]; // einfache ArrayList-Struktur implementieren
    {
      int length;
      int startPosWords = 0;
      int endPosWords;
      int startPosGaps = 0;
      int endPosGaps;
      // Wörter und Lücken nach Längen sortiert durchgehen
      while (startPosWords < words.size()) {
        length = words.get(startPosWords).word.length();
        // Endindex zu dieser Länge ermitteln
        for (endPosWords = startPosWords;
            endPosWords < words.size() && length == words.get(endPosWords).word.length(); ++endPosWords)
          ;
        for (endPosGaps = startPosGaps;
            endPosGaps < gapList.size() && length == gapList.get(endPosGaps).length; ++endPosGaps)
          ;
        // alle Lücken und Wörter auf Matches überprüfen
        for (int i = startPosWords; i < endPosWords; ++i) for (int j = startPosGaps; j < endPosGaps; ++j) {
          // wenn Lücke und Wort passen:
          if (gapList.get(j).matches(words.get(i).word)) {
            System.out.println(gapList.get(j).gapString + " matches " + words.get(i).word);
            // Grapharrayindex der Lücke
            int k = j + gapIndexOffset;
            // Arrays ggf. erweitern
            if (graphSize[i] >= graph[i].length) {
              graph[i] = Arrays.copyOf(graph[i], graphSize[i] << 1);
              otherEdgeIndex[i] = Arrays.copyOf(otherEdgeIndex[i], graphSize[i] << 1);
            }
            if (graphSize[k] >= graph[k].length) {
              graph[k] = Arrays.copyOf(graph[k], graphSize[k] << 1);
              otherEdgeIndex[k] = Arrays.copyOf(otherEdgeIndex[k], graphSize[k] << 1);
            }
            graph[i][graphSize[i]] = k; // neue Kante eintragen
            graph[k][graphSize[k]] = i;
            otherEdgeIndex[i][graphSize[i]] = graphSize[k]; // Index beim anderen Knoten eintragen
            otherEdgeIndex[k][graphSize[k]] = graphSize[i];
            ++graphSize[i]; // Größe aktualisieren
            ++graphSize[k];
          }
        }
        startPosWords = endPosWords;
        startPosGaps = endPosGaps;
      }
    }
    printGraph(graph, graphSize, gapList, words);
    // Stack mit Blättern
    int[] leafs = new int[1];
    int stackSize = 0;
    for (int i = 0; i < graph.length; ++i) {
      if (graphSize[i] == 1) {
        // ggf. Array erweitern
        if (leafs.length <= stackSize) leafs = Arrays.copyOf(leafs, leafs.length << 1);
        // Blatt eintragen
        leafs[stackSize] = i;
        ++stackSize;
      }
    }
    boolean[] matched = new boolean[graph.length];
    while (stackSize > 0) {
      // Blatt aus Stack extrahieren
      --stackSize;
      int leafElement = leafs[stackSize];
      // schon gematcht?
      if (matched[leafElement]) continue;
      // zugehöriges Element ermitteln
      int matchedElement = graph[leafElement][0];
      // in Wort und Lücke teilen
      int gap = Math.max(leafElement, matchedElement);
      int gapIndex = gap - gapIndexOffset;
      int wordIndex = Math.min(leafElement, matchedElement);
      // matchen: Wort in die Lücke eintragen
      gapList.get(gapIndex).matchedWord = words.get(wordIndex).word;
      // Wortdaten anpassen
      --words.get(wordIndex).count;
      matched[gap] = true; // matched-Array aktualisieren
      matched[wordIndex] = words.get(wordIndex).count == 0;
      System.out.println("Matching " + gapList.get(gapIndex).gapString + " and " + words.get(wordIndex).word);
      // Blatt aus Graph entfernen:
      int ni = otherEdgeIndex[leafElement][0];
      --graphSize[matchedElement];
      // Blatt ans Ende tauschen (Ende zum Blatt kopieren, da das Blatt sowieso nicht mehr gebraucht wird)
      graph[matchedElement][ni] = graph[matchedElement][graphSize[matchedElement]];
      otherEdgeIndex[matchedElement][ni] = otherEdgeIndex[matchedElement][graphSize[matchedElement]];
      // Indexliste auf der anderen Seite aktualisieren
      otherEdgeIndex[graph[matchedElement][ni]][otherEdgeIndex[matchedElement][ni]] = ni;
      --graphSize[leafElement]; // = 0
      // wenn entweder Lücke? oder (Wort? && Wortvorkommen == 0) -> anderen Knoten auch entfernen
      // bedeutet: das Wort muss dann übrigbleiben, wenn es noch woanders vorkommt
      if (matchedElement >= gapIndexOffset || words.get(wordIndex).count == 0) {
        // alle Kanten entfernen
        for (int i = 0; i < graphSize[matchedElement]; ++i) {
          int other = graph[matchedElement][i];
          int j = otherEdgeIndex[matchedElement][i];
          --graphSize[other];
          // ans Ende tauschen
          graph[other][j] = graph[other][graphSize[other]];
          otherEdgeIndex[other][j] = otherEdgeIndex[other][graphSize[other]];
          // Indexliste auf der anderen Seite aktualisieren
          otherEdgeIndex[graph[other][j]][otherEdgeIndex[other][j]] = j;
          // neues Blatt entstanden?
          if (graphSize[other] == 1) {
            // ggf. Array erweitern
            if (leafs.length <= stackSize) leafs = Arrays.copyOf(leafs, leafs.length << 1);
            // und neues Blatt hinzufügen
            leafs[stackSize] = other;
            ++stackSize;
          }
        }
        graphSize[matchedElement] = 0;
      }
    }
    // Ergebnis ausgeben!
    for (int i = 0; i < sentenceOrderGapList.size(); ++i) {
      System.out.print(sentenceOrderGapList.get(i).sentencePart());
    }
    System.out.println();
  }

  // Zuordnungsgraph visuell ausgeben (mit GraphViz - nur wenn installiert)
  private static void printGraph(int[][] graph, int[] size, List<Gap> gaps, List<Word> words) {
    ProcessBuilder builder = new ProcessBuilder("neato", "-Tsvg", "-oGraph.svg");
    try {
      Process p = builder.start();
      PrintStream out = new PrintStream(p.getOutputStream());
      out.println("graph {");
      for (int i = 0; i < words.size(); ++i) for (int j = 0; j < size[i]; ++j) {
        out.println("\"" + words.get(i) + "\" -- \"" + (graph[i][j] - words.size()) + "/"
            + gaps.get(graph[i][j] - words.size()).gapString + "\" [len=2]");
      }
      out.println("}");
      out.close();
    } catch (Exception e) {
      e.printStackTrace(); // wenn nicht installiert
    }
  }

  // Lücken-Datentyp
  public static class Gap {
    public final boolean firstGap;     // Satzanfang?
    public final int     length;       // Länge
    public final int[]   positions;    // Positionen der Buchstaben
    public final char[]  chars;        // Buchstaben
    public final String  followingSeq; // folgende Satzzeichen
    private final String gapString;    // String-Repräsentation der Lücke (Debugging)

    public String matchedWord; // zugeordnetes Wort

    public Gap(boolean fg, int l, int[] p, char[] c, String f) {
      firstGap = fg;
      length = l;
      positions = p;
      chars = c;
      followingSeq = f;
      char[] gs = new char[length];
      for (int i = 0; i < length; ++i) gs[i] = '_';
      for (int i = 0; i < positions.length; ++i) gs[positions[i]] = chars[i];
      gapString = new String(gs);
    }

    public boolean matches(String str) { // Überprüfung von Lücke-Wort-Matches
      boolean matches = str.length() == length && (!firstGap || Character.isUpperCase(str.charAt(0)));
      for (int i = 0; i < positions.length && matches; ++i) matches = str.charAt(positions[i]) == chars[i];
      return matches;
    }

    public String sentencePart() { // Satzteil für die Ausgabe am Ende
      return (matchedWord == null ? gapString : matchedWord) + followingSeq;
    }

    public String toString() { // Informationen über die Lücke
      return length + ":" + Arrays.toString(positions) + ":" + Arrays.toString(chars) + ":" + followingSeq
          + ":" + firstGap;
    }
  }

  // Wort-Datentyp
  public static class Word {
    public final String word;  // Wort
    public int          count; // Häufigkeit

    public Word(String w, int c) {
      this.word = w;
      this.count = c;
    }

    public String toString() { // Informationen über das Wort
      return word + ": " + count;
    }
  }
}
