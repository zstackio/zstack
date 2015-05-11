package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicHostUuid;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicVmState;
import org.zstack.header.vm.VmTracerCanonicalEvents.HostChangedData;
import org.zstack.header.vm.VmTracerCanonicalEvents.StrangerVmFoundData;
import org.zstack.header.vm.VmTracerCanonicalEvents.VmStateChangedData;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public abstract class VmTracer {
    private static final CLogger logger = Utils.getLogger(VmTracer.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evtf;

    private class Tracer {
        String hostUuid;
        Map<String, VmInstanceState> hostSideStates;
        Map<String, VmInstanceState> mgmtSideStates;

        private void buildManagementServerSideVmStates() {
            mgmtSideStates = new HashMap<String, VmInstanceState>();
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.uuid, VmInstanceVO_.state);
            q.add(VmInstanceVO_.hostUuid, Op.EQ, hostUuid);
            List<Tuple> ts = q.listTuple();

            q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.uuid, VmInstanceVO_.state);
            q.add(VmInstanceVO_.hostUuid, Op.NULL);
            q.add(VmInstanceVO_.lastHostUuid, Op.EQ, hostUuid);
            ts.addAll(q.listTuple());

            for (Tuple t : ts) {
                mgmtSideStates.put(t.get(0, String.class), t.get(1, VmInstanceState.class));
            }
        }

        private void checkFromHostSide() {
            for (Map.Entry<String, VmInstanceState> e : hostSideStates.entrySet()) {
                String vmUuid = e.getKey();
                VmInstanceState actualState = e.getValue();

                VmInstanceState expectedState = mgmtSideStates.get(vmUuid);
                if (expectedState == null) {
                    // an anonymous vm showing on this host
                    handleAnonymousVm(vmUuid, actualState);
                } else if (actualState != expectedState) {
                    // vm state changed on host side
                    handleStateChangeOnHostSide(vmUuid, actualState, expectedState);
                }
            }
        }

        private void handleStateChangeOnHostSide(final String vmUuid, final VmInstanceState actualState, final VmInstanceState expectedState) {
            ChangeVmMetaDataMsg msg = new ChangeVmMetaDataMsg();
            AtomicVmState s = new AtomicVmState();
            s.setExpected(expectedState);
            s.setValue(actualState);
            msg.setState(s);
            msg.setVmInstanceUuid(vmUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg, new CloudBusCallBack() {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("[Vm Tracer] failed to change vm[uuid:%s] from state[%s] to state[%s]", vmUuid, expectedState, actualState));
                    } else {
                        ChangeVmMetaDataReply cr = reply.castReply();
                        if (cr.isChangeStateDone()) {
                            fireStateChangeEvent(vmUuid, expectedState, actualState);
                            logger.debug(String.format("[Vm Tracer] changed vm[uuid:%s] from state[%s] to state[%s]", vmUuid, expectedState, actualState));
                        }
                    }
                }
            });
        }

        private void fireStateChangeEvent(String vmUuid, VmInstanceState from, VmInstanceState to) {
            VmStateChangedData data = new VmStateChangedData();
            data.setVmUuid(vmUuid);
            data.setFrom(from);
            data.setTo(to);
            evtf.fire(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, data);
        }

        private void fireHostChangeEvent(String vmUuid, String from, String to) {
            HostChangedData data = new HostChangedData();
            data.setVmUuid(vmUuid);
            data.setFrom(from);
            data.setTo(to);
            evtf.fire(VmTracerCanonicalEvents.HOST_CHANGED_PATH, data);
        }

        private void handleAnonymousVm(final String vmUuid, final VmInstanceState actualState) {
            final VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            if (vm == null) {
                logger.debug(String.format("[Vm Tracer] detects stranger vm[identity:%s, state:%s]", vmUuid, actualState));
                StrangerVmFoundData data = new StrangerVmFoundData();
                data.setVmIdentity(vmUuid);
                data.setVmState(actualState);
                data.setHostUuid(hostUuid);
                evtf.fire(VmTracerCanonicalEvents.STRANGER_VM_FOUND_PATH, data);
                return;
            }

            ChangeVmMetaDataMsg msg = new ChangeVmMetaDataMsg();
            if (vm.getState() != actualState) {
                AtomicVmState s = new AtomicVmState();
                s.setExpected(vm.getState());
                s.setValue(actualState);
                msg.setState(s);
            }

            AtomicHostUuid h = new AtomicHostUuid();
            h.setExpected(vm.getHostUuid());
            h.setValue(hostUuid);
            msg.setHostUuid(h);
            msg.setVmInstanceUuid(vmUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.getUuid());
            bus.send(msg, new CloudBusCallBack() {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("[Vm Tracer] failed to change vm[uuid:%s] meta data, %s", vmUuid, reply.getError()));
                    } else {
                        ChangeVmMetaDataReply cr = reply.castReply();
                        if (cr.isChangeStateDone()) {
                            fireStateChangeEvent(vmUuid, vm.getState(), actualState);
                            logger.debug(String.format("[Vm Tracer] changed vm[uuid:%s] state from %s to %s", vmUuid, vm.getState(), actualState));
                        }
                        if (cr.isChangeHostUuidDone()) {
                            fireHostChangeEvent(vmUuid, null, hostUuid);
                            logger.debug(String.format("[Vm Tracer] vm[uuid:%s] show up on host[uuid:%s], from origin host[uuid:%s]", vmUuid, hostUuid, vm.getHostUuid()));
                        }
                    }
                }
            });
        }

        private void checkFromManagementServerSide() {
            // from mgmt server we only check missing vm, vm state change has been updated by host side check
            for (Map.Entry<String, VmInstanceState> e : mgmtSideStates.entrySet()) {
                String vmUuid = e.getKey();
                VmInstanceState expectedState = e.getValue();
                if (expectedState != VmInstanceState.Stopped && !hostSideStates.containsKey(vmUuid)) {
                    handleMissingVm(vmUuid, expectedState);
                }
            }
        }

        private void handleMissingVm(final String vmUuid, final VmInstanceState expectedState) {
            ChangeVmMetaDataMsg msg = new ChangeVmMetaDataMsg();

            AtomicHostUuid h = new AtomicHostUuid();
            AtomicVmState s = new AtomicVmState();
            s.setExpected(expectedState);
            s.setValue(VmInstanceState.Stopped);

            if (expectedState == VmInstanceState.Unknown) {
                SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
                q.select(VmInstanceVO_.hostUuid);
                q.add(VmInstanceVO_.uuid, Op.EQ, vmUuid);
                String huuid = q.findValue();

                h.setExpected(huuid);
                h.setValue(null);
            } else {
                if (expectedState == VmInstanceState.Created || expectedState == VmInstanceState.Starting) {
                    h.setExpected(null);
                    h.setValue(null);
                } else if (expectedState == VmInstanceState.Running || expectedState == VmInstanceState.Stopping
                        || expectedState == VmInstanceState.Migrating || expectedState == VmInstanceState.Rebooting) {
                    h.setExpected(hostUuid);
                    h.setValue(null);
                }

            }

            msg.setNeedHostAndStateBothMatch(true);
            msg.setState(s);
            msg.setHostUuid(h);
            msg.setVmInstanceUuid(vmUuid);

            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg, new CloudBusCallBack() {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("[Vm Tracer] failed to change vm[uuid:%s] meta data, %s", vmUuid, reply.getError()));
                    } else {
                        ChangeVmMetaDataReply cr = reply.castReply();
                        if (cr.isChangeStateDone()) {
                            fireStateChangeEvent(vmUuid, expectedState, VmInstanceState.Stopped);
                            logger.debug(String.format("[Vm Tracer] changed vm[uuid:%s] state from %s to %s", vmUuid, expectedState, VmInstanceState.Stopped));
                        }
                        if (cr.isChangeHostUuidDone()) {
                            fireHostChangeEvent(vmUuid, null, hostUuid);
                            logger.debug(String.format("[Vm Tracer] vm[uuid:%s] missing on host[uuid:%s]", vmUuid, hostUuid));
                        }
                    }
                }
            });
        }

        void trace() {
            buildManagementServerSideVmStates();
            checkFromHostSide();
            checkFromManagementServerSide();
        }
    }

    protected void reportVmState(final String hostUuid, final Map<String, VmInstanceState> vmStates) {
        for (VmInstanceState state : vmStates.values()) {
            if (state != VmInstanceState.Running && state != VmInstanceState.Unknown) {
                throw new CloudRuntimeException(String.format("host can only report vm state as Running and Unknown, got %s", state));
            }
        }

        if (!CoreGlobalProperty.VM_TRACER_ON) {
            logger.debug(String.format("vm tracer is off, skip reporting vm state on host[uuid:%s]", hostUuid));
            return;
        }

        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return String.format("trace-vm-state-on-host-%s", hostUuid);
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public String getName() {
                return String.format("trace-vm-state-on-host-%s", hostUuid);
            }

            @Override
            public Object call() throws Exception {
                Tracer t = new Tracer();
                t.hostUuid = hostUuid;
                t.hostSideStates = vmStates;
                t.trace();
                return null;
            }
        });
    }

}
