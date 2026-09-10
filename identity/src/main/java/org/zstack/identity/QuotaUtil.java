package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.FixedSizeRequiredRequest;
import org.zstack.header.identity.quota.FunctionalSizeRequiredRequest;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;/**
 * Created by miao on 16-10-9.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaUtil {
    private static final CLogger logger = Utils.getLogger(QuotaUtil.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;

    public static class QuotaCompareInfo {
        public String currentAccountUuid;
        public String resourceTargetOwnerAccountUuid;
        public String quotaName;
        public long quotaValue;
        public long currentUsed;
        public long request;
    }

    @Transactional(readOnly = true)
    public String getResourceOwnerAccountUuid(String resourceUuid) {
        SimpleQuery<AccountResourceRefVO> q;
        q = dbf.createQuery(AccountResourceRefVO.class);
        q.select(AccountResourceRefVO_.ownerAccountUuid);
        q.add(AccountResourceRefVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        String owner = q.findValue();
        if (owner == null || owner.equals("")) {
            throw new CloudRuntimeException(
                    String.format("cannot find owner account uuid for resource[uuid:%s]", resourceUuid));
        } else {
            return owner;
        }
    }

    @Transactional(readOnly = true)
    public ErrorCode checkQuotaAndReturn(QuotaCompareInfo quotaCompareInfo) {
        logger.trace(String.format("dump quota QuotaCompareInfo: \n %s",
                JSONObjectUtil.toJsonString(quotaCompareInfo)));
        String accountName = Q.New(AccountVO.class)
                .select(AccountVO_.name)
                .eq(AccountVO_.uuid, quotaCompareInfo.resourceTargetOwnerAccountUuid)
                .findValue();
        if (quotaCompareInfo.currentUsed + quotaCompareInfo.request > quotaCompareInfo.quotaValue) {
            return err(ORG_ZSTACK_IDENTITY_10002, IdentityErrors.QUOTA_EXCEEDING,
                    "quota exceeding." +
                            "The resource owner(or target resource owner) account[uuid: %s name: %s] exceeds a quota[name: %s, value: %s], " +
                            "Current used:%s, Request:%s. Please contact the administrator.",
                    quotaCompareInfo.resourceTargetOwnerAccountUuid, StringUtils.trimToEmpty(accountName),
                    quotaCompareInfo.quotaName, quotaCompareInfo.quotaValue,
                    quotaCompareInfo.currentUsed, quotaCompareInfo.request
            );
        }

        return null;
    }

    public void CheckQuota(QuotaCompareInfo quotaCompareInfo) {
        ErrorCode error = checkQuotaAndReturn(quotaCompareInfo);
        if (error != null) {
            throw new ApiMessageInterceptionException(error);
        }
    }

    public Map<String, Quota.QuotaPair> makeQuotaPairs(String accountUuid) {
        SimpleQuery<QuotaVO> q = dbf.createQuery(QuotaVO.class);
        q.select(QuotaVO_.name, QuotaVO_.value);
        q.add(QuotaVO_.identityType, SimpleQuery.Op.EQ, AccountVO.class.getSimpleName());
        q.add(QuotaVO_.identityUuid, SimpleQuery.Op.EQ, accountUuid);
        List<Tuple> ts = q.listTuple();

        Map<String, Quota.QuotaPair> pairs = new HashMap<>();
        for (Tuple t : ts) {
            String name = t.get(0, String.class);
            long value = t.get(1, Long.class);
            Quota.QuotaPair p = new Quota.QuotaPair();
            p.setName(name);
            p.setValue(value);
            pairs.put(name, p);
        }

        return pairs;
    }

    public AccountType getAccountType(String accountUuid) {
        SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
        q.select(AccountVO_.type);
        q.add(AccountVO_.uuid, SimpleQuery.Op.EQ, accountUuid);
        return q.findValue();
    }

    public boolean isAdminAccount(String accountUuid) {
        return getAccountType(accountUuid) == AccountType.SystemAdmin;
    }

    public String getResourceType(String resourceUuid) {
        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        AccountResourceRefVO accResRefVO = q.find();
        return accResRefVO.getResourceType();
    }

    public ErrorCode buildQuataExceedError(String currentAccountUuid, String quotaName, long quotaValue){
        return err(ORG_ZSTACK_IDENTITY_10003, IdentityErrors.QUOTA_EXCEEDING,
                "quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]. Please contact the administrator.",
                currentAccountUuid, quotaName, quotaValue);
    }


    public ErrorCode buildQuataExceedError(String currentAccountUuid, String quotaName, long quotaValue, long currentUsed, long request){
        return err(ORG_ZSTACK_IDENTITY_10004, IdentityErrors.QUOTA_EXCEEDING,
                "quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]," +
                        " Current used:%s, Request:%s. Please contact the administrator.",
                currentAccountUuid, quotaName, quotaValue, currentUsed, request);
    }

    @Transactional(readOnly = true)
    public void checkQuota(APIMessage msg) {
        checkQuota(msg, msg.getSession().getAccountUuid(), msg.getSession().getAccountUuid());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveQuota(String accountUuid, String operationUuid, String resourceUuid, Map<String, Long> requests) {
        if (AccountConstant.isAdminPermission(accountUuid) || isAdminAccount(accountUuid)) {
            return;
        }
        if (requests.isEmpty()) {
            return;
        }

        GLock lock = lockQuotaReservation(accountUuid);
        try {
        Long existing = dbf.getEntityManager().createQuery(
                "select count(r) from QuotaReservationVO r where r.operationUuid = :operationUuid", Long.class)
                .setParameter("operationUuid", operationUuid)
                .getSingleResult();
        if (existing != null && existing > 0) {
            lock.unlock();
            return;
        }
        Map<String, Quota.QuotaPair> pairs = makeQuotaPairs(accountUuid);
        Map<String, Long> reservations = new HashMap<>();
        List<Tuple> tuples = dbf.getEntityManager().createQuery(
                "select r.quotaName, sum(r.amount) from QuotaReservationVO r where r.accountUuid = :accountUuid and r.quotaName in (:quotaNames) group by r.quotaName", Tuple.class)
                .setParameter("accountUuid", accountUuid)
                .setParameter("quotaNames", requests.keySet())
                .getResultList();
        for (Tuple tuple : tuples) {
            reservations.put(tuple.get(0, String.class), tuple.get(1, Long.class));
        }
        Map<String, Long> limitedRequests = new HashMap<>();
        for (Map.Entry<String, Long> request : requests.entrySet()) {
            Quota.QuotaPair pair = pairs.get(request.getKey());
            if (pair == null || pair.getValue() == Quota.DEFAULT_NO_LIMITATION) {
                continue;
            }
            Long used = acntMgr.getQuotasDefinitions().get(request.getKey()).getQuotaUsage(accountUuid);
            Long reserved = reservations.get(request.getKey());
            long total = (used == null ? 0 : used) + (reserved == null ? 0 : reserved);
            if (total + request.getValue() > pair.getValue()) {
                throw new ApiMessageInterceptionException(buildQuataExceedError(accountUuid, request.getKey(), pair.getValue(), total, request.getValue()));
            }
            limitedRequests.put(request.getKey(), request.getValue());
        }

        for (Map.Entry<String, Long> request : limitedRequests.entrySet()) {
            QuotaReservationVO reservation = new QuotaReservationVO();
            reservation.setAccountUuid(accountUuid);
            reservation.setQuotaName(request.getKey());
            reservation.setAmount(request.getValue());
            reservation.setOperationUuid(operationUuid);
            reservation.setResourceUuid(resourceUuid);
            dbf.persist(reservation);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
        } catch (Throwable t) {
            lock.unlock();
            throw t;
        }
    }

    private GLock lockQuotaReservation(String accountUuid) {
        GLock lock = new GLock(String.format("quota-reservation-%s", accountUuid), 120, dbf);
        lock.lock();
        return lock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseQuotaReservation(String operationUuid) {
        Query query = dbf.getEntityManager().createQuery("delete from QuotaReservationVO r where r.operationUuid = :operationUuid");
        query.setParameter("operationUuid", operationUuid);
        query.executeUpdate();
    }

    @Transactional
    public void releaseExpiredQuotaReservations() {
        List<Tuple> operations = dbf.getEntityManager().createQuery(
                "select r.operationUuid, min(r.createDate) from QuotaReservationVO r group by r.operationUuid", Tuple.class)
                .getResultList();
        if (operations.isEmpty()) {
            return;
        }

        Timestamp expiredDate = new Timestamp(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(IdentityGlobalConfig.QUOTA_RESERVATION_TIMEOUT.value(Long.class)));
        Set<String> operationUuids = new HashSet<>();
        for (Tuple operation : operations) {
            String operationUuid = operation.get(0, String.class);
            Timestamp createDate = operation.get(1, Timestamp.class);
            if (createDate.before(expiredDate)) {
                operationUuids.add(operationUuid);
            }
        }

        if (!operationUuids.isEmpty()) {
            Query query = dbf.getEntityManager().createQuery("delete from QuotaReservationVO r where r.operationUuid in (:operationUuids)");
            query.setParameter("operationUuids", operationUuids);
            query.executeUpdate();
        }
    }

    @Transactional(readOnly = true)
    public void checkQuota(Message msg, String currentAccountUuid, String targetAccountUuid) {
        if (!(msg instanceof APIMessage) && !(msg instanceof NeedQuotaCheckMessage)) {
            return;
        }

        if (AccountConstant.isAdminPermission(targetAccountUuid) || isAdminAccount(targetAccountUuid)) {
            return;
        }

        List<QuotaMessageHandler<? extends Message>> handlers = acntMgr.getQuotaMessageHandlerMap().entrySet().stream()
                .filter((entry) -> entry.getKey().isAssignableFrom(msg.getClass()))
                .map((Map.Entry::getValue))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (handlers.isEmpty()) {
            return;
        }

        Map<String, Quota.QuotaPair> pairs = makeQuotaPairs(targetAccountUuid);
        logger.trace("dump quota pairs of before handle" + targetAccountUuid + " :\n" + JSONObjectUtil.toJsonString(pairs));
        for (QuotaMessageHandler checker : handlers) {
            if (needSkipCheck(msg, checker)) {
                logger.trace(String.format("skip quota message[name: %s] check[name: %s]", msg.getClass(), checker.getClass()));
                continue;
            }

            logger.trace(String.format("start quota message[name: %s] check[name: %s]", msg.getClass(), checker.getClass()));
            for (Object request : checker.getFunctionalSizeRequiredRequests()) {
                checkFunctionalSizeRequest(msg, (FunctionalSizeRequiredRequest) request, currentAccountUuid, targetAccountUuid, pairs);
            }

            for (Object request : checker.getFixedSizeRequiredRequests()) {
                checkFixedSizeRequest((FixedSizeRequiredRequest) request, currentAccountUuid, targetAccountUuid, pairs);
            }
        }
    }

    private boolean needSkipCheck(Message msg, QuotaMessageHandler checker) {
        for (Object condition : checker.getConditions()) {
            Boolean result = (Boolean) ((Function) condition).call(msg);

            // if condition not match return skip
            if (result == Boolean.FALSE) {
                return true;
            }
        }

        return false;
    }

    @Transactional(readOnly = true)
    private void checkFunctionalSizeRequest(Message msg,
                                            FunctionalSizeRequiredRequest requiredRequest,
                                            String currentAccountUuid,
                                            String targetAccountUuid,
                                            Map<String, Quota.QuotaPair> pairs) {
        Long asked = (Long) requiredRequest.getFunction().call(msg);
        Long used = acntMgr.getQuotasDefinitions().get(requiredRequest.getQuotaName()).getQuotaUsage(targetAccountUuid);

        if (used == null) {
            used = 0L;
        }

        if (asked == null || asked == 0) {
            logger.trace("skip quota check because no required");
            return;
        }

        Quota.QuotaPair pair = pairs.get(requiredRequest.getQuotaName());
        if (pair == null) {
            logger.warn(String.format("quota[name: %s] not defined for account[uuid: %s], skip check",
                    requiredRequest.getQuotaName(), targetAccountUuid));
            return;
        }

        QuotaUtil.QuotaCompareInfo quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
        quotaCompareInfo.resourceTargetOwnerAccountUuid = targetAccountUuid;
        quotaCompareInfo.quotaName = requiredRequest.getQuotaName();
        quotaCompareInfo.quotaValue = pair.getValue();
        quotaCompareInfo.currentUsed = used;
        quotaCompareInfo.request = asked;
        CheckQuota(quotaCompareInfo);
    }

    @Transactional(readOnly = true)
    private void checkFixedSizeRequest(FixedSizeRequiredRequest fixedSizeRequiredRequest, String currentAccountUuid, String targetAccountUuid, Map<String, Quota.QuotaPair> pairs) {
        Long asked = fixedSizeRequiredRequest.getValue();
        Long used = acntMgr.getQuotasDefinitions().get(fixedSizeRequiredRequest.getQuotaName()).getQuotaUsage(targetAccountUuid);
        if (asked == 0) {
            return;
        }

        Quota.QuotaPair pair = pairs.get(fixedSizeRequiredRequest.getQuotaName());
        if (pair == null) {
            logger.warn(String.format("quota[name: %s] not defined for account[uuid: %s], skip check",
                    fixedSizeRequiredRequest.getQuotaName(), targetAccountUuid));
            return;
        }

        QuotaUtil.QuotaCompareInfo quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
        quotaCompareInfo.resourceTargetOwnerAccountUuid = targetAccountUuid;
        quotaCompareInfo.quotaName = fixedSizeRequiredRequest.getQuotaName();
        logger.debug("get quota pair of " + fixedSizeRequiredRequest.getQuotaName() + ":\n" + JSONObjectUtil.toJsonString(pair));
        quotaCompareInfo.quotaValue = pair.getValue();
        quotaCompareInfo.currentUsed = used;
        quotaCompareInfo.request = asked;
        CheckQuota(quotaCompareInfo);
    }
}
