package io.mktflow.json;

import io.mktflow.json.records.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonEdgeCasesTest {

    @BeforeAll
    static void init() {
        TestInit.ensureInitialized();
    }

    @Test
    void unicodeInStrings() {
        var person = new Person("Ren\u00e9 \u00e9l\u00e8ve", 25, List.of("\u2603", "\ud83d\ude00"));
        String json = Json.toJson(person);
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(person, restored);
    }

    @Test
    void escapedCharactersInStrings() {
        var person = new Person("line1\nline2\ttab\\back\"quote", 1, List.of());
        String json = Json.toJson(person);
        // Verify escaping happened
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\\\"));
        assertTrue(json.contains("\\\""));
        // Roundtrip
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(person, restored);
    }

    @Test
    void emptyString() {
        var person = new Person("", 0, List.of(""));
        String json = Json.toJson(person);
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(person, restored);
    }

    @Test
    void bigNumbers() {
        var big = new AllTypes(
                Integer.MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE, Float.MAX_VALUE, false,
                Integer.MIN_VALUE, Long.MIN_VALUE, Double.MIN_VALUE, Float.MIN_VALUE, false,
                null,
                new BigDecimal("99999999999999999999999999999.999999999999999"),
                new BigInteger("99999999999999999999999999999"),
                Status.INACTIVE);
        String json = Json.toJson(big);
        AllTypes restored = Json.fromJson(json, AllTypes.class);
        assertEquals(big.primitiveInt(), restored.primitiveInt());
        assertEquals(big.primitiveLong(), restored.primitiveLong());
        assertEquals(big.bigDecimal(), restored.bigDecimal());
        assertEquals(big.bigInteger(), restored.bigInteger());
    }

    @Test
    void nestedInList() {
        var a1 = new Address("street1", "city1", "zip1");
        var a2 = new Address("street2", "city2", "zip2");
        var nested = new Nested("multi", a1, List.of(a1, a2));
        String json = Json.toJson(nested);
        Nested restored = Json.fromJson(json, Nested.class);
        assertEquals(nested, restored);
    }

    @Test
    void deserializeWithExtraFields() {
        // Extra fields in JSON should be ignored (not in record)
        String json = """
                {"name":"Alice","age":30,"tags":[],"extra":"ignored","unknown":42}""";
        Person p = Json.fromJson(json, Person.class);
        assertEquals("Alice", p.name());
        assertEquals(30, p.age());
        assertEquals(List.of(), p.tags());
    }

    @Test
    void negativeNumbers() {
        var person = new Person("neg", -42, List.of());
        String json = Json.toJson(person);
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(-42, restored.age());
    }

    @Test
    void zeroValues() {
        var person = new Person("zero", 0, List.of());
        String json = Json.toJson(person);
        assertTrue(json.contains("\"age\":0"));
        Person restored = Json.fromJson(json, Person.class);
        assertEquals(0, restored.age());
    }

    @Test
    void unicodeEscapeInJson() {
        String json = """
                {"name":"Ren\\u00e9","age":1,"tags":[]}""";
        Person p = Json.fromJson(json, Person.class);
        assertEquals("Ren\u00e9", p.name());
    }

    @Test
    void deserializeScientificNotation() {
        String json = """
                {"primitiveInt":1,"primitiveLong":2,"primitiveDouble":3.14e2,"primitiveFloat":1.5,\
                "primitiveBoolean":false,"boxedInt":null,"boxedLong":null,"boxedDouble":null,"boxedFloat":null,\
                "boxedBoolean":null,"text":null,"bigDecimal":1.23e4,"bigInteger":100,"status":null}""";
        AllTypes a = Json.fromJson(json, AllTypes.class);
        assertEquals(314.0, a.primitiveDouble(), 0.001);
        assertEquals(new BigDecimal("1.23E+4"), a.bigDecimal());
    }
}
