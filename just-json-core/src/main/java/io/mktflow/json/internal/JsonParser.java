package io.mktflow.json.internal;

import io.mktflow.json.JsonException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class JsonParser {

    private final List<JsonToken> tokens;
    private int pos;

    public JsonParser(List<JsonToken> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public static JsonValue parse(String json) {
        if (json == null || json.isBlank()) {
            throw new JsonException("Input JSON string is null or empty");
        }
        var tokenizer = new JsonTokenizer(json);
        var tokens = tokenizer.tokenize();
        var parser = new JsonParser(tokens);
        JsonValue value = parser.parseValue();
        if (parser.current().type() != JsonToken.Type.EOF) {
            throw new JsonException("Unexpected token after end of JSON: " + parser.current());
        }
        return value;
    }

    private JsonToken current() {
        return tokens.get(pos);
    }

    private JsonToken advance() {
        JsonToken token = tokens.get(pos);
        pos++;
        return token;
    }

    private JsonToken expect(JsonToken.Type type) {
        JsonToken token = advance();
        if (token.type() != type) {
            throw new JsonException("Expected " + type + " but got " + token.type() + " at position " + token.position());
        }
        return token;
    }

    private JsonValue parseValue() {
        return switch (current().type()) {
            case LEFT_BRACE -> parseObject();
            case LEFT_BRACKET -> parseArray();
            case STRING -> new JsonValue.JsonString(advance().value());
            case NUMBER -> new JsonValue.JsonNumber(advance().value());
            case TRUE -> { advance(); yield new JsonValue.JsonBoolean(true); }
            case FALSE -> { advance(); yield new JsonValue.JsonBoolean(false); }
            case NULL -> { advance(); yield new JsonValue.JsonNull(); }
            default -> throw new JsonException("Unexpected token " + current().type() + " at position " + current().position());
        };
    }

    private JsonValue.JsonObject parseObject() {
        advance(); // skip {
        var members = new LinkedHashMap<String, JsonValue>();
        if (current().type() == JsonToken.Type.RIGHT_BRACE) {
            advance();
            return new JsonValue.JsonObject(members);
        }
        parseObjectMember(members);
        while (current().type() == JsonToken.Type.COMMA) {
            advance(); // skip ,
            parseObjectMember(members);
        }
        expect(JsonToken.Type.RIGHT_BRACE);
        return new JsonValue.JsonObject(members);
    }

    private void parseObjectMember(LinkedHashMap<String, JsonValue> members) {
        String key = expect(JsonToken.Type.STRING).value();
        expect(JsonToken.Type.COLON);
        JsonValue value = parseValue();
        members.put(key, value);
    }

    private JsonValue.JsonArray parseArray() {
        advance(); // skip [
        var elements = new ArrayList<JsonValue>();
        if (current().type() == JsonToken.Type.RIGHT_BRACKET) {
            advance();
            return new JsonValue.JsonArray(elements);
        }
        elements.add(parseValue());
        while (current().type() == JsonToken.Type.COMMA) {
            advance(); // skip ,
            elements.add(parseValue());
        }
        expect(JsonToken.Type.RIGHT_BRACKET);
        return new JsonValue.JsonArray(elements);
    }
}
