package com.mparticle.kits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mparticle.kits.AppboyKit.StringTypeParser;

import org.junit.Test;

public class TypeDetectionTests {

    @Test
    public void testEnableTypeDetection() {
        SomeParser parser = new SomeParser(true);
        assertEquals("foo", parser.parseValue("key", "foo"));
        assertEquals(1, parser.parseValue("key", "1"));
        assertEquals(-99, parser.parseValue("key", "-99"));
        assertEquals(Integer.MAX_VALUE, parser.parseValue("key", String.valueOf(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE, parser.parseValue("key", String.valueOf(Integer.MIN_VALUE)));
        assertEquals(Integer.MAX_VALUE + 1L, parser.parseValue("key", String.valueOf(Integer.MAX_VALUE + 1L)));
        assertEquals(Integer.MIN_VALUE - 1L, parser.parseValue("key", String.valueOf(Integer.MIN_VALUE - 1L)));
        assertEquals(Long.MAX_VALUE, parser.parseValue("key", String.valueOf(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, parser.parseValue("key", String.valueOf(Long.MIN_VALUE)));
        assertEquals(432.2561, parser.parseValue("key", "432.2561"));
        assertEquals(-1.0001, parser.parseValue("key", "-1.0001"));
        assertEquals(false, parser.parseValue("key", "false"));
        assertEquals(true, parser.parseValue("key", "True"));

        assertTrue(parser.parseValue("key", "1.0") instanceof Double);
        assertTrue(parser.parseValue("key", String.valueOf(Integer.MAX_VALUE + 1L)) instanceof Long);
        assertTrue(parser.parseValue("key", String.valueOf(Integer.MAX_VALUE)) instanceof Integer);
        assertTrue(parser.parseValue("key", "0") instanceof Integer);
        assertTrue(parser.parseValue("key", "true") instanceof Boolean);
        assertTrue(parser.parseValue("key", "True") instanceof Boolean);
    }

    @Test
    public void testDisableTypeDetection() {
        SomeParser parser = new SomeParser(false);
        assertEquals("foo", parser.parseValue("key", "foo"));
        assertEquals("1", parser.parseValue("key", "1"));
        assertEquals(String.valueOf(Integer.MAX_VALUE + 1L), parser.parseValue("key", String.valueOf(Integer.MAX_VALUE + 1L)));
        assertEquals("432.2561", parser.parseValue("key", "432.2561"));
        assertEquals("true", parser.parseValue("key", "true"));
    }

    private class SomeParser extends StringTypeParser {
        SomeParser(Boolean enableTypeDetection) {
            super(enableTypeDetection);
        }
        @Override public void toString(String key, String value) { /* do nothing */ }
        @Override public void toInt(String key, int value) { /* do nothing */ }
        @Override public void toLong(String key, long value) { /* do nothing */ }
        @Override public void toDouble(String key, double value) { /* do nothing */ }
        @Override public void toBoolean(String key, boolean value) { /* do nothing */ }
    }
}