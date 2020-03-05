package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TagUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * @author: zhanyong.miao
 * @date: 2019-12-28
 **/
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LoadBalancerWeightOperator {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;

    public Long getWeight(String listenerUuid, String nicUuid) {
        DebugUtils.Assert(listenerUuid != null && nicUuid != null, String.format("invalid parameter listener uuid:%s nicUuid:%s", listenerUuid, nicUuid));
        List<Map<String, String>> tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensOfTagsByResourceUuid(listenerUuid);

        for (Map<String, String>  token: tokens) {
            if (!nicUuid.equals(token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN))) {
                continue;
            }
            String weight = token.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN);
            return Long.valueOf(weight);
        }
        return LoadBalancerConstants.BALANCER_WEIGHT_default;
    }

    public Map<String, Long> getWeight(List<String> systemTags) {
        Map<String, Long> ret = new HashMap<>();

        if (systemTags == null) {
            return ret;
        }

        for (String systemTag : systemTags) {
            if(!LoadBalancerSystemTags.BALANCER_WEIGHT.isMatch(systemTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(LoadBalancerSystemTags.BALANCER_WEIGHT.getTagFormat(), systemTag);
            String nicUuid = token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN);
            String weight = token.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN);
            ret.put(nicUuid, Long.valueOf(weight));
        }

        return ret;
    }

    public void setWeight(String listenerUuid, String nicUuid, Long weight) {
        DebugUtils.Assert(listenerUuid != null && nicUuid != null, String.format("invalid parameter listener uuid:%s nicUuid:%s", listenerUuid, nicUuid));

        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.uuid);
        q.add(SystemTagVO_.resourceType, SimpleQuery.Op.EQ, LoadBalancerListenerVO.class.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, listenerUuid);
        q.add(SystemTagVO_.tag, SimpleQuery.Op.LIKE, TagUtils.tagPatternToSqlPattern(LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(
                map(e(LoadBalancerSystemTags.BALANCER_NIC_TOKEN, nicUuid))
        )));
        final String tagUuid = q.findValue();

        if (tagUuid == null) {
            SystemTagCreator creator = LoadBalancerSystemTags.BALANCER_WEIGHT.newSystemTagCreator(listenerUuid);
            creator.setTagByTokens(map(
                    e(LoadBalancerSystemTags.BALANCER_NIC_TOKEN, nicUuid),
                    e(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN, weight)
            ));
            creator.create();
        } else {
            LoadBalancerSystemTags.BALANCER_WEIGHT.updateByTagUuid(tagUuid, LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(map(
                    e(LoadBalancerSystemTags.BALANCER_NIC_TOKEN, nicUuid),
                    e(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN, weight)
            )));
        }
    }

    public void setWeight(List<String> systemTags, String listenerUuid) {
        for (String systemTag: systemTags) {
            if (!LoadBalancerSystemTags.BALANCER_WEIGHT.isMatch(systemTag)) {
                continue;
            }
            Map<String, String> token = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensByTag(systemTag);
            setWeight(listenerUuid, token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN), Long.valueOf(token.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN)));
        }
    }

    public void deleteWeight(String listenerUuid, String nicUuid) {
        DebugUtils.Assert(listenerUuid != null && nicUuid != null, String.format("invalid parameter listener uuid:%s nicUuid:%s", listenerUuid, nicUuid));

        LoadBalancerSystemTags.BALANCER_WEIGHT.delete(listenerUuid,
                TagUtils.tagPatternToSqlPattern(LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(
                map(e(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN, nicUuid))
        )));
    }

    public void deleteWeight(List<String> systemTags, String listenerUuid) {
        DebugUtils.Assert(listenerUuid != null && systemTags != null, String.format("invalid parameter listener uuid:%s systemtags:%s", listenerUuid, systemTags));

        for (String systemTag: systemTags) {
            if(!LoadBalancerSystemTags.BALANCER_WEIGHT.isMatch(systemTag)) {
                continue;
            }
            tagMgr.deleteSystemTag(systemTag, listenerUuid, LoadBalancerListenerVO.class.getSimpleName(), false);
        }
    }

    public void deleteWeight(List<String> vnicUuids) {
        for (String vnicUuid: vnicUuids) {
            LoadBalancerSystemTags.BALANCER_WEIGHT.delete(null, LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(map(
                    e(LoadBalancerSystemTags.BALANCER_NIC_TOKEN, vnicUuid),
                    e(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN, "%")
            )));
        }
    }
}
