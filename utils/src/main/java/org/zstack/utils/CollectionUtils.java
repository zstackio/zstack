package org.zstack.utils;

import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.function.ListFunction;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 */
public class CollectionUtils {
    private static final CLogger logger = Utils.getLogger(CollectionUtils.class);

    public static <K, V> List<K> transformToList(Collection<V> from, ListFunction<K, V> func) {
        List<K> ret = new ArrayList<K>();
        for (V v : from) {
            List<K> k = func.call(v);
            if (k == null) {
                continue;
            }
            ret.addAll(k);
        }

        return ret;
    }

    public static <K, V> List<K> transformToList(Collection<V> from, Function<K, V> func) {
        List<K> ret = new ArrayList<K>();
        for (V v : from) {
            K k = func.call(v);
            if (k == null) {
                continue;
            }
            ret.add(k);
        }

        return ret;
    }

    public static <K, V> Set<K> transformToSet(Collection<V> from, Function<K, V> func) {
        Set<K> ret = new HashSet<K>();
        for (V v : from) {
            K k = func.call(v);
            if (k == null) {
                continue;
            }
            ret.add(k);
        }

        return ret;
    }

    public static <K, V> Set<K> transformToSet(Collection<V> from, ListFunction<K, V> func) {
        Set<K> ret = new HashSet<K>();
        for (V v : from) {
            List<K> k = func.call(v);
            if (k == null) {
                continue;
            }
            ret.addAll(k);
        }

        return ret;
    }

    public static <K, V> K find(Collection<V> from, Function<K, V> func) {
        for (V v : from) {
            K k = func.call(v);
            if (k != null) {
                return k;
            }
        }

        return null;
    }

    public static <K> void forEach(Collection<K> cols, ForEachFunction<K> func) {
        for (K c : cols) {
            func.run(c);
        }
    }

    public static <K> void safeForEach(Collection<K> cols, ForEachFunction<K> func) {
        for (K c : cols) {
            try {
                func.run(c);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception happened"), t);
            }
        }
    }

    public static <K> List<K> removeDuplicateFromList(List<K> lst) {
        return new ArrayList<K>(new LinkedHashSet<K>(lst));
    }

    public static <T> Predicate<T> distinctByKey(java.util.function.Function<? super T,Object> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @SuppressWarnings("unchecked")
    public static <T> void shuffleByKeySeed(List<T> list, String keySeed, java.util.function.Function<? super T, Comparable> keyExtractor) {
        list.sort(Comparator.comparing(keyExtractor));

        long seed = 0;
        char[] keyArrary = keySeed.toCharArray();
        for (char c : keyArrary) {
            seed += c;
        }

        Random r = new Random(seed);
        List<T> result = new ArrayList<>();
        for (int i = list.size(); i > 0; i--) {
            result.add(list.remove(r.nextInt(i)));
        }
        list.addAll(result);
    }
}
