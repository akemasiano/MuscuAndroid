package fr.muscu.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatCmTest {
    @Test
    fun centimetresUseAFrenchDecimalCommaAndAtMostOneDecimal() {
        assertEquals("38,5", formatCm(38.5))
        assertEquals("38", formatCm(38.0))
        assertEquals("38", formatCm(38.04))
        assertEquals("112", formatCm(112.0))
    }

    @Test
    fun deltasAreSignedAndNegligibleOnesShowZero() {
        assertEquals("+0,5", formatCmDelta(0.5))
        assertEquals("+2", formatCmDelta(2.0))
        assertEquals("0", formatCmDelta(0.0))
        assertEquals("0", formatCmDelta(0.01))
        assertEquals("0", formatCmDelta(-0.04))
        // Selon le JDK, le signe moins peut être un tiret ou un vrai signe moins typographique.
        assertEquals("-1", formatCmDelta(-1.0).replace('−', '-'))
        assertEquals("-1,5", formatCmDelta(-1.5).replace('−', '-'))
    }
}
