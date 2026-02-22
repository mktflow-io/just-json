package io.mktflow.json.internal;

import io.mktflow.json.JsonException;

import java.util.ArrayList;
import java.util.List;

public final class JsonTokenizer {

    private final String input;
    private int pos;

    public JsonTokenizer(String input) {
        this.input = input;
        this.pos = 0;
    }

    public List<JsonToken> tokenize() {
        var tokens = new ArrayList<JsonToken>();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) {
                break;
            }
            char c = input.charAt(pos);
            switch (c) {
                case '{' -> tokens.add(new JsonToken(JsonToken.Type.LEFT_BRACE, "{", pos++));
                case '}' -> tokens.add(new JsonToken(JsonToken.Type.RIGHT_BRACE, "}", pos++));
                case '[' -> tokens.add(new JsonToken(JsonToken.Type.LEFT_BRACKET, "[", pos++));
                case ']' -> tokens.add(new JsonToken(JsonToken.Type.RIGHT_BRACKET, "]", pos++));
                case ':' -> tokens.add(new JsonToken(JsonToken.Type.COLON, ":", pos++));
                case ',' -> tokens.add(new JsonToken(JsonToken.Type.COMMA, ",", pos++));
                case '"' -> tokens.add(readString());
                case 't' -> tokens.add(readLiteral("true", JsonToken.Type.TRUE));
                case 'f' -> tokens.add(readLiteral("false", JsonToken.Type.FALSE));
                case 'n' -> tokens.add(readLiteral("null", JsonToken.Type.NULL));
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        tokens.add(readNumber());
                    } else {
                        throw new JsonException("Unexpected character '" + c + "' at position " + pos);
                    }
                }
            }
        }
        tokens.add(new JsonToken(JsonToken.Type.EOF, "", pos));
        return tokens;
    }

    private void skipWhitespace() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private JsonToken readString() {
        int start = pos;
        pos++; // skip opening quote
        var sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos >= input.length()) {
                    throw new JsonException("Unterminated string escape at position " + pos);
                }
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 >= input.length()) {
                            throw new JsonException("Unterminated unicode escape at position " + pos);
                        }
                        String hex = input.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> throw new JsonException("Invalid escape character '\\" + escaped + "' at position " + pos);
                }
                pos++;
            } else if (c == '"') {
                pos++; // skip closing quote
                return new JsonToken(JsonToken.Type.STRING, sb.toString(), start);
            } else {
                sb.append(c);
                pos++;
            }
        }
        throw new JsonException("Unterminated string starting at position " + start);
    }

    private JsonToken readNumber() {
        int start = pos;
        if (pos < input.length() && input.charAt(pos) == '-') {
            pos++;
        }
        if (pos >= input.length() || input.charAt(pos) < '0' || input.charAt(pos) > '9') {
            throw new JsonException("Invalid number at position " + start);
        }
        if (input.charAt(pos) == '0') {
            pos++;
        } else {
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
        }
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            if (pos >= input.length() || input.charAt(pos) < '0' || input.charAt(pos) > '9') {
                throw new JsonException("Invalid number at position " + start);
            }
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
        }
        if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
            pos++;
            if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                pos++;
            }
            if (pos >= input.length() || input.charAt(pos) < '0' || input.charAt(pos) > '9') {
                throw new JsonException("Invalid number at position " + start);
            }
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
        }
        return new JsonToken(JsonToken.Type.NUMBER, input.substring(start, pos), start);
    }

    private JsonToken readLiteral(String expected, JsonToken.Type type) {
        int start = pos;
        if (pos + expected.length() > input.length() || !input.substring(pos, pos + expected.length()).equals(expected)) {
            throw new JsonException("Expected '" + expected + "' at position " + pos);
        }
        pos += expected.length();
        return new JsonToken(type, expected, start);
    }
}
