package org.zstack.utils.function;

import java.util.List;

/**
*/
public interface ListFunction<K, V> {
    List<K> call(V arg);
}
