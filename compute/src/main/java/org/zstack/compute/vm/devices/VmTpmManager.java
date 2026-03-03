package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Objects;

public class VmTpmManager {
    private static final CLogger logger = Utils.getLogger(VmTpmManager.class);

    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    public TpmVO persistTpmVO(String tpmUuid, String vmUuid) {
        if (tpmUuid == null) {
            tpmUuid = Platform.getUuid();
        }
        TpmVO tpm = new TpmVO();
        tpm.setUuid(tpmUuid);
        tpm.setResourceName("TPM-for-VM-" + vmUuid);
        tpm.setVmInstanceUuid(vmUuid);
        databaseFacade.persistAndRefresh(tpm);

        logger.debug("Persisted TpmVO for VM " + vmUuid + " with uuid=" + tpm.getUuid());
        return tpm;
    }

    /**
     * @param bootMode boot mode, null is Legacy
     */
    public static boolean isUefiBootMode(String bootMode) {
        return Objects.equals(bootMode, ImageBootMode.UEFI.toString())
                || Objects.equals(bootMode, ImageBootMode.UEFI_WITH_CSM.toString());
    }
}
