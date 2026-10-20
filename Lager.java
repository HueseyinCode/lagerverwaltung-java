import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Lokale Lagerverwaltung; TSV-Datei als einfache persistente Speicherung. */
public class Lager {
    record Produkt(String id, String name, int bestand) {}
    static final Path DATEI = Path.of("lager.tsv");

    static Map<String, Produkt> laden() throws IOException {
        Map<String, Produkt> produkte = new TreeMap<>();
        if (!Files.exists(DATEI)) return produkte;
        for (String zeile : Files.readAllLines(DATEI, StandardCharsets.UTF_8)) {
            String[] teile = zeile.split("\t", -1);
            if (teile.length != 3) throw new IOException("Ungültige Lagerdatei.");
            int bestand;
            try { bestand = Integer.parseInt(teile[2]); }
            catch (NumberFormatException e) { throw new IOException("Ungültiger Bestand.", e); }
            if (!teile[0].matches("[A-Za-z0-9_-]+") || teile[1].isBlank() || bestand < 0
                    || produkte.containsKey(teile[0])) throw new IOException("Ungültige Produktdaten.");
            produkte.put(teile[0], new Produkt(teile[0], teile[1], bestand));
        }
        return produkte;
    }

    static void speichern(Map<String, Produkt> produkte) throws IOException {
        List<String> zeilen = produkte.values().stream()
            .map(p -> p.id() + "\t" + p.name() + "\t" + p.bestand()).toList();
        Path temp = DATEI.resolveSibling("lager.tsv.tmp");
        Files.write(temp, zeilen, StandardCharsets.UTF_8);
        try { Files.move(temp, DATEI, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, DATEI, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void main(String[] args) throws IOException {
        Map<String, Produkt> produkte = laden();
        System.out.println("Befehle: liste | neu ID Name | buche ID Menge | ende");
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                String eingabe = scanner.nextLine().trim();
                if (eingabe.equals("ende")) break;
                if (eingabe.equals("liste")) {
                    System.out.println("ID | Name | Bestand");
                    produkte.values().forEach(p -> System.out.printf("%s | %s | %d%n", p.id(), p.name(), p.bestand()));
                    continue;
                }
                String[] teile = eingabe.split("\\s+", 3);
                try {
                    Map<String, Produkt> neu = new TreeMap<>(produkte);
                    if (teile.length != 3) throw new IllegalArgumentException("Befehl benötigt ID und Wert.");
                    String id = teile[1];
                    if (teile[0].equals("neu")) {
                        if (!id.matches("[A-Za-z0-9_-]+") || neu.containsKey(id))
                            throw new IllegalArgumentException("ID ungültig oder bereits vorhanden.");
                        if (teile[2].isBlank() || teile[2].contains("\t"))
                            throw new IllegalArgumentException("Name ungültig.");
                        neu.put(id, new Produkt(id, teile[2], 0));
                    } else if (teile[0].equals("buche")) {
                        Produkt p = neu.get(id);
                        if (p == null) throw new IllegalArgumentException("Produkt nicht gefunden.");
                        int menge = Integer.parseInt(teile[2]);
                        int bestand = Math.addExact(p.bestand(), menge);
                        if (bestand < 0) throw new IllegalArgumentException("Bestand reicht nicht aus.");
                        neu.put(id, new Produkt(id, p.name(), bestand));
                    } else throw new IllegalArgumentException("Unbekannter Befehl.");
                    speichern(neu);
                    produkte = neu;
                    System.out.println("Gespeichert.");
                } catch (IllegalArgumentException | ArithmeticException e) {
                    System.out.println("Eingabe abgelehnt: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Speichern fehlgeschlagen: " + e.getMessage());
                }
            }
        }
    }
}
