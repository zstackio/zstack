package org.zstack.sugonSdnController.controller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
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
import org.zstack.network.l3.L3NetworkSystemTags;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.header.*;
import org.zstack.sugonSdnController.controller.api.*;
import org.zstack.sugonSdnController.controller.api.types.*;
import org.zstack.sugonSdnController.header.APICreateL2TfNetworkMsg;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.network.NetworkUtils.getSubnetInfo;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SugonSdnController implements TfSdnController, SdnController {
    private static final CLogger logger = Utils.getLogger(SugonSdnController.class);

    private SdnControllerVO sdnControllerVO;

    private TfHttpClient client;

    public SugonSdnController(SdnControllerVO vo) {
        sdnControllerVO = vo;
        client = new TfHttpClient(vo.getIp());
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
            client = new TfHttpClient(msg.getIp());
            Domain domain = (Domain) client.getDomain();
            if(domain == null){
                completion.fail(operr("get default domain on tf controller failed"));
                return;
            }
            Project defaultProject = (Project) client.findById(Project.class, accountUuid);
            if(defaultProject == null){
                Project project = new Project();
                project.setParent(domain);
                project.setDisplayName(SugonSdnControllerConstant.ZSTACK_DEFAULT_ACCOUNT);
                project.setName(accountUuid);
                project.setUuid(accountUuid);
                Status status = client.create(project);
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
        } catch (Exception e) {
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
            Project project = (Project) client.findById(Project.class, accountUuid);
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
                Status status = client.create(virtualNetwork);
                if(status.isSuccess()){
                    logger.info("create tf l2 network success, name:" + name);
                    completion.success();
                }else{
                    completion.fail(operr("create tf l2 network[name:%s] on tf controller failed ", name));
                }
            }
        } catch (Exception e) {
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
            VirtualNetwork virtualNetwork = (VirtualNetwork) client.findById(VirtualNetwork.class, uuid);
            if(virtualNetwork == null){
                completion.fail(operr("get virtual network[uuid:%s] on tf controller failed ", uuid));
            }else{
                virtualNetwork.setDisplayName(name);
                Status status = client.update(virtualNetwork);
                if(status.isSuccess()){
                    logger.info("update tf l2 network success, name:" + name);
                    completion.success();
                }else{
                    completion.fail(operr("update tf l2 network[name:%s] on tf controller failed ", name));
                }
            }
        } catch (Exception e) {
            String message = String.format("update tf l2 network[name:%s] on tf controller failed due to: %s ", name, e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void deleteL2Network(L2NetworkVO l2NetworkVO, List<String> systemTags, Completion completion) {
        String uuid = StringDSL.transToTfUuid(l2NetworkVO.getUuid());
        try {
            Status status = client.delete(VirtualNetwork.class, uuid);
            if(status.isSuccess()){
                logger.info("delete tf l2 network success, uuid:" + uuid);
                completion.success();
            }else{
                completion.fail(operr("delete tf l2 network[uuid:%s] on tf controller failed ", uuid));
            }
        } catch (Exception e) {
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
            Domain domain = (Domain) apiConnector.findByFQN(Domain.class, SugonSdnControllerConstant.TF_DEFAULT_DOMAIN);
            Project project = new Project();
            project.setParent(domain);
            project.setUuid(StringDSL.transToTfUuid(account.getUuid()));
            project.setDisplayName(account.getName());
            project.setName(StringDSL.transToTfUuid(account.getUuid()));
            Status status = apiConnector.create(project);
            if (!status.isSuccess()) {
                String message = String.format("create tf project[name:%s] failed due to：%s ",account.getName(), status.getMsg());
//                String message = String.format("create tf project[name:%s] failed due to：%s ",account.getName(), "tf api call failed");
                logger.error(message);
            }
        } catch (Exception e){
            String message = String.format("create tf project[name:%s] failed due to：%s ",account.getName(), e.getMessage());
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
                String message = String.format("delete tf project[name:%s] failed due to：%s ",account.getName(), status.getMsg());
//                String message = String.format("delete tf project[name:%s] failed due to：%s ",account.getName(), "tf api call failed");
                logger.error(message);
            }
        } catch (Exception e){
            String message = String.format("delete tf project[name:%s] failed due to：%s ",account.getName(), e.getMessage());
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
                String message = String.format("update tf project[name:%s] failed due to：%s ",account.getName(), status.getMsg());
//                String message = String.format("update tf project[name:%s] failed due to：%s ",account.getName(), "tf api call failed");
                logger.error(message);
            }
        } catch (Exception e){
            String message = String.format("update tf project[name:%s] failed due to：%s ",account.getName(), e.getMessage());
            logger.error(message, e);
        }
    }

    @Override
    public void deleteL3Network(L3NetworkVO l3NetworkVO, Completion completion) {
        try {
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                // 判断tf网络是否存在
                if(vn.getNetworkIpam()!=null)  {
                    Optional<IpamSubnetType> opCheck = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(k->k.getSubnetUuid().equals(StringDSL.transToTfUuid(l3NetworkVO.getUuid())))
                            .findFirst();
                    if (opCheck.isPresent()) {
                        // 移除指定的三层子网
                        vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().remove(opCheck.get());
                        if(vn.getNetworkIpam().get(0).getAttr().getIpamSubnets()==null||vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().size()==0){
                            vn.getNetworkIpam().clear();
                        }
                    }
                    // 更新 tf 网络信息
                    Status status = client.update(vn);
                    if(!status.isSuccess()){
                        completion.fail(operr("delete tf l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
//                        completion.fail(operr("delete tf l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),"tf api call failed"));
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
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
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
                        Status status = client.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("update tf l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
                        } else{
                            completion.success();
                        }
                    }
                    // 未找到指定的子网，继续执行后续的zstack逻辑
                    completion.success();
                } else{
                    String message = String.format("update tf l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf network-ipam is missing");
                    logger.info(message);
                    completion.success();
                }
            } else{
                String message = String.format("update tf l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), "tf virtual network is missing");
                logger.error(message);
                completion.fail(operr(message));
            }
        } catch (Exception e){
            String message = String.format("update tf l3 network[name:%s] on tf controller failed due to: %s ",l3NetworkVO.getName(), e.getMessage());
            logger.error(message, e);
            completion.fail(operr(message));
        }
    }

    @Override
    public void addL3IpRangeByCidr(L3NetworkVO l3NetworkVO, APIAddIpRangeByNetworkCidrMsg msg, Completion completion) {
        try {
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork) client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
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
                // 特殊IP定义
                SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(new SubnetUtils(msg.getNetworkCidr()));
                String gatewayIp = subnetInfo.getLowAddress();
                String serviceIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnetInfo.getLowAddress())+1);
                String startIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnetInfo.getLowAddress())+2);
                String endIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnetInfo.getHighAddress())-1);
                // DNS -> 查询zstack数据库
                List<L3NetworkDnsVO> dns = Q.New(L3NetworkDnsVO.class).eq(L3NetworkDnsVO_.l3NetworkUuid, l3NetworkVO.getUuid()).list();
                if(dns!=null&&dns.size()>0){
                    ipamSubnetType.setDnsServerAddress(dns.get(0).getDns());
                } else{
                    // 设置默认DnsServerAddress
                    ipamSubnetType.setDnsServerAddress(serviceIp);
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
                ipamSubnetType.setEnableDhcp(getEnableDHCPFlag(l3NetworkVO.getUuid()));

                // 设置默认网关
                ipamSubnetType.setDefaultGateway(gatewayIp);
                // 设置可分配IP池范围
                AllocationPoolType allocationPoolType = new AllocationPoolType(startIp,endIp);
                ipamSubnetType.addAllocationPools(allocationPoolType);
                // 设置分配IP从小到大
                ipamSubnetType.setAddrFromStart(true);
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
                Status status = client.update(vn);
                if(!status.isSuccess()){
                    completion.fail(operr("add tf l3 subnet[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
//                    completion.fail(operr("add tf l3 subnet[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),"tf api call failed"));
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
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
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
                        Status status = client.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("add host router to l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
//                            completion.fail(operr("add host router to l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),"tf api call failed"));
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
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
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
                        Status status = client.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("delete host route from l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
//                            completion.fail(operr("delete host route from l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),"tf api call failed"));
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
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            // 判断tf网络是否存在
            if(vn!=null){
                if(vn.getNetworkIpam()!=null) {
                    // 判断子网是否存在
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        DhcpOptionsListType type = checkOp.get().getDhcpOptionList();
                        if(type!=null){
                            String currDhcpValue = type.getDhcpOption().get(0).getDhcpOptionValue();
                            if(StringUtils.isNotEmpty(currDhcpValue)){
                                type.getDhcpOption().get(0).setDhcpOptionValue(currDhcpValue+" "+ msg.getDns());
                            } else{
                                type.getDhcpOption().get(0).setDhcpOptionValue(msg.getDns());
                            }
                        } else{
                            DhcpOptionsListType dhcpOptionsListType = new DhcpOptionsListType();
                            DhcpOptionType dhcpOptionType = new DhcpOptionType();
                            dhcpOptionType.setDhcpOptionValue(msg.getDns());
                            dhcpOptionType.setDhcpOptionName("6");
                            dhcpOptionsListType.addDhcpOption(dhcpOptionType);
                            checkOp.get().setDhcpOptionList(dhcpOptionsListType);
                        }
                        // 更新 tf 网络信息
                        Status status = client.update(vn);
                        if(!status.isSuccess()){
                            completion.fail(operr("add dns to l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
//                            completion.fail(operr("add dns to l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),"tf api call failed"));
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
            // 获取 tf 网络信息
            VirtualNetwork vn = (VirtualNetwork)client.findById(VirtualNetwork.class, StringDSL.transToTfUuid(l3NetworkVO.getL2NetworkUuid()));
            if(vn!=null){
                // 判断tf网络是否存在
                if(vn.getNetworkIpam()!=null) {
                    Optional<IpamSubnetType> checkOp = vn.getNetworkIpam().get(0).getAttr().getIpamSubnets().stream()
                            .filter(m->StringDSL.transToTfUuid(l3NetworkVO.getUuid()).equals(m.getSubnetUuid()))
                            .findFirst();
                    if(checkOp.isPresent()){
                        // 子网存在
                        String dnsValue = checkOp.get().getDhcpOptionList().getDhcpOption().get(0).getDhcpOptionValue();
                        List<String> dnsValues = new ArrayList<>(Arrays.asList(dnsValue.split(" ")));
                        dnsValues.remove(msg.getDns());
                        checkOp.get().getDhcpOptionList().getDhcpOption().get(0).setDhcpOptionValue(StringUtils.join(dnsValues, " "));
                        // 更新 tf 网络信息
                        Status status = client.update(vn);
                        if (!status.isSuccess()) {
                            completion.fail(operr("delete dns from to l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),status.getMsg()));
//                            completion.fail(operr("delete dns from to l3 network[name:%s] on tf controller failed due to：%s", l3NetworkVO.getName(),"tf api call failed"));
                        } else {
                            completion.success();
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

    private boolean getEnableDHCPFlag(String l3Uuid){
        String enableDHCP = L3NetworkSystemTags.ENABLE_DHCP.getTokenByResourceUuid(l3Uuid, L3NetworkSystemTags.ENABLE_DHCP_TOKEN);
        return Boolean.parseBoolean(enableDHCP);
    }
}
