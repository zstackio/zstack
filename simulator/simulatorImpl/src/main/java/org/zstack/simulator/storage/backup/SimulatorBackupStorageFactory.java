package org.zstack.simulator.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.backup.APIAddSimulatorBackupStorageMsg;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageConstant;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashSet;

public class SimulatorBackupStorageFactory implements BackupStorageFactory {
    private static final CLogger logger = Utils.getLogger(SimulatorBackupStorageFactory.class);
	private static final BackupStorageType type = BackupStorageType.createIfAbsent(
			SimulatorBackupStorageConstant.SIMULATOR_BACKUP_STORAGE_TYPE);
	
	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private CloudBus bus;
	
	static {
		HashSet<String> schemes = new HashSet<>();
		Collections.addAll(schemes,
			BackupStorageConstant.SCHEME_HTTP,
			BackupStorageConstant.SCHEME_HTTPS,
			BackupStorageConstant.SCHEME_NFS);
		type.setSupportedSchemes(schemes);
		
		type.setExposed(CoreGlobalProperty.EXPOSE_SIMULATOR_TYPE);
	}
	
	@Override
	public BackupStorageType getBackupStorageType() {
		return type;
	}

	@Override
	public BackupStorageInventory createBackupStorage(BackupStorageVO vo, APIAddBackupStorageMsg msg) {
	    APIAddSimulatorBackupStorageMsg smsg = (APIAddSimulatorBackupStorageMsg) msg;
		vo.setTotalCapacity(smsg.getTotalCapacity());
        vo.setAvailableCapacity(smsg.getAvailableCapacity());
		vo = dbf.persistAndRefresh(vo);
        return BackupStorageInventory.valueOf(vo);
	}

	@Override
	public BackupStorage getBackupStorage(BackupStorageVO vo) {
		return new SimulatorBackupStorage(vo);
	}

    @Override
    public BackupStorageInventory reload(String uuid) {
        BackupStorageVO vo = dbf.findByUuid(uuid, BackupStorageVO.class);
        return BackupStorageInventory.valueOf(vo);
    }

}
