package com.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.hash.ApacheHash;
import org.zstack.utils.hash.ConsistentHash;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestConsistentHash {

    private String uuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void test() {
        List<String> nodes = new ArrayList<String>();
        Map<String, Integer> m = new HashMap();
        for (int i=0; i<10; i++) {
            String guid = uuid();
            nodes.add(guid);
            m.put(guid, new Integer(0));
        }

        ConsistentHash<String> chash = new ConsistentHash(new ApacheHash(), 1000, nodes);

        for (int i=0; i<100000; i++) {
            String msg = uuid();
            String node = chash.get(msg);
            Integer count = (Integer) m.get(node);
            int c = count + 1;
            m.put(node, c);
        }

        for (Map.Entry<String, Integer> e : m.entrySet()) {
            System.out.println(String.format("node[%s]: %s", e.getKey(), e.getValue()));
        }
    }
}
