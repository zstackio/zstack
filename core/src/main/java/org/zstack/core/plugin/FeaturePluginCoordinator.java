package org.zstack.core.plugin;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.external.plugin.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeState;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.plugin.sdk.LicenseCheckPoint;
import org.zstack.header.plugin.sdk.PluginDescriptor;
import org.zstack.header.plugin.sdk.PluginLicenseChecker;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLUGIN_10002;

/**
 * Keeps the feature plugins of all management nodes the same without restarting them: after an install or uninstall
 * each node lays the files out with zstack-ctl and loads, switches or unloads the plugins in its running management
 * node. A node that starts loads the installed plugins and fetches packages it misses from the others.
 */
public class FeaturePluginCoordinator implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FeaturePluginCoordinator.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private FeaturePluginLoader loader;

    // plugin id -> sha256 of the package this node has loaded
    private final Map<String, String> loadedSha = new ConcurrentHashMap<>();

    String runCtl(String args) {
        ShellResult ret = ShellUtils.runAndReturn(String.format("zstack-ctl plugin %s", args));
        if (ret.getRetCode() != 0) {
            String output = ret.getStderr() == null || ret.getStderr().trim().isEmpty() ? ret.getStdout() : ret.getStderr();
            // a failed mysql or flyway command line printed by zstack-ctl carries the database password
            if (DatabaseGlobalProperty.DbPassword != null && !DatabaseGlobalProperty.DbPassword.isEmpty()) {
                output = output.replace(DatabaseGlobalProperty.DbPassword, "******");
            }
            // zstack-ctl colors its error output
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_PLUGIN_10002, "%s",
                    output.replaceAll("\u001B\\[[;\\d]*m", "").trim()));
        }
        return ret.getStdout();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> runCtlJson(String args) {
        // the json result is the last line, warnings may come before it
        String out = runCtl(args).trim();
        return JSONObjectUtil.toObject(out.substring(out.lastIndexOf('\n') + 1), LinkedHashMap.class);
    }

    static boolean agentChanged(Map<String, Object> ctlResult) {
        return Boolean.TRUE.equals(ctlResult.get("agentChanged"));
    }

    /**
     * Lets the hypervisor modules deploy the changed agent parts of feature plugins to their hosts; a failure is only
     * logged, the hosts get the change when they reconnect.
     */
    void packageAdded(FeaturePluginPackageInventory pkg) {
        for (FeaturePluginAgentExtensionPoint ext : pluginRgty.getExtensionList(FeaturePluginAgentExtensionPoint.class)) {
            try {
                ext.packageAdded(pkg.getId(), pkg.getSha256(), new File(pkg.getPath()));
            } catch (Throwable t) {
                logger.warn(String.format("%s failed to preinstall the agent packages of %s", ext.getClass().getSimpleName(), pkg.getPath()), t);
            }
        }
    }

    void agentPartsChanged() {
        for (FeaturePluginAgentExtensionPoint ext : pluginRgty.getExtensionList(FeaturePluginAgentExtensionPoint.class)) {
            try {
                ext.agentPartsChanged();
            } catch (Throwable t) {
                logger.warn(String.format("%s failed to deploy the agent parts of feature plugins", ext.getClass().getSimpleName()), t);
            }
        }
    }

    /**
     * Loads the plugin drivers zstack-ctl put into the driver folder, which needs no restart.
     * A driver that fails to load is logged; it is loaded again when the management node restarts.
     */
    void refreshDrivers() {
        RefreshPluginDriversMsg msg = new RefreshPluginDriversMsg();
        bus.makeLocalServiceId(msg, PluginManager.SERVICE_ID);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to load plugin drivers of feature plugins: %s", reply.getError()));
        }
    }

    ErrorCode licenseError(PluginDescriptor plugin, LicenseCheckPoint point) {
        for (PluginLicenseChecker checker : pluginRgty.getExtensionList(PluginLicenseChecker.class)) {
            ErrorCode err = checker.check(plugin, point);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    /**
     * Brings the plugins loaded in this management node to the records zstack-ctl keeps: loads new ones, switches
     * changed ones to their new package and unloads removed ones.
     *
     * @return plugin id -> why it could not be loaded
     */
    synchronized Map<String, String> apply() {
        FeaturePlugins.refresh();
        Map<String, FeaturePluginInventory> wanted = new LinkedHashMap<>();
        for (FeaturePluginInventory r : FeaturePlugins.records()) {
            // a record with a status of zstack-ctl's has no files to load
            if (r.getStatus() == null && r.getSha256() != null) {
                wanted.put(r.getId(), r);
            }
        }

        for (String id : loader.loadedIds()) {
            if (!wanted.containsKey(id)) {
                loader.unload(id);
                loadedSha.remove(id);
                FeaturePlugins.clearState(id);
            }
        }

        Map<String, String> failures = new LinkedHashMap<>();
        for (FeaturePluginInventory r : wanted.values()) {
            String id = r.getId();
            if (r.getSha256().equals(loadedSha.get(id)) && loader.isLoaded(id)) {
                continue;
            }
            File dir = FeaturePlugins.pluginDir(id, r.getSha256());
            try {
                if (loader.isLoaded(id)) {
                    loader.switchTo(id, dir);
                } else {
                    loader.load(id, dir);
                }
                loadedSha.put(id, r.getSha256());
                FeaturePlugins.setState(id, FeaturePluginInventory.STATUS_LOADED, null);
                // what an unlicensed plugin is kept from doing waits for the licensing decision, it is only marked
                ErrorCode err = licenseError(new PluginDescriptor(id, r.getVersion(), r.getLicenseAddon()), LicenseCheckPoint.LOAD);
                if (err != null) {
                    logger.warn(String.format("feature plugin[%s] is not licensed: %s", id, err));
                    FeaturePlugins.setState(id, FeaturePluginInventory.STATUS_UNLICENSED, err.getReadableDetails());
                }
            } catch (Throwable t) {
                logger.warn(String.format("failed to load feature plugin %s from %s", id, dir), t);
                failures.put(id, t.getMessage());
                FeaturePlugins.setState(id, FeaturePluginInventory.STATUS_FAILED, t.getMessage());
                if (!loader.isLoaded(id)) {
                    loadedSha.remove(id);
                }
            }
        }
        FeaturePlugins.setClassLoaders(loader.classLoaders());
        return failures;
    }

    /**
     * Applies the records here, lists the loaded plugins in this node's ui index and deploys changed agent parts.
     */
    Map<String, String> applyAndPublish(boolean agentChanged) {
        Map<String, String> failures = apply();
        try {
            runCtl("reconcile --json --publish-ui");
        } catch (Exception e) {
            logger.warn("failed to publish the ui parts of feature plugins", e);
        }
        if (agentChanged) {
            agentPartsChanged();
        }
        return failures;
    }

    /**
     * Loads the installed plugins while the management node starts; start_node has laid their files out and written
     * the ui index. A plugin that fails to load is marked Failed and the node starts anyway.
     */
    public void loadInstalledPlugins() {
        try {
            apply().forEach((id, error) -> logger.warn(String.format("feature plugin %s is not loaded: %s", id, error)));
        } catch (Throwable t) {
            logger.warn("failed to load feature plugins", t);
        }
    }

    private List<ManagementNodeVO> runningPeers() {
        return Q.New(ManagementNodeVO.class)
                .notEq(ManagementNodeVO_.uuid, Platform.getManagementServerId())
                .eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING)
                .list();
    }

    /**
     * Sends the package (null after an uninstall) to the other running management nodes, which lay it out and apply
     * the records.
     *
     * @return host name of a node -> why it could not apply the change
     */
    Map<String, String> syncToPeers(String packagePath) {
        String content = null;
        String sha256 = null;
        if (packagePath != null) {
            try {
                byte[] bytes = Files.readAllBytes(new File(packagePath).toPath());
                content = Base64.getEncoder().encodeToString(bytes);
                sha256 = DigestUtils.sha256Hex(bytes);
            } catch (IOException e) {
                throw new OperationFailureException(operr(ORG_ZSTACK_CORE_PLUGIN_10002, "%s", e.getMessage()));
            }
        }

        Map<String, String> failures = new LinkedHashMap<>();
        for (ManagementNodeVO peer : runningPeers()) {
            SyncFeaturePluginPackageMsg msg = new SyncFeaturePluginPackageMsg();
            msg.setPackageName(packagePath == null ? null : new File(packagePath).getName());
            msg.setSha256(sha256);
            msg.setContent(content);
            bus.makeServiceIdByManagementNodeId(msg, PluginManager.SERVICE_ID, peer.getUuid());
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                logger.warn(String.format("management node[%s] failed to apply the feature plugin change: %s",
                        peer.getHostName(), reply.getError()));
                failures.put(peer.getHostName(), reply.getError().getReadableDetails());
            }
        }
        return failures;
    }

    private void addPackage(String packageName, byte[] content) throws IOException {
        File dir = Files.createTempDirectory("feature-plugin").toFile();
        try {
            File pkg = new File(dir, new File(packageName).getName());
            FileUtils.writeByteArrayToFile(pkg, content);
            runCtl(String.format("add %s", pkg.getAbsolutePath()));
        } finally {
            FileUtils.deleteQuietly(dir);
        }
    }

    void handle(SyncFeaturePluginPackageMsg msg) {
        // zstack-ctl runs for a while, keep it off the message thread and one sync at a time
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "sync-feature-plugins";
            }

            @Override
            public void run(SyncTaskChain chain) {
                syncFromPeer(msg);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void syncFromPeer(SyncFeaturePluginPackageMsg msg) {
        SyncFeaturePluginPackageReply reply = new SyncFeaturePluginPackageReply();
        try {
            if (msg.getContent() != null) {
                byte[] content = Base64.getDecoder().decode(msg.getContent());
                if (!DigestUtils.sha256Hex(content).equals(msg.getSha256())) {
                    throw new OperationFailureException(operr(ORG_ZSTACK_CORE_PLUGIN_10002,
                            "feature plugin package[%s] is damaged in transit", msg.getPackageName()));
                }
                addPackage(msg.getPackageName(), content);
            }
            Map<String, Object> result = runCtlJson("reconcile --json");
            refreshDrivers();
            Map<String, String> failures = applyAndPublish(agentChanged(result));
            if (!failures.isEmpty()) {
                reply.setError(operr(ORG_ZSTACK_CORE_PLUGIN_10002, "%s", failures));
            }
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
        } catch (Exception e) {
            reply.setError(operr(ORG_ZSTACK_CORE_PLUGIN_10002, "%s", e.getMessage()));
        }
        bus.reply(msg, reply);
    }

    void handle(FetchFeaturePluginPackageMsg msg) {
        FetchFeaturePluginPackageReply reply = new FetchFeaturePluginPackageReply();
        try {
            List<FeaturePluginPackageInventory> packages = JSONObjectUtil.toCollection(runCtl("list --json").trim(),
                    ArrayList.class, FeaturePluginPackageInventory.class);
            FeaturePluginPackageInventory pkg = packages.stream()
                    .filter(p -> msg.getSha256().equals(p.getSha256()))
                    .findFirst().orElse(null);
            if (pkg == null) {
                reply.setError(operr(ORG_ZSTACK_CORE_PLUGIN_10002, "no feature plugin package with sha256[%s]", msg.getSha256()));
            } else {
                reply.setPackageName(new File(pkg.getPath()).getName());
                reply.setContent(Base64.getEncoder().encodeToString(Files.readAllBytes(new File(pkg.getPath()).toPath())));
            }
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
        } catch (Exception e) {
            reply.setError(operr(ORG_ZSTACK_CORE_PLUGIN_10002, "%s", e.getMessage()));
        }
        bus.reply(msg, reply);
    }

    @Override
    public void managementNodeReady() {
        thdf.submit(new Task<Void>() {
            @Override
            public Void call() {
                try {
                    fetchMissingPackages();
                } catch (Exception e) {
                    logger.warn("failed to fetch missing feature plugin packages from other management nodes", e);
                }
                return null;
            }

            @Override
            public String getName() {
                return "fetch-missing-feature-plugin-packages";
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void fetchMissingPackages() throws IOException {
        List<Map<String, Object>> missing = (List<Map<String, Object>>) runCtlJson("reconcile --json --dry-run").get("missing");
        if (missing == null || missing.isEmpty()) {
            return;
        }

        for (Map<String, Object> m : missing) {
            for (ManagementNodeVO peer : runningPeers()) {
                FetchFeaturePluginPackageMsg msg = new FetchFeaturePluginPackageMsg();
                msg.setSha256((String) m.get("sha256"));
                bus.makeServiceIdByManagementNodeId(msg, PluginManager.SERVICE_ID, peer.getUuid());
                MessageReply reply = bus.call(msg);
                if (reply.isSuccess()) {
                    FetchFeaturePluginPackageReply r = reply.castReply();
                    addPackage(r.getPackageName(), Base64.getDecoder().decode(r.getContent()));
                    break;
                }
            }
        }

        Map<String, Object> result = runCtlJson("reconcile --json");
        refreshDrivers();
        logger.info("fetched missing feature plugin packages from other management nodes, loading them");
        applyAndPublish(agentChanged(result)).forEach((id, error) ->
                logger.warn(String.format("feature plugin %s is not loaded: %s", id, error)));
    }
}
