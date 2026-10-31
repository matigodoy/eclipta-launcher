package ar.com.eclipta.launcher;

/** Normalizes Argentine plates: old "ABC 123", Mercosur "AB 123 CD". Anything else stays as typed. */
final class PlateFormat {
    private PlateFormat() {}

    static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.toUpperCase(java.util.Locale.US).replaceAll("[^A-Z0-9]", "");
        if (s.matches("[A-Z]{3}[0-9]{3}")) return s.substring(0, 3) + " " + s.substring(3);
        if (isMercosur(s)) return s.substring(0, 2) + " " + s.substring(2, 5) + " " + s.substring(5);
        return raw.trim().toUpperCase(java.util.Locale.US);
    }

    static boolean isMercosur(String plate) {
        return plate != null && plate.replace(" ", "").matches("[A-Z]{2}[0-9]{3}[A-Z]{2}");
    }
}
