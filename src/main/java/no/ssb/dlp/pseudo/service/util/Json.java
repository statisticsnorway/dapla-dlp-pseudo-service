package no.ssb.dlp.pseudo.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@UtilityClass
public class Json {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Convert JSON to Object
     */
    public static <T> T toObject(Class<T> type, String json) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        }
        catch (IOException e) {
            throw new JsonException("Error mapping JSON to " + type.getSimpleName() + " object", e);
        }
    }

    /**
     * Convert JSON to Object
     *
     * Use with generics, like new TypeReference<HashMap<MyPair, String>>() {}
     */
    public static <T> T toObject(TypeReference<T> type, String json) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        }
        catch (IOException e) {
            throw new JsonException("Error mapping JSON to " + type.getType() + " object", e);
        }
    }

    /**
     * Convert JSON to String->Object map
     */
    public static Map<String, Object> toGenericMap(String json) {
        return toObject(new TypeReference<Map<String, Object>>() {}, json);
    }

    /**
     * Convert Object to JSON
     */
    public static String from(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonException("Error mapping " +  object.getClass().getSimpleName() + " object to JSON", e);
        }
    }

    /**
     * Convert Object to pretty (indented) JSON
     */
    public static String prettyFrom(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonException("Error mapping " +  object.getClass().getSimpleName() + " object to JSON", e);
        }
    }

    public static class JsonException extends RuntimeException {
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Scramble values of specified properties from a JSON structure.
     */
    public static byte[] withScrambledProps(byte[] json, Iterable<String> propsToScramble) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
            for (String propName : propsToScramble) {
                jsonNode.findParents(propName)
                  .forEach(n -> ((ObjectNode) n).replace(propName, new TextNode("***")));
            }
            return OBJECT_MAPPER.writeValueAsBytes(jsonNode);
        } catch (Exception e) {
            throw new JsonException("Error scrambling JSON properties " + propsToScramble, e);
        }
    }

    /**
     * Scramble values of specified properties from a JSON structure.
     */
    public static String withScrambledProps(String json, Iterable<String> propsToScramble) {
        return new String(withScrambledProps(json.getBytes(StandardCharsets.UTF_8), propsToScramble));
    }

}
