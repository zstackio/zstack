package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.logging.Event;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmStateChangedOnHostMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

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

            String sql = "select vm.uuid, vm.state from VmInstanceVO vm where vm.hostUuid = :huuid or (vm.hostUuid is null and vm.lastHostUuid = :huuid)" +
                    " and vm.state not in (:vmstates)";
            TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
            q.setParameter("huuid", hostUuid);
            q.setParameter("vmstates", list(VmInstanceState.Destroyed, VmInstanceState.Destroying));
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
                    handleAnonymousVm(vmUuid, actualState, expectedState);
                } else if (actualState != expectedState) {
                    // vm state changed on host side
                    handleStateChangeOnHostSide(vmUuid, actualState, expectedState);
                }
            }
        }

        private void handleStateChangeOnHostSide(final String vmUuid, final VmInstanceState actualState, VmInstanceState expected) {
            VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
            msg.setVmStateAtTracingMoment(expected);
            msg.setVmInstanceUuid(vmUuid);
            msg.setStateOnHost(actualState);
            msg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg);
        }

        private void handleAnonymousVm(final String vmUuid, final VmInstanceState actualState, VmInstanceState expected) {
            final VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            if (vm == null) {
                new Event().log(VmLabels.STRANGER_VM, hostUuid, vmUuid);
                /*
                logger.debug(String.format("[Vm Tracer] detects stranger vm[identity:%s, state:%s]", vmUuid, actualState));
                StrangerVmFoundData data = new StrangerVmFoundData();
                data.setVmIdentity(vmUuid);
                data.setVmState(actualState);
                data.setHostUuid(hostUuid);
                evtf.fire(VmTracerCanonicalEvents.STRANGER_VM_FOUND_PATH, data);
                */
                return;
            }

            handleStateChangeOnHostSide(vmUuid, actualState, expected);
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
            msg.setVmStateAtTracingMoment(expectedState);
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
            if (state != VmInstanceState.Running && state != VmInstanceState.Stopped && state != VmInstanceState.Paused) {
                throw new CloudRuntimeException(String.format("host can only report vm state as Running, Stopped or Paused, got %s", state));
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
