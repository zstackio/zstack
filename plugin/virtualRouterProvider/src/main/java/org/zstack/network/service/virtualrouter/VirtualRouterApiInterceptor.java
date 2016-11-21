package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.IpRangeVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.identity.QuotaUtil;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VirtualRouterApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIQueryVirtualRouterOfferingMsg) {
            validate((APIQueryVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APICreateVirtualRouterOfferingMsg) {
            validate((APICreateVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterOfferingMsg) {
            validate((APIUpdateVirtualRouterOfferingMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateVirtualRouterOfferingMsg msg) {
        if (msg.getIsDefault() != null) {
            if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.PERMISSION_DENIED,
                        "cannot change the default field of a virtual router offering; only admin can do the operation"
                ));
            }
        }

        if (msg.getImageUuid() != null) {
            SimpleQuery<ImageVO> q = dbf.createQuery(ImageVO.class);
            q.select(ImageVO_.mediaType, ImageVO_.format);
            q.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
            Tuple t = q.findTuple();
            ImageMediaType type = t.get(0, ImageMediaType.class);
            String format = t.get(1, String.class);

            if (type != ImageMediaType.RootVolumeTemplate) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("image[uuid:%s]'s mediaType is %s, the mediaType of a virtual router image must be %s",
                                msg.getImageUuid(), type, ImageMediaType.RootVolumeTemplate)
                ));
            }

            if (ImageConstant.ISO_FORMAT_STRING.equals(format)) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("image[uuid:%s] is of format %s, cannot be used for virtual router", msg.getImageUuid(), format)
                ));
            }
        }
    }

    private void validate(APICreateVirtualRouterOfferingMsg msg) {
        if (msg.isDefault() != null) {
            if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.PERMISSION_DENIED,
                        "cannot create a virtual router offering with the default field set; only admin can do the operation"
                ));
            }
        }

        if (msg.getPublicNetworkUuid() == null) {
            msg.setPublicNetworkUuid(msg.getManagementNetworkUuid());
        }

        SimpleQuery<L3NetworkVO> q = dbf.createQuery(L3NetworkVO.class);
        q.select(L3NetworkVO_.zoneUuid);
        q.add(L3NetworkVO_.uuid, Op.EQ, msg.getManagementNetworkUuid());
        String zoneUuid = q.findValue();
        if (!zoneUuid.equals(msg.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("management network[uuid:%s] is not in the same zone[uuid:%s] this offering is going to create",
                            msg.getManagementNetworkUuid(), msg.getZoneUuid())
            ));
        }

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            checkIfManagementNetworkReachable(msg.getManagementNetworkUuid());
        }

        q = dbf.createQuery(L3NetworkVO.class);
        q.select(L3NetworkVO_.zoneUuid);
        q.add(L3NetworkVO_.uuid, Op.EQ, msg.getPublicNetworkUuid());
        zoneUuid = q.findValue();
        if (!zoneUuid.equals(msg.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("public network[uuid:%s] is not in the same zone[uuid:%s] this offering is going to create",
                            msg.getManagementNetworkUuid(), msg.getZoneUuid())
            ));
        }

        SimpleQuery<ImageVO> imq = dbf.createQuery(ImageVO.class);
        imq.select(ImageVO_.mediaType, ImageVO_.format);
        imq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        Tuple t = imq.findTuple();

        ImageMediaType type = t.get(0, ImageMediaType.class);
        if (type != ImageMediaType.RootVolumeTemplate) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("image[uuid:%s]'s mediaType is %s, the mediaType of a virtual router image must be %s",
                            msg.getImageUuid(), type, ImageMediaType.RootVolumeTemplate)
            ));
        }

        String format = t.get(1, String.class);
        if (ImageConstant.ISO_FORMAT_STRING.equals(format)) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("image[uuid:%s] is of format %s, cannot be used for virtual router", msg.getImageUuid(), format)
            ));
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.IN, list(msg.getPublicNetworkUuid(), msg.getManagementNetworkUuid()));
        List<NetworkServiceL3NetworkRefVO> nrefs= nq.list();
        for (NetworkServiceL3NetworkRefVO nref : nrefs) {
            if (NetworkServiceType.SNAT.toString().equals(nref.getNetworkServiceType())) {
                if (nref.getL3NetworkUuid().equals(msg.getManagementNetworkUuid())) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("the L3 network[uuid: %s] has the SNAT service enabled, it cannot be used as a management network", msg.getManagementNetworkUuid())
                    ));
                } else if (nref.getL3NetworkUuid().equals(msg.getPublicNetworkUuid())) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("the L3 network[uuid: %s] has the SNAT service enabled, it cannot be used as a public network", msg.getPublicNetworkUuid())
                    ));
                }
            }
        }
    }

    private void checkIfManagementNetworkReachable(String managementNetworkUuid) {
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.l3NetworkUuid, Op.EQ, managementNetworkUuid);
        List<IpRangeVO> iprs = q.list();
        if (iprs.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("the management network[uuid:%s] doesn't have any IP range", managementNetworkUuid)
            ));
        }

        String startIp = iprs.get(0).getStartIp();
        if (!NetworkUtils.isIpRoutedByDefaultGateway(startIp)) {
            // the mgmt server is in the same subnet of the mgmt network
            return;
        }

        String gateway = iprs.get(0).getGateway();
        for (int i=0; i<3; i++) {
            ShellResult ret = ShellUtils.runAndReturn(String.format("ping -c 1 -W 2 %s", gateway));
            if (ret.isReturnCode(0)) {
                return;
            }
        }

        throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                String.format("the management network[uuid:%s, gateway:%s] is not reachable", managementNetworkUuid, gateway)
        ));
    }

    private void validate(APIQueryVirtualRouterOfferingMsg msg) {
        boolean found = false;
        for (QueryCondition qcond : msg.getConditions()) {
            if ("type".equals(qcond.getName())) {
                qcond.setOp(QueryOp.EQ.toString());
                qcond.setValue(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
                found = true;
                break;
            }
        }

        if (!found) {
            msg.addQueryCondition("type", QueryOp.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
        }
    }
}
