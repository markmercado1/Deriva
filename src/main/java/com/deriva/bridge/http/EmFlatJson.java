package com.deriva.bridge.http;

import java.util.HashMap;
import java.util.Map;

/** Strict bounded protocol grammar: object of strings, nulls and unsigned integer values.
 * Rejects duplicate keys, nested values, trailing input and all unneeded JSON types.
 * No dependency, reflection or substring/regex parsing of JSON. */
final class EmFlatJson {
    private final String text;
    private int position;
    private EmFlatJson(String text) { this.text = text; }

    static Map<String, Object> parse(String text) {
        return new EmFlatJson(text).object();
    }

    private Map<String, Object> object() {
        Map<String, Object> values = new HashMap<>();
        expect('{');
        whitespace();
        if (take('}')) { finish(); return values; }
        do {
            whitespace();
            String key = string();
            expect(':');
            whitespace();
            Object value;
            if (peek() == '"') value = string();
            else if (text.startsWith("null", position)) { position += 4; value = null; }
            else value = integer();
            if (values.containsKey(key)) throw invalid();
            values.put(key, value);
            whitespace();
            if (take('}')) { finish(); return values; }
            expect(',');
        } while (true);
    }

    private long integer() {
        int start = position;
        while (peek() >= '0' && peek() <= '9') position++;
        if (position == start || (position - start > 1 && text.charAt(start) == '0')) throw invalid();
        try { return Long.parseLong(text.substring(start, position)); }
        catch (NumberFormatException ex) { throw invalid(); }
    }

    private String string() {
        if (!take('"')) throw invalid();
        StringBuilder result = new StringBuilder();
        while (position < text.length()) {
            char c = text.charAt(position++);
            if (c == '"') return result.toString();
            if (c < 0x20 || Character.isSurrogate(c)) throw invalid();
            if (c == '\\') {
                if (position == text.length()) throw invalid();
                char escape = text.charAt(position++);
                c = switch (escape) {
                    case '"', '\\', '/' -> escape;
                    case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n';
                    case 'r' -> '\r'; case 't' -> '\t';
                    case 'u' -> unicode();
                    default -> throw invalid();
                };
            }
            result.append(c);
        }
        throw invalid();
    }

    private char unicode() {
        if (position + 4 > text.length()) throw invalid();
        int value = 0;
        for (int i = 0; i < 4; i++) {
            char c = text.charAt(position++);
            int digit = Character.digit(c, 16);
            if (digit < 0 || c > 127) throw invalid();
            value = value * 16 + digit;
        }
        char c = (char) value;
        // Protocol keys, hosts and identifiers never require surrogate pairs.
        if (Character.isSurrogate(c)) throw invalid();
        return c;
    }

    private char peek() { return position < text.length() ? text.charAt(position) : '\0'; }
    private boolean take(char c) {
        if (peek() != c) return false;
        position++; return true;
    }
    private void whitespace() {
        while (peek() == ' ' || peek() == '\n' || peek() == '\r' || peek() == '\t') position++;
    }
    private void expect(char c) { whitespace(); if (!take(c)) throw invalid(); }
    private void finish() { whitespace(); if (position != text.length()) throw invalid(); }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid JSON"); }
}
