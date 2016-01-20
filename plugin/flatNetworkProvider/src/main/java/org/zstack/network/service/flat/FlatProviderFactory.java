package org.zstack.network.service.flat;

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
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatProviderFactory implements NetworkServiceProviderFactory, PrepareDbInitialValueExtensionPoint,
        L2NetworkCreateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatProviderFactory.class);

    @Autowired
    private DatabaseFacade dbf;

    private NetworkServiceProviderInventory flatProvider;

    @Override
    public NetworkServiceProviderType getType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void createNetworkServiceProvider(APIAddNetworkServiceProviderMsg msg, NetworkServiceProviderVO vo) {

    }

    @Override
    public NetworkServiceProvider getNetworkServiceProvider(NetworkServiceProviderVO vo) {
        return new FlatProvider(vo);
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO rpvo = query.find();
        if (rpvo != null) {
            flatProvider = NetworkServiceProviderInventory.valueOf(rpvo);
            return;
        }

        rpvo = new NetworkServiceProviderVO();
        rpvo.setUuid(Platform.getUuid());
        rpvo.setName("Flat Network Service Provider");
        rpvo.setDescription("Flat Network Service Provider");
        rpvo.getNetworkServiceTypes().add(NetworkServiceType.DHCP.toString());
        rpvo.getNetworkServiceTypes().add(UserdataConstant.USERDATA_TYPE_STRING);
        //rpvo.getNetworkServiceTypes().add(NetworkServiceType.DNS.toString());
        rpvo.setType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        rpvo = dbf.persistAndRefresh(rpvo);
        flatProvider = NetworkServiceProviderInventory.valueOf(rpvo);
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setL2NetworkUuid(l2Network.getUuid());
        ref.setNetworkServiceProviderUuid(flatProvider.getUuid());
        dbf.persist(ref);
        logger.debug(String.format("successfully attach flat network service provider[uuid:%s] to the L2 network[uuid:%s, name:%s]",
                flatProvider.getUuid(), l2Network.getUuid(), l2Network.getName()));
    }
}
