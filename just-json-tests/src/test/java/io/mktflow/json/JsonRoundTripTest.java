package io.mktflow.json;

import io.mktflow.json.records.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonRoundTripTest {

    @BeforeAll
    static void init() {
        TestInit.ensureInitialized();
    }

    @Test
    void roundTripPerson() {
        var original = new Person("Alice", 30, List.of("dev", "java"));
        String json = Json.toJson(original);
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(original, restored);
    }

    @Test
    void roundTripOrder() {
        var original = new Order("ORD-1", new BigDecimal("99.99"), 5, Status.ACTIVE);
        String json = Json.toJson(original);
        Order restored = Json.fromJson(json, Order.class);
        assertEquals(original, restored);
    }

    @Test
    void roundTripNested() {
        var address = new Address("123 Main St", "Paris", "75001");
        var other = new Address("456 Side St", "Lyon", "69001");
        var original = new Nested("home", address, List.of(address, other));
        String json = Json.toJson(original);
        Nested restored = Json.fromJson(json, Nested.class);
        assertEquals(original, restored);
    }

    @Test
    void roundTripAllTypes() {
        var original = new AllTypes(1, 2L, 3.14, 1.5f, true,
                10, 20L, 3.14, 1.5f, true, "hello",
                new BigDecimal("123.456"), new BigInteger("999"), Status.PENDING);
        String json = Json.toJson(original);
        AllTypes restored = Json.fromJson(json, AllTypes.class);
        assertEquals(original.primitiveInt(), restored.primitiveInt());
        assertEquals(original.primitiveLong(), restored.primitiveLong());
        assertEquals(original.primitiveDouble(), restored.primitiveDouble(), 0.0001);
        assertEquals(original.primitiveFloat(), restored.primitiveFloat(), 0.0001);
        assertEquals(original.primitiveBoolean(), restored.primitiveBoolean());
        assertEquals(original.boxedInt(), restored.boxedInt());
        assertEquals(original.boxedLong(), restored.boxedLong());
        assertEquals(original.text(), restored.text());
        assertEquals(original.bigDecimal(), restored.bigDecimal());
        assertEquals(original.bigInteger(), restored.bigInteger());
        assertEquals(original.status(), restored.status());
    }

    @Test
    void roundTripWithMap() {
        var original = new WithMap("test", Map.of("a", 1, "b", 2));
        String json = Json.toJson(original);
        WithMap restored = Json.fromJson(json, WithMap.class);
        assertEquals(original.name(), restored.name());
        assertEquals(original.scores(), restored.scores());
    }

    @Test
    void roundTripWithNulls() {
        var original = new Person(null, 0, null);
        String json = Json.toJson(original);
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(original, restored);
    }

    @Test
    void roundTripEmpty() {
        var original = new Empty();
        String json = Json.toJson(original);
        Empty restored = Json.fromJson(json, Empty.class);
        assertEquals(original, restored);
    }

    @Test
    void roundTripNullNested() {
        var original = new Nested("label", null, null);
        String json = Json.toJson(original);
        Nested restored = Json.fromJson(json, Nested.class);
        assertEquals(original, restored);
    }
}
