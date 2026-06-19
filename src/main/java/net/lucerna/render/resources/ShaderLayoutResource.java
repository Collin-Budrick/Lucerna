package net.lucerna.render.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ShaderLayoutResource {
    public static final String RESOURCE_PATH = "assets/lucerna/shaders/layout.json";
    public static final String RESOURCE_ID = "lucerna:shaders/layout.json";

    private ShaderLayoutResource() {
    }

    public static ShaderLayout read(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        JsonElement element = JsonParser.parseReader(reader);
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException(RESOURCE_PATH + " must contain a JSON object");
        }
        return fromJson(element.getAsJsonObject());
    }

    public static ShaderLayout fromJson(JsonObject root) {
        Objects.requireNonNull(root, "root");
        return new ShaderLayout(
                intValue(root, "schemaVersion"),
                stringValue(root, "layoutVersion"),
                stringValue(root, "namespace"),
                descriptorSets(root),
                attachments(root),
                passes(root)
        );
    }

    private static List<ShaderDescriptorSet> descriptorSets(JsonObject root) {
        JsonArray array = requiredArray(root, "descriptorSets");
        List<ShaderDescriptorSet> descriptorSets = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject descriptorSet = objectValue(array.get(i), "descriptorSets[" + i + "]");
            descriptorSets.add(new ShaderDescriptorSet(
                    intValue(descriptorSet, "set"),
                    stringValue(descriptorSet, "name"),
                    stringValue(descriptorSet, "scope"),
                    descriptorBindings(descriptorSet, "descriptorSets[" + i + "].bindings")
            ));
        }
        return descriptorSets;
    }

    private static List<ShaderDescriptorBinding> descriptorBindings(JsonObject descriptorSet, String path) {
        JsonArray array = requiredArray(descriptorSet, "bindings");
        List<ShaderDescriptorBinding> bindings = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject binding = objectValue(array.get(i), path + "[" + i + "]");
            bindings.add(new ShaderDescriptorBinding(
                    intValue(binding, "binding"),
                    stringValue(binding, "name"),
                    stringValue(binding, "descriptorType"),
                    stringList(binding, "stages"),
                    stringValue(binding, "access"),
                    stringValue(binding, "updateFrequency"),
                    optionalString(binding, "format"),
                    optionalString(binding, "notes")
            ));
        }
        return bindings;
    }

    private static List<ShaderAttachment> attachments(JsonObject root) {
        JsonArray array = requiredArray(root, "attachments");
        List<ShaderAttachment> attachments = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject attachment = objectValue(array.get(i), "attachments[" + i + "]");
            attachments.add(new ShaderAttachment(
                    stringValue(attachment, "name"),
                    ShaderPassId.of(stringValue(attachment, "ownerPass")),
                    stringValue(attachment, "format"),
                    stringValue(attachment, "resolution"),
                    intValue(attachment, "samples"),
                    stringList(attachment, "usage"),
                    stringList(attachment, "consumers"),
                    optionalStringList(attachment, "fallbackFormats"),
                    optionalString(attachment, "notes")
            ));
        }
        return attachments;
    }

    private static List<ShaderPassDescriptor> passes(JsonObject root) {
        JsonArray array = requiredArray(root, "passes");
        List<ShaderPassDescriptor> passes = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject pass = objectValue(array.get(i), "passes[" + i + "]");
            passes.add(new ShaderPassDescriptor(
                    ShaderPassId.of(stringValue(pass, "id")),
                    intValue(pass, "numericId"),
                    stringValue(pass, "stage"),
                    stringValue(pass, "directory"),
                    ShaderPassType.fromLayoutValue(stringValue(pass, "type")),
                    intValue(pass, "executionOrder"),
                    stringValue(pass, "placeholderShader"),
                    stringList(pass, "descriptorSets"),
                    stringList(pass, "reads"),
                    stringList(pass, "writes"),
                    stringValue(pass, "pushConstants"),
                    booleanValue(pass, "sideEffectFreePlaceholder"),
                    optionalString(pass, "handoff")
            ));
        }
        return passes;
    }

    private static JsonArray requiredArray(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonArray()) {
            throw new JsonParseException("Expected array field '" + name + "'");
        }
        return element.getAsJsonArray();
    }

    private static JsonObject objectValue(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("Expected object at " + path);
        }
        return element.getAsJsonObject();
    }

    private static String stringValue(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Expected string field '" + name + "'");
        }
        return element.getAsString();
    }

    private static String optionalString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Expected optional string field '" + name + "'");
        }
        return element.getAsString();
    }

    private static int intValue(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Expected integer field '" + name + "'");
        }
        return element.getAsInt();
    }

    private static boolean booleanValue(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("Expected boolean field '" + name + "'");
        }
        return element.getAsBoolean();
    }

    private static List<String> stringList(JsonObject object, String name) {
        return readStringList(requiredArray(object, name), name);
    }

    private static List<String> optionalStringList(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Expected optional string array field '" + name + "'");
        }
        return readStringList(element.getAsJsonArray(), name);
    }

    private static List<String> readStringList(JsonArray array, String name) {
        List<String> values = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Expected string entry at '" + name + "[" + i + "]'");
            }
            values.add(element.getAsString());
        }
        return values;
    }
}
