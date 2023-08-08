package org.zstack.utils;

import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.function.ListFunction;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public static <T> List<T> getDuplicateElementsOfList(List<T> list) {
        List<T> result = new ArrayList<T>();
        Set<T> set = new HashSet<T>();
        for (T e : list) {
            if (!set.add(e)) {
                result.add(e);
            }
        }
        return result;
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

    public static <FROM, TO> List<TO> transform(Collection<FROM> from, java.util.function.Function<FROM, TO> func) {
        return from.stream().map(func).collect(Collectors.toList());
    }

    public static <FROM, TO> List<TO> flatten(Collection<FROM> from, java.util.function.Function<FROM, Collection<TO>> func) {
        return from.stream().map(func).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
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

    public static <T> Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
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

    public static <T> List<T> merge(List<T> listA, List<T> listB) {
        listA = org.apache.commons.collections.CollectionUtils.isNotEmpty(listA) ? listA : new ArrayList<T>();
        listB = org.apache.commons.collections.CollectionUtils.isNotEmpty(listB) ? listB : new ArrayList<T>();
        List<T> list = new ArrayList<T>();
        list.addAll(listA);
        list.addAll(listB);
        return list;
    }

    public static <T> boolean isEmpty(Collection<T> coll) {
        return coll == null || coll.isEmpty();
    }

    public static <K, V> boolean isEmpty(Map<K, V> coll) {
        return coll == null || coll.isEmpty();
    }

    public static <T> List<T> filter(List<T> list, Predicate<? super T> predicate) {
        return list.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T> T findOrNull(List<T> list, Predicate<? super T> predicate) {
        return list.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * null  -> []
     * []    -> []
     * ["A"] -> ["A"]
     */
    public static <T> List<T> emptyListIfNull(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
