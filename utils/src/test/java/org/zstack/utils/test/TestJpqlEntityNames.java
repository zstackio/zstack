package org.zstack.utils.test;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.utils.data.JpqlEntityNames;

import java.util.Arrays;
import java.util.HashSet;

public class TestJpqlEntityNames {
    private static void check(String jpql, String... names) {
        Assert.assertEquals(jpql, new HashSet<>(Arrays.asList(names)), JpqlEntityNames.of(jpql));
    }

    @Test
    public void findsEntitiesInFromUpdateJoinAndCommaLists() {
        check("select t from NetProbeTaskVO t, HostVO h where t.hostUuid = h.uuid", "NetProbeTaskVO", "HostVO");
        check("update NetProbeTaskVO t set t.name = :n where t.uuid = :u", "NetProbeTaskVO");
        check("delete from NetProbeResultVO r where r.id in (:ids)", "NetProbeResultVO");
        check("select count(t) from TxVO t join t.x y", "TxVO");
        check("SELECT a FROM AVO a INNER JOIN BVO b ON a.uuid = b.uuid", "AVO", "BVO");
        check("select a from AVO a where a.uuid in (select b.uuid from BVO b)", "AVO", "BVO");
    }
}
