package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.storage.primary.AllocatePrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/2/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageAllocateVolumeFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        FlowChain chain  = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("localstorage-create-volumes-for-vm-%s", spec.getVmInventory().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    @Transactional(readOnly = true)
                    private boolean isThereOtherStorageForTheHost(String localStorageUuid) {
                        String sql = "select count(pri) from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, HostVO host where pri.uuid = ref.primaryStorageUuid and ref.clusterUuid = host.clusterUuid and host.uuid = :huuid and pri.uuid != :puuid";
                        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                        q.setParameter("huuid", spec.getDestHost().getUuid());
                        q.setParameter("puuid", localStorageUuid);
                        return q.getSingleResult() > 0;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<AllocatePrimaryStorageMsg> msgs = new ArrayList<AllocatePrimaryStorageMsg>();

                        AllocateLocalStorageMsg rmsg = new AllocateLocalStorageMsg();
                        rmsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
                        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
                        rmsg.setHostUuid(spec.getDestHost().getUuid());
                        if (ImageMediaType.ISO.toString().equals(spec.getImageSpec().getInventory().getMediaType())) {
                            rmsg.setSize(spec.getRootDiskOffering().getDiskSize());
                            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
                        } else {
                            //TODO: find a way to allow specifying strategy for root disk
                            rmsg.setSize(spec.getImageSpec().getInventory().getSize());
                            rmsg.setHostUuid(spec.getDestHost().getUuid());
                        }
                        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);

                        msgs.add(rmsg);

                        if (!spec.getDataDiskOfferings().isEmpty()) {
                            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
                            q.select(LocalStorageHostRefVO_.primaryStorageUuid);
                            q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, spec.getDestHost().getUuid());
                            String priUuid = q.findValue();

                            boolean anyOtherStorage = isThereOtherStorageForTheHost(priUuid);

                            for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
                                AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                                amsg.setSize(dinv.getDiskSize());
                                amsg.setHostUuid(spec.getDestHost().getUuid());
                                if (anyOtherStorage) {
                                    amsg.setAllocationStrategy(dinv.getAllocatorStrategy());
                                    amsg.addVoidPrimaryStoratgeUuid(priUuid);
                                } else {
                                    amsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
                                }
                                amsg.setDiskOfferingUuid(dinv.getUuid());
                                bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                                msgs.add(amsg);
                            }
                        }
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {

                    }
                });
            }
        }).start();
    }

    @Override
    public void rollback(FlowTrigger trigger, Map data) {

    }
}
