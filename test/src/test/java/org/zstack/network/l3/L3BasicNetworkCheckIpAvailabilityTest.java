package org.zstack.network.l3;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.network.l3.*;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class L3BasicNetworkCheckIpAvailabilityTest {
    private DatabaseFacade dbf;
    private SimpleQuery<IpRangeVO> ipRangeQuery;
    private SimpleQuery<AddressPoolVO> addressPoolQuery;
    private SimpleQuery<UsedIpVO> usedIpQuery;
    private L3BasicNetwork l3Network;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        dbf = mock(DatabaseFacade.class);
        ipRangeQuery = mock(SimpleQuery.class);
        addressPoolQuery = mock(SimpleQuery.class);
        usedIpQuery = mock(SimpleQuery.class);

        when(dbf.createQuery(IpRangeVO.class)).thenReturn(ipRangeQuery);
        when(dbf.createQuery(AddressPoolVO.class)).thenReturn(addressPoolQuery);
        when(dbf.createQuery(UsedIpVO.class)).thenReturn(usedIpQuery);
        when(usedIpQuery.isExists()).thenReturn(false);

        L3NetworkVO vo = new L3NetworkVO();
        vo.setUuid("00000000000040008000000000000001");
        vo.setCategory(L3NetworkCategory.System);
        vo.setType(L3NetworkConstant.L3_BASIC_NETWORK_TYPE);

        l3Network = new L3BasicNetwork(vo);
        l3Network.dbf = dbf;
    }

    private Tuple rangeTuple(String uuid, String startIp, String endIp, String gateway) {
        Tuple t = mock(Tuple.class);
        when(t.get(0, String.class)).thenReturn(uuid);
        when(t.get(1, String.class)).thenReturn(startIp);
        when(t.get(2, String.class)).thenReturn(endIp);
        when(t.get(3, String.class)).thenReturn(gateway);
        return t;
    }

    private CheckIpAvailabilityMsg msg(String ip) {
        CheckIpAvailabilityMsg msg = new CheckIpAvailabilityMsg();
        msg.setL3NetworkUuid("00000000000040008000000000000001");
        msg.setIp(ip);
        return msg;
    }

    @Test
    public void standaloneAddressPoolStartIpRemainsAvailable() {
        Tuple poolTuple = rangeTuple("pool-1", "100.64.101.101", "100.64.101.101", "100.64.101.101");
        when(ipRangeQuery.listTuple()).thenReturn(Collections.singletonList(poolTuple));
        when(addressPoolQuery.listValue()).thenReturn(Collections.singletonList("pool-1"));

        CheckIpAvailabilityReply reply = l3Network.checkIpAvailability(msg("100.64.101.101"));

        assertTrue(reply.isAvailable());
        assertNull(reply.getReason());
    }

    @Test
    public void normalRangeGatewayRejectedEvenWhenAddressPoolStartIpMatches() {
        Tuple poolTuple = rangeTuple("pool-1", "100.64.102.1", "100.64.102.1", "100.64.102.1");
        Tuple normalTuple = rangeTuple("normal-1", "100.64.102.2", "100.64.102.100", "100.64.102.1");
        Tuple otherPoolTuple = rangeTuple("pool-2", "100.64.102.201", "100.64.102.201", "100.64.102.201");
        List<Tuple> tuples = Arrays.asList(poolTuple, normalTuple, otherPoolTuple);

        when(ipRangeQuery.listTuple()).thenReturn(tuples);
        when(addressPoolQuery.listValue()).thenReturn(Arrays.asList("pool-1", "pool-2"));

        CheckIpAvailabilityReply gwReply = l3Network.checkIpAvailability(msg("100.64.102.1"));
        assertFalse(gwReply.isAvailable());
        assertEquals(IpNotAvailabilityReason.GATEWAY.toString(), gwReply.getReason());

        CheckIpAvailabilityReply otherPoolReply = l3Network.checkIpAvailability(msg("100.64.102.201"));
        assertTrue(otherPoolReply.isAvailable());
    }

    @Test
    public void ipv6NormalRangeGatewayCanonicalComparisonStillRejectsGateway() {
        Tuple normalV6Tuple = rangeTuple("normal-v6", "2001:db8:83::2", "2001:db8:83::ff", "2001:0db8:0083::0001");
        when(ipRangeQuery.listTuple()).thenReturn(Collections.singletonList(normalV6Tuple));
        when(addressPoolQuery.listValue()).thenReturn(Collections.emptyList());

        CheckIpAvailabilityReply reply = l3Network.checkIpAvailability(msg("2001:db8:83::1"));

        assertFalse(reply.isAvailable());
        assertEquals(IpNotAvailabilityReason.GATEWAY.toString(), reply.getReason());
    }
}
