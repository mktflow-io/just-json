package io.mktflow.json.processor;

import io.mktflow.json.JsonProperty;
import io.mktflow.json.JsonRecord;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("io.mktflow.json.JsonRecord")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class JsonRecordProcessor extends AbstractProcessor {

    private final List<String> adapterClassNames = new ArrayList<>();
    private boolean registryGenerated = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var annotatedElements = roundEnv.getElementsAnnotatedWith(JsonRecord.class);

        for (Element element : annotatedElements) {
            if (!(element instanceof TypeElement typeElement)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@JsonRecord can only be applied to records", element);
                continue;
            }
            if (typeElement.getRecordComponents().isEmpty() && typeElement.getKind().toString().equals("CLASS")) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@JsonRecord can only be applied to records", element);
                continue;
            }
            generateAdapter(typeElement);
        }

        // Generate registry in the same round as adapters (not in processingOver)
        // so the generated source file participates in compilation
        if (!registryGenerated && !adapterClassNames.isEmpty() && !annotatedElements.isEmpty()) {
            generateRegistry();
            registryGenerated = true;
        }

        return true;
    }

    private String getJsonKey(RecordComponentElement comp) {
        JsonProperty prop = comp.getAnnotation(JsonProperty.class);
        return prop != null ? prop.value() : comp.getSimpleName().toString();
    }

    /**
     * Checks if an enum type has any constants annotated with @JsonProperty.
     * Returns a map of constant name -> custom JSON value, or empty map if none.
     */
    private Map<String, String> getEnumCustomNames(TypeElement enumType) {
        var result = new LinkedHashMap<String, String>();
        for (Element enclosed : enumType.getEnclosedElements()) {
            if (enclosed instanceof VariableElement ve && ve.getKind().toString().equals("ENUM_CONSTANT")) {
                JsonProperty prop = ve.getAnnotation(JsonProperty.class);
                if (prop != null) {
                    result.put(ve.getSimpleName().toString(), prop.value());
                }
            }
        }
        return result;
    }

    private boolean enumHasCustomNames(TypeElement enumType) {
        return !getEnumCustomNames(enumType).isEmpty();
    }

    /**
     * Collects ALL enum constant names for a given enum type (for building complete maps).
     * Returns constant name -> JSON value (custom if annotated, otherwise enum name).
     */
    private Map<String, String> getFullEnumMapping(TypeElement enumType) {
        var result = new LinkedHashMap<String, String>();
        var customNames = getEnumCustomNames(enumType);
        for (Element enclosed : enumType.getEnclosedElements()) {
            if (enclosed instanceof VariableElement ve && ve.getKind().toString().equals("ENUM_CONSTANT")) {
                String name = ve.getSimpleName().toString();
                result.put(name, customNames.getOrDefault(name, name));
            }
        }
        return result;
    }

    private void generateAdapter(TypeElement recordElement) {
        String packageName = processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();
        String recordSimpleName = recordElement.getSimpleName().toString();
        String adapterSimpleName = recordSimpleName + "JsonAdapter";
        String adapterQualifiedName = packageName.isEmpty() ? adapterSimpleName : packageName + "." + adapterSimpleName;

        adapterClassNames.add(adapterQualifiedName);

        List<? extends RecordComponentElement> components = recordElement.getRecordComponents();

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(adapterQualifiedName, recordElement);
            try (var out = new PrintWriter(file.openWriter())) {
                if (!packageName.isEmpty()) {
                    out.println("package " + packageName + ";");
                    out.println();
                }

                out.println("import io.mktflow.json.Json;");
                out.println("import io.mktflow.json.internal.JsonMapper;");
                out.println("import io.mktflow.json.internal.JsonValue;");
                out.println("import io.mktflow.json.internal.JsonWriter;");
                out.println();
                out.println("/**");
                out.println(" * Generated JSON adapter for {@link " + recordSimpleName + "}.");
                out.println(" * Do not modify — regenerated by the annotation processor.");
                out.println(" */");
                out.println("public final class " + adapterSimpleName + " {");
                out.println();
                out.println("    private " + adapterSimpleName + "() {}");
                out.println();

                // Generate static enum maps for any enum fields with @JsonProperty
                generateEnumMaps(out, components);

                // static initializer to register with Json facade
                out.println("    static {");
                out.println("        Json.registerAdapter(" + recordSimpleName + ".class, " + adapterSimpleName + "::toJson, " + adapterSimpleName + "::fromJson);");
                out.println("    }");
                out.println();

                // ensure class is loaded
                out.println("    public static void ensureRegistered() {}");
                out.println();

                // --- toJson ---
                out.println("    public static String toJson(" + recordSimpleName + " obj) {");
                out.println("        if (obj == null) return \"null\";");
                out.println("        StringBuilder sb = new StringBuilder();");
                out.println("        sb.append('{');");

                for (int i = 0; i < components.size(); i++) {
                    RecordComponentElement comp = components.get(i);
                    String fieldName = comp.getSimpleName().toString();
                    String jsonKey = getJsonKey(comp);
                    TypeMirror fieldType = comp.asType();

                    if (i > 0) {
                        out.println("        sb.append(',');");
                    }
                    out.println("        JsonWriter.writeString(\"" + jsonKey + "\", sb);");
                    out.println("        sb.append(':');");
                    generateWriteField(out, fieldName, fieldType, recordSimpleName);
                }

                out.println("        sb.append('}');");
                out.println("        return sb.toString();");
                out.println("    }");
                out.println();

                // --- fromJson ---
                out.println("    public static " + recordSimpleName + " fromJson(JsonValue value) {");
                out.println("        if (value instanceof JsonValue.JsonNull) return null;");
                out.println("        JsonValue.JsonObject obj = (JsonValue.JsonObject) value;");

                for (RecordComponentElement comp : components) {
                    String fieldName = comp.getSimpleName().toString();
                    String jsonKey = getJsonKey(comp);
                    TypeMirror fieldType = comp.asType();
                    generateReadField(out, fieldName, jsonKey, fieldType);
                }

                out.print("        return new " + recordSimpleName + "(");
                for (int i = 0; i < components.size(); i++) {
                    if (i > 0) out.print(", ");
                    out.print(components.get(i).getSimpleName().toString());
                }
                out.println(");");
                out.println("    }");

                out.println("}");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate adapter for " + recordSimpleName + ": " + e.getMessage(), recordElement);
        }
    }

    private void generateEnumMaps(PrintWriter out, List<? extends RecordComponentElement> components) {
        for (RecordComponentElement comp : components) {
            TypeMirror fieldType = comp.asType();
            if (fieldType.getKind() == TypeKind.DECLARED) {
                DeclaredType dt = (DeclaredType) fieldType;
                TypeElement te = (TypeElement) dt.asElement();
                if (isEnum(te) && enumHasCustomNames(te)) {
                    generateEnumMapFields(out, te);
                }
            }
            // Also check List<Enum> fields
            if (fieldType.getKind() == TypeKind.DECLARED) {
                DeclaredType dt = (DeclaredType) fieldType;
                String typeName = ((TypeElement) dt.asElement()).getQualifiedName().toString();
                if (typeName.equals("java.util.List") && !dt.getTypeArguments().isEmpty()) {
                    TypeMirror elemType = dt.getTypeArguments().getFirst();
                    if (elemType.getKind() == TypeKind.DECLARED) {
                        TypeElement elemTe = (TypeElement) ((DeclaredType) elemType).asElement();
                        if (isEnum(elemTe) && enumHasCustomNames(elemTe)) {
                            generateEnumMapFields(out, elemTe);
                        }
                    }
                }
            }
        }
    }

    private final java.util.Set<String> generatedEnumMaps = new java.util.HashSet<>();

    private void generateEnumMapFields(PrintWriter out, TypeElement enumType) {
        String enumQualified = enumType.getQualifiedName().toString();
        if (generatedEnumMaps.contains(enumQualified)) {
            return; // already generated for this adapter
        }
        generatedEnumMaps.add(enumQualified);

        String enumSimple = enumType.getSimpleName().toString();
        Map<String, String> mapping = getFullEnumMapping(enumType);

        // Generate DESERIALIZE_<EnumName> : Map<String, EnumType>
        String deserMapName = "DESERIALIZE_" + toConstantName(enumSimple);
        out.println("    private static final java.util.Map<String, " + enumQualified + "> " + deserMapName + " = java.util.Map.ofEntries(");
        int idx = 0;
        for (var entry : mapping.entrySet()) {
            String comma = (idx < mapping.size() - 1) ? "," : "";
            out.println("        java.util.Map.entry(\"" + entry.getValue() + "\", " + enumQualified + "." + entry.getKey() + ")" + comma);
            idx++;
        }
        out.println("    );");
        out.println();

        // Generate SERIALIZE_<EnumName> : Map<EnumType, String>
        String serMapName = "SERIALIZE_" + toConstantName(enumSimple);
        out.println("    private static final java.util.Map<" + enumQualified + ", String> " + serMapName + " = java.util.Map.ofEntries(");
        idx = 0;
        for (var entry : mapping.entrySet()) {
            String comma = (idx < mapping.size() - 1) ? "," : "";
            out.println("        java.util.Map.entry(" + enumQualified + "." + entry.getKey() + ", \"" + entry.getValue() + "\")" + comma);
            idx++;
        }
        out.println("    );");
        out.println();
    }

    private String toConstantName(String simpleName) {
        // Convert CamelCase to UPPER_SNAKE_CASE
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private void generateWriteField(PrintWriter out, String fieldName, TypeMirror type, String recordName) {
        String accessor = "obj." + fieldName + "()";
        TypeKind kind = type.getKind();

        switch (kind) {
            case INT -> out.println("        JsonWriter.writeInt(" + accessor + ", sb);");
            case LONG -> out.println("        JsonWriter.writeLong(" + accessor + ", sb);");
            case DOUBLE -> out.println("        JsonWriter.writeDouble(" + accessor + ", sb);");
            case FLOAT -> out.println("        JsonWriter.writeFloat(" + accessor + ", sb);");
            case BOOLEAN -> out.println("        JsonWriter.writeBoolean(" + accessor + ", sb);");
            case DECLARED -> generateWriteDeclaredField(out, accessor, (DeclaredType) type);
            default -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Unsupported field type: " + type + " for field " + fieldName);
        }
    }

    private void generateWriteDeclaredField(PrintWriter out, String accessor, DeclaredType type) {
        String typeName = ((TypeElement) type.asElement()).getQualifiedName().toString();

        switch (typeName) {
            case "java.lang.String" -> out.println("        JsonWriter.writeString(" + accessor + ", sb);");
            case "java.lang.Integer" -> out.println("        JsonWriter.writeInteger(" + accessor + ", sb);");
            case "java.lang.Long" -> out.println("        JsonWriter.writeLongBoxed(" + accessor + ", sb);");
            case "java.lang.Double" -> out.println("        JsonWriter.writeDoubleBoxed(" + accessor + ", sb);");
            case "java.lang.Float" -> out.println("        JsonWriter.writeFloatBoxed(" + accessor + ", sb);");
            case "java.lang.Boolean" -> out.println("        JsonWriter.writeBooleanBoxed(" + accessor + ", sb);");
            case "java.math.BigDecimal" -> out.println("        JsonWriter.writeBigDecimal(" + accessor + ", sb);");
            case "java.math.BigInteger" -> out.println("        JsonWriter.writeBigInteger(" + accessor + ", sb);");
            case "java.util.List" -> {
                if (type.getTypeArguments().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Raw List type not supported, use List<T>");
                    return;
                }
                TypeMirror elementType = type.getTypeArguments().getFirst();
                String elementWriter = getElementWriterLambda(elementType);
                out.println("        JsonWriter.writeList(" + accessor + ", sb, " + elementWriter + ");");
            }
            case "java.util.Map" -> {
                if (type.getTypeArguments().size() < 2) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Raw Map type not supported, use Map<String, V>");
                    return;
                }
                TypeMirror valueType = type.getTypeArguments().get(1);
                String valueWriter = getElementWriterLambda(valueType);
                out.println("        JsonWriter.writeMap(" + accessor + ", sb, " + valueWriter + ");");
            }
            default -> {
                // Check if it's an enum
                TypeElement typeElement = (TypeElement) type.asElement();
                if (isEnum(typeElement)) {
                    if (enumHasCustomNames(typeElement)) {
                        String serMapName = "SERIALIZE_" + toConstantName(typeElement.getSimpleName().toString());
                        out.println("        JsonWriter.writeEnum(" + accessor + ", sb, " + serMapName + ");");
                    } else {
                        out.println("        JsonWriter.writeEnum(" + accessor + ", sb);");
                    }
                } else if (isAnnotatedJsonRecord(typeElement)) {
                    // Nested @JsonRecord — delegate to its adapter (fully qualified for cross-package)
                    String adapterName = getAdapterQualifiedName(typeElement);
                    out.println("        if (" + accessor + " == null) { sb.append(\"null\"); } else { sb.append(" + adapterName + ".toJson(" + accessor + ")); }");
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Unsupported type: " + typeName);
                }
            }
        }
    }

    private String getElementWriterLambda(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType dt = (DeclaredType) type;
            String name = ((TypeElement) dt.asElement()).getQualifiedName().toString();
            return switch (name) {
                case "java.lang.String" -> "JsonWriter::writeString";
                case "java.lang.Integer" -> "JsonWriter::writeInteger";
                case "java.lang.Long" -> "JsonWriter::writeLongBoxed";
                case "java.lang.Double" -> "JsonWriter::writeDoubleBoxed";
                case "java.lang.Float" -> "JsonWriter::writeFloatBoxed";
                case "java.lang.Boolean" -> "JsonWriter::writeBooleanBoxed";
                case "java.math.BigDecimal" -> "JsonWriter::writeBigDecimal";
                case "java.math.BigInteger" -> "JsonWriter::writeBigInteger";
                default -> {
                    TypeElement te = (TypeElement) dt.asElement();
                    if (isEnum(te)) {
                        if (enumHasCustomNames(te)) {
                            String serMapName = "SERIALIZE_" + toConstantName(te.getSimpleName().toString());
                            yield "(v, s) -> JsonWriter.writeEnum(v, s, " + serMapName + ")";
                        } else {
                            yield "JsonWriter::writeEnum";
                        }
                    } else if (isAnnotatedJsonRecord(te)) {
                        String adapterName = getAdapterQualifiedName(te);
                        yield "(v, s) -> { if (v == null) { s.append(\"null\"); } else { s.append(" + adapterName + ".toJson(v)); } }";
                    } else {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Unsupported list/map element type: " + name);
                        yield "null";
                    }
                }
            };
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Unsupported list/map element type: " + type);
        return "null";
    }

    private void generateReadField(PrintWriter out, String fieldName, String jsonKey, TypeMirror type) {
        String getValue = "obj.members().get(\"" + jsonKey + "\")";
        TypeKind kind = type.getKind();

        switch (kind) {
            case INT -> out.println("        int " + fieldName + " = JsonMapper.toInt(" + getValue + ");");
            case LONG -> out.println("        long " + fieldName + " = JsonMapper.toLong(" + getValue + ");");
            case DOUBLE -> out.println("        double " + fieldName + " = JsonMapper.toDouble(" + getValue + ");");
            case FLOAT -> out.println("        float " + fieldName + " = JsonMapper.toFloat(" + getValue + ");");
            case BOOLEAN -> out.println("        boolean " + fieldName + " = JsonMapper.toBoolean(" + getValue + ");");
            case DECLARED -> generateReadDeclaredField(out, fieldName, (DeclaredType) type, getValue);
            default -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Unsupported field type: " + type + " for field " + fieldName);
        }
    }

    private void generateReadDeclaredField(PrintWriter out, String fieldName, DeclaredType type, String getValue) {
        String typeName = ((TypeElement) type.asElement()).getQualifiedName().toString();

        switch (typeName) {
            case "java.lang.String" -> out.println("        String " + fieldName + " = JsonMapper.toString(" + getValue + ");");
            case "java.lang.Integer" -> out.println("        Integer " + fieldName + " = JsonMapper.toIntegerBoxed(" + getValue + ");");
            case "java.lang.Long" -> out.println("        Long " + fieldName + " = JsonMapper.toLongBoxed(" + getValue + ");");
            case "java.lang.Double" -> out.println("        Double " + fieldName + " = JsonMapper.toDoubleBoxed(" + getValue + ");");
            case "java.lang.Float" -> out.println("        Float " + fieldName + " = JsonMapper.toFloatBoxed(" + getValue + ");");
            case "java.lang.Boolean" -> out.println("        Boolean " + fieldName + " = JsonMapper.toBooleanBoxed(" + getValue + ");");
            case "java.math.BigDecimal" -> out.println("        java.math.BigDecimal " + fieldName + " = JsonMapper.toBigDecimal(" + getValue + ");");
            case "java.math.BigInteger" -> out.println("        java.math.BigInteger " + fieldName + " = JsonMapper.toBigInteger(" + getValue + ");");
            case "java.util.List" -> {
                if (type.getTypeArguments().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Raw List type not supported, use List<T>");
                    return;
                }
                TypeMirror elementType = type.getTypeArguments().getFirst();
                String elementMapper = getElementMapperLambda(elementType);
                String typeDecl = "java.util.List<" + getTypeString(elementType) + ">";
                out.println("        " + typeDecl + " " + fieldName + " = JsonMapper.toList(" + getValue + ", " + elementMapper + ");");
            }
            case "java.util.Map" -> {
                if (type.getTypeArguments().size() < 2) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Raw Map type not supported, use Map<String, V>");
                    return;
                }
                TypeMirror valueType = type.getTypeArguments().get(1);
                String valueMapper = getElementMapperLambda(valueType);
                String typeDecl = "java.util.Map<String, " + getTypeString(valueType) + ">";
                out.println("        " + typeDecl + " " + fieldName + " = JsonMapper.toMap(" + getValue + ", " + valueMapper + ");");
            }
            default -> {
                TypeElement typeElement = (TypeElement) type.asElement();
                if (isEnum(typeElement)) {
                    if (enumHasCustomNames(typeElement)) {
                        String deserMapName = "DESERIALIZE_" + toConstantName(typeElement.getSimpleName().toString());
                        out.println("        " + typeName + " " + fieldName + " = JsonMapper.toEnum(" + getValue + ", " + deserMapName + ");");
                    } else {
                        out.println("        " + typeName + " " + fieldName + " = JsonMapper.toEnum(" + getValue + ", " + typeName + ".class);");
                    }
                } else if (isAnnotatedJsonRecord(typeElement)) {
                    String adapterName = getAdapterQualifiedName(typeElement);
                    out.println("        " + typeName + " " + fieldName + " = " + adapterName + ".fromJson(" + getValue + ");");
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Unsupported type: " + typeName + " for field " + fieldName);
                }
            }
        }
    }

    private String getElementMapperLambda(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType dt = (DeclaredType) type;
            String name = ((TypeElement) dt.asElement()).getQualifiedName().toString();
            return switch (name) {
                case "java.lang.String" -> "JsonMapper::toString";
                case "java.lang.Integer" -> "JsonMapper::toIntegerBoxed";
                case "java.lang.Long" -> "JsonMapper::toLongBoxed";
                case "java.lang.Double" -> "JsonMapper::toDoubleBoxed";
                case "java.lang.Float" -> "JsonMapper::toFloatBoxed";
                case "java.lang.Boolean" -> "JsonMapper::toBooleanBoxed";
                case "java.math.BigDecimal" -> "JsonMapper::toBigDecimal";
                case "java.math.BigInteger" -> "JsonMapper::toBigInteger";
                default -> {
                    TypeElement te = (TypeElement) dt.asElement();
                    if (isEnum(te)) {
                        if (enumHasCustomNames(te)) {
                            String deserMapName = "DESERIALIZE_" + toConstantName(te.getSimpleName().toString());
                            yield "v -> JsonMapper.toEnum(v, " + deserMapName + ")";
                        } else {
                            yield "v -> JsonMapper.toEnum(v, " + name + ".class)";
                        }
                    } else if (isAnnotatedJsonRecord(te)) {
                        String adapterName = getAdapterQualifiedName(te);
                        yield adapterName + "::fromJson";
                    } else {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Unsupported list/map element type: " + name);
                        yield "null";
                    }
                }
            };
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Unsupported list/map element type: " + type);
        return "null";
    }

    private String getTypeString(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType dt = (DeclaredType) type;
            return ((TypeElement) dt.asElement()).getQualifiedName().toString();
        }
        return type.toString();
    }

    private boolean isEnum(TypeElement element) {
        return element.getSuperclass() != null
                && element.getSuperclass().toString().startsWith("java.lang.Enum");
    }

    private boolean isAnnotatedJsonRecord(TypeElement element) {
        return element.getAnnotation(JsonRecord.class) != null;
    }

    private String getAdapterQualifiedName(TypeElement recordElement) {
        String pkg = processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();
        String simpleName = recordElement.getSimpleName().toString() + "JsonAdapter";
        return pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
    }

    private void generateRegistry() {
        try {
            // Determine a common package or use the first adapter's package
            String registryPackage = "";
            if (!adapterClassNames.isEmpty()) {
                String first = adapterClassNames.getFirst();
                int lastDot = first.lastIndexOf('.');
                if (lastDot > 0) {
                    registryPackage = first.substring(0, lastDot);
                }
            }

            String registryQualifiedName = registryPackage.isEmpty()
                    ? "JsonAdapterRegistry"
                    : registryPackage + ".JsonAdapterRegistry";

            JavaFileObject file = processingEnv.getFiler().createSourceFile(registryQualifiedName);
            try (var out = new PrintWriter(file.openWriter())) {
                if (!registryPackage.isEmpty()) {
                    out.println("package " + registryPackage + ";");
                    out.println();
                }
                out.println("/**");
                out.println(" * Generated registry — triggers class loading of all JSON adapters.");
                out.println(" * Call {@code JsonAdapterRegistry.initialize()} once at application startup.");
                out.println(" */");
                out.println("public final class JsonAdapterRegistry {");
                out.println();
                out.println("    private JsonAdapterRegistry() {}");
                out.println();
                out.println("    public static void initialize() {");
                for (String adapterClassName : adapterClassNames) {
                    out.println("        " + adapterClassName + ".ensureRegistered();");
                }
                out.println("    }");
                out.println("}");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate JsonAdapterRegistry: " + e.getMessage());
        }
    }
}
