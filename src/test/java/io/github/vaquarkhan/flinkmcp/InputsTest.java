package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vaquarkhan.flinkmcp.util.Inputs;
import org.junit.jupiter.api.Test;

class InputsTest {

    @Test
    void requireId_acceptsValidIds() {
        assertEquals("job-1", Inputs.requireId("job-1"));
        assertEquals("abc_DEF.09-", Inputs.requireId("abc_DEF.09-"));
    }

    @Test
    void requireId_rejectsPathTraversalAndSlashes() {
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId("id/yarn-cancel"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId("../.."));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId("a/../b"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId("id%2f.."));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId("has space"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId(null));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireId(""));
    }

    @Test
    void requireInt_validatesDigits() {
        assertEquals("42", Inputs.requireInt("42"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireInt("-1"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireInt("1.5"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireInt("abc"));
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireInt(null));
    }

    @Test
    void jsonEscape_escapesQuotesAndControls() {
        String out = Inputs.jsonEscape("a\"b\\c\n\r\t\b\f");
        assertTrue(out.contains("\\\""));
        assertTrue(out.contains("\\\\"));
        assertTrue(out.contains("\\n"));
        assertTrue(out.contains("\\r"));
        assertTrue(out.contains("\\t"));
        assertTrue(out.contains("\\b"));
        assertTrue(out.contains("\\f"));
        assertEquals("\\u0001", Inputs.jsonEscape("\u0001"));
    }
}
