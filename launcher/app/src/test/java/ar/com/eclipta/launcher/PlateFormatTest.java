package ar.com.eclipta.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlateFormatTest {
    @Test
    public void oldFormat() {
        assertEquals("ABC 123", PlateFormat.normalize("abc123"));
        assertEquals("ABC 123", PlateFormat.normalize(" a-b c 1 2 3 "));
    }

    @Test
    public void mercosur() {
        assertEquals("AB 123 CD", PlateFormat.normalize("ab123cd"));
        assertTrue(PlateFormat.isMercosur("AB 123 CD"));
        assertFalse(PlateFormat.isMercosur("ABC 123"));
    }

    @Test
    public void otherCountriesStayAsTyped() {
        assertEquals("191 QXS 75", PlateFormat.normalize("191 qxs 75"));
        assertEquals("", PlateFormat.normalize(null));
    }

    @Test
    public void cityNameSkipsPostalCodes() {
        assertEquals("Córdoba", CityName.pick("X5000IAE", "Capital", "Córdoba"));
        assertEquals("Punilla", CityName.pick(null, "Punilla", "Córdoba"));
        assertEquals("Capital Federal", CityName.pick("Capital Federal", "Otro"));
        assertEquals("9 de Julio", CityName.pick("9 de Julio", "Otro"));
        assertFalse(CityName.isPlace("5000"));
        assertFalse(CityName.isPlace("SW1A 1AA"));
        assertFalse(CityName.isPlace("  "));
        assertEquals(null, CityName.pick("X5000IAE", null));
    }
}
