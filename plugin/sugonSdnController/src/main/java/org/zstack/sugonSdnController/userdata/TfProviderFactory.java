package org.zstack.sugonSdnController.userdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.network.service.userdata.UserdataConstant;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by fuwei on 11/15/2022.
 */
public class TfProviderFactory implements NetworkServiceProviderFactory, PrepareDbInitialValueExtensionPoint,
        L2NetworkCreateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfProviderFactory.class);

    @Autowired
    private DatabaseFacade dbf;

    private NetworkServiceProviderInventory tfProvider;

    @Override
    public NetworkServiceProviderType getType() {
        return TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void createNetworkServiceProvider(APIAddNetworkServiceProviderMsg msg, NetworkServiceProviderVO vo) {

    }

    @Override
    public NetworkServiceProvider getNetworkServiceProvider(NetworkServiceProviderVO vo) {
        return new TfProvider(vo);
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, Op.EQ, TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO rpvo = query.find();
        if (rpvo != null) {
            tfProvider = NetworkServiceProviderInventory.valueOf(rpvo);

            // check if any network service type missing, if any, complement them
            SimpleQuery<NetworkServiceTypeVO> q = dbf.createQuery(NetworkServiceTypeVO.class);
            q.add(NetworkServiceTypeVO_.networkServiceProviderUuid, Op.EQ, tfProvider.getUuid());
            List<NetworkServiceTypeVO> refs = q.list();
            Set<String> types = new HashSet<String>();
            for (NetworkServiceTypeVO ref : refs) {
                types.add(ref.getType());
            }

            if (!types.contains(UserdataConstant.USERDATA_TYPE_STRING)) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(tfProvider.getUuid());
                ref.setType(UserdataConstant.USERDATA_TYPE_STRING);
                dbf.persist(ref);
            }

            return;
        }

        rpvo = new NetworkServiceProviderVO();
        rpvo.setUuid(Platform.getUuid());
        rpvo.setName("Tf Network Service Provider");
        rpvo.setDescription("Tf Network Service Provider");
        rpvo.getNetworkServiceTypes().add(UserdataConstant.USERDATA_TYPE_STRING);
        rpvo.setType(TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE_STRING);
        rpvo = dbf.persistAndRefresh(rpvo);
        tfProvider = NetworkServiceProviderInventory.valueOf(rpvo);
        logger.info("Success create Tf Network Service Provider");
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        if (!SugonSdnControllerConstant.L2_TF_NETWORK_TYPE.equals(l2Network.getType())){
            return;
        }
        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setL2NetworkUuid(l2Network.getUuid());
        ref.setNetworkServiceProviderUuid(tfProvider.getUuid());
        dbf.persist(ref);
        logger.debug(String.format("successfully attach flat network service provider[uuid:%s] to the L2 network[uuid:%s, name:%s]",
                tfProvider.getUuid(), l2Network.getUuid(), l2Network.getName()));
    }
}
