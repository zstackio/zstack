package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.*;
import org.zstack.header.simulator.APIAddSimulatorHostMsg;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.simulator.SimulatorHostVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.tag.SystemTagCreator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class SimulatorFactory implements HypervisorFactory, HostBaseExtensionFactory {
    private static final HypervisorType hypervisorType = new HypervisorType(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE, CoreGlobalProperty.EXPOSE_SIMULATOR_TYPE);
    public static final VolumeFormat SIMULATOR_VOLUME_FORMAT = new VolumeFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING, hypervisorType, CoreGlobalProperty.EXPOSE_SIMULATOR_TYPE);
    private Map<String, Host> hosts = Collections.synchronizedMap(new HashMap<String, Host>());

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public HostVO createHost(HostVO vo, AddHostMessage msg) {
        APIAddSimulatorHostMsg smsg = (APIAddSimulatorHostMsg) msg;

        SimulatorHostVO svo = new SimulatorHostVO(vo);
        svo.setMemoryCapacity(smsg.getMemoryCapacity());
        svo.setCpuCapacity(smsg.getCpuCapacity());
        svo.setUuid(vo.getUuid());
        dbf.persistAndRefresh(svo);

        SystemTagCreator creator = HostSystemTags.OS_DISTRIBUTION.newSystemTagCreator(vo.getUuid());
        creator.setTagByTokens(map(e(HostSystemTags.OS_DISTRIBUTION_TOKEN, "zstack")));
        creator.inherent = true;
        creator.create();

        creator = HostSystemTags.OS_RELEASE.newSystemTagCreator(vo.getUuid());
        creator.setTagByTokens(map(e(HostSystemTags.OS_RELEASE_TOKEN, "simulator")));
        creator.inherent = true;
        creator.create();

        creator = HostSystemTags.OS_VERSION.newSystemTagCreator(vo.getUuid());
        creator.setTagByTokens(map(e(HostSystemTags.OS_VERSION_TOKEN, "0.1")));
        creator.inherent = true;
        creator.create();

        return svo;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public HostInventory getHostInventory(HostVO vo) {
        return HostInventory.valueOf(vo);
    }

    @Override
    public HostInventory getHostInventory(String uuid) {
        HostVO vo = dbf.findByUuid(uuid, HostVO.class);
        return HostInventory.valueOf(vo);
    }

    @Override
    public Host getHost(HostVO vo) {
        Host host = hosts.get(vo.getUuid());
        if (host == null) {
            SimulatorHostVO svo = dbf.findByUuid(vo.getUuid(), SimulatorHostVO.class);
            host = new SimulatorHost(svo);
            hosts.put(vo.getUuid(), host);
        }
        return host;
    }

    @Override
    public List<Class> getMessageClasses() {
        return asList(CreateVmOnHypervisorMsg.class,
                StopVmOnHypervisorMsg.class,
                RebootVmOnHypervisorMsg.class,
                StartVmOnHypervisorMsg.class,
                MigrateVmOnHypervisorMsg.class,
                AttachVolumeToVmOnHypervisorMsg.class,
                DetachVolumeFromVmOnHypervisorMsg.class,
                DestroyVmOnHypervisorMsg.class);
    }
}
