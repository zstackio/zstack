package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkChecker;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.sdnController.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;

import static org.zstack.core.Platform.argerr;

/**
 * Created by shixin.ruan on 09/17/2019.
 */
public class HardwareVxlanNetworkPoolFactory implements L2NetworkFactory, GlobalApiMessageInterceptor {
    private static CLogger logger = Utils.getLogger(HardwareVxlanNetworkPoolFactory.class);
    static L2NetworkType type = new L2NetworkType(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VxlanNetworkChecker vxlanInterceptor;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion completion) {
        HardwareL2VxlanNetworkPoolVO vo = new HardwareL2VxlanNetworkPoolVO(ovo);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setSdnControllerUuid(((APICreateL2HardwareVxlanNetworkPoolMsg) msg).getSdnControllerUuid());

        HardwareVxlanNetworkPool hardwareVxlanPool = new HardwareVxlanNetworkPool(vo);

        Map data = new HashMap();
        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setData(data);
        fchain.setName(String.format("create-hardware-vxlan-network-pool-%s", msg.getName()));
        fchain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("pre-check-for-create-hardware-vxlan-network-pool-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        hardwareVxlanPool.preCreateVxlanNetworkPoolOnSdnController(vo, new Completion(trigger) {
                            @Override
                            public void success() {
                                data.put(SdnControllerConstant.Params.HARDWARE_VXLAN_POOLS.toString(), vo);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("create-hardware-vxlan-on-db-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HardwareL2VxlanNetworkPoolVO vo = (HardwareL2VxlanNetworkPoolVO) data.get(SdnControllerConstant.Params.HARDWARE_VXLAN_POOLS.toString());
                        vo = dbf.persistAndRefresh(vo);

                        HardwareL2VxlanNetworkPoolInventory inv = HardwareL2VxlanNetworkPoolInventory.valueOf(vo);
                        String info = String.format("successfully create HardwareVxlanNetworkPool, %s", JSONObjectUtil.toJsonString(inv));
                        logger.debug(info);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });
                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(HardwareL2VxlanNetworkPoolInventory.valueOf(dbf.findByUuid(vo.getUuid(), HardwareL2VxlanNetworkPoolVO.class)));
                    }
                });
                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        dbf.removeByPrimaryKey(vo.getUuid(), HardwareL2VxlanNetworkPoolVO.class);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new HardwareVxlanNetworkPool(vo);
    }


    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APIAttachL2NetworkToClusterMsg.class,
                APICreateL3NetworkMsg.class,
                APICreateL2VxlanNetworkMsg.class,
                APICreateL2HardwareVxlanNetworkMsg.class,
                APICreateL2HardwareVxlanNetworkPoolMsg.class);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APICreateL2VxlanNetworkMsg) {
            validate((APICreateL2VxlanNetworkMsg) msg);
        } else if (msg instanceof APICreateL2HardwareVxlanNetworkMsg) {
            validate((APICreateL2HardwareVxlanNetworkMsg) msg);
        } else if (msg instanceof APICreateL2HardwareVxlanNetworkPoolMsg) {
            validate((APICreateL2HardwareVxlanNetworkPoolMsg) msg);
        }

        return msg;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        L2NetworkVO l2NetworkVO = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (!l2NetworkVO.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE)) {
            return;
        }

        String overlappedPool = vxlanInterceptor.getOverlapVniRangePool(L2NetworkInventory.valueOf(l2NetworkVO), msg.getClusterUuid());
        if (overlappedPool != null) {
            throw new ApiMessageInterceptionException(argerr("overlap vni range with vxlan network pool [%s]", overlappedPool));
        }
    }

    private void validate(APICreateL3NetworkMsg msg) {
        String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();
        if (type.equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE)) {
            throw new ApiMessageInterceptionException(argerr("hareware vxlan network pool doesn't support create l3 network"));
        }
    }

    private void validate(APICreateL2HardwareVxlanNetworkPoolMsg msg) {
        if (msg.getPhysicalInterface() == null || msg.getPhysicalInterface().equals("")) {
            throw new ApiMessageInterceptionException(argerr("hareware vxlan network pool must configure the physical interface"));
        }
    }

    private void validate(APICreateL2VxlanNetworkMsg msg) {
        VxlanNetworkPoolVO poolVO = dbf.findByUuid(msg.getPoolUuid(), VxlanNetworkPoolVO.class);
        if (poolVO.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE)
                && !msg.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE)) {
            throw new ApiMessageInterceptionException(argerr("ONLY hareware vxlan network can be created in hareware vxlan pool"));
        }

        if (!poolVO.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE)
                && msg.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE)) {
            throw new ApiMessageInterceptionException(argerr("hareware vxlan network can ONLY be created in hareware vxlan pool"));
        }
    }

    private void validate(APICreateL2HardwareVxlanNetworkMsg msg) {
        VxlanNetworkPoolVO poolVO = dbf.findByUuid(msg.getPoolUuid(), VxlanNetworkPoolVO.class);
        if (msg.getZoneUuid() != null && !msg.getZoneUuid().equals(poolVO.getZoneUuid())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("the zone uuid provided not equals to zone uuid of pool [%s], please correct it or do not fill it",
                            msg.getPoolUuid())
            ));
        } else if (msg.getZoneUuid() == null) {
            msg.setZoneUuid(poolVO.getZoneUuid());
        }

        // FIXME: only support H3cSdnController who use vni as vlan id
        // check interface name length
        if (msg.getVni() != null && NetworkUtils.generateVlanDeviceName(
                poolVO.getPhysicalInterface(), msg.getVni()).length() > L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE) {
            throw new ApiMessageInterceptionException(argerr("cannot create vlan-device on %s because it's too long"
                    , msg.getPhysicalInterface()));
        }
    }
}
