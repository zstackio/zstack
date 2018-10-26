package org.zstack.compute.vm;

import org.apache.commons.collections.map.HashedMap;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.Map;

/**
 * Created by kayo on 2018/10/25.
 */
public class VmCapabilities {
    private boolean supportLiveMigration = true;
    private boolean supportVolumeMigration = true;
    private boolean supportReimage= true;

    public boolean isSupportLiveMigration() {
        return supportLiveMigration;
    }

    public void setSupportLiveMigration(boolean supportLiveMigration) {
        this.supportLiveMigration = supportLiveMigration;
    }

    public boolean isSupportVolumeMigration() {
        return supportVolumeMigration;
    }

    public void setSupportVolumeMigration(boolean supportVolumeMigration) {
        this.supportVolumeMigration = supportVolumeMigration;
    }

    public boolean isSupportReimage() {
        return supportReimage;
    }

    public void setSupportReimage(boolean supportReimage) {
        this.supportReimage = supportReimage;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashedMap();
        map.put(VmInstanceConstant.Capability.LiveMigration.toString(), isSupportLiveMigration());
        map.put(VmInstanceConstant.Capability.VolumeMigration.toString(), isSupportVolumeMigration());
        map.put(VmInstanceConstant.Capability.Reimage.toString(), isSupportReimage());

        return map;
    }
}
