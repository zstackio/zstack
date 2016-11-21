package org.zstack.network.l3;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

import static org.zstack.utils.CollectionDSL.list;

public class L3NetworkManagerImpl extends AbstractService implements L3NetworkManager, ReportQuotaExtensionPoint,
        ResourceOwnerPreChangeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(L3NetworkManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    private Map<String, L3NetworkFactory> l3NetworkFactories = Collections.synchronizedMap(new HashMap<String, L3NetworkFactory>());
    private Map<String, IpAllocatorStrategy> ipAllocatorStrategies = Collections.synchronizedMap(new HashMap<String, IpAllocatorStrategy>());

    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(L3NetworkDeletionMsg.class);
    }

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
        if (msg instanceof L3NetworkMessage) {
            passThrough((L3NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateL3NetworkMsg) {
            handle((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APIListL3NetworkMsg) {
            handle((APIListL3NetworkMsg) msg);
        } else if (msg instanceof L3NetworkMessage) {
            passThrough((L3NetworkMessage) msg);
        } else if (msg instanceof APIListIpRangeMsg) {
            handle((APIListIpRangeMsg) msg);
        } else if (msg instanceof APISearchL3NetworkMsg) {
            handle((APISearchL3NetworkMsg) msg);
        } else if (msg instanceof APIGetL3NetworkMsg) {
            handle((APIGetL3NetworkMsg) msg);
        } else if (msg instanceof APIGetL3NetworkTypesMsg) {
            handle((APIGetL3NetworkTypesMsg) msg);
        } else if (msg instanceof APIGetIpAddressCapacityMsg) {
            handle((APIGetIpAddressCapacityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIGetIpAddressCapacityMsg msg) {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();

        class IpCapacity {
            long total;
            long avail;
        }

        IpCapacity ret = new Callable<IpCapacity>() {
            private long calcTotalIp(List<Tuple> ts) {
                long total = 0;
                for (Tuple t : ts) {
                    String sip = t.get(0, String.class);
                    String eip = t.get(1, String.class);
                    total += NetworkUtils.getTotalIpInRange(sip, eip);
                }

                return total;
            }

            @Override
            @Transactional(readOnly = true)
            public IpCapacity call() {
                IpCapacity ret = new IpCapacity();

                if (msg.getIpRangeUuids() != null && !msg.getIpRangeUuids().isEmpty()) {
                    String sql = "select ipr.startIp, ipr.endIp from IpRangeVO ipr where ipr.uuid in (:uuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", msg.getIpRangeUuids());
                    List<Tuple> ts = q.getResultList();
                    ret.total = calcTotalIp(ts);

                    sql = "select count(uip) from UsedIpVO uip where uip.ipRangeUuid in (:uuids)";
                    TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
                    cq.setParameter("uuids", msg.getIpRangeUuids());
                    Long used = cq.getSingleResult();
                    ret.avail = ret.total - used;
                    return ret;
                } else if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
                    String sql = "select ipr.startIp, ipr.endIp from IpRangeVO ipr, L3NetworkVO l3 where ipr.l3NetworkUuid = l3.uuid and l3.uuid in (:uuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", msg.getL3NetworkUuids());
                    List<Tuple> ts = q.getResultList();
                    ret.total = calcTotalIp(ts);

                    sql = "select count(uip) from UsedIpVO uip where uip.l3NetworkUuid in (:uuids)";
                    TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
                    cq.setParameter("uuids", msg.getL3NetworkUuids());
                    Long used = cq.getSingleResult();
                    ret.avail = ret.total - used;
                    return ret;
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select ipr.startIp, ipr.endIp from IpRangeVO ipr, L3NetworkVO l3, ZoneVO zone where ipr.l3NetworkUuid = l3.uuid and l3.zoneUuid = zone.uuid and zone.uuid in (:uuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", msg.getZoneUuids());
                    List<Tuple> ts = q.getResultList();
                    ret.total = calcTotalIp(ts);

                    sql = "select count(uip) from UsedIpVO uip, L3NetworkVO l3, ZoneVO zone where uip.l3NetworkUuid = l3.uuid and l3.zoneUuid = zone.uuid and zone.uuid in (:uuids)";
                    TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
                    cq.setParameter("uuids", msg.getZoneUuids());
                    Long used = cq.getSingleResult();
                    ret.avail = ret.total - used;
                    return ret;
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        reply.setTotalCapacity(ret.total);
        reply.setAvailableCapacity(ret.avail);
        bus.reply(msg, reply);
    }

    private void handle(APIGetL3NetworkTypesMsg msg) {
        APIGetL3NetworkTypesReply reply = new APIGetL3NetworkTypesReply();
        List<String> lst = new ArrayList<String>();
        lst.addAll(L3NetworkType.getAllTypeNames());
        reply.setL3NetworkTypes(lst);
        bus.reply(msg, reply);
    }

    private void handle(APIGetL3NetworkMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, L3NetworkInventory.class);
        APIGetL3NetworkReply reply = new APIGetL3NetworkReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchL3NetworkMsg msg) {
        SearchQuery<L3NetworkInventory> sq = SearchQuery.create(msg, L3NetworkInventory.class);
        String content = sq.listAsString();
        APISearchL3NetworkReply reply = new APISearchL3NetworkReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APIListIpRangeMsg msg) {
        List<IpRangeVO> vos = dl.listByApiMessage(msg, IpRangeVO.class);
        List<IpRangeInventory> invs = IpRangeInventory.valueOf(vos);
        APIListIpRangeReply reply = new APIListIpRangeReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void passThrough(String l3NetworkUuid, Message msg) {
        L3NetworkVO vo = dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            L3NetworkEO eo = dbf.findByUuid(l3NetworkUuid, L3NetworkEO.class);
            vo = ObjectUtils.newAndCopy(eo, L3NetworkVO.class);
        }

        if (vo == null) {
            ErrorCode err = errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("Unable to find L3Network[uuid:%s], it may have been deleted", l3NetworkUuid));
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        L3NetworkFactory factory = getL3NetworkFactory(L3NetworkType.valueOf(vo.getType()));
        L3Network nw = factory.getL3Network(vo);
        nw.handleMessage(msg);
    }

    private void passThrough(L3NetworkMessage msg) {
        passThrough(msg.getL3NetworkUuid(), (Message) msg);
    }

    private void handle(APIListL3NetworkMsg msg) {
        List<L3NetworkVO> vos = dl.listByApiMessage(msg, L3NetworkVO.class);
        List<L3NetworkInventory> invs = L3NetworkInventory.valueOf(vos);
        APIListL3NetworkReply reply = new APIListL3NetworkReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APICreateL3NetworkMsg msg) {
        SimpleQuery<L2NetworkVO> query = dbf.createQuery(L2NetworkVO.class);
        query.select(L2NetworkVO_.zoneUuid);
        query.add(L2NetworkVO_.uuid, Op.EQ, msg.getL2NetworkUuid());
        String zoneUuid = query.findValue();
        assert zoneUuid != null;

        L3NetworkVO vo = new L3NetworkVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setDnsDomain(msg.getDnsDomain());
        vo.setL2NetworkUuid(msg.getL2NetworkUuid());
        vo.setName(msg.getName());
        vo.setSystem(msg.isSystem());
        vo.setZoneUuid(zoneUuid);
        vo.setState(L3NetworkState.Enabled);


        L3NetworkFactory factory = getL3NetworkFactory(L3NetworkType.valueOf(msg.getType()));
        L3NetworkInventory inv = factory.createL3Network(vo, msg);

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), L3NetworkVO.class);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), L3NetworkVO.class.getSimpleName());

        APICreateL3NetworkEvent evt = new APICreateL3NetworkEvent(msg.getId());
        evt.setInventory(inv);
        logger.debug(String.format("Successfully created L3Network[name:%s, uuid:%s]", inv.getName(), inv.getUuid()));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        for (L3NetworkFactory f : pluginRgty.getExtensionList(L3NetworkFactory.class)) {
            L3NetworkFactory old = l3NetworkFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate L3NetworkFactory[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getType()));
            }
            l3NetworkFactories.put(f.getType().toString(), f);
        }

        for (IpAllocatorStrategy f : pluginRgty.getExtensionList(IpAllocatorStrategy.class)) {
            IpAllocatorStrategy old = ipAllocatorStrategies.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate IpAllocatorStrategy[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getType()));
            }
            ipAllocatorStrategies.put(f.getType().toString(), f);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public L3NetworkFactory getL3NetworkFactory(L3NetworkType type) {
        L3NetworkFactory factory = l3NetworkFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find L3NetworkFactory for type(%s)", type));
        }

        return factory;
    }

    @Override
    public IpAllocatorStrategy getIpAllocatorStrategy(IpAllocatorType type) {
        IpAllocatorStrategy factory = ipAllocatorStrategies.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find IpAllocatorStrategy for type(%s)", type));
        }

        return factory;
    }

    @Override
    public UsedIpInventory reserveIp(IpRangeInventory ipRange, String ip) {
        try {
            UsedIpVO vo = new UsedIpVO(ipRange.getUuid(), ip);
            vo.setIpInLong(NetworkUtils.ipv4StringToLong(ip));
            String uuid = ipRange.getUuid() + ip;
            uuid = UUID.nameUUIDFromBytes(uuid.getBytes()).toString().replaceAll("-", "");
            vo.setUuid(uuid);
            vo.setL3NetworkUuid(ipRange.getL3NetworkUuid());
            vo.setNetmask(ipRange.getNetmask());
            vo.setGateway(ipRange.getGateway());
            vo = dbf.persistAndRefresh(vo);
            return UsedIpInventory.valueOf(vo);
        } catch (JpaSystemException e) {
            if (e.getRootCause() instanceof MySQLIntegrityConstraintViolationException) {
                logger.debug(String.format("Concurrent ip allocation. " +
                        "Ip[%s] in ip range[uuid:%s] has been allocated, try allocating another one. " +
                        "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                        "we will try finding another ip", ip, ipRange.getUuid()));
                logger.trace("", e);
            } else {
                throw e;
            }
        }

        return null;
    }

    @Override
    public boolean isIpRangeFull(IpRangeVO vo) {
        int total = NetworkUtils.getTotalIpInRange(vo.getStartIp(), vo.getEndIp());
        SimpleQuery<UsedIpVO> query = dbf.createQuery(UsedIpVO.class);
        query.add(UsedIpVO_.ipRangeUuid, Op.EQ, vo.getUuid());
        long used = query.count();
        return used >= total;
    }

    @Override
    public List<Long> getUsedIpInRange(String ipRangeUuid) {
        SimpleQuery<UsedIpVO> query = dbf.createQuery(UsedIpVO.class);
        query.select(UsedIpVO_.ipInLong);
        query.add(UsedIpVO_.ipRangeUuid, Op.EQ, ipRangeUuid);
        List<Long> used = query.listValue();
        Collections.sort(used);
        return used;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateL3NetworkMsg) {
                        check((APICreateL3NetworkMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(L3NetworkConstant.QUOTA_L3_NUM);
                usage.setUsed(getUsedL3(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedL3(String accountUuid) {
                String sql = "select count(l3) from L3NetworkVO l3, AccountResourceRefVO ref where l3.uuid = ref.resourceUuid and " +
                        "ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", L3NetworkVO.class.getSimpleName());
                Long l3n = q.getSingleResult();
                l3n = l3n == null ? 0 : l3n;
                return l3n;
            }

            private void check(APICreateL3NetworkMsg msg, Map<String, QuotaPair> pairs) {
                long l3Num = pairs.get(L3NetworkConstant.QUOTA_L3_NUM).getValue();
                long l3n = getUsedL3(msg.getSession().getAccountUuid());

                if (l3n + 1 > l3Num) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding.  The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), L3NetworkConstant.QUOTA_L3_NUM, l3Num)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        quota.setOperator(checker);
        quota.addMessageNeedValidation(APICreateL3NetworkMsg.class);

        QuotaPair p = new QuotaPair();
        p.setName(L3NetworkConstant.QUOTA_L3_NUM);
        p.setValue(20);
        quota.addPair(p);

        return list(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid) {
    }

}
