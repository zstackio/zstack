package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by weiwang on 19/05/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MtuGetter {
    private static final CLogger logger = Utils.getLogger(MtuGetter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    public Integer getMtu(String l3NetworkUuid) {
        /* Existing l3: take the value of l3mtu */
        Integer mtu = getMtuFromL3MtuSystemTags(l3NetworkUuid);

        return mtu;
    }

    public Integer getL2Mtu(L2NetworkInventory l2Inv) {

        Integer mtu = null;

        /* get max mtu of l3 network in l2 network */
        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l2Inv.getUuid()).select(L3NetworkVO_.uuid).listValues();
        for (String uuid : l3Uuids) {
            String token = NetworkServiceSystemTag.L3_MTU.getTokenByResourceUuid(uuid, NetworkServiceSystemTag.MTU_TOKEN);
            if (token == null) {
                continue;
            }

            if (mtu == null) {
                mtu = Integer.valueOf(token);
                continue;
            }
            Integer newMtu = Integer.valueOf(token);
            if (newMtu > mtu) {
                mtu = newMtu;
            }
        }

        /* compare to default mtu */
        for (L2NetworkDefaultMtu e : pluginRgty.getExtensionList(L2NetworkDefaultMtu.class)) {
            if (l2Inv.getType().equals(e.getL2NetworkType())) {
                Integer l2mtu = e.getDefaultMtu(l2Inv);
                if (mtu == null) {
                    mtu = l2mtu;
                    break;
                }
                if (l2mtu > mtu) {
                    mtu = l2mtu;
                }
                break;
            }
        }

        if (mtu != null) {
            return mtu;
        }

        logger.warn(String.format("unknown network type [%s], set mtu as default [%s]",
                l2Inv.getType(), mtu));
        return Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_DUMMY.value());
    }

    public Integer getDefaultL2MtuFromGlobalConfig(L2NetworkInventory l2Inv) {
        for (L2NetworkDefaultMtu e : pluginRgty.getExtensionList(L2NetworkDefaultMtu.class)) {
            if (l2Inv.getType().equals(e.getL2NetworkType())) {
                return e.getDefaultMtu(l2Inv);
            }
        }
        return null;
    }

    public Integer getMtuFromL3MtuSystemTags(String l3NetworkUuid) {
        List<Map<String, String>> tokenList = NetworkServiceSystemTag.L3_MTU.getTokensOfTagsByResourceUuid(l3NetworkUuid);
        for (Map<String, String> token : tokenList) {
            return Integer.valueOf(token.get(NetworkServiceSystemTag.MTU_TOKEN));
        }
        return null;
    }

    public void createL3MtuSystemTag(String l3Uuid, int mtu) {
        SystemTagCreator creator = NetworkServiceSystemTag.L3_MTU.newSystemTagCreator(l3Uuid);
        creator.ignoreIfExisting = true;
        creator.inherent = false;
        creator.setTagByTokens(
                map(
                        e(NetworkServiceSystemTag.MTU_TOKEN, mtu),
                        e(NetworkServiceSystemTag.L3_UUID_TOKEN, l3Uuid)
                )
        );
        creator.recreate = true;
        creator.create();
    }

}
