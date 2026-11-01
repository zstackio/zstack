package org.zstack.core.plugin;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.external.plugin.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeState;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_PLUGIN_10002;

/**
 * Keeps the feature plugins of all management nodes the same: syncs packages to the other nodes after
 * an install or uninstall, restarts the nodes one by one, and fetches missing packages when a node starts.
 */
public class FeaturePluginCoordinator implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FeaturePluginCoordinator.class);
    private static final int RESTART_DELAY_SECONDS = 10;
    private static final long PEER_RESTART_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(15);
    private static final long PEER_POLL_INTERVAL_SECONDS = 10;

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;

    String runCtl(String args) {
        ShellResult ret = ShellUtils.runAndReturn(String.format("zstack-ctl plugin %s", args));
        if (ret.getRetCode() != 0) {
            String output = ret.getStderr() == null || ret.getStderr().trim().isEmpty() ? ret.getStdout() : ret.getStderr();
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

    void restartLater() {
        // leave time for the caller to fetch the API result before the management node stops
        ShellUtils.runAndReturn(String.format("bash -c 'nohup sh -c \"sleep %d; zstack-ctl restart_node\" >/dev/null 2>&1 &'",
                RESTART_DELAY_SECONDS));
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

    private List<ManagementNodeVO> runningPeers() {
        return Q.New(ManagementNodeVO.class)
                .notEq(ManagementNodeVO_.uuid, Platform.getManagementServerId())
                .eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING)
                .list();
    }

    /**
     * Sends the package (null after an uninstall) to the other running management nodes and lets them reconcile.
     *
     * @return uuids of the nodes that need a restart; a node that failed to sync is included,
     * it fetches the package again when it starts
     */
    List<String> syncToPeers(String packagePath) {
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

        List<String> restart = new ArrayList<>();
        for (ManagementNodeVO peer : runningPeers()) {
            SyncFeaturePluginPackageMsg msg = new SyncFeaturePluginPackageMsg();
            msg.setPackageName(packagePath == null ? null : new File(packagePath).getName());
            msg.setSha256(sha256);
            msg.setContent(content);
            bus.makeServiceIdByManagementNodeId(msg, PluginManager.SERVICE_ID, peer.getUuid());
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                logger.warn(String.format("failed to sync feature plugins to management node[%s], it fetches them again" +
                        " after restarting: %s", peer.getHostName(), reply.getError()));
                restart.add(peer.getUuid());
            } else if (((SyncFeaturePluginPackageReply) reply).isNeedRestart()) {
                restart.add(peer.getUuid());
            }
        }
        return restart;
    }

    /**
     * Restarts the given management nodes one after another in the background, this node last. Stops when a node
     * does not come back in time, so that the cluster is never restarted as a whole.
     */
    void restartInOrder(List<String> peerUuids, boolean restartSelf) {
        thdf.submit(new Task<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    restartPeersInOrder();
                } catch (Throwable t) {
                    // this node still applies its own change, it would otherwise refuse plugin operations until restarted
                    logger.error("failed to restart the other management nodes for feature plugins", t);
                    if (restartSelf) {
                        restartLater();
                    }
                }
                return null;
            }

            private void restartPeersInOrder() throws InterruptedException {
                List<ManagementNodeVO> peers = peerUuids.isEmpty() ? new ArrayList<>()
                        : Q.New(ManagementNodeVO.class).in(ManagementNodeVO_.uuid, peerUuids).list();
                peers.sort(Comparator.comparing(ManagementNodeVO::getUuid));
                for (ManagementNodeVO peer : peers) {
                    Timestamp before = dbf.getCurrentSqlTime();
                    RestartManagementNodeForFeaturePluginMsg msg = new RestartManagementNodeForFeaturePluginMsg();
                    bus.makeServiceIdByManagementNodeId(msg, PluginManager.SERVICE_ID, peer.getUuid());
                    MessageReply reply = bus.call(msg);
                    if (!reply.isSuccess() || !waitForRestart(peer.getHostName(), before)) {
                        logger.error(String.format("management node[%s] did not restart in %s seconds for feature plugins," +
                                " stop restarting the other management nodes", peer.getHostName(), PEER_RESTART_TIMEOUT_SECONDS));
                        return;
                    }
                }

                if (restartSelf) {
                    restartLater();
                }
            }

            @Override
            public String getName() {
                return "restart-management-nodes-for-feature-plugins";
            }
        });
    }

    private boolean waitForRestart(String hostName, Timestamp before) throws InterruptedException {
        // a management node recreates its record with a new joinDate when it starts
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(PEER_RESTART_TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            TimeUnit.SECONDS.sleep(PEER_POLL_INTERVAL_SECONDS);
            ManagementNodeVO vo = Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.hostName, hostName).limit(1).find();
            if (vo != null && vo.getState() == ManagementNodeState.RUNNING && vo.getJoinDate() != null
                    && vo.getJoinDate().after(before)) {
                return true;
            }
        }
        return false;
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
            reply.setNeedRestart(Boolean.TRUE.equals(runCtlJson("reconcile --json").get("needRestart")));
            refreshDrivers();
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

    void handle(RestartManagementNodeForFeaturePluginMsg msg) {
        bus.reply(msg, new MessageReply());
        restartLater();
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

        // restart only if this really changed the plugins, a package no node has does not cause restarts
        if (Boolean.TRUE.equals(runCtlJson("reconcile --json").get("needRestart"))) {
            logger.info("fetched missing feature plugin packages from other management nodes, restart to load them");
            restartLater();
        }
    }
}
