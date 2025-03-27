package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class SecurityGroupCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(SecurityGroupCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static String NAME = SecurityGroupVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(SecurityGroupVO.class);
        completion.success();
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        List<SecurityGroupInventory> sgInv = securityGroupUuidsFromAction(action);
        if (sgInv == null || sgInv.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(sgInv).each((inv, wcomp) -> {
            SecurityGroupDeletionMsg msg = new SecurityGroupDeletionMsg();
            msg.setUuid(inv.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, inv.getUuid());
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to delete security group[uuid:%s], %s", msg.getUuid(), reply.getError()));
                    }

                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private List<SecurityGroupInventory> securityGroupUuidsFromAction(CascadeAction action) {
        List<SecurityGroupInventory> ret = null;
        if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccountInventory> accounts = action.getParentIssuerContext();
            List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

            List<String> sgUuids= Q.New(AccountResourceRefVO.class)
                    .select(AccountResourceRefVO_.resourceUuid)
                    .eq(AccountResourceRefVO_.resourceType, SecurityGroupVO.class.getSimpleName())
                    .in(AccountResourceRefVO_.ownerAccountUuid, accountUuids)
                    .listValues();
            if (sgUuids.isEmpty()) {
                return null;
            }

            List<SecurityGroupVO> sgVos = Q.New(SecurityGroupVO.class)
                    .in(SecurityGroupVO_.uuid, sgUuids).list();
            ret = SecurityGroupInventory.valueOf(sgVos);
        }
        return ret;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VmInstanceVO.class.getSimpleName(), AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<SecurityGroupInventory> sgInvs = securityGroupUuidsFromAction(action);
            if (sgInvs != null && !sgInvs.isEmpty()) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(sgInvs);
            }
            return action;
        }

        return null;
    }
}
