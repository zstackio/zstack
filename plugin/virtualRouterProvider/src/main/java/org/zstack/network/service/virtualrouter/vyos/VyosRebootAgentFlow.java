package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterMetadataOperator;
import org.zstack.network.service.virtualrouter.VirtualRouterMetadataVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosRebootAgentFlow extends VyosRunScriptFlow {
    private static final CLogger logger = Utils.getLogger(VyosRebootAgentFlow.class);

    @Override
    public void initEnv() {
        setLogger(Utils.getLogger(VyosRebootAgentFlow.class));
    }

    @Override
    public boolean isSkipRunningScript(Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        boolean needRebootAgent = Boolean.parseBoolean((String) data.getOrDefault(ApplianceVmConstant.Params.needRebootAgent.toString(), "false"));
        // no need to reboot agent
        return !needRebootAgent;
    }

    @Override
    public void createScript() {
        String script = "sudo bash /home/vyos/zvrboot.bin\n" +
                "sudo bash /home/vyos/zvr.bin\n" +
                "sudo bash /etc/init.d/zstack-virtualrouteragent restart\n";
        super.script(script);
    }

    @Override
    public String getTaskName() {
        return VyosRebootAgentFlow.class.getName();
    }

    @Override
    public String getScriptName() {
        return "vyos reboot";
    }

    @Override
    public void afterExecuteScript() {
        if (getVrUuid() != null) {
            updateZvrVersion(getVrUuid());
        }
    }


    private void updateZvrVersion(String vrUuid) {
        VirtualRouterMetadataVO vo = dbf.findByUuid(vrUuid, VirtualRouterMetadataVO.class);
        String managementVersion = getManagementVersion();
        if (managementVersion == null) {
            return;
        }

        if (vo != null) {
            vo.setZvrVersion(managementVersion);
            dbf.update(vo);
        } else {
            vo = new VirtualRouterMetadataVO();
            vo.setUuid(vrUuid);
            dbf.persist(vo);
        }
    }

    private String getManagementVersion() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "3.10.0.0";
        }

        String managementVersion;
        String path;
        try {
            path = PathUtil.findFileOnClassPath(VyosConstants.VYOS_VERSION_PATH, true).getAbsolutePath();
        } catch (RuntimeException e) {
            logger.error(String.format("vyos version file find file because %s", e.getMessage()));
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            managementVersion = br.readLine();
        } catch (IOException e) {
            logger.error(String.format("vyos version file %s read error: %s", path, e.getMessage()));
            return null;
        }

        if (!(VirtualRouterMetadataOperator.zvrVersionCheck(managementVersion))) {
            logger.error(String.format("vyos version file format error: %s", managementVersion));
            return null;
        }

        return managementVersion;
    }
}
