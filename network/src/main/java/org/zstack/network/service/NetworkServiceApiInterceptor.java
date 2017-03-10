package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import java.util.*;

/**
 */
public class NetworkServiceApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            validate((APIAttachNetworkServiceToL3NetworkMsg)msg);
        }

        return msg;
    }

    private void validate(APIAttachNetworkServiceToL3NetworkMsg msg) {
        if (msg.getNetworkServices().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("networkServices cannot be empty"));
        }

        SimpleQuery<NetworkServiceTypeVO> q = dbf.createQuery(NetworkServiceTypeVO.class);
        q.add(NetworkServiceTypeVO_.networkServiceProviderUuid, Op.IN, msg.getNetworkServices().keySet());
        List<NetworkServiceTypeVO> vos = q.list();
        Map<String, Set<String>> actual = new HashMap<String, Set<String>>();
        for (NetworkServiceTypeVO vo : vos) {
            Set<String> types = actual.get(vo.getNetworkServiceProviderUuid());
            if (types == null) {
                types = new HashSet<String>();
                actual.put(vo.getNetworkServiceProviderUuid(), types);
            }

            types.add(vo.getType());
        }

        for (Map.Entry<String, List<String>> e : msg.getNetworkServices().entrySet()) {
            String puuid = e.getKey();
            List<String> types = e.getValue();
            if (types == null || types.isEmpty())  {
                throw new ApiMessageInterceptionException(argerr("network service for provider[uuid:%s] must be specified", puuid));
            }

            final Set<String> actualTypes = actual.get(puuid);
            if (actualTypes == null) {
                throw new ApiMessageInterceptionException(argerr("cannot find network service provider[uuid:%s] or it provides no services", puuid));
            }

            if (!actualTypes.containsAll(types)) {
                List<String> notSupported = CollectionUtils.transformToList(types, new Function<String, String>() {
                    @Override
                    public String call(String type) {
                        if (!actualTypes.contains(type)) {
                            return type;
                        }
                        return null;
                    }
                });

                throw new ApiMessageInterceptionException(argerr("network service provider[uuid:%s] doesn't provide services%s", puuid, notSupported));
            }
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nwsq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nwsq.select(NetworkServiceL3NetworkRefVO_.networkServiceType);
        nwsq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        List<String> existingNwsTypes = nwsq.listValue();

        for (List<String> types : msg.getNetworkServices().values()) {
            for (String type : types) {
                if (existingNwsTypes.contains(type)) {
                    throw new ApiMessageInterceptionException(operr("there has been a network service[%s] attached to L3 network[uuid:%s]", type, msg.getL3NetworkUuid()));
                }
            }
        }
    }
}
