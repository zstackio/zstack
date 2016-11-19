package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.appliancevm.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.configuration.APIUpdateInstanceOfferingEvent;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefInventory;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipInventory;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VirtualRouterManagerImpl extends AbstractService implements VirtualRouterManager,
        PrepareDbInitialValueExtensionPoint, L2NetworkCreateExtensionPoint,
        GlobalApiMessageInterceptor, AddExpandedQueryExtensionPoint {
	private final static CLogger logger = Utils.getLogger(VirtualRouterManagerImpl.class);
	
	private final static List<String> supportedL2NetworkTypes = new ArrayList<String>();
	private NetworkServiceProviderInventory virtualRouterProvider;
	private Map<String, VirtualRouterHypervisorBackend> hypervisorBackends = new HashMap<String, VirtualRouterHypervisorBackend>();
    private Map<String, Integer> vrParallelismDegrees = new ConcurrentHashMap<String, Integer>();

    private List<String> virtualRouterPostCreateFlows;
    private List<String> virtualRouterPostStartFlows;
    private List<String> virtualRouterPostRebootFlows;
    private List<String> virtualRouterPostDestroyFlows;
    private List<String> virtualRouterReconnectFlows;
    private FlowChainBuilder postCreateFlowsBuilder;
    private FlowChainBuilder postStartFlowsBuilder;
    private FlowChainBuilder postRebootFlowsBuilder;
    private FlowChainBuilder postDestroyFlowsBuilder;
    private FlowChainBuilder reconnectFlowsBuilder;

    private List<VirtualRouterPostCreateFlowExtensionPoint> postCreateFlowExtensionPoints;
    private List<VirtualRouterPostStartFlowExtensionPoint> postStartFlowExtensionPoints;
    private List<VirtualRouterPostRebootFlowExtensionPoint> postRebootFlowExtensionPoints;
    private List<VirtualRouterPostReconnectFlowExtensionPoint> postReconnectFlowExtensionPoints;
    private List<VirtualRouterPostDestroyFlowExtensionPoint> postDestroyFlowExtensionPoints;

	static {
		supportedL2NetworkTypes.add(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
		supportedL2NetworkTypes.add(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
	}
	
	@Autowired
	private CloudBus bus;
	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private VirtualRouterProviderFactory providerFactory;
	@Autowired
	private PluginRegistry pluginRgty;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApplianceVmFacade apvmf;
    

	@Override
    @MessageSafe
	public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
	}

	private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVirtualRouterVmMsg) {
            handle((CreateVirtualRouterVmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final CreateVirtualRouterVmMsg msg) {
        thdf.chainSubmit(new ChainTask() {
            @Override
            public String getSyncSignature() {
                return String.format("create-vr-for-l3-%s", msg.getL3Network().getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createVirtualRouter(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void createVirtualRouter(final CreateVirtualRouterVmMsg msg, final NoErrorCompletion completion) {
        final L3NetworkInventory l3Network = msg.getL3Network();
        final VirtualRouterOfferingInventory offering = msg.getOffering();
        final CreateVirtualRouterVmReply reply = new CreateVirtualRouterVmReply();
        final String accountUuid = acntMgr.getOwnerAccountUuidOfResource(l3Network.getUuid());

        class newVirtualRouterJob {
            private void failAndReply(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            private void openFirewall(ApplianceVmSpec aspec, String l3NetworkUuid, int port, ApplianceVmFirewallProtocol protocol) {
                ApplianceVmFirewallRuleInventory r = new ApplianceVmFirewallRuleInventory();
                r.setL3NetworkUuid(l3NetworkUuid);
                r.setStartPort(port);
                r.setEndPort(port);
                r.setProtocol(protocol.toString());
                aspec.getFirewallRules().add(r);
            }

            private void openAdditionalPorts(ApplianceVmSpec aspec, String mgmtNwUuid) {
                final List<String> tcpPorts = VirtualRouterGlobalProperty.TCP_PORTS_ON_MGMT_NIC;
                if (!tcpPorts.isEmpty()) {
                    List<Integer> ports = CollectionUtils.transformToList(tcpPorts, new Function<Integer, String>() {
                        @Override
                        public Integer call(String arg) {
                            return Integer.valueOf(arg);
                        }
                    });
                    for (int p : ports) {
                        openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.tcp);
                    }
                }

                final List<String> udpPorts = VirtualRouterGlobalProperty.UDP_PORTS_ON_MGMT_NIC;
                if (!udpPorts.isEmpty()) {
                    List<Integer> ports = CollectionUtils.transformToList(udpPorts, new Function<Integer, String>() {
                        @Override
                        public Integer call(String arg) {
                            return Integer.valueOf(arg);
                        }
                    });
                    for (int p : ports) {
                        openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.udp);
                    }
                }
            }

            void create() {
                List<String> neededService = l3Network.getNetworkServiceTypesFromProvider(new Callable<String>() {
                    @Override
                    public String call() {
                        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
                        q.select(NetworkServiceProviderVO_.uuid);
                        q.add(NetworkServiceProviderVO_.type, Op.EQ, msg.getProviderType());
                        return q.findValue();
                    }
                }.call());

                if (neededService.contains(NetworkServiceType.SNAT.toString()) && offering.getPublicNetworkUuid() == null) {
                    String err = String.format("L3Network[uuid:%s, name:%s] requires SNAT service, but default virtual router offering[uuid:%s, name:%s] doesn't have a public network", l3Network.getUuid(), l3Network.getName(), offering.getUuid(), offering.getName());
                    logger.warn(err);
                    failAndReply(errf.instantiateErrorCode(VirtualRouterErrors.NO_PUBLIC_NETWORK_IN_OFFERING, err));
                    return;
                }

                ImageVO imgvo = dbf.findByUuid(offering.getImageUuid(), ImageVO.class);

                final ApplianceVmSpec aspec = new ApplianceVmSpec();
                aspec.setSyncCreate(false);
                aspec.setTemplate(ImageInventory.valueOf(imgvo));
                aspec.setApplianceVmType(ApplianceVmType.valueOf(msg.getApplianceVmType()));
                aspec.setInstanceOffering(offering);
                aspec.setAccountUuid(accountUuid);
                aspec.setName(String.format("%s.l3.%s", msg.getApplianceVmType(), l3Network.getName()));
                aspec.setInherentSystemTags(msg.getInherentSystemTags());
                aspec.setSshUsername(VirtualRouterGlobalConfig.SSH_USERNAME.value());
                aspec.setSshPort(VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class));
                aspec.setAgentPort(msg.getApplianceVmAgentPort());

                L3NetworkInventory mgmtNw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getManagementNetworkUuid(), L3NetworkVO.class));
                ApplianceVmNicSpec mgmtNicSpec = new ApplianceVmNicSpec();
                mgmtNicSpec.setL3NetworkUuid(mgmtNw.getUuid());
                mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.MANAGEMENT_NIC_MASK.toString());
                aspec.setManagementNic(mgmtNicSpec);

                String mgmtNwUuid = mgmtNw.getUuid();
                String pnwUuid;

                // NOTE: don't open 22 port here; 22 port is default opened on mgmt network in virtual router with restricted rules
                // open 22 here will cause a non-restricted rule to be added
                openFirewall(aspec, mgmtNwUuid, 7272, ApplianceVmFirewallProtocol.tcp);
                openAdditionalPorts(aspec, mgmtNwUuid);

                if (offering.getPublicNetworkUuid() != null && !offering.getManagementNetworkUuid().equals(offering.getPublicNetworkUuid())) {
                    L3NetworkInventory pnw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getPublicNetworkUuid(), L3NetworkVO.class));
                    ApplianceVmNicSpec pnicSpec = new ApplianceVmNicSpec();
                    pnicSpec.setL3NetworkUuid(pnw.getUuid());
                    pnicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_NIC_MASK.toString());
                    aspec.getAdditionalNics().add(pnicSpec);
                    pnwUuid = pnicSpec.getL3NetworkUuid();
                    aspec.setDefaultRouteL3Network(pnw);
                } else {
                    // use management nic for both management and public
                    mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_AND_MANAGEMENT_NIC_MASK.toString());
                    pnwUuid = mgmtNwUuid;
                    aspec.setDefaultRouteL3Network(mgmtNw);
                }


                if (!l3Network.getUuid().equals(mgmtNwUuid) && !l3Network.getUuid().equals(pnwUuid)) {
                    if (neededService.contains(NetworkServiceType.SNAT.toString()) && !msg.isNotGatewayForGuestL3Network()) {
                        DebugUtils.Assert(!l3Network.getIpRanges().isEmpty(), String.format("how can l3Network[uuid:%s] doesn't have ip range", l3Network.getUuid()));
                        IpRangeInventory ipr = l3Network.getIpRanges().get(0);
                        ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
                        nicSpec.setL3NetworkUuid(l3Network.getUuid());
                        nicSpec.setAcquireOnNetwork(false);
                        nicSpec.setNetmask(ipr.getNetmask());
                        nicSpec.setIp(ipr.getGateway());
                        nicSpec.setGateway(ipr.getGateway());
                        aspec.getAdditionalNics().add(nicSpec);
                    } else {
                        ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
                        nicSpec.setL3NetworkUuid(l3Network.getUuid());
                        aspec.getAdditionalNics().add(nicSpec);
                    }
                }

                ApplianceVmNicSpec guestNicSpec = mgmtNicSpec.getL3NetworkUuid().equals(l3Network.getUuid()) ? mgmtNicSpec : CollectionUtils.find(aspec.getAdditionalNics(), new Function<ApplianceVmNicSpec, ApplianceVmNicSpec>() {
                    @Override
                    public ApplianceVmNicSpec call(ApplianceVmNicSpec arg) {
                        return arg.getL3NetworkUuid().equals(l3Network.getUuid()) ? arg : null;
                    }
                });

                guestNicSpec.setMetaData(guestNicSpec.getMetaData() == null ? VirtualRouterNicMetaData.GUEST_NIC_MASK.toString()
                        : String.valueOf(Integer.valueOf(guestNicSpec.getMetaData()) | VirtualRouterNicMetaData.GUEST_NIC_MASK));

                if (neededService.contains(NetworkServiceType.DHCP.toString())) {
                    openFirewall(aspec, l3Network.getUuid(), 68, ApplianceVmFirewallProtocol.udp);
                    openFirewall(aspec, l3Network.getUuid(), 67, ApplianceVmFirewallProtocol.udp);
                }
                if (neededService.contains(NetworkServiceType.DNS.toString())) {
                    openFirewall(aspec, l3Network.getUuid(), 53, ApplianceVmFirewallProtocol.udp);
                }

                logger.debug(String.format("unable to find running virtual for L3Network[name:%s, uuid:%s], is about to create a new one",  l3Network.getName(), l3Network.getUuid()));
                apvmf.createApplianceVm(aspec, new ReturnValueCompletion<ApplianceVmInventory>(completion) {
                    @Override
                    public void success(ApplianceVmInventory apvm) {
                        String paraDegree = VirtualRouterSystemTags.VR_OFFERING_PARALLELISM_DEGREE.getTokenByResourceUuid(offering.getUuid(), VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN);
                        if (paraDegree != null) {
                            VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.createTag(apvm.getUuid(), map(e(
                                    VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN,
                                    paraDegree
                            )));
                        }

                        reply.setInventory(VirtualRouterVmInventory.valueOf(dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class)));
                        bus.reply(msg, reply);
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        failAndReply(errorCode);
                    }
                });
            }
        }

        new newVirtualRouterJob().create();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISearchVirtualRouterOffingMsg) {
            handle((APISearchVirtualRouterOffingMsg)msg);
        } else if (msg instanceof APIGetVirtualRouterOfferingMsg) {
            handle((APIGetVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterOfferingMsg) {
            handle((APIUpdateVirtualRouterOfferingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVirtualRouterOfferingMsg msg) {
        VirtualRouterOfferingVO ovo = dbf.findByUuid(msg.getUuid(), VirtualRouterOfferingVO.class);
        boolean updated = false;
        if (msg.getName() != null) {
            ovo.setName(msg.getName());
            updated = true;
        }
        if (msg.getDescription() != null) {
            ovo.setDescription(msg.getDescription());
            updated = true;
        }
        if (msg.getImageUuid() != null) {
            ovo.setImageUuid(msg.getImageUuid());
            updated = true;
        }

        if (updated) {
            ovo = dbf.updateAndRefresh(ovo);
        }

        if (msg.getIsDefault() != null) {
            DefaultVirtualRouterOfferingSelector selector = new DefaultVirtualRouterOfferingSelector();
            selector.setZoneUuid(ovo.getZoneUuid());
            selector.setPreferToBeDefault(msg.getIsDefault());
            selector.setOfferingUuid(ovo.getUuid());
            selector.selectDefaultOffering();
        }

        APIUpdateInstanceOfferingEvent evt = new APIUpdateInstanceOfferingEvent(msg.getId());
        evt.setInventory(VirtualRouterOfferingInventory.valueOf(dbf.reload(ovo)));
        bus.publish(evt);
    }

    private void handle(APIGetVirtualRouterOfferingMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, VirtualRouterOfferingInventory.class);
        APIGetVirtualRouterOfferingReply reply = new APIGetVirtualRouterOfferingReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchVirtualRouterOffingMsg msg) {
        SearchQuery<VirtualRouterOfferingInventory> q = SearchQuery.create(msg, VirtualRouterOfferingInventory.class);
        APISearchVirtualRouterOffingReply reply = new APISearchVirtualRouterOffingReply();
        String res = q.listAsString();
        reply.setContent(res);
        bus.reply(msg, reply);
    }

    @Override
	public String getId() {
		return bus.makeLocalServiceId(VirtualRouterConstant.SERVICE_ID);
	}

	@Override
	public boolean start() {
		populateExtensions();
        deployAnsible();
		buildWorkFlowBuilder();

        VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                if (VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.isMatch(tag.getTag())) {
                    String value = VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.getTokenByTag(tag.getTag(), VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN);
                    vrParallelismDegrees.put(tag.getResourceUuid(), Integer.valueOf(value));
                }
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                if (VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.isMatch(tag.getTag())) {
                    vrParallelismDegrees.remove(tag.getResourceUuid());
                }
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
            }
        });
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}
	
	public void prepareDbInitialValue() {
		SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
		query.add(NetworkServiceProviderVO_.type, Op.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
		NetworkServiceProviderVO rpvo = query.find();
		if (rpvo != null) {
			virtualRouterProvider = NetworkServiceProviderInventory.valueOf(rpvo);
			return;
		}
		
		NetworkServiceProviderVO vo = new NetworkServiceProviderVO();
        vo.setUuid(Platform.getUuid());
		vo.setName(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
		vo.setDescription("zstack virtual router network service provider");
		vo.getNetworkServiceTypes().add(NetworkServiceType.DHCP.toString());
		vo.getNetworkServiceTypes().add(NetworkServiceType.DNS.toString());
		vo.getNetworkServiceTypes().add(NetworkServiceType.SNAT.toString());
		vo.getNetworkServiceTypes().add(NetworkServiceType.PortForwarding.toString());
        vo.getNetworkServiceTypes().add(EipConstant.EIP_NETWORK_SERVICE_TYPE);
        vo.getNetworkServiceTypes().add(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
		vo.setType(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
		vo = dbf.persistAndRefresh(vo);
		virtualRouterProvider = NetworkServiceProviderInventory.valueOf(vo);
	}
	
	private void populateExtensions() {
		for (VirtualRouterHypervisorBackend extp : pluginRgty.getExtensionList(VirtualRouterHypervisorBackend.class)) {
			VirtualRouterHypervisorBackend old = hypervisorBackends.get(extp.getVirtualRouterSupportedHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VirtualRouterHypervisorBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), old.getVirtualRouterSupportedHypervisorType()));
            }
			hypervisorBackends.put(extp.getVirtualRouterSupportedHypervisorType().toString(), extp);
		}

		postCreateFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostCreateFlowExtensionPoint.class);
        postStartFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostStartFlowExtensionPoint.class);
        postRebootFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostRebootFlowExtensionPoint.class);
        postReconnectFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostReconnectFlowExtensionPoint.class);
        postDestroyFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostDestroyFlowExtensionPoint.class);
	}
	
	private NetworkServiceProviderVO getRouterVO() {
		SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
		query.add(NetworkServiceProviderVO_.type, Op.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
		return query.find();
	}

	@Override
	public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {
	}

	@Override
	public void afterCreateL2Network(L2NetworkInventory l2Network) {
		if (!supportedL2NetworkTypes.contains(l2Network.getType())) {
			return;
		}
		
		NetworkServiceProviderVO vo = getRouterVO();
		NetworkServiceProvider router = providerFactory.getNetworkServiceProvider(vo);
		try {
			router.attachToL2Network(l2Network, null);
		} catch (NetworkException e) {
			String err = String.format("unable to attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s], %s",
					vo.getUuid(), vo.getName(), vo.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType(), e.getMessage());
			logger.warn(err, e);
			return;
		}
		
		NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
		ref.setNetworkServiceProviderUuid(vo.getUuid());
		ref.setL2NetworkUuid(l2Network.getUuid());
		dbf.persist(ref);
		String info = String.format("successfully attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
				vo.getUuid(), vo.getName(), vo.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType());
		logger.debug(info);
	}

	@Override
	public NetworkServiceProviderInventory getVirtualRouterProvider() {
		return virtualRouterProvider;
	}

    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(VirtualRouterConstant.ANSIBLE_MODULE_PATH, VirtualRouterConstant.ANSIBLE_PLAYBOOK_NAME);
    }
	
	@Override
	public VirtualRouterHypervisorBackend getHypervisorBackend(HypervisorType hypervisorType) {
		VirtualRouterHypervisorBackend b = hypervisorBackends.get(hypervisorType.toString());
		if (b == null) {
			throw new CloudRuntimeException(String.format("unable to find VirtualRouterHypervisorBackend for hypervisorType[%s]", hypervisorType));
		}
		return b;
	}

	@Override
	public String buildUrl(String mgmtNicIp, String subPath) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(VirtualRouterGlobalProperty.AGENT_URL_SCHEME);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(mgmtNicIp);
        }

        ub.port(VirtualRouterGlobalProperty.AGENT_PORT);
        if (!"".equals(VirtualRouterGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(VirtualRouterGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);

        return ub.build().toUriString();
    }

	private void buildWorkFlowBuilder() {
        postCreateFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostCreateFlows).construct();
        postStartFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostStartFlows).construct();
        postRebootFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostRebootFlows).construct();
        postDestroyFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostDestroyFlows).construct();
        reconnectFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterReconnectFlows).construct();
	}

    @Override
    public List<String> selectL3NetworksNeedingSpecificNetworkService(List<String> candidate, NetworkServiceType nsType) {
        if (candidate.isEmpty()) {
            return new ArrayList<>(0);
        }
        
        SimpleQuery<NetworkServiceL3NetworkRefVO> q = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        q.select(NetworkServiceL3NetworkRefVO_.l3NetworkUuid);
        q.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.IN, candidate);
        q.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, nsType.toString());
        // no need to specify provider type, L3 networks identified by candidates are served by virtual router or vyos
        return q.listValue();
    }

    @Override
    public boolean isL3NetworkNeedingNetworkServiceByVirtualRouter(String l3Uuid, String nsType) {
        SimpleQuery<NetworkServiceL3NetworkRefVO> q = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        q.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, l3Uuid);
        q.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, nsType);
        // no need to specify provider type, L3 networks identified by candidates are served by virtual router or vyos
        return q.isExists();
    }

    private void acquireVirtualRouterVmInternal(VirtualRouterStruct struct,  final ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        final L3NetworkInventory l3Nw = struct.getL3Network();
        final VirtualRouterOfferingValidator validator = struct.getOfferingValidator();
        final VirtualRouterVmSelector selector = struct.getVirtualRouterVmSelector();

        VirtualRouterVmInventory vr = new Callable<VirtualRouterVmInventory>() {
            @Transactional(readOnly = true)
            private VirtualRouterVmVO findVR() {
                String sql = "select vr from VirtualRouterVmVO vr, VmNicVO nic where vr.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
                TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
                q.setParameter("l3Uuid", l3Nw.getUuid());
                q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
                List<VirtualRouterVmVO> vrs = q.getResultList();

                if (vrs.isEmpty()) {
                    return null;
                }

                if (selector == null) {
                    return findTheEarliestOne(vrs);
                } else {
                    return selector.select(vrs);
                }
            }

            private VirtualRouterVmVO findTheEarliestOne(List<VirtualRouterVmVO> vrs) {
                VirtualRouterVmVO vr = null;
                for (VirtualRouterVmVO v : vrs) {
                    if (vr == null) {
                        vr = v;
                        continue;
                    }

                    vr = vr.getCreateDate().before(v.getCreateDate()) ? vr : v;
                }
                return vr;
            }

            @Override
            public VirtualRouterVmInventory call() {
                VirtualRouterVmVO vr = findVR();
                if (vr != null && !VmInstanceState.Running.equals(vr.getState())) {
                    throw new OperationFailureException(errf.stringToOperationError(
                            String.format("virtual router[uuid:%s] for l3 network[uuid:%s] is not in Running state, current state is %s. We don't have HA feature now(it's coming soon), please restart it from UI and then try starting this vm again",
                                    vr.getUuid(), l3Nw.getUuid(), vr.getState())
                    ));
                }

                return vr == null ? null : new VirtualRouterVmInventory(vr);
            }
        }.call();

        if (vr != null) {
            completion.success(vr);
            return;
        }

        List<VirtualRouterOfferingInventory> offerings = findOfferingByGuestL3Network(l3Nw);
        if (offerings == null) {
            String err = String.format("unable to find a virtual router offering for l3Network[uuid:%s] in zone[uuid:%s], please at least create a default virtual router offering in that zone",
                    l3Nw.getUuid(), l3Nw.getZoneUuid());
            logger.warn(err);
            completion.fail(errf.instantiateErrorCode(VirtualRouterErrors.NO_DEFAULT_OFFERING, err));
            return;
        }

        if (struct.getVirtualRouterOfferingSelector() == null) {
            struct.setVirtualRouterOfferingSelector(new VirtualRouterOfferingSelector() {
                @Override
                public VirtualRouterOfferingInventory selectVirtualRouterOffering(L3NetworkInventory l3, List<VirtualRouterOfferingInventory> candidates) {
                    VirtualRouterOfferingInventory def = candidates.stream().filter(VirtualRouterOfferingInventory::isDefault).findAny().get();
                    return def == null ? candidates.get(0) : def;
                }
            });
        }

        VirtualRouterOfferingInventory offering = struct.getVirtualRouterOfferingSelector().selectVirtualRouterOffering(l3Nw, offerings);

        if (validator != null) {
            validator.validate(offering);
        }

        CreateVirtualRouterVmMsg msg = new CreateVirtualRouterVmMsg();
        msg.setNotGatewayForGuestL3Network(struct.isNotGatewayForGuestL3Network());
        msg.setL3Network(l3Nw);
        msg.setOffering(offering);
        msg.setInherentSystemTags(struct.getInherentSystemTags());
        msg.setProviderType(struct.getProviderType());
        msg.setApplianceVmType(struct.getApplianceVmType());
        msg.setApplianceVmAgentPort(struct.getApplianceVmAgentPort());
        bus.makeTargetServiceIdByResourceUuid(msg, VirtualRouterConstant.SERVICE_ID, l3Nw.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success(((CreateVirtualRouterVmReply) reply).getInventory());
                }
            }
        });
    }

    @Override
    public void acquireVirtualRouterVm(VirtualRouterStruct struct, final ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        //TODO: find a way to remove the GLock
        final GLock lock = new GLock(String.format("glock-vr-l3-%s", struct.getL3Network().getUuid()), TimeUnit.HOURS.toSeconds(1));
        lock.setSeparateThreadEnabled(false);
        lock.lock();
        acquireVirtualRouterVmInternal(struct, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(VirtualRouterVmInventory returnValue) {
                lock.unlock();
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                lock.unlock();
                completion.fail(errorCode);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public VirtualRouterVmInventory getVirtualRouterVm(L3NetworkInventory l3Nw) {
        String sql = "select vm from VirtualRouterVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("l3Uuid", l3Nw.getUuid());
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        List<VirtualRouterVmVO> vos = q.getResultList();
        if (vos.isEmpty()) {
            return null;
        }
        return VirtualRouterVmInventory.valueOf(vos.get(0));
    }

    @Override
    public boolean isVirtualRouterRunningForL3Network(String l3Uuid) {
        return countVirtualRouterRunningForL3Network(l3Uuid) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countVirtualRouterRunningForL3Network(String l3Uuid) {
        String sql = "select count(vm) from ApplianceVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and vm.state = :vmState and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("l3Uuid", l3Uuid);
        q.setParameter("vmState", VmInstanceState.Running);
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        return q.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVirtualRouterForL3Network(String l3Uuid) {
        String sql = "select vm from ApplianceVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("l3Uuid", l3Uuid);
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        Long count = q.getSingleResult();
        return count > 0;
    }

    @Transactional(readOnly = true)
    private List<VirtualRouterOfferingInventory> findOfferingByGuestL3Network(L3NetworkInventory guestL3) {
        String sql = "select offering from VirtualRouterOfferingVO offering, SystemTagVO stag where offering.uuid = stag.resourceUuid and stag.resourceType = :type and offering.zoneUuid = :zoneUuid and stag.tag = :tag";
        TypedQuery<VirtualRouterOfferingVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterOfferingVO.class);
        q.setParameter("type", InstanceOfferingVO.class.getSimpleName());
        q.setParameter("zoneUuid", guestL3.getZoneUuid());
        q.setParameter("tag", VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK.instantiateTag(map(e(VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK_TOKEN, guestL3.getUuid()))));
        List<VirtualRouterOfferingVO> vos = q.getResultList();
        if (!vos.isEmpty()) {
            return VirtualRouterOfferingInventory.valueOf1(vos);
        }

        sql ="select offering from VirtualRouterOfferingVO offering where offering.zoneUuid = :zoneUuid";
        q = dbf.getEntityManager().createQuery(sql, VirtualRouterOfferingVO.class);
        q.setParameter("zoneUuid", guestL3.getZoneUuid());
        vos = q.getResultList();
        return vos.isEmpty() ? null : VirtualRouterOfferingInventory.valueOf1(vos);
    }

    @Override
    public List<Flow> getPostCreateFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postCreateFlowsBuilder.getFlows());
        flows.addAll(postCreateFlowExtensionPoints.stream().map(VirtualRouterPostCreateFlowExtensionPoint::virtualRouterPostCreateFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public List<Flow> getPostStartFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postStartFlowsBuilder.getFlows());
        flows.addAll(postStartFlowExtensionPoints.stream().map(VirtualRouterPostStartFlowExtensionPoint::virtualRouterPostStartFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public List<Flow> getPostRebootFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postRebootFlowsBuilder.getFlows());
        flows.addAll(postRebootFlowExtensionPoints.stream().map(VirtualRouterPostRebootFlowExtensionPoint::virtualRouterPostRebootFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public List<Flow> getPostStopFlows() {
        return null;
    }

    @Override
    public List<Flow> getPostMigrateFlows() {
        return null;
    }

    @Override
    public List<Flow> getPostDestroyFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postDestroyFlowsBuilder.getFlows());
        flows.addAll(postDestroyFlowExtensionPoints.stream().map(VirtualRouterPostDestroyFlowExtensionPoint::virtualRouterPostDestroyFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public FlowChain getReconnectFlowChain() {
        FlowChain chain = reconnectFlowsBuilder.build();
        for (VirtualRouterPostReconnectFlowExtensionPoint ext : postReconnectFlowExtensionPoints) {
            chain.then(ext.virtualRouterPostReconnectFlow());
        }
        return chain;
    }

    @Override
    public int getParallelismDegree(String vrUuid) {
        Integer degree = vrParallelismDegrees.get(vrUuid);
        return degree == null ? VirtualRouterGlobalConfig.COMMANDS_PARALELLISM_DEGREE.value(Integer.class) : degree;
    }

    public void setVirtualRouterPostStartFlows(List<String> virtualRouterPostStartFlows) {
        this.virtualRouterPostStartFlows = virtualRouterPostStartFlows;
    }

    public void setVirtualRouterPostRebootFlows(List<String> virtualRouterPostRebootFlows) {
        this.virtualRouterPostRebootFlows = virtualRouterPostRebootFlows;
    }


    public void setVirtualRouterPostDestroyFlows(List<String> virtualRouterPostDestroyFlows) {
        this.virtualRouterPostDestroyFlows = virtualRouterPostDestroyFlows;
    }

    public void setVirtualRouterPostCreateFlows(List<String> virtualRouterPostCreateFlows) {
        this.virtualRouterPostCreateFlows = virtualRouterPostCreateFlows;
    }

    public void setVirtualRouterReconnectFlows(List<String> virtualRouterReconnectFlows) {
        this.virtualRouterReconnectFlows = virtualRouterReconnectFlows;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> classes = new ArrayList<Class>();
        classes.add(APIAttachNetworkServiceToL3NetworkMsg.class);
        return classes;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            validate((APIAttachNetworkServiceToL3NetworkMsg) msg);
        }
        return msg;
    }

    private void validate(APIAttachNetworkServiceToL3NetworkMsg msg) {
        List<String> services = msg.getNetworkServices().get(virtualRouterProvider.getUuid());
        if (services == null) {
            return;
        }

        boolean snat = false;
        boolean portForwarding = false;
        boolean eip = false;

        SimpleQuery<NetworkServiceL3NetworkRefVO> q = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        q.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        List<NetworkServiceL3NetworkRefVO> refs = q.list();
        for (NetworkServiceL3NetworkRefVO ref : refs) {
            if (ref.getNetworkServiceType().equals(NetworkServiceType.SNAT.toString())) {
                snat = true;
            }
        }

        for (String s : services) {
            if (NetworkServiceType.PortForwarding.toString().equals(s)) {
                portForwarding = true;
            }
            if (NetworkServiceType.SNAT.toString().equals(s)) {
                snat = true;
            }
            if (EipConstant.EIP_NETWORK_SERVICE_TYPE.equals(s)) {
                eip = true;
            }
        }

        if (!snat && eip) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("failed tot attach virtual router network services to l3Network[uuid:%s]. When eip is selected, snat must be selected too", msg.getL3NetworkUuid())
            ));
        }

        if (!snat && portForwarding) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("failed tot attach virtual router network services to l3Network[uuid:%s]. When port forwarding is selected, snat must be selected too", msg.getL3NetworkUuid())
            ));
        }
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterEipRef");
        struct.setExpandedInventoryKey("virtualRouterVmUuid");
        struct.setHidden(true);
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VirtualRouterEipRefInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterVipRef");
        struct.setExpandedInventoryKey("virtualRouterVmUuid");
        struct.setHidden(true);
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VirtualRouterVipInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterPortforwardingRef");
        struct.setExpandedInventoryKey("virtualRouterVmUuid");
        struct.setHidden(true);
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VirtualRouterPortForwardingRuleRefInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterOffering");
        struct.setExpandedInventoryKey("uuid");
        struct.setForeignKey("instanceOfferingUuid");
        struct.setInventoryClass(VirtualRouterOfferingInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        struct.setSuppressedInventoryClass(InstanceOfferingInventory.class);
        structs.add(struct);

        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        List<ExpandedQueryAliasStruct> aliases = new ArrayList<ExpandedQueryAliasStruct>();

        ExpandedQueryAliasStruct as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(ApplianceVmInventory.class);
        as.setAlias("eip");
        as.setExpandedField("virtualRouterEipRef.eip");
        aliases.add(as);

        as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(ApplianceVmInventory.class);
        as.setAlias("vip");
        as.setExpandedField("virtualRouterVipRef.vip");
        aliases.add(as);

        as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(ApplianceVmInventory.class);
        as.setAlias("portForwarding");
        as.setExpandedField("virtualRouterPortforwardingRef.portForwarding");
        aliases.add(as);
        return aliases;
    }
}
