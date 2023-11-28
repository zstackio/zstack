package org.zstack.core.upgrade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpgradeChecker implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(UpgradeChecker.class);
    
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected EventFacade evtf;

    @Autowired
    protected PluginRegistry pluginRgty;

    private Set<String> grayScaleConfigChangeSet = new HashSet<>();

    @Override
    public void managementNodeReady() {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return;
        }
        initGrayScaleConfig();
    }

    public void initGrayScaleConfig() {
        File oldGrayUpgradeFile = PathUtil.findFileOnClassPath("grayUpgrade/old_grayUpgrade.json", true);
        File grayUpgradeFile = PathUtil.findFileOnClassPath("grayUpgrade/grayUpgrade.json", true);

        Map<String, Map<String, String>> grayUpgradeConfigMap;
        Map<String, Map<String, String>> oldGrayUpgradeConfigMap;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            oldGrayUpgradeConfigMap = objectMapper.readValue(oldGrayUpgradeFile, new TypeReference<Map<String, Map<String, String>>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(String.format("unable to parse grayUpgrade json file[%s], exception: %s", oldGrayUpgradeFile.getAbsolutePath(), e.getMessage()));
        }

        try {
            grayUpgradeConfigMap = objectMapper.readValue(grayUpgradeFile, new TypeReference<Map<String, Map<String, String>>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(String.format("unable to parse grayUpgrade json file[%s], exception: %s", grayUpgradeFile.getAbsolutePath(), e.getMessage()));
        }

        grayUpgradeConfigMap.keySet().stream()
                .filter(key -> !oldGrayUpgradeConfigMap.containsKey(key) || !oldGrayUpgradeConfigMap.get(key).equals(grayUpgradeConfigMap.get(key)))
                .forEach(grayScaleConfigChangeSet::add);
    }

    public Boolean checkAgentHttpParamChanges(String commandName) {
        if (grayScaleConfigChangeSet.contains(commandName)) {
            return true;
        }

        String rspName;
        String responseName;
        Pattern pattern = Pattern.compile("(Cmd|Command)$");
        Matcher matcher = pattern.matcher(commandName);
        if (matcher.find()) {
            rspName = matcher.replaceFirst("Rsp");
            responseName = matcher.replaceFirst("Response");
        } else {
            rspName = commandName.concat("Rsp");
            responseName = commandName.concat("Response");
        }

        if (grayScaleConfigChangeSet.contains(rspName) || grayScaleConfigChangeSet.contains(responseName)) {
            return true;
        }

        return false;
    }

    public void updateAgentVersion(String agentUuid, String agentType, String expectVersion, String currentVersion) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return;
        }
        
        AgentVersionVO agentVersionVO = dbf.findByUuid(agentUuid, AgentVersionVO.class);
        if(agentVersionVO == null){
            agentVersionVO = new AgentVersionVO();
            agentVersionVO.setUuid(agentUuid);
            agentVersionVO.setAgentType(agentType);
            agentVersionVO.setCurrentVersion(currentVersion);
            agentVersionVO.setExpectVersion(expectVersion);
            dbf.persist(agentVersionVO);
            return;
        }
        
        if (Objects.equals(agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion())) {
            return;
        }

        if (!Objects.equals(agentVersionVO.getCurrentVersion(), currentVersion)) {
            agentVersionVO.setCurrentVersion(currentVersion);
            dbf.update(agentVersionVO);
        }
    }
}
