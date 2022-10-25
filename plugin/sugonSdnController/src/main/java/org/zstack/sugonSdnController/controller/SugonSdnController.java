package org.zstack.sugonSdnController.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.header.*;
import org.zstack.sugonSdnController.controller.api.ApiConnector;
import org.zstack.sugonSdnController.controller.api.ApiConnectorFactory;
import org.zstack.sugonSdnController.controller.api.Status;
import org.zstack.sugonSdnController.controller.api.types.*;
import org.zstack.sugonSdnController.header.APICreateL2TfNetworkMsg;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SugonSdnController implements TfSdnController, SdnController {
    private static final CLogger logger = Utils.getLogger(SugonSdnController.class);

    private SdnControllerVO sdnControllerVO;

    public SugonSdnController(SdnControllerVO vo) {
        sdnControllerVO = vo;
    }

    @Override
    public void preInitSdnController(APIAddSdnControllerMsg msg, Completion completion) {
        try {
            long count = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER).count();
            if(count > 0) {
                completion.fail(operr("tf sdn controller already exists."));
                return;
            }
            AccountVO accountVO = Q.New(AccountVO.class).eq(AccountVO_.name, SugonSdnControllerConstant.ZSTACK_DEFAULT_ACCOUNT).find();
            if(accountVO == null) {
                completion.fail(operr("get default admin account from zstack db failed"));
                return;
            }
            String accountUuid = StringDSL.transToTfUuid(accountVO.getUuid());
            ApiConnector apiConnector = ApiConnectorFactory.build(msg.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            assert apiConnector != null;
            Domain domain = (Domain) apiConnector.findByFQN(Domain.class, SugonSdnControllerConstant.TF_DEFAULT_DOMAIN);
            if(domain == null){
                completion.fail(operr("get default domain on tf controller failed"));
                return;
            }
            Project defaultProject = (Project) apiConnector.findById(Project.class, accountUuid);
            if(defaultProject == null){
                Project project = new Project();
                project.setParent(domain);
                project.setDisplayName(SugonSdnControllerConstant.ZSTACK_DEFAULT_ACCOUNT);
                project.setName(accountUuid);
                project.setUuid(accountUuid);
                Status status = apiConnector.create(project);
                if(status.isSuccess()){
                    logger.info("create tf project for zstack admin success");
                    completion.success();
                }else{
                    completion.fail(operr("create tf project for zstack admin on tf controller failed"));
                }
            }else{
                logger.warn("tf project for zstack admin already exists: " + accountUuid);
                completion.success();
            }
        } catch (IOException e) {
            String message = String.format("create tf project for zstack admin on tf controller failed due to: %s", e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void initSdnController(APIAddSdnControllerMsg msg, Completion completion) {
        completion.success();
    }

    @Override
    public void postInitSdnController(APIAddSdnControllerMsg msg, Completion completion){
        completion.success();
    }

    @Override
    public void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void createVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void createL2Network(L2NetworkVO l2NetworkVO, APICreateL2NetworkMsg msg, List<String> systemTags, Completion completion) {
        String accountUuid = StringDSL.transToTfUuid(l2NetworkVO.getAccountUuid());
        String uuid = StringDSL.transToTfUuid(l2NetworkVO.getUuid());
        String name = l2NetworkVO.getName();
        try {
            APICreateL2TfNetworkMsg l2TfNetworkMsg = (APICreateL2TfNetworkMsg) msg;
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            Project project = (Project) apiConnector.findById(Project.class, accountUuid);
            if(project == null) {
                completion.fail(operr("get project[uuid:%s] on tf controller failed ", accountUuid));
            }else{
                VirtualNetwork virtualNetwork = new VirtualNetwork();
                virtualNetwork.setParent(project);
                virtualNetwork.setDisplayName(l2NetworkVO.getName());
                virtualNetwork.setName(uuid);
                virtualNetwork.setUuid(uuid);
                if(l2TfNetworkMsg.getIpPrefix() != null) {
                    IPSegmentType ipSegmentType = new IPSegmentType(l2TfNetworkMsg.getIpPrefix(), l2TfNetworkMsg.getIpPrefixLength());
                    virtualNetwork.setIpSegment(ipSegmentType);
                }
                Status status = apiConnector.create(virtualNetwork);
                if(status.isSuccess()){
                    logger.info("create tf l2 network success, name:" + name);
                    completion.success();
                }else{
                    completion.fail(operr("create tf l2 network[name:%s] on tf controller failed ", name));
                }
            }
        } catch (IOException e) {
            String message = String.format("create tf l2 network[name:%s] on tf controller failed due to: %s", name, e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void updateL2Network(L2NetworkVO l2NetworkVO, List<String> systemTags, Completion completion) {
        String uuid = StringDSL.transToTfUuid(l2NetworkVO.getUuid());
        String name = l2NetworkVO.getName();
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, uuid);
            if(virtualNetwork == null){
                completion.fail(operr("get virtual network[uuid:%s] on tf controller failed ", uuid));
            }else{
                virtualNetwork.setDisplayName(name);
                Status status = apiConnector.update(virtualNetwork);
                if(status.isSuccess()){
                    logger.info("update tf l2 network success, name:" + name);
                    completion.success();
                }else{
                    completion.fail(operr("update tf l2 network[name:%s] on tf controller failed ", name));
                }
            }
        } catch (IOException e) {
            String message = String.format("update tf l2 network[name:%s] on tf controller failed due to: %s ", name, e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void deleteL2Network(L2NetworkVO l2NetworkVO, List<String> systemTags, Completion completion) {
        String uuid = StringDSL.transToTfUuid(l2NetworkVO.getUuid());
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            Status status = apiConnector.delete(VirtualNetwork.class, uuid);
            if(status.isSuccess()){
                logger.info("delete tf l2 network success, uuid:" + uuid);
                completion.success();
            }else{
                completion.fail(operr("delete tf l2 network[uuid:%s] on tf controller failed ", uuid));
            }
        } catch (IOException e) {
            String message = String.format("delete tf l2 network[uuid:%s] on tf controller failed due to: %s", uuid, e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion) {
        completion.success();
    }

    @Override
    public void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion) {
        completion.success();
    }

    @Override
    public List<SdnVniRange> getVniRange(SdnControllerInventory controller) {
        return Collections.emptyList();
    }

    @Override
    public List<SdnVlanRange> getVlanRange(SdnControllerInventory controller) {
        return Collections.emptyList();
    }

    @Override
    public void createAccount(AccountInventory account) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            Project project = new Project();
            project.getDefaultParent();
            project.getQualifiedName();
            project.setUuid(StringDSL.transToTfUuid(account.getUuid()));
            project.setDisplayName(account.getName());
            project.setName(account.getName());
            Status status = apiConnector.create(project);
            if (!status.isSuccess()) {
                logger.error("tf sync create account fail");
            }
        } catch (Exception e){
            String message = String.format("tf sync create account occur exception: %s", e.getMessage());
            logger.error(message, e);
        }
    }

    @Override
    public void deleteAccount(AccountInventory account) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            Project project = new Project();
            project.setUuid(StringDSL.transToTfUuid(account.getUuid()));
            Status status = apiConnector.delete(project);
            if (!status.isSuccess()) {
                logger.error("tf sync delete account fail");
            }
        } catch (Exception e){
            String message = String.format("tf sync delete account occur exception: %s", e.getMessage());
            logger.error(message, e);
        }
    }

    @Override
    public void updateAccount(AccountInventory account) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            Project project = new Project();
            project.setUuid(StringDSL.transToTfUuid(account.getUuid()));
            project.setDisplayName(account.getName());
            Status status = apiConnector.update(project);
            if (!status.isSuccess()) {
                logger.error("tf sync update account fail");
            }
        } catch (Exception e){
            String message = String.format("tf sync update account occur exception: %s", e.getMessage());
            logger.error(message, e);
        }
    }

    @Override
    public void deleteL3Network(L3NetworkVO l3NetworkVO, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                // 判断tf网络是否存在
                if(vn.getNetworkIpam()!=null)  {
                    Optional<IpamSubnetType> opCheck = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(k->k.getSubnetUuid().equals(StringDSL.transToTfUuid(l3NetworkVO.getUuid())))
                            .findFirst();
                    if (opCheck.isPresent()) {
                        // 移除指定的三层子网
                        vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().remove(opCheck.get());
                    }
                    // 更新 tf 网络信息
                    Status status = apiConnector.update(vn);
                    if(!status.isSuccess()){
                        completion.fail(operr("call tf api failed"));
                    } else{
                        completion.success();
                    }
                } else{
                    String message = String.format("delete tf l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.info(message);
                    completion.success();
                }
            } else{
                String message = String.format("delete tf l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("delete tf l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }

    }

    @Override
    public void updateL3Network(L3NetworkVO l3NetworkVO, APIUpdateL3NetworkMsg msg, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            // 判断tf网络是否存在
            if(vn!=null){
                if(vn.getNetworkIpam()!=null) {
                    // 判断子网是否存在
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        if(StringUtils.isNotBlank(msg.getName())&&StringUtils.isNotEmpty(msg.getName())){
                            checkOp.get().setSubnetName(msg.getName());
                        }
                        if(StringUtils.isNotBlank(msg.getDnsDomain())&&StringUtils.isNotEmpty(msg.getDnsDomain())){
                            if(checkOp.get().getDnsNameservers()!=null) {
                                checkOp.get().getDnsNameservers().clear();
                            }
                            checkOp.get().addDnsNameservers(msg.getDnsDomain());
                        }
                        // 更新 tf 网络信息
                        Status status = apiConnector.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("call tf api failed"));
                        } else{
                            completion.success();
                        }
                    }
                    // 未找到指定的子网，继续执行后续的zstack逻辑
                    completion.success();
                } else{
                    String message = String.format("update l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.info(message);
                    completion.success();
                }
            } else{
                String message = String.format("update l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("update l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void addL3IpRangeByCidr(L3NetworkVO l3NetworkVO, APIAddIpRangeByNetworkCidrMsg msg, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                IpamSubnetType ipamSubnetType = new IpamSubnetType();
                // subnet_uuid
                ipamSubnetType.setSubnetUuid(StringDSL.transToTfUuid(l3NetworkVO.getUuid()));
                // subnet：IP Range
                SubnetType subnetType = new SubnetType();
                subnetType.setIpPrefix(msg.getNetworkCidr().split("/")[0]);
                subnetType.setIpPrefixLen(Integer.parseInt(msg.getNetworkCidr().split("/")[1]));
                ipamSubnetType.setSubnet(subnetType);
                // subnet_name
                ipamSubnetType.setSubnetName(l3NetworkVO.getName());
                // DNS -> 查询zstack数据库
                List<L3NetworkDnsVO> dns = Q.New(L3NetworkDnsVO.class).eq(L3NetworkDnsVO_.l3NetworkUuid, l3NetworkVO.getUuid()).list();
                if(dns!=null&&dns.size()>0){
                    ipamSubnetType.setDnsServerAddress(dns.get(0).getDns());
                }
                // host_route -> 查询zstack数据库
                List<L3NetworkHostRouteVO> hostRoutes = Q.New(L3NetworkHostRouteVO.class).eq(L3NetworkHostRouteVO_.l3NetworkUuid, l3NetworkVO.getUuid()).list();
                RouteTableType routeTableType = new RouteTableType();
                if(hostRoutes!=null&&hostRoutes.size()>0){
                    hostRoutes.stream().forEach(k->{
                        routeTableType.addRoute(new RouteType(k.getPrefix(),k.getNexthop(),null));
                    });
                    ipamSubnetType.setHostRoutes(routeTableType);
                }
                // 封装实体 -> ObjectReference<VnSubnetsType>
                IpamSubnets ipamSubnets = new IpamSubnets();
                ipamSubnets.addSubnets(ipamSubnetType);
                VnSubnetsType vnSubnetsType = new VnSubnetsType();
                vnSubnetsType.addIpamSubnets(ipamSubnetType);
                if(vn.getNetworkIpam()!=null){
                    vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().add(ipamSubnetType);
                } else{
                    NetworkIpam networkIpam = new NetworkIpam();
                    networkIpam.setName("default-network-ipam");
                    vn.setNetworkIpam(networkIpam,vnSubnetsType);
                }
                // 更新 tf 网络信息
                Status status = apiConnector.update(vn);
                if(!status.isSuccess()){
                    completion.fail(operr("call tf api failed"));
                } else{
                    completion.success();
                }
            } else{
                String message = String.format("add tf l3 subnet[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("add tf l3 subnet[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void addL3HostRoute(L3NetworkVO l3NetworkVO, APIAddHostRouteToL3NetworkMsg msg, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                // 判断tf网络是否存在
                if(vn.getNetworkIpam()!=null) {
                    // 判断子网是否存在
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        // 子网存在
                        Set<L3NetworkHostRouteVO> hostRouters =l3NetworkVO.getHostRoutes();
                        RouteTableType routeTableType = new RouteTableType();
                        if(hostRouters!=null&&hostRouters.size()>0){
                            hostRouters.stream().forEach(k->{
                                routeTableType.addRoute(new RouteType(k.getPrefix(),k.getNexthop(),null));
                            });
                        }
                        routeTableType.addRoute(new RouteType(msg.getPrefix(),msg.getNexthop(),null));
                        // 替换host route
                        checkOp.get().setHostRoutes(routeTableType);
                        // 更新 tf 网络信息
                        Status status = apiConnector.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("call tf api failed"));
                        } else{
                            completion.success();
                        }
                    } else{
                        completion.success();
                    }
                } else{
                    String message = String.format("add host router to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.error(message);
                    completion.success();
                }
            } else{
                String message = String.format("add host router to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("add host router to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void deleteL3HostRoute(L3NetworkVO l3NetworkVO, APIRemoveHostRouteFromL3NetworkMsg msg, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                // 判断tf网络是否存在
                if(vn.getNetworkIpam()!=null) {
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        // 子网存在
                        Set<L3NetworkHostRouteVO> hostRouters =l3NetworkVO.getHostRoutes();
                        RouteTableType routeTableType = new RouteTableType();
                        if(hostRouters!=null&&hostRouters.size()>0){
                            hostRouters.stream().forEach(k->{
                                // prefix相同的主机路由不再通知tf
                                if(!k.getPrefix().equals(msg.getPrefix())) {
                                    routeTableType.addRoute(new RouteType(k.getPrefix(), k.getNexthop(), null));
                                }
                            });
                        }
                        // 替换host route
                        checkOp.get().setHostRoutes(routeTableType);
                        // 更新 tf 网络信息
                        Status status = apiConnector.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("call tf api failed"));
                        } else{
                            completion.success();
                        }
                    } else{
                        completion.success();
                    }
                } else{
                    String message = String.format("delete host route from l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.info(message);
                    completion.success();
                }
            } else{
                String message = String.format("delete host route from l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("delete host route from l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void addL3Dns(L3NetworkVO l3NetworkVO, APIAddDnsToL3NetworkMsg msg, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            // 判断tf网络是否存在
            if(vn!=null){
                if(vn.getNetworkIpam()!=null) {
                    // 判断子网是否存在
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        checkOp.get().setDnsServerAddress(msg.getDns());
                        // 更新 tf 网络信息
                        Status status = apiConnector.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("call tf api failed"));
                        } else{
                            completion.success();
                        }
                    }
                    // 未找到指定的子网，继续执行后续的zstack逻辑
                    completion.success();
                } else{
                    String message = String.format("add dns to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.info(message);
                    completion.success();
                }
            } else{
                String message = String.format("add dns to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("add dns to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void deleteL3Dns(L3NetworkVO l3NetworkVO, APIRemoveDnsFromL3NetworkMsg msg, Completion completion) {
        try {
            ApiConnector apiConnector = ApiConnectorFactory.build(sdnControllerVO.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)apiConnector.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                // 判断tf网络是否存在
                if(vn.getNetworkIpam()!=null) {
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        // 子网存在
                        IpamSubnetType type = checkOp.get();
                        // 判断删除的dns和子网的dns是否一致
                        if(type.getDnsServerAddress().equals(msg.getDns())) {
                            String defaultDns = type.getDefaultGateway().substring(0, type.getDefaultGateway().lastIndexOf(".")) + ".253";
                            checkOp.get().setDnsServerAddress(defaultDns);
                            // 更新 tf 网络信息
                            Status status = apiConnector.update(vn);
                            if (!status.isSuccess()) {
                                completion.fail(operr("call tf api failed"));
                            } else {
                                completion.success();
                            }
                        } else{
                            completion.fail(operr("error dns address"));
                        }
                    }
                    // 未找到指定的子网，继续执行后续的zstack逻辑
                    completion.success();
                } else{
                    String message = String.format("delete dns from to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.info(message);
                    completion.success();
                }
            } else{
                String message = String.format("delete dns from to l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("delete dns from l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }
}
