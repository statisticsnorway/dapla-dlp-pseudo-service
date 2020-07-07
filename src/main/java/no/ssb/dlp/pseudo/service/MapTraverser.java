package no.ssb.dlp.pseudo.service;

import com.google.common.base.Joiner;
import no.ssb.avro.convert.core.FieldDescriptor;
import no.ssb.dapla.dlp.pseudo.func.util.FromString;
import no.ssb.dlp.pseudo.service.util.MoreCollectors;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MapTraverser {
    private static final Joiner PATH_JOINER = Joiner.on("/").skipNulls();
    private static final String ROOT_PATH = "";

    public static <T extends Map<String,Object>> T traverse(T map, ValueInterceptor interceptor) {
        return (T) traverse(ROOT_PATH, map, interceptor);
    }

    private static Object traverse(String path, Object node, ValueInterceptor interceptor) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            return map.entrySet().stream()
                .collect(MoreCollectors.toMapWithNullValues(
                  e -> e.getKey(),
                  e -> {
                      String nextPath = PATH_JOINER.join(path, e.getKey());
                      return isTraversable(e.getValue())
                        ? traverse(nextPath, e.getValue(), interceptor)
                        : processValue(e.getValue(), nextPath, interceptor);
                  },
                  RecordMap::new
                ));
        }
        else if (node instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) node;
            AtomicInteger i = new AtomicInteger();
            return collection.stream()
              .map(value -> {
                  String nextPath = path + "[" + i.getAndIncrement() + "]";
                  return isTraversable(value)
                    ? traverse(nextPath, value, interceptor)
                    : processValue(value, nextPath, interceptor);
                })
              .collect(Collectors.toList());
        }
        else {
            return processValue(node, path, interceptor);
        }
    }

    static Object processValue(Object value, String path, ValueInterceptor interceptor) {
        String newValue = interceptor.apply(new FieldDescriptor(path), (value == null) ? null : String.valueOf(value));
        if (newValue != null) {
            return FromString.convert(newValue, value.getClass());
        }

        return value;
    }

    private static boolean isTraversable(Object o) {
        return (o instanceof Map) || (o instanceof Collection);
    }

    /**
     * An interceptor that allows for overriding a value based on the value itself, and the field name to which it is applied.
     */
    public interface ValueInterceptor {

        /**
         * Applied when setting a value to a corresponding field. E.g cake='chocolate' where fieldName=cake and value=chocolate.
         * @param field The field descriptor (path, name, ...) of the field to assign a value.
         * @param value The intercepted value.
         * @return The actual value that will be applied to the field.
         */
        String apply(FieldDescriptor field, String value);
    }

}
