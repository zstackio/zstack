package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.FixedSizeRequiredRequest;
import org.zstack.header.identity.quota.FunctionalSizeRequiredRequest;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.TaskContext;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;/**
 * Created by miao on 16-10-9.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaUtil {
    static final String QUOTA_OPERATOR_ACCOUNT_UUID = "quotaOperatorAccountUuid";
    public static final String QUOTA_CHECK_NAMES = "quotaCheckNames";
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

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAndCheckQuota(String accountUuid, String quotaName, long request) {
        Collection<?> quotaNames = (Collection<?>) TaskContext.getTaskContextItem(QUOTA_CHECK_NAMES);
        if (quotaNames == null || !quotaNames.contains(quotaName)) {
            return;
        }

        if (accountUuid == null || request == 0 || AccountConstant.isAdminPermission(accountUuid)) {
            return;
        }

        String operatorAccountUuid = (String) TaskContext.getTaskContextItem(QUOTA_OPERATOR_ACCOUNT_UUID);
        if (operatorAccountUuid != null && (AccountConstant.isAdminPermission(operatorAccountUuid)
                || isAdminAccount(operatorAccountUuid))) {
            return;
        }

        AccountVO account = dbf.getEntityManager().find(AccountVO.class, accountUuid, LockModeType.PESSIMISTIC_WRITE);
        if (account == null || account.getType() == AccountType.SystemAdmin) {
            return;
        }

        Quota.QuotaPair pair = makeQuotaPairs(accountUuid).get(quotaName);
        if (pair == null) {
            return;
        }

        Long used = acntMgr.getQuotasDefinitions().get(quotaName).getQuotaUsage(accountUuid);
        QuotaCompareInfo info = new QuotaCompareInfo();
        info.currentAccountUuid = operatorAccountUuid == null ? accountUuid : operatorAccountUuid;
        info.resourceTargetOwnerAccountUuid = accountUuid;
        info.quotaName = quotaName;
        info.quotaValue = pair.getValue();
        info.currentUsed = used == null ? 0L : used;
        info.request = request;
        ErrorCode error = checkQuotaAndReturn(info);
        if (error != null) {
            throw new OperationFailureException(error);
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

    @Transactional(readOnly = true)
    public void checkQuota(Message msg, String currentAccountUuid, String targetAccountUuid) {
        if (!(msg instanceof APIMessage) && !(msg instanceof NeedQuotaCheckMessage)) {
            return;
        }

        if (msg instanceof APIMessage) {
            TaskContext.putTaskContextItem(QUOTA_CHECK_NAMES, Collections.emptyList());
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
        List<String> checkedQuotaNames = new ArrayList<>();
        logger.trace("dump quota pairs of before handle" + targetAccountUuid + " :\n" + JSONObjectUtil.toJsonString(pairs));
        for (QuotaMessageHandler checker : handlers) {
            if (needSkipCheck(msg, checker)) {
                logger.trace(String.format("skip quota message[name: %s] check[name: %s]", msg.getClass(), checker.getClass()));
                continue;
            }

            logger.trace(String.format("start quota message[name: %s] check[name: %s]", msg.getClass(), checker.getClass()));
            for (Object request : checker.getFunctionalSizeRequiredRequests()) {
                if (checkFunctionalSizeRequest(msg, (FunctionalSizeRequiredRequest) request, currentAccountUuid, targetAccountUuid, pairs)) {
                    checkedQuotaNames.add(((FunctionalSizeRequiredRequest) request).getQuotaName());
                }
            }

            for (Object request : checker.getFixedSizeRequiredRequests()) {
                if (checkFixedSizeRequest((FixedSizeRequiredRequest) request, currentAccountUuid, targetAccountUuid, pairs)) {
                    checkedQuotaNames.add(((FixedSizeRequiredRequest) request).getQuotaName());
                }
            }
        }
        if (msg instanceof APIMessage) {
            TaskContext.putTaskContextItem(QUOTA_CHECK_NAMES, checkedQuotaNames);
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
    private boolean checkFunctionalSizeRequest(Message msg,
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
            return false;
        }

        Quota.QuotaPair pair = pairs.get(requiredRequest.getQuotaName());
        if (pair == null) {
            logger.warn(String.format("quota[name: %s] not defined for account[uuid: %s], skip check",
                    requiredRequest.getQuotaName(), targetAccountUuid));
            return false;
        }

        QuotaUtil.QuotaCompareInfo quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
        quotaCompareInfo.resourceTargetOwnerAccountUuid = targetAccountUuid;
        quotaCompareInfo.quotaName = requiredRequest.getQuotaName();
        quotaCompareInfo.quotaValue = pair.getValue();
        quotaCompareInfo.currentUsed = used;
        quotaCompareInfo.request = asked;
        CheckQuota(quotaCompareInfo);
        return true;
    }

    @Transactional(readOnly = true)
    private boolean checkFixedSizeRequest(FixedSizeRequiredRequest fixedSizeRequiredRequest, String currentAccountUuid, String targetAccountUuid, Map<String, Quota.QuotaPair> pairs) {
        Long asked = fixedSizeRequiredRequest.getValue();
        Long used = acntMgr.getQuotasDefinitions().get(fixedSizeRequiredRequest.getQuotaName()).getQuotaUsage(targetAccountUuid);
        if (asked == 0) {
            return false;
        }

        Quota.QuotaPair pair = pairs.get(fixedSizeRequiredRequest.getQuotaName());
        if (pair == null) {
            logger.warn(String.format("quota[name: %s] not defined for account[uuid: %s], skip check",
                    fixedSizeRequiredRequest.getQuotaName(), targetAccountUuid));
            return false;
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
        return true;
    }
}
