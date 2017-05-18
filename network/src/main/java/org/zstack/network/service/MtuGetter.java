package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

/**
 * Created by weiwang on 19/05/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MtuGetter {
    private static final CLogger logger = Utils.getLogger(MtuGetter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    public Integer getMtu(String l3NetworkUuid) {
        String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid).eq(L3NetworkVO_.uuid, l3NetworkUuid).findValue();

        Integer mtu = null;

        List<Map<String, String>> tokenList = NetworkServiceSystemTag.L3_MTU.getTokensOfTagsByResourceUuid(l3NetworkUuid);
        for (Map<String, String> token : tokenList) {
            mtu = Integer.valueOf(token.get(NetworkServiceSystemTag.MTU_TOKEN));
        }

        if (mtu != null) {
            return mtu;
        }

        String l2type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, l2NetworkUuid).findValue();
        for (L2NetworkDefaultMtu e : pluginRgty.getExtensionList(L2NetworkDefaultMtu.class)) {
            if (l2type.equals(e.getL2NetworkType())) {
                mtu = e.getDefaultMtu();
            }
        }

        if (mtu != null) {
            return mtu;
        } else {
            mtu = Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_DUMMY.getDefaultValue());
            logger.warn(String.format("unknown network type [%s], set mtu as default [%s]",
                    l2type, mtu));
            return mtu;
        }
    }

}
