package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.storage.ceph.CephMonAO;
import org.zstack.storage.ceph.CephMonBase;
import org.zstack.storage.ceph.MonStatus;

import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class CephPrimaryStorageMonBase extends CephMonBase {

    @Autowired
    private DatabaseFacade dbf;

    public CephPrimaryStorageMonVO getSelf() {
        return (CephPrimaryStorageMonVO) self;
    }

    @Override
    public void connect(final Completion completion) {
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-mon-%s-ceph-primary-storage-%s", self.getHostname(), getSelf().getPrimaryStorageUuid()));
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (!CoreGlobalProperty.UNIT_TEST_ON) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "check-tools";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            checkTools();
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "deploy-agent";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "echo-agent";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        self.setStatus(MonStatus.Connected);
                        dbf.update(self);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    public CephPrimaryStorageMonBase(CephMonAO self) {
        super(self);
    }
}
