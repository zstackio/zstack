package org.zstack.core.upgrade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
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
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class UpgradeChecker implements Component, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(UpgradeChecker.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected EventFacade evtf;

    @Autowired
    protected PluginRegistry pluginRgty;

    private static Map<String, Map<String, String>> grayUpgradeConfigMap = new HashMap<>();
    private static volatile ConcurrentLinkedQueue<String> grayScaleApiWhiteList = new ConcurrentLinkedQueue<>();
    private static Set<String> predefinedApiClassSet = new HashSet<>();
    private static boolean globalConfigExtensionInstalled = false;

    private static String INITIAL_AGENT_VERSION = "3.10.38";

    @Override
    public boolean start() {
        populateGlobalConfigForGrayscaleUpgrade();

        if (UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            initGrayScaleConfig();
        }
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

    private synchronized void populateGlobalConfigForGrayscaleUpgrade() {
        initPredefinedApiClassSet();
        refreshGrayScaleApiWhiteList(UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING.value());

        if (globalConfigExtensionInstalled) {
            return;
        }
        UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING
                .installValidateExtension((category, name, oldValue, newValue) -> {
                    List<String> apiClassNames = parseGrayScaleApiClassNames(newValue);

                    List<String> matchedApiClassName = apiClassNames.stream()
                            .filter(className -> APIMessage.apiMessageClasses
                                    .stream()
                                    .anyMatch(clazz -> clazz.getSimpleName().equals(className)))
                            .collect(Collectors.toList());

                    apiClassNames = new ArrayList<>(apiClassNames);
                    apiClassNames.removeAll(matchedApiClassName);
                    if (!apiClassNames.isEmpty()) {
                        throw new GlobalConfigException(String.format("Failed to find api class name: %s", apiClassNames));
                    }
        });

        UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING.installUpdateExtension((oldConfig, newConfig) -> {
            refreshGrayScaleApiWhiteList(newConfig.value());
        });
        globalConfigExtensionInstalled = true;
    }

    private List<String> parseGrayScaleApiClassNames(String value) {
        try {
            return Arrays.stream(StringUtils.defaultString(value).split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        } catch (PatternSyntaxException exception) {
            throw new GlobalConfigException(String.format("Failed to split config value by ','," +
                    ", because %s. Please input a string separate api by ','", exception));
        }
    }

    private void refreshGrayScaleApiWhiteList(String configValue) {
        List<String> apiClassNames = parseGrayScaleApiClassNames(configValue);

        ConcurrentLinkedQueue<String> whiteList = new ConcurrentLinkedQueue<>();
        whiteList.addAll(predefinedApiClassSet);
        apiClassNames.removeAll(predefinedApiClassSet);
        whiteList.addAll(apiClassNames);
        grayScaleApiWhiteList = whiteList;
    }

    public synchronized void addPredefinedApiClassName(String className) {
        if (StringUtils.isEmpty(className)) {
            return;
        }

        predefinedApiClassSet.add(className);
        refreshGrayScaleApiWhiteList(UpgradeGlobalConfig.ALLOWED_API_LIST_GRAYSCALE_UPGRADING.value());
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

        final AgentVersionVO agentVersionVO;
        AgentVersionVO versionVO = dbf.findByUuid(agentUuid, AgentVersionVO.class);
        if (versionVO == null) {
            agentVersionVO = new AgentVersionVO();
            agentVersionVO.setCurrentVersion(INITIAL_AGENT_VERSION);
            agentVersionVO.setExpectVersion(dbf.getDbVersion());
        } else {
            agentVersionVO = versionVO;
        }
        String currentAgentVersion = agentVersionVO.getCurrentVersion() == null ? INITIAL_AGENT_VERSION : agentVersionVO.getCurrentVersion();

        // if agent version not changed skip gray scale check
        if (agentVersionVO.getExpectVersion().equals(currentAgentVersion)) {
            logger.trace(String.format("agent[uuid: %s] expected version: %s, current version :%s matched," +
                            " skip grayscale upgrade check",
                    agentUuid,
                    currentAgentVersion,
                    currentAgentVersion));
            return null;
        }

        // grayscale api white list check
        if (ThreadContext.containsKey(Constants.THREAD_CONTEXT_API)) {
            String className = ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
            if (className != null && grayScaleApiWhiteList
                    .stream()
                    .noneMatch(className::contains)) {
                return operr(ORG_ZSTACK_CORE_UPGRADE_10000, "Api: %s is not allowed by allowedApiListGrayscaleUpgrading: %s.",
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

        logger.debug("grayscale compare agentVersionVO " + JSONObjectUtil.toJsonString(agentVersionVO));
        for (Map<String, String> fields : relatedFieldsVersionMap) {
            // check if current command has unexpected versions
            logger.debug("grayscale compare fields " + JSONObjectUtil.toJsonString(fields));
            VersionComparator currentVersion = new VersionComparator(currentAgentVersion);
            Set<Map.Entry<String, String>> entries = fields.entrySet()
                    .stream()
                    .filter(entry -> {//logger.debug(String.format("entry key: %s, value:%s", entry.getKey(), entry.getValue()));
                        String ver = entry.getValue();
                        if (StringUtils.isEmpty(ver)) {
                            logger.warn("null version for field: " + entry.getKey());
                            return false;
                        }
                        return currentVersion.lessThan(ver);
                    })
                    .collect(Collectors.toSet());

            // do not have new version changes
            if (entries.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("agent[uuid: %s] current version: %s\n", agentUuid, currentAgentVersion));
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
                    sb.append(String.format("agent[uuid: %s] current version: %s\n", agentUuid, currentAgentVersion));
                    fields.forEach((key, value) -> sb.append(String.format("field: %s, support version: %s\n", key, value)));
                    sb.append("after check those fields' usage in command, allow operations");
                    logger.trace(sb.toString());
                }
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("This operation is not allowed on host[uuid:%s] during grayscale upgrade: \n", agentUuid));
            entries.forEach(entry -> sb.append(String.format("field: %s, current agent version %s, support version: %s\n", entry.getKey(), currentAgentVersion, entry.getValue())));
            return operr(ORG_ZSTACK_CORE_UPGRADE_10001, sb.toString());
        }

        return null;
    }

    public void refreshAgentExpectedVersion(String agentUuid, String agentType, String expectVersion) {
        updateAgentVersion(agentUuid, agentType, expectVersion, null);
    }

    public void updateAgentVersion(String agentUuid, String agentType, String expectVersion, String currentVersion) {
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

        String originExpectVersion = agentVersionVO.getExpectVersion();
        String originCurrentVersion = agentVersionVO.getCurrentVersion();
        boolean changed = false;

        if (!Objects.equals(originExpectVersion, expectVersion)) {
            agentVersionVO.setExpectVersion(expectVersion);
            changed = true;
        }

        if (currentVersion != null && !Objects.equals(originCurrentVersion, currentVersion)) {
            agentVersionVO.setCurrentVersion(currentVersion);
            changed = true;
        }

        if (changed) {
            logger.trace(String.format("Update agent[uuid: %s] version\n" +
                    "From:\n" +
                    "expected version: %s, current version: %s\n" +
                    "To:\n" +
                    "expected version: %s, current version: %s\n",
                    agentUuid, originExpectVersion, originCurrentVersion,
                    agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion()));
            dbf.update(agentVersionVO);
        } else {
            logger.trace(String.format("Agent[uuid: %s] version expected version: %s, current version: %s, not changed",
                    agentUuid, agentVersionVO.getExpectVersion(), agentVersionVO.getCurrentVersion()));
        }
    }

    /**
     * check weather need to skip agent deployment or other initialize operation on current agent
     *
     * During agent ping, agent version will be recorded as a metadata and combine with agent
     * cmd param version checking if its using new params could be checked to avoid incompatible
     * operations.
     *
     * But some early versions cloud do not prepared agent version in db record which should wait
     * for the first ping to take back the result. So this method is used to avoid unexpected agent
     * deployment or init is triggered during (the version initializing). So if an agent do not
     * have agent version or version mismatched will be avoided to do the upgrade until a manual
     * api is used to upgrade the agent.
     *
     * @param agentUuid the uuid of used agent
     * @return true means skip the operations
     */
    public boolean skipInnerDeployOrInitOnCurrentAgent(String agentUuid) {
        if (!UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            return false;
        }

        AgentVersionVO agentVersionVO = dbf.findByUuid(agentUuid, AgentVersionVO.class);
        if (agentVersionVO == null) {
            return true;
        }

        return !agentVersionVO.getExpectVersion().equals(agentVersionVO.getCurrentVersion());
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
                    argerr(ORG_ZSTACK_CORE_UPGRADE_10002, "Disable grayscale upgrade by %s \n before you want to update whole cluster's hosts' os." +
                            " Or try update cluster os with specific hostUuid instead.", UpgradeGlobalConfig.GRAYSCALE_UPGRADE.toString())
            );
        }

        AgentVersionVO agent = Q.New(AgentVersionVO.class)
                .eq(AgentVersionVO_.uuid, msg.getHostUuid())
                .find();

        if (agent == null) {
            throw new ApiMessageInterceptionException(
                    argerr(ORG_ZSTACK_CORE_UPGRADE_10003, "Can not found agent version, upgrade cluster os is not supported during grayscale upgrade")
            );
        }

        if (agent.getCurrentVersion().equals(agent.getExpectVersion())) {
            return;
        }

        throw new ApiMessageInterceptionException(
                argerr(ORG_ZSTACK_CORE_UPGRADE_10004, "Host[uuid: %s] agent version is not upgraded, please reconnect host before update os", msg.getHostUuid())
        );
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APIUpdateClusterOSMsg.class);
    }
}
