package org.zstack.core.upgrade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class UpgradeChecker implements Component {
    private static final CLogger logger = Utils.getLogger(UpgradeChecker.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected EventFacade evtf;

    @Autowired
    protected PluginRegistry pluginRgty;

    Map<String, Map<String, String>> grayUpgradeConfigMap = new HashMap<>();

    @Override
    public boolean start() {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return true;
        }
        initGrayScaleConfig();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void initGrayScaleConfig() {
        File grayUpgradeFile = PathUtil.findFileOnClassPath("grayUpgrade/grayUpgrade.json", true);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            grayUpgradeConfigMap = objectMapper.readValue(grayUpgradeFile, new TypeReference<Map<String, Map<String, String>>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(String.format("unable to parse grayUpgrade json file[%s], exception: %s", grayUpgradeFile.getAbsolutePath(), e.getMessage()));
        }
    }

    private List<Map<String, String>> findCommandAndResponseFields(String commandName) {
        Map<String, String> commandFields = grayUpgradeConfigMap.get(commandName);
        if (commandFields == null) {
            return null;
        }

        List<Map<String, String>> resultList = new ArrayList<>();
        resultList.add(commandFields);

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

        Map<String, String> rspFields = grayUpgradeConfigMap.get(rspName);
        if (rspFields != null) {
            resultList.add(rspFields);
        }

        Map<String, String> responseFields = grayUpgradeConfigMap.get(responseName);
        if (responseFields != null) {
            resultList.add(responseFields);
        }

        return resultList;
    }

    public ErrorCode checkAgentHttpParamChanges(String agentUuid, String commandName) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return null;
        }

        AgentVersionVO agentVersionVO = dbf.findByUuid(agentUuid, AgentVersionVO.class);
        if (agentVersionVO == null) {
            return operr("No agent[uuid: %s] version found, do not support grayscale upgrade", agentUuid);
        }

        // if agent version not changed skip gray scale check
        if (agentVersionVO.getExpectVersion().equals(agentVersionVO.getCurrentVersion())) {
            logger.trace(String.format("agent[uuid: %s] expected version: %s, current version :%s matched," +
                            " skip grayscale upgrade check",
                    agentUuid,
                    agentVersionVO.getCurrentVersion(),
                    agentVersionVO.getCurrentVersion()));
            return null;
        }

        List<Map<String, String>> relatedFieldsVersionMap = findCommandAndResponseFields(commandName);
        if (relatedFieldsVersionMap == null || relatedFieldsVersionMap.isEmpty()) {
            return operr("No command %s not found, do not support grayscale upgrade", commandName);
        }

        for (Map<String, String> fields : relatedFieldsVersionMap) {
            // check if current command has unexpected versions
            VersionComparator currentVersion = new VersionComparator(agentVersionVO.getCurrentVersion());
            Set<Map.Entry<String, String>> entries = fields.entrySet()
                    .stream()
                    .filter(entry -> currentVersion.lessThan(entry.getValue()))
                    .collect(Collectors.toSet());

            // do not have new version changes
            if (entries.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("agent[uuid: %s] current version: %s\n", agentUuid, agentVersionVO.getCurrentVersion()));
                    fields.forEach((key, value) -> sb.append(String.format("field: %s, support version: %s\n", key, value)));
                    sb.append("all fields is supported by current agent, allow operations");
                    logger.trace(sb.toString());
                }
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("This operation is not allowed on host[uuid:%s] during grayscale upgrade: \n", agentUuid));
            entries.forEach(entry -> sb.append(String.format("field: %s, current agent version %s, support version: %s\n", entry.getKey(), agentVersionVO.getCurrentVersion(), entry.getValue())));
            return operr(sb.toString());
        }

        return null;
    }

    public void updateAgentVersion(String agentUuid, String agentType, String expectVersion, String currentVersion) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return;
        }

        AgentVersionVO agentVersionVO = dbf.findByUuid(agentUuid, AgentVersionVO.class);
        if (agentVersionVO == null) {
            agentVersionVO = new AgentVersionVO();
            agentVersionVO.setUuid(agentUuid);
            agentVersionVO.setAgentType(agentType);
            agentVersionVO.setCurrentVersion(currentVersion);
            agentVersionVO.setExpectVersion(expectVersion);
            dbf.persist(agentVersionVO);
            return;
        }

        if (currentVersion == null) {
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

    public boolean skipConnectAgent(String agentUuid) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return false;
        }
        
        AgentVersionVO agentVersionVO = dbf.findByUuid(agentUuid, AgentVersionVO.class);
        if (agentVersionVO == null) {
            return true;
        }
        if (!agentVersionVO.getExpectVersion().equals(agentVersionVO.getCurrentVersion())) {
            return true;
        }
        return false;
    }
}
