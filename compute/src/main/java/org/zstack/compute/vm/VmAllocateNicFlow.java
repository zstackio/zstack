package org.zstack.compute.vm;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.orm.jpa.JpaSystemException;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.taskProgress;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateNicFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        taskProgress("create nics");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        // it's unlikely a vm having more than 512 nics
        final BitSet deviceIdBitmap = new BitSet(512);
        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
            deviceIdBitmap.set(nic.getDeviceId());
        }

        List<ErrorCode> errs = new ArrayList<>();
        Map<String, String> vmStaticIps = new StaticIpOperator().getStaticIpbyVmUuid(spec.getVmInventory().getUuid());
        List<L3NetworkInventory> firstL3s = VmNicSpec.getFirstL3NetworkInventoryOfSpec(spec.getL3Networks());
        new While<>(firstL3s).each((nw, wcomp) -> {
            int deviceId = deviceIdBitmap.nextClearBit(0);
            deviceIdBitmap.set(deviceId);
            MacOperator mo = new MacOperator();
            String customMac = mo.getMac(spec.getVmInventory().getUuid(), nw.getUuid());
            if (customMac != null){
                mo.deleteCustomMacSystemTag(spec.getVmInventory().getUuid(), nw.getUuid(), customMac);
                customMac = customMac.toLowerCase();
            } else {
                customMac = NetworkUtils.generateMacWithDeviceId((short) deviceId);
            }
            final String mac = customMac;

            AllocateIpMsg msg = new AllocateIpMsg();
            msg.setL3NetworkUuid(nw.getUuid());
            msg.setAllocateStrategy(spec.getIpAllocatorStrategy());
            String staticIp = vmStaticIps.get(nw.getUuid());
            if (staticIp != null) {
                msg.setRequiredIp(staticIp);
            } else {
                l3nm.updateIpAllocationMsg(msg, customMac);
            }
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nw.getUuid());
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        AllocateIpReply areply = reply.castReply();
                        VmNicInventory nic = new VmNicInventory();
                        nic.setUuid(Platform.getUuid());
                        nic.setIp(areply.getIpInventory().getIp());
                        nic.setIpVersion(areply.getIpInventory().getIpVersion());
                        nic.setUsedIpUuid(areply.getIpInventory().getUuid());
                        nic.setVmInstanceUuid(spec.getVmInventory().getUuid());
                        nic.setL3NetworkUuid(areply.getIpInventory().getL3NetworkUuid());
                        nic.setMac(mac);
                        nic.setHypervisorType(spec.getDestHost().getHypervisorType());
                        if (mo.checkDuplicateMac(nic.getHypervisorType(), nic.getMac())) {
                            trigger.fail(operr("Duplicate mac address [%s]", nic.getMac()));
                            return;
                        }

                        assert nic.getL3NetworkUuid() != null;

                        nic.setDeviceId(deviceId);
                        nic.setNetmask(areply.getIpInventory().getNetmask());
                        nic.setGateway(areply.getIpInventory().getGateway());
                        nic.setInternalName(VmNicVO.generateNicInternalName(spec.getVmInventory().getInternalId(), nic.getDeviceId()));

                        new SQLBatch() {
                            private VmNicVO persistAndRetryIfMacCollision(VmNicVO vo) {
                                int tries = 5;
                                while (tries-- > 0) {
                                    try {
                                        persist(vo);
                                        return reload(vo);
                                    } catch (JpaSystemException e) {
                                        if (e.getRootCause() instanceof MySQLIntegrityConstraintViolationException &&
                                                e.getRootCause().getMessage().contains("Duplicate entry")) {
                                            logger.debug(String.format("Concurrent mac allocation. Mac[%s] has been allocated, try allocating another one. " +
                                                    "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                                                    "we will try finding another mac", vo.getMac()));
                                            logger.trace("", e);
                                            vo.setMac(NetworkUtils.generateMacWithDeviceId((short) vo.getDeviceId()));
                                        } else {
                                            throw e;
                                        }
                                    }
                                }
                                return null;
                            }

                            @Override
                            protected void scripts() {
                                String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());

                                VmNicVO vo = new VmNicVO();
                                vo.setUuid(nic.getUuid());
                                vo.setIp(nic.getIp());
                                vo.setL3NetworkUuid(nic.getL3NetworkUuid());
                                vo.setUsedIpUuid(nic.getUsedIpUuid());
                                vo.setVmInstanceUuid(nic.getVmInstanceUuid());
                                vo.setDeviceId(nic.getDeviceId());
                                vo.setMac(nic.getMac());
                                vo.setHypervisorType(nic.getHypervisorType());
                                vo.setNetmask(nic.getNetmask());
                                vo.setGateway(nic.getGateway());
                                vo.setIpVersion(nic.getIpVersion());
                                vo.setInternalName(nic.getInternalName());
                                vo.setAccountUuid(acntUuid);
                                vo = persistAndRetryIfMacCollision(vo);
                                if (vo == null) {
                                    throw new FlowException(errf.instantiateErrorCode(VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
                                }
                                /* update usedIpVo */
                                SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, vo.getUsedIpUuid()).set(UsedIpVO_.vmNicUuid, nic.getUuid()).update();

                                spec.getDestNics().add(nic);
                            }
                        }.execute();
                        wcomp.done();
                    } else {
                        errs.add(reply.getError());
                        wcomp.allDone();
                    }
                }
            });
        }).run(new NoErrorCompletion() {
            @Override
            public void done() {
                if (errs.size() > 0) {
                    trigger.fail(errs.get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> destNics = spec.getDestNics();
        if (destNics.isEmpty()) {
            chain.rollback();
            return;
        }

        List<ReturnIpMsg> msgs = new ArrayList<ReturnIpMsg>();
        final List<String> nicUuids = new ArrayList<String>();
        for (VmNicInventory nic : destNics) {
            VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
            for (UsedIpVO ip: nicVO.getUsedIps()) {
                ReturnIpMsg msg = new ReturnIpMsg();
                msg.setL3NetworkUuid(ip.getL3NetworkUuid());
                msg.setUsedIpUuid(ip.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                msgs.add(msg);
            }

            nicUuids.add(nic.getUuid());
        }

        bus.send(msgs, 1, new CloudBusListCallBack(chain) {
            @Override
            public void run(List<MessageReply> replies) {
                dbf.removeByPrimaryKeys(nicUuids, VmNicVO.class);
                chain.rollback();
            }
        });
    }
}
