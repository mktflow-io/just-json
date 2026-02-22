package io.mktflow.json;

import io.mktflow.json.records.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonErrorHandlingTest {

    @BeforeAll
    static void init() {
        TestInit.ensureInitialized();
    }

    @Test
    void nullInput() {
        assertThrows(JsonException.class, () -> Json.fromJson(null, Person.class));
    }

    @Test
    void emptyInput() {
        assertThrows(JsonException.class, () -> Json.fromJson("", Person.class));
    }

    @Test
    void blankInput() {
        assertThrows(JsonException.class, () -> Json.fromJson("   ", Person.class));
    }

    @Test
    void malformedJson() {
        assertThrows(JsonException.class, () -> Json.fromJson("{invalid}", Person.class));
    }

    @Test
    void unterminatedString() {
        assertThrows(JsonException.class, () -> Json.fromJson("{\"name\":\"unterminated", Person.class));
    }

    @Test
    void unterminatedObject() {
        assertThrows(JsonException.class, () -> Json.fromJson("{\"name\":\"test\"", Person.class));
    }

    @Test
    void unterminatedArray() {
        assertThrows(JsonException.class, () -> Json.fromJson("[1, 2", Person.class));
    }

    @Test
    void trailingCommaInObject() {
        assertThrows(JsonException.class, () -> Json.fromJson("{\"name\":\"test\",}", Person.class));
    }

    @Test
    void trailingCommaInArray() {
        assertThrows(JsonException.class, () -> Json.fromJson("{\"name\":\"a\",\"age\":1,\"tags\":[1,]}", Person.class));
    }

    @Test
    void typeMismatchStringForInt() {
        String json = """
                {"name":"Alice","age":"thirty","tags":[]}""";
        assertThrows(Exception.class, () -> Json.fromJson(json, Person.class));
    }

    @Test
    void typeMismatchIntForString() {
        String json = """
                {"name":42,"age":30,"tags":[]}""";
        assertThrows(JsonException.class, () -> Json.fromJson(json, Person.class));
    }

    @Test
    void invalidEnum() {
        String json = """
                {"id":"1","price":10,"quantity":1,"status":"UNKNOWN_VALUE"}""";
        assertThrows(Exception.class, () -> Json.fromJson(json, Order.class));
    }

    @Test
    void unregisteredType() {
        record Unregistered(String x) {}
        assertThrows(JsonException.class, () -> Json.toJson(new Unregistered("a")));
        assertThrows(JsonException.class, () -> Json.fromJson("{}", Unregistered.class));
    }

    @Test
    void invalidEscapeSequence() {
        assertThrows(JsonException.class, () -> Json.fromJson("{\"name\":\"bad\\x\"}", Person.class));
    }

    @Test
    void extraContentAfterJson() {
        assertThrows(JsonException.class, () -> Json.fromJson("{\"name\":\"a\",\"age\":1,\"tags\":[]}extra", Person.class));
    }
}
