package org.zstack.network.service;

import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkHostRouteVO;
import org.zstack.header.network.l3.L3NetworkHostRouteVO_;

import java.util.ArrayList;
import java.util.List;

public class NetworkServiceHelper {
    public static class HostRouteInfo {
        public String prefix;
        public String nexthop;
    }

    public static List<HostRouteInfo> getL3NetworkHostRoute(String l3NetworkUuid){
        List<L3NetworkHostRouteVO> vos = Q.New(L3NetworkHostRouteVO.class).eq(L3NetworkHostRouteVO_.l3NetworkUuid, l3NetworkUuid).list();
        if (vos == null || vos.isEmpty()) {
            return new ArrayList<>();
        }

        List<HostRouteInfo> res = new ArrayList<>();
        for (L3NetworkHostRouteVO vo : vos) {
            HostRouteInfo info = new HostRouteInfo();
            info.prefix = vo.getPrefix();
            info.nexthop = vo.getNexthop();
            res.add(info);
        }

        return res;
    }
}
