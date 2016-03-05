package org.zstack.utils.hash;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {
    private final CLogger logger = Utils.getLogger(ConsistentHash.class);
    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = new TreeMap<Integer, T>();

    public ConsistentHash(HashFunction hashFunction, int numberOfReplicas,
                          Collection<T> nodes) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;

        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeName = node.toString() + i;
            circle.put(hashFunction.hash(nodeName), node);
        }
        logger.debug(String.format("after adding, consistent hash circle has %s virtual nodes now", circle.size()));
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeName = node.toString() + i;
            circle.remove(hashFunction.hash(nodeName));
        }
        logger.debug(String.format("after removing, consistent hash circle has %s virtual nodes now", circle.size()));
    }

    public boolean hasNode(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeName = node.toString() + i;
            if (circle.containsKey(hashFunction.hash(node))) {
                return true;
            }
        }

        return false;
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hashFunction.hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }
}
