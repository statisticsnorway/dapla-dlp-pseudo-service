package no.ssb.dlp.pseudo.service.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * There's a known bug in the JDK - Collectors.toMap fails on null values:
 * Ref: https://bugs.openjdk.java.net/browse/JDK-8148463
 *
 * This is a workaround to allow null values in Map constructed using a Collector, as suggested here:
 * https://stackoverflow.com/questions/24630963/java-8-nullpointerexception-in-collectors-tomap
 */
public class MoreCollectors {

    private MoreCollectors() {}

    /**
     * In contrast to {@link Collectors#toMap(Function, Function)} the result map
     * may have null values.
     */
    public static <T, K, U, M extends Map<K, U>> Collector<T, M, M> toMapWithNullValues(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toMapWithNullValues(keyMapper, valueMapper, HashMap::new);
    }

    /**
     * In contrast to {@link Collectors#toMap(Function, Function, BinaryOperator, Supplier)}
     * the result map may have null values.
     */
    public static <T, K, U, M extends Map<K, U>> Collector<T, M, M> toMapWithNullValues(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, Supplier<Map<K, U>> supplier) {
        return new Collector<T, M, M>() {

            @Override
            public Supplier<M> supplier() {
                return () -> {
                    @SuppressWarnings("unchecked")
                    M map = (M) supplier.get();
                    return map;
                };
            }

            @Override
            public BiConsumer<M, T> accumulator() {
                return (map, element) -> {
                    K key = keyMapper.apply(element);
                    if (map.containsKey(key)) {
                        throw new IllegalStateException("Duplicate key " + key);
                    }
                    map.put(key, valueMapper.apply(element));
                };
            }

            @Override
            public BinaryOperator<M> combiner() {
                return (left, right) -> {
                    int total = left.size() + right.size();
                    left.putAll(right);
                    if (left.size() < total) {
                        throw new IllegalStateException("Duplicate key(s)");
                    }
                    return left;
                };
            }

            @Override
            public Function<M, M> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
            }

        };
    }
}
