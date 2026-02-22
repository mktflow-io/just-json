package io.mktflow.json.internal;

public record JsonToken(Type type, String value, int position) {

    public enum Type {
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        COLON,
        COMMA,
        STRING,
        NUMBER,
        TRUE,
        FALSE,
        NULL,
        EOF
    }
}
