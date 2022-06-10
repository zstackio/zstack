package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 */
public class KVMConnectExtensionForL2Network extends AbstractKVMConnectExtensionForL2Network implements KVMHostConnectExtensionPoint {
    @Autowired
    protected L2NetworkManager l2Mgr;
    protected static final CLogger logger = Utils.getLogger(KVMConnectExtensionForL2Network.class);

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-l2-network";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<L2NetworkInventory> l2s = getL2Networks(context.getInventory().getClusterUuid());

                if (l2s.isEmpty()) {
                    trigger.next();
                    return;
                }

                prepareNetwork(l2s, context.getInventory().getUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }
}
