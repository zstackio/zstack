package org.zstack.core.upgrade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.Constants;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.APIUpdateClusterOSMsg;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.APIReconnectHostMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.APIRebootVmInstanceMsg;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.APIStopVmInstanceMsg;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class UpgradeChecker implements Component, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(UpgradeChecker.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected EventFacade evtf;

    @Autowired
    protected PluginRegistry pluginRgty;

    private static Map<String, Map<String, String>> grayUpgradeConfigMap = new HashMap<>();
    private static ConcurrentLinkedQueue<String> grayScaleApiWhiteList = new ConcurrentLinkedQueue<>();
    private static Set<String> predefinedApiClassSet = new HashSet<>();

    @Override
    public boolean start() {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return true;
        }
        initGrayScaleConfig();
        populateGlobalConfigForGrayscaleUpgrade();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void initPredefinedApiClassSet() {
        predefinedApiClassSet.add(APIStartVmInstanceMsg.class.getSimpleName());
        predefinedApiClassSet.add(APIStopVmInstanceMsg.class.getSimpleName());
        predefinedApiClassSet.add(APIRebootVmInstanceMsg.class.getSimpleName());
        predefinedApiClassSet.add(APIRequestConsoleAccessMsg.class.getSimpleName());
        predefinedApiClassSet.add(APIMigrateVmMsg.class.getSimpleName());
        predefinedApiClassSet.add(APIReconnectHostMsg.class.getSimpleName());
        predefinedApiClassSet.add(APIUpdateClusterOSMsg.class.getSimpleName());
    }

    private void populateGlobalConfigForGrayscaleUpgrade() {
        initPredefinedApiClassSet();
        grayScaleApiWhiteList.addAll(predefinedApiClassSet);
        List<String> predefinedClasses;
        try {
            predefinedClasses = Arrays.asList(UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING.value().split(","));
        } catch (PatternSyntaxException exception) {
            throw new CloudRuntimeException(String.format("Failed to split config value by ','," +
                    ", because %s. Please input a string separate api by ','", exception));
        }
        grayScaleApiWhiteList.addAll(predefinedClasses);

        UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING
                .installValidateExtension((category, name, oldValue, newValue) -> {
                    List<String> apiClassNames;
                    try {
                        apiClassNames = Arrays.asList(newValue.split(","));
                    } catch (PatternSyntaxException exception) {
                        throw new GlobalConfigException(String.format("Failed to split config value by ','," +
                                ", because %s. Please input a string separate api by ','", exception));
                    }

                    List<String> matchedApiClassName = apiClassNames.stream()
                            .filter(className -> APIMessage.apiMessageClasses
                                    .stream()
                                    .anyMatch(clazz -> clazz.getSimpleName().equals(className)))
                            .collect(Collectors.toList());

                    apiClassNames.removeAll(matchedApiClassName);
                    if (!apiClassNames.isEmpty()) {
                        throw new GlobalConfigException(String.format("Failed to find api class name: %s", apiClassNames));
                    }
        });

        UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING.installUpdateExtension((oldConfig, newConfig) -> {
            List<String> apiClassNames = Arrays.asList(newConfig.value().split(","));
            grayScaleApiWhiteList.clear();

            apiClassNames.removeAll(predefinedApiClassSet);
            grayScaleApiWhiteList.addAll(predefinedApiClassSet);
            grayScaleApiWhiteList.addAll(apiClassNames);
        });
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

    public ErrorCode checkAgentHttpParamChanges(String agentUuid, String commandName, Object cmd) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            logger.trace("grayscale upgrade is not enabled, skip http param check");
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

        // grayscale api white list check
        if (ThreadContext.containsKey(Constants.THREAD_CONTEXT_API)) {
            String className = ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
            if (className != null && grayScaleApiWhiteList
                    .stream()
                    .noneMatch(className::contains)) {
                return operr("Api: %s is not allowed by allowedApiListGrayscaleUpgrading: %s.",
                        className,
                        grayScaleApiWhiteList);
            }
        }

        List<Map<String, String>> relatedFieldsVersionMap = findCommandAndResponseFields(commandName);
        if (relatedFieldsVersionMap == null || relatedFieldsVersionMap.isEmpty()) {
            logger.debug(String.format("Command: %s is not contained in gray scale upgrade check", commandName));
            return null;
        }

        final Object commandObj;
        if (cmd instanceof String) {
            try {
                commandObj = JSONObjectUtil.toObject((String) cmd, Class.forName(commandName));
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("Failed to transform string: %s\n to \n object class: %s", cmd, commandName));
            }
        } else {
            commandObj = cmd;
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

            // check if field used in command
            entries = entries.stream().filter(entry -> {
                Object value = FieldUtils.getFieldValue(entry.getKey(), commandObj);
                // not used return
                if (value == null) {
                    logger.trace(String.format("Command obj do not use field %s, allow the usage", entry.getKey()));
                    return false;
                }

                if (value instanceof Collection) {
                    if (((Collection<?>) value).isEmpty()) {
                        logger.trace(String.format("Command obj use empty field %s, allow the usage", entry.getKey()));
                        return false;
                    }
                }

                return true;
            }).collect(Collectors.toSet());

            if (entries.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("agent[uuid: %s] current version: %s\n", agentUuid, agentVersionVO.getCurrentVersion()));
                    fields.forEach((key, value) -> sb.append(String.format("field: %s, support version: %s\n", key, value)));
                    sb.append("after check those fields' usage in command, allow operations");
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
            logger.trace(String.format("Create agent[uuid: %s] version\n" +
                            "From:\n" +
                            "expected version: null, current version: null\n" +
                            "To:\n" +
                            "expected version: %s, current version: %s\n",
                    agentUuid,
                    agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion()));
            return;
        }

        if (currentVersion == null) {
            logger.trace(String.format("Update agent[uuid: %s] version to null is not supported, skip updating", agentUuid));
            return;
        }

        if (Objects.equals(agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion())) {
            logger.trace(String.format("Agent[uuid: %s] version expected version: %s, current version: %s, not changed", agentUuid, agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion()));
            return;
        }

        if (!Objects.equals(agentVersionVO.getCurrentVersion(), currentVersion)) {
            String originCurrentVersion = agentVersionVO.getCurrentVersion();
            agentVersionVO.setCurrentVersion(currentVersion);
            logger.trace(String.format("Update agent[uuid: %s] version\n" +
                    "From:\n" +
                    "expected version: %s, current version: %s\n" +
                    "To:\n" +
                    "expected version: %s, current version: %s\n",
                    agentUuid,
                    agentVersionVO.getExpectVersion(), originCurrentVersion,
                    agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion()));
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

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdateClusterOSMsg) {
            validate((APIUpdateClusterOSMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateClusterOSMsg msg) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return;
        }

        if (msg.getHostUuid() == null) {
            throw new ApiMessageInterceptionException(
                    argerr("Disable grayscale upgrade by %s \n before you want to update whole cluster's hosts' os." +
                            " Or try update cluster os with specific hostUuid instead.", UpgradeGlobalConfig.GRAYSCALE_UPGRADE.toString())
            );
        }

        AgentVersionVO agent = Q.New(AgentVersionVO.class)
                .eq(AgentVersionVO_.uuid, msg.getHostUuid())
                .find();

        if (agent == null) {
            throw new ApiMessageInterceptionException(
                    argerr("Can not found agent version, upgrade cluster os is not supported during grayscale upgrade")
            );
        }

        if (agent.getCurrentVersion().equals(agent.getExpectVersion())) {
            return;
        }

        throw new ApiMessageInterceptionException(
                argerr("Host[uuid: %s] agent version is not upgraded, please reconnect host before update os", msg.getHostUuid())
        );
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APIUpdateClusterOSMsg.class);
    }
}
