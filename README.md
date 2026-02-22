# just-json

Zero-dependency, zero-reflection JSON library for Java records.

Mapping between records and JSON is generated at compile time by an annotation processor. No introspection at runtime — compatible with GraalVM native-image without configuration.

## Usage

Annotate a record with `@JsonRecord`:

```java
import io.mktflow.json.JsonRecord;
import java.util.List;

@JsonRecord
public record Person(String name, int age, List<String> tags) {}
```

Serialize and deserialize via the `Json` facade:

```java
import io.mktflow.json.Json;

// Initialize adapters once at startup
PersonJsonAdapter.ensureRegistered();
// or, if a registry was generated:
// JsonAdapterRegistry.initialize();

// Serialize
String json = Json.toJson(new Person("Alice", 30, List.of("dev", "java")));
// → {"name":"Alice","age":30,"tags":["dev","java"]}

// Deserialize
Person p = Json.fromJson(json, Person.class);
```

## Maven coordinates

```xml
<dependencies>
    <!-- Runtime dependency -->
    <dependency>
        <groupId>io.mktflow</groupId>
        <artifactId>just-json-core</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <!-- Annotation processor (compile-only) -->
    <dependency>
        <groupId>io.mktflow</groupId>
        <artifactId>just-json-processor</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.mktflow</groupId>
                        <artifactId>just-json-processor</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </path>
                    <path>
                        <groupId>io.mktflow</groupId>
                        <artifactId>just-json-core</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Supported types

| Type | Serialization | Deserialization |
|------|:---:|:---:|
| `int`, `long`, `double`, `float`, `boolean` | yes | yes |
| `Integer`, `Long`, `Double`, `Float`, `Boolean` | yes | yes |
| `String` | yes | yes |
| `BigDecimal` | yes | yes |
| `BigInteger` | yes | yes |
| `Enum` | yes | yes |
| `List<T>` | yes | yes |
| `Map<String, V>` | yes | yes |
| Nested `@JsonRecord` | yes | yes |
| `null` | yes | yes |

## Java modules (JPMS)

The library ships with `module-info.java` descriptors:

- **`io.mktflow.json`** — core runtime, exports `io.mktflow.json` and `io.mktflow.json.internal`
- **`io.mktflow.json.processor`** — annotation processor, provides `javax.annotation.processing.Processor`

```java
module my.app {
    requires io.mktflow.json;
}
```

The `io.mktflow.json.internal` package is exported because generated adapter code references it (the adapters live in the user's package). The `META-INF/services` SPI descriptor is also included for classpath compatibility.

## How it works

```
                    COMPILATION (annotation processor)
                    ─────────────────────────────────
@JsonRecord record Person(...)  →  PersonJsonAdapter (generated)
                                     toJson(Person) → String
                                     fromJson(JsonValue) → Person


                    RUNTIME (parser + writer)
                    ────────────────────────
JSON String → JsonTokenizer → JsonParser → JsonValue tree → PersonJsonAdapter.fromJson() → Person
Person → PersonJsonAdapter.toJson() → StringBuilder → String
```

The annotation processor reads record components at compile time and generates a `<Record>JsonAdapter` class per annotated record, plus a `JsonAdapterRegistry` that triggers class loading of all adapters. No reflection is used at runtime.

## Project structure

```
just-json/
├── just-json-core/          runtime: parser, writer, helpers, @JsonRecord
├── just-json-processor/     annotation processor (compile-only)
└── just-json-tests/         integration tests
```

## Building

```bash
sdk env          # Java 25 (GraalVM)
mvn clean test   # build + run 52 tests
```

## Requirements

- Java 25+
- Maven 3.x
