package ar.com.eclipta.launcher;

/** Picks what the location pill shows. Geocoders sometimes return a postal code as the locality ("X5000IAE"). */
final class CityName {
    private CityName() {}

    /** First candidate that reads as a place, or null. */
    static String pick(String... candidates) {
        for (String c : candidates) if (isPlace(c)) return c;
        return null;
    }

    /**
     * False for postal codes: every word has a digit. "9 de Julio" is a real town, so digits alone don't count.
     * Also false for the department "Capital" that Argentine provinces use: it says nothing, the province does.
     */
    static boolean isPlace(String name) {
        if (name == null || name.trim().matches("(?i)(departamento )?capital( department)?")) return false;
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty() && !word.matches(".*\\d.*")) return true;
        }
        return false;
    }
}
