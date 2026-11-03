package ar.com.eclipta.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resource rules that newer emulators do not catch but the units' Android 9 does. */
public class ResourcesTest {
    private static final Pattern GRADIENT = Pattern.compile("<gradient\\b[^>]*>", Pattern.DOTALL);

    /** Android 9 drops the radius of a radial or sweep gradient when it applies a theme color, and crashes. */
    @Test
    public void noThemeColorsInRadialGradients() throws IOException {
        List<String> bad = new ArrayList<>();
        File[] dirs = new File("src/main/res").listFiles((d, n) -> n.startsWith("drawable"));
        assertTrue("res not found from " + new File("").getAbsolutePath(), dirs != null && dirs.length > 0);
        for (File dir : dirs) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".xml"));
            if (files == null) continue;
            for (File f : files) {
                Matcher m = GRADIENT.matcher(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                while (m.find()) {
                    String tag = m.group();
                    if (!tag.contains("android:type=\"linear\"") && tag.contains("android:type=") && tag.contains("?attr/")) {
                        bad.add(dir.getName() + "/" + f.getName());
                    }
                }
            }
        }
        assertTrue("Radial gradient with a theme color (crashes on Android 9): " + bad, bad.isEmpty());
    }

    /** Store-safe APK: makes, logos and wake animations only arrive in imported packs. */
    @Test
    public void noBrandsInTheApk() {
        File assets = new File("src/main/assets");
        assertTrue("assets not found from " + new File("").getAbsolutePath(), assets.isDirectory());
        assertFalse("assets/brands is back", new File(assets, "brands").exists());
        File[] packs = new File(assets, "packs").listFiles(File::isDirectory);
        assertTrue(packs != null && packs.length > 0);
        for (File pack : packs) {
            assertFalse(pack.getName() + " has a logo", new File(pack, "logo.webp").exists());
            assertFalse(pack.getName() + " has an animation", new File(pack, "sting.webp").exists());
        }
    }
}
