package org.zstack.test.deployer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Controller;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.APIQueryNetworkServiceProviderMsg;
import org.zstack.header.network.service.APIQueryNetworkServiceProviderReply;
import org.zstack.header.network.service.NetworkServiceProviderInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.deployer.schema.*;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

public class Deployer {
    private static final CLogger logger = Utils.getLogger(Deployer.class);
    private String xmlName;
    private JAXBContext context;
    private Api api;
    private DeployerConfig config;

    private static Map<Class<?>, AbstractDeployer> deployers = new HashMap<Class<?>, AbstractDeployer>();

    public Map<String, ZoneInventory> zones = new HashMap<String, ZoneInventory>();
    public Map<String, ClusterInventory> clusters = new HashMap<String, ClusterInventory>();
    public Map<String, HostInventory> hosts = new HashMap<String, HostInventory>();
    public Map<String, PrimaryStorageInventory> primaryStorages = new HashMap<String, PrimaryStorageInventory>();
    public Map<String, BackupStorageInventory> backupStorages = new HashMap<String, BackupStorageInventory>();
    public Map<String, ImageInventory> images = new HashMap<String, ImageInventory>();
    public Map<String, InstanceOfferingInventory> instanceOfferings = new HashMap<String, InstanceOfferingInventory>();
    public Map<String, DiskOfferingInventory> diskOfferings = new HashMap<String, DiskOfferingInventory>();
    public Map<String, L2NetworkInventory> l2Networks = new HashMap<String, L2NetworkInventory>();
    public Map<String, L3NetworkInventory> l3Networks = new HashMap<String, L3NetworkInventory>();
    public Map<String, VmInstanceInventory> vms = new HashMap<String, VmInstanceInventory>();
    public Map<String, AccountInventory> accounts = new HashMap<String, AccountInventory>();
    public Map<String, PolicyInventory> polices = new HashMap<String, PolicyInventory>();
    public Map<String, UserGroupInventory> groups = new HashMap<String, UserGroupInventory>();
    public Map<String, SecurityGroupInventory> securityGroups = new HashMap<String, SecurityGroupInventory>();
    public Map<String, PortForwardingRuleInventory> portForwardingRules = new HashMap<String, PortForwardingRuleInventory>();
    public Map<String, EipInventory> eips = new HashMap<String, EipInventory>();
    public Map<String, IpRangeInventory> ipRanges = new HashMap<String, IpRangeInventory>();
    public Map<String, LoadBalancerInventory> loadBalancers = new HashMap<String, LoadBalancerInventory>();
    public Map<String, LoadBalancerListenerInventory> loadBalancerListeners = new HashMap<String, LoadBalancerListenerInventory>();

    private Map<String, List<ClusterInventory>> primaryStoragesToAttach = new HashMap<String, List<ClusterInventory>>();
    private Map<String, List<ClusterInventory>> l2NetworksToAttach = new HashMap<String, List<ClusterInventory>>();
    private Map<String, List<ZoneInventory>> backupStoragesToAttach = new HashMap<String, List<ZoneInventory>>();
    private Map<String, List<L3NetworkInventory>> dnsToAttach = new HashMap<String, List<L3NetworkInventory>>();


    private BeanConstructor beanConstructor;
    private Set<String> springConfigs = new HashSet<String>();
    private ComponentLoader loader;
    private boolean isServerStart;

    public String SPRING_CONFIG_PORTAL_FOR_UNIT_TEST = "PortalForUnitTest.xml";
    public String SPRING_CONFIG_ZONE_MANAGER = "ZoneManager.xml";
    public String SPRING_CONFIG_CLUSTER_MANAGER = "ClusterManager.xml";
    public String SPRING_CONFIG_HOST_MANAGER = "HostManager.xml";
    public String SPRING_CONFIG_PRIMARY_STORAGE_MANAGER = "PrimaryStorageManager.xml";
    public String SPRING_CONFIG_SIMULATOR = "Simulator.xml";
    public String SPRING_CONFIG_BACK_STORAGE_MANAGER = "BackupStorageManager.xml";
    public String SPRING_CONFIG_IMAGE_MANAGER = "ImageManager.xml";
    public String SPRING_CONFIG_HOST_ALLOCATOR_MANAGER = "HostAllocatorManager.xml";
    public String SPRING_CONFIG_CONFIGURATION_MANAGER = "ConfigurationManager.xml";
    public String SPRING_CONFIG_VOLUME_MANAGER = "VolumeManager.xml";
    public String SPRING_CONFIG_NETWORK_MANAGER = "NetworkManager.xml";
    public String SPRING_CONFIG_VM_INSTANCE_MANAGER = "VmInstanceManager.xml";
    public String SPRING_CONFIG_ACCOUNT_MANAGER = "AccountManager.xml";
    public String SPRING_CONFIG_SECURITY_GROUP_MANAGER = "SecurityGroupManager.xml";
    public String SPRING_CONFIG_APPLIANCE_VM_FACADE = "ApplianceVmFacade.xml";
    public String SPRING_CONFIG_NETWORK_SERVICE = "NetworkService.xml";
    public String SPRING_CONFIG_VIP_SERVICE = "vip.xml";
    public String SPRING_CONFIG_EIP_SERVICE = "eip.xml";
    public String SPRING_CONFIG_SNAPSHOT_SERVICE = "volumeSnapshot.xml";
    public String SPRING_CONFIG_TAG_MANAGER = "tag.xml";

    private void scanDeployer() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractDeployer.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(org.springframework.stereotype.Component.class));
        for (BeanDefinition bd : scanner.findCandidateComponents("org.zstack.test")) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                AbstractDeployer d = (AbstractDeployer) clazz.newInstance();
                deployers.put(d.getSupportedDeployerClassType(), d);
                logger.debug(String.format("Scanned a deployer[%s] supporting %s", d.getClass().getName(), d.getSupportedDeployerClassType()));
            } catch (Exception e) {
                logger.warn(String.format("unable to create deployer[%s], it's probably there are some beans requried by deployer is not loaded, skip it. error message:\n%s", bd.getBeanClassName(), e.getMessage()));
            }

        }
    }

    public Deployer(String xmlName, BeanConstructor constructor) {
        this.xmlName = xmlName;
        this.beanConstructor = constructor;
    }

    public Deployer(String xmlName) {
        this(xmlName, new BeanConstructor());
    }

    private void addDefaultConfig(String config) {
        if (config != null) {
            springConfigs.add(config);
        }
    }

    public ComponentLoader getComponentLoader() {
        if (loader == null) {
            addDefaultConfig(this.SPRING_CONFIG_PORTAL_FOR_UNIT_TEST);
            addDefaultConfig(this.SPRING_CONFIG_CLUSTER_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_ZONE_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_HOST_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_SIMULATOR);
            addDefaultConfig(this.SPRING_CONFIG_PRIMARY_STORAGE_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_BACK_STORAGE_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_IMAGE_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_HOST_ALLOCATOR_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_CONFIGURATION_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_NETWORK_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_VOLUME_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_VM_INSTANCE_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_ACCOUNT_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_SECURITY_GROUP_MANAGER);
            addDefaultConfig(this.SPRING_CONFIG_APPLIANCE_VM_FACADE);
            addDefaultConfig(this.SPRING_CONFIG_NETWORK_SERVICE);
            addDefaultConfig(this.SPRING_CONFIG_VIP_SERVICE);
            addDefaultConfig(this.SPRING_CONFIG_EIP_SERVICE);
            addDefaultConfig(this.SPRING_CONFIG_SNAPSHOT_SERVICE);
            addDefaultConfig(this.SPRING_CONFIG_TAG_MANAGER);

            for (String xml : springConfigs) {
                beanConstructor.addXml(xml);
            }
            loader = beanConstructor.build();
        }
        return loader;
    }

    public void startServer() {
        if (!isServerStart) {
            getComponentLoader();
            api = new Api();
            api.startServer();
            isServerStart = true;
        }
    }

    public static void registerDeployer(AbstractDeployer deployer) {
        deployers.put(deployer.getSupportedDeployerClassType(), deployer);
    }

    private Class<?> getGenericTypeOfField(Field f) {
        Type t = f.getGenericType();
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return (Class<?>) pt.getActualTypeArguments()[0];
        }

        throw new CloudRuntimeException(String.format("Field[%s] doesn't have generic type", f.getName()));
    }

    private void exceptionIfNotCollection(Field f) {
        if (!Collection.class.isAssignableFrom(f.getType())) {
            throw new CloudRuntimeException(String.format("Field[%s] of class[%s] must be type of Collection, but it's %s", f.getName(), f.getDeclaringClass(),
                    f.getType()));
        }
    }

    @SuppressWarnings("rawtypes")
    private AbstractDeployer getDeployer(Class<?> clazz) {
        AbstractDeployer d = deployers.get(clazz);
        if (d == null) {
            logger.debug(String.format("Cannot find deployer for class[%s]", clazz.getName()));
        }
        return d;
    }

    @SuppressWarnings("unused")
    private void deployZone() throws IllegalArgumentException, IllegalAccessException, ApiSenderException {
        ZoneUnion zu = config.getZones();
        if (zu == null) {
            return;
        }
        for (Field f : zu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> zoneClass = getGenericTypeOfField(f);
            ZoneDeployer zd = (ZoneDeployer) getDeployer(zoneClass);
            List val = (List) f.get(zu);
            if (val != null && !val.isEmpty()) {
                zd.deploy((List) f.get(zu), config, this);
            }
        }
    }

    @SuppressWarnings("unused")
    private void deploySecurityGroup() throws IllegalArgumentException, IllegalAccessException, ApiSenderException {
        SecurityGroupUnion sc = config.getSecurityGroups();
        if (sc == null) {
            return;
        }
        for (Field f : sc.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> zoneClass = getGenericTypeOfField(f);
            SecurityGroupDeployer scd = (SecurityGroupDeployer) getDeployer(zoneClass);
            List val = (List) f.get(sc);
            if (val != null && !val.isEmpty()) {
                scd.deploy((List) f.get(sc), config, this);
            }
        }
    }

    @SuppressWarnings("unused")
    private void deployPortForwarding() throws IllegalArgumentException, IllegalAccessException, ApiSenderException {
        PortForwardingUnion pf = config.getPortForwardings();
        if (pf == null) {
            return;
        }
        for (Field f : pf.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> deployerClass = getGenericTypeOfField(f);
            PortForwardingDeployer scd = (PortForwardingDeployer) getDeployer(deployerClass);
            List val = (List) f.get(pf);
            if (val != null && !val.isEmpty()) {
                scd.deploy((List) f.get(pf), config, this);
            }
        }
    }

    private void deployEip() throws IllegalAccessException, ApiSenderException {
        EipUnion eip = config.getEips();
        if (eip == null) {
            return;
        }
        for (Field f : eip.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> deployerClass = getGenericTypeOfField(f);
            EipDeployer ed = (EipDeployer) getDeployer(deployerClass);
            List val = (List) f.get(eip);
            if (val != null && !val.isEmpty()) {
                ed.deploy(val, config, this);
            }
        }
    }

    private void deployLb() throws IllegalAccessException, ApiSenderException {
        LbUnion lb = config.getLbs();
        if (lb == null) {
            return;
        }
        for (Field f : lb.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> deployerClass = getGenericTypeOfField(f);
            LbDeployer ld = (LbDeployer) getDeployer(deployerClass);
            List val = (List) f.get(lb);
            if (val != null && !val.isEmpty()) {
                ld.deploy(val, config, this);
            }
        }
    }


    public void deployCluster(ClusterUnion cu, ZoneInventory zinv) {
        if (cu == null) {
            return;
        }

        for (Field f : cu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> clusterClass = getGenericTypeOfField(f);
            ClusterDeployer cd = (ClusterDeployer) getDeployer(clusterClass);
            try {
                List val = (List) f.get(cu);
                if (val != null && !val.isEmpty()) {
                    cd.deploy(val, zinv, config, this);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    public void deployHost(HostUnion hu, ClusterInventory cinv) {
        if (hu == null) {
            return;
        }

        for (Field f : hu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> hostClass = getGenericTypeOfField(f);
            HostDeployer hd = (HostDeployer) getDeployer(hostClass);
            try {
                List val = (List) f.get(hu);
                if (val != null && !val.isEmpty()) {
                    hd.deploy(val, cinv, config, this);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    public void deployPrimaryStorage(PrimaryStorageUnion pu, ZoneInventory zone) {
        if (pu == null) {
            return;
        }
        for (Field f : pu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> primaryStorageClass = getGenericTypeOfField(f);
            PrimaryStorageDeployer pd = (PrimaryStorageDeployer) getDeployer(primaryStorageClass);
            try {
                List val = (List) f.get(pu);
                if (val != null && !val.isEmpty()) {
                    pd.deploy(val, zone, config, this);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    private void deployBackupStorage() throws IllegalArgumentException, IllegalAccessException, ApiSenderException {
        BackupStorageUnion bu = config.getBackupStorages();
        if (bu == null) {
            return;
        }
        for (Field f : bu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> backupStorageClass = getGenericTypeOfField(f);
            BackupStorageDeployer bd = (BackupStorageDeployer) getDeployer(backupStorageClass);
            List val = (List) f.get(bu);
            if (val != null && !val.isEmpty()) {
                bd.deploy(val, config, this);
            }
        }
    }

    public void deployL2Network(L2NetworkUnion l2u, ZoneInventory zone) {
        if (l2u == null) {
            return;
        }
        for (Field f : l2u.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> l2NetworkClass = getGenericTypeOfField(f);
            L2NetworkDeployer l2d = (L2NetworkDeployer) getDeployer(l2NetworkClass);
            try {
                List val = (List) f.get(l2u);
                if (val != null && !val.isEmpty()) {
                    l2d.deploy(val, zone, config, this);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    private String getNetworkServiceProviderUuid(List<NetworkServiceProviderInventory> providers, String providerType) {
        for (NetworkServiceProviderInventory p : providers) {
            if (p.getType().equals(providerType)) {
                return p.getUuid();
            }
        }
        throw new CloudRuntimeException(String.format("unable to find network service provider[name:%s]", providerType));
    }

    public void attachNetworkServiceToL3Network(L3NetworkInventory l3, List<NetworkServiceConfig> services) throws ApiSenderException {
        if (services == null || services.isEmpty()) {
            return;
        }

        APIQueryNetworkServiceProviderMsg msg = new APIQueryNetworkServiceProviderMsg();
        msg.setConditions(new ArrayList<QueryCondition>());
        APIQueryNetworkServiceProviderReply r = api.query(msg, APIQueryNetworkServiceProviderReply.class);
        List<NetworkServiceProviderInventory> providers = r.getInventories();
        for (NetworkServiceConfig nc : services) {
            String uuid = getNetworkServiceProviderUuid(providers, nc.getProvider());
            api.attachNetworkServiceToL3Network(l3.getUuid(), uuid, nc.getServiceType());
        }
    }

    public void deployL3Network(L3NetworkUnion l3u, L2NetworkInventory l2Network) {
        if (l3u == null) {
            return;
        }
        for (Field f : l3u.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> l3NetworkClass = getGenericTypeOfField(f);
            L3NetworkDeployer l3d = (L3NetworkDeployer) getDeployer(l3NetworkClass);
            try {
                List val = (List) f.get(l3u);
                if (val != null && !val.isEmpty()) {
                    l3d.deploy(val, l2Network, config, this);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    private void deployVm() throws IllegalArgumentException, IllegalAccessException, ApiSenderException {
        VmUnion vu = config.getVm();
        if (vu == null) {
            return;
        }
        for (Field f : vu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> vmClass = getGenericTypeOfField(f);
            VmDeployer vd = (VmDeployer) getDeployer(vmClass);
            List val = (List) f.get(vu);
            if (val != null && !val.isEmpty()) {
                vd.deploy(val, config, this);
            }
        }
    }

    private void doAttachPrimaryStorage() throws ApiSenderException {
        for (Map.Entry<String, List<ClusterInventory>> e : primaryStoragesToAttach.entrySet()) {
            PrimaryStorageInventory pinv = primaryStorages.get(e.getKey());
            if (pinv == null) {
                throw new CloudRuntimeException(String.format("Cannot find PrimaryStorage[name:%s] to attach", e.getKey()));
            }

            for (ClusterInventory cinv : e.getValue()) {
                api.attachPrimaryStorage(cinv.getUuid(), pinv.getUuid());
            }
        }
    }

    private void doAttachBackupStorage() throws ApiSenderException {
        for (Map.Entry<String, List<ZoneInventory>> e : backupStoragesToAttach.entrySet()) {
            BackupStorageInventory binv = backupStorages.get(e.getKey());
            if (binv == null) {
                throw new CloudRuntimeException(String.format("Cannot find BackupStorage[name:%s] to attach", e.getKey()));
            }

            for (ZoneInventory zinv : e.getValue()) {
                api.attachBackupStorage(zinv.getUuid(), binv.getUuid());
            }
        }
    }

    private void doAttachL2Network() throws ApiSenderException {
        for (Map.Entry<String, List<ClusterInventory>> e : l2NetworksToAttach.entrySet()) {
            L2NetworkInventory l2inv = l2Networks.get(e.getKey());
            if (l2inv == null) {
                throw new CloudRuntimeException(String.format("Cannot find L2Network[name:%s] to attach", e.getKey()));
            }

            for (ClusterInventory cinv : e.getValue()) {
                api.attachL2NetworkToCluster(l2inv.getUuid(), cinv.getUuid());
            }
        }
    }

    private void deployInstanceOffering() throws ApiSenderException, IllegalArgumentException, IllegalAccessException {
        InstanceOfferingUnion iou = config.getInstanceOfferings();
        if (iou == null) {
            return;
        }
        for (Field f : iou.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> instanceOfferingClass = getGenericTypeOfField(f);
            InstanceOfferingDeployer id = (InstanceOfferingDeployer) getDeployer(instanceOfferingClass);
            List val = (List) f.get(iou);
            if (val != null && !val.isEmpty()) {
                id.deploy(val, config, this);
            }
        }
    }

    private void deployDiskOffering() throws ApiSenderException {
        for (DiskOfferingConfig dc : config.getDiskOffering()) {
            DiskOfferingInventory dinv = new DiskOfferingInventory();
            dinv.setName(dc.getName());
            dinv.setDescription(dc.getDescription());
            dinv.setDiskSize(parseSizeCapacity(dc.getDiskSize()));
            dinv.setAllocatorStrategy(dc.getAllocatorStrategy());

            SessionInventory session = dc.getAccountRef() == null ? null : loginByAccountRef(dc.getAccountRef(), config);

            dinv = api.addDiskOfferingByFullConfig(dinv, session);
            diskOfferings.put(dinv.getName(), dinv);
        }
    }

    private void deployImage() throws IllegalArgumentException, IllegalAccessException, ApiSenderException {
        ImageUnion iu = config.getImages();
        if (iu == null) {
            return;
        }
        for (Field f : iu.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            exceptionIfNotCollection(f);
            Class<?> ImageClass = getGenericTypeOfField(f);
            ImageDeployer id = (ImageDeployer) getDeployer(ImageClass);
            List val = (List) f.get(iu);
            if (val != null && !val.isEmpty()) {
                id.deploy(val, config, this);
            }
        }
    }

    public Deployer load() {
        try {
            context = JAXBContext.newInstance("org.zstack.test.deployer.schema");
            URL configFile = this.getClass().getClassLoader().getResource(xmlName);
            if (configFile == null) {
                throw new IllegalArgumentException(
                        String.format("Can not find deploy configure file[%s] in classpath", xmlName));
            }
            Unmarshaller unmarshaller = context.createUnmarshaller();
            config = (DeployerConfig) unmarshaller.unmarshal(configFile);
            logger.debug(String.format("validating deployer config[%s]:", xmlName));
            DeployerValidator validator = new DeployerValidator(config);
            validator.vaildate();
            getComponentLoader();
            api = new Api();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        return this;
    }

    public Deployer prepareApiClient() {
        api.prepare();
        return this;
    }

    public Deployer start() {
        if (!isServerStart) {
            api.startServer();
            isServerStart = true;
        }
        return this;
    }

    public void deploy() {
        scanDeployer();

        try {
            deployAccount();
            deployZone();
            deployBackupStorage();
            doAttachPrimaryStorage();
            doAttachBackupStorage();
            doAttachL2Network();
            deploySecurityGroup();
            deployImage();
            deployDiskOffering();
            deployInstanceOffering();
            deployVm();
            deployPortForwarding();
            deployEip();
            deployLb();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void build() {
        load();
        start();
        deploy();
    }

    public Api getApi() {
        return api;
    }

    public long parseSizeCapacity(String capacity) {
        capacity = capacity.toUpperCase();
        if (capacity.endsWith("G")) {
            capacity = capacity.replaceAll("G", "").trim();
            return SizeUnit.GIGABYTE.toByte(Long.valueOf(capacity));
        } else if (capacity.endsWith("T")) {
            capacity = capacity.replaceAll("T", "").trim();
            return SizeUnit.TERABYTE.toByte(Long.valueOf(capacity));
        } else if (capacity.endsWith("M")) {
            capacity = capacity.replaceAll("M", "").trim();
            return SizeUnit.MEGABYTE.toByte(Long.valueOf(capacity));
        } else if (capacity.endsWith("K")) {
            capacity = capacity.replaceAll("K", "").trim();
            return SizeUnit.KILOBYTE.toByte(Long.valueOf(capacity));
        } else if (capacity.endsWith("B")) {
            capacity = capacity.replaceAll("B", "").trim();
            return SizeUnit.BYTE.toByte(Long.valueOf(capacity));
        } else {
            return Long.valueOf(capacity);
        }
    }

    public void addIpRange(List<IpRangeConfig> ipRanges, L3NetworkInventory l3inv, SessionInventory session) throws ApiSenderException {
        for (IpRangeConfig ipc : ipRanges) {
            IpRangeInventory ipinv = new IpRangeInventory();
            ipinv.setDescription(ipc.getDescription());
            ipinv.setEndIp(ipc.getEndIp());
            ipinv.setStartIp(ipc.getStartIp());
            ipinv.setGateway(ipc.getGateway());
            ipinv.setL3NetworkUuid(l3inv.getUuid());
            ipinv.setName(ipc.getName());
            ipinv.setNetmask(ipc.getNetmask());
            if (session == null) {
                ipinv = api.addIpRangeByFullConfig(ipinv);
            } else {
                ipinv = api.addIpRangeByFullConfig(ipinv, session);
            }
            this.ipRanges.put(ipinv.getName(), ipinv);
        }
    }

    public void attachPrimaryStorage(List<String> primaryStorageNames, ClusterInventory cluster) {
        for (String primaryStorageName : primaryStorageNames) {
            List<ClusterInventory> clusters = primaryStoragesToAttach.get(primaryStorageName);
            if (clusters == null) {
                clusters = new ArrayList<ClusterInventory>(1);
                primaryStoragesToAttach.put(primaryStorageName, clusters);
            }
            clusters.add(cluster);
        }
    }

    public void attachBackupStorage(List<String> backupStorageNames, ZoneInventory zone) {
        for (String backupStorageName : backupStorageNames) {
            List<ZoneInventory> zones = backupStoragesToAttach.get(backupStorageName);
            if (zones == null) {
                zones = new ArrayList<ZoneInventory>(1);
                backupStoragesToAttach.put(backupStorageName, zones);
            }
            zones.add(zone);
        }
    }

    public void attachL2Network(List<String> l2NetworkNames, ClusterInventory cluster) {
        for (String l2NetworkName : l2NetworkNames) {
            List<ClusterInventory> clusters = l2NetworksToAttach.get(l2NetworkName);
            if (clusters == null) {
                clusters = new ArrayList<ClusterInventory>(1);
                l2NetworksToAttach.put(l2NetworkName, clusters);
            }
            clusters.add(cluster);
        }
    }

    public void attachDns(List<String> dnsNames, L3NetworkInventory l3Network) {
        for (String dnsName : dnsNames) {
            List<L3NetworkInventory> l3Networks = dnsToAttach.get(dnsName);
            if (l3Networks == null) {
                l3Networks = new ArrayList<L3NetworkInventory>(1);
                dnsToAttach.put(dnsName, l3Networks);
            }
            l3Networks.add(l3Network);
        }
    }

    private void attachPolicyToUser(String policyName, UserInventory uinv, SessionInventory session) throws ApiSenderException {
        PolicyInventory pinv = polices.get(policyName);
        assert pinv != null;
        api.attachPolicyToUser(uinv.getAccountUuid(), uinv.getUuid(), pinv.getUuid(), session);
    }

    private void attachUserToGroup(String groupName, UserInventory uinv, SessionInventory session) throws ApiSenderException {
        UserGroupInventory ginv = groups.get(groupName);
        assert ginv != null;
        api.attachUserToGroup(uinv.getAccountUuid(), uinv.getUuid(), ginv.getUuid(), session);
    }

    private void deployUser(AccountConfig ac, AccountInventory ainv) throws ApiSenderException {
        SessionInventory session = api.loginByAccount(ainv.getName(), ac.getPassword());
        for (UserConfig uc : ac.getUser()) {
            UserInventory uinv = api.createUser(ainv.getUuid(), uc.getName(), uc.getPassword(), session);
            for (String policyName : uc.getPolicyRef()) {
                attachPolicyToUser(policyName, uinv, session);
            }
            for (String groupName : uc.getGroupRef()) {
                attachUserToGroup(groupName, uinv, session);
            }
        }
    }

    private void attachPolicyToGroup(String policyName, UserGroupInventory ginv, SessionInventory session) throws ApiSenderException {
        PolicyInventory pinv = polices.get(policyName);
        assert pinv != null;
        api.attachPolicyToGroup(ginv.getAccountUuid(), ginv.getUuid(), pinv.getUuid(), session);
    }

    private void deployGroup(AccountConfig ac, AccountInventory ainv) throws ApiSenderException {
        SessionInventory session = api.loginByAccount(ainv.getName(), ac.getPassword());
        for (GroupConfig gc : ac.getGroup()) {
            UserGroupInventory ginv = api.createGroup(ainv.getUuid(), gc.getName(), session);
            for (String policyName : gc.getPolicyRef()) {
                attachPolicyToGroup(policyName, ginv, session);
            }
            groups.put(ginv.getName(), ginv);
        }
    }

    private void deployAccount() throws ApiSenderException, IOException {
        for (AccountConfig ac : config.getAccount()) {
            AccountInventory inv = api.createAccount(ac.getName(), ac.getPassword());
            deployGroup(ac, inv);
            deployUser(ac, inv);
            this.accounts.put(inv.getName(), inv);
        }
    }

    public Deployer addSpringConfig(String xml) {
        springConfigs.add(xml);
        return this;
    }

    public SessionInventory loginByAccountRef(String accountRef, DeployerConfig dc) throws ApiSenderException {
        AccountInventory acnt = this.accounts.get(accountRef);
        assert acnt != null;
        AccountConfig targetAccount = null;
        for (AccountConfig ac : dc.getAccount()) {
            if (ac.getName().equals(accountRef)) {
                targetAccount = ac;
                break;
            }
        }
        assert targetAccount != null;

        return api.loginByAccount(targetAccount.getName(), targetAccount.getPassword());
    }
}
