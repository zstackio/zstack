package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
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
import javax.persistence.TypedQuery;
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

        @Transactional(readOnly = true)
        private void buildManagementServerSideVmStates() {
            mgmtSideStates = new HashMap<String, VmInstanceState>();

            String sql = "select vm.uuid, vm.state from VmInstanceVO vm where vm.hostUuid = :huuid or (vm.hostUuid is null and vm.lastHostUuid = :huuid)";
            TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
            q.setParameter("huuid", hostUuid);
            List<Tuple> ts = q.getResultList();

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
                    handleStateChangeOnHostSide(vmUuid, actualState);
                }
            }
        }

        private void handleStateChangeOnHostSide(final String vmUuid, final VmInstanceState actualState) {
            VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
            msg.setVmInstanceUuid(vmUuid);
            msg.setStateOnHost(actualState);
            msg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg);
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

            handleStateChangeOnHostSide(vmUuid, actualState);
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
            VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
            msg.setHostUuid(hostUuid);
            msg.setVmInstanceUuid(vmUuid);
            msg.setStateOnHost(VmInstanceState.Stopped);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg);
        }

        void trace() {
            buildManagementServerSideVmStates();
            checkFromHostSide();
            checkFromManagementServerSide();
        }
    }

    protected void reportVmState(final String hostUuid, final Map<String, VmInstanceState> vmStates) {
        for (VmInstanceState state : vmStates.values()) {
            if (state != VmInstanceState.Running && state != VmInstanceState.Stopped) {
                throw new CloudRuntimeException(String.format("host can only report vm state as Running or Stopped, got %s", state));
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
