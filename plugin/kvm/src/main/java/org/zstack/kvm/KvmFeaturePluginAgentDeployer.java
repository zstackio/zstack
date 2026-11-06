package org.zstack.kvm;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusSteppingCallback;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.plugin.FeaturePluginAgentExtensionPoint;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.ReconnectHostMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands.ApplyFeaturePluginCmd;
import org.zstack.kvm.KVMAgentCommands.ApplyFeaturePluginRsp;
import org.zstack.kvm.KVMAgentCommands.FeaturePluginPackagesCmd;
import org.zstack.kvm.KVMAgentCommands.FeaturePluginPackagesRsp;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Deploys the kvmagent parts of feature plugins. zstack-ctl keeps them with an index in &lt;dataDir&gt;/feature-plugins/agent;
 * they are copied into the kvm ansible module, whose playbook deploys them to a host with another copy of the index when
 * it connects, and pushed to the connected hosts when they change. A host the push fails on is reconnected. The system
 * packages an agent part depends on are installed when its plugin package is uploaded, a host that could not install
 * them gets them from the playbook.
 */
public class KvmFeaturePluginAgentDeployer implements FeaturePluginAgentExtensionPoint, KVMHostAddSshFileMd5CheckerExtensionPoint,
        KVMHostConnectExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmFeaturePluginAgentDeployer.class);
    // where the kvm playbook keeps the index of the feature plugins it deployed
    private static final String HOST_INDEX = "/var/lib/zstack/kvm/package/feature-plugins/index";
    private static final long APPLY_TIMEOUT = TimeUnit.SECONDS.toMillis(8);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private RESTFacade restf;

    // plugin ids whose agent parts changed while a host was connecting, after its playbook may have deployed the old ones
    private final Map<String, Set<String>> missedByConnectingHosts = new ConcurrentHashMap<>();

    private File moduleDir() {
        return new File(PathUtil.join(PathUtil.getZStackHomeFolder(), "ansible", "files", "kvm", "feature-plugins"));
    }

    private String index() {
        return PathUtil.join(moduleDir().getAbsolutePath(), "index");
    }

    private static Map<String, String> readIndex(File index) {
        Map<String, String> ret = new TreeMap<>();
        if (!index.exists()) {
            return ret;
        }
        try {
            for (String line : FileUtils.readLines(index, StandardCharsets.UTF_8)) {
                String[] parts = line.trim().split(" ");
                if (parts.length == 2) {
                    ret.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("cannot read %s", index), e);
        }
        return ret;
    }

    /**
     * Copies the agent parts into the kvm ansible module, after deploying the module replaced it.
     *
     * @return the index the module had before, plugin id -&gt; sha256
     */
    synchronized Map<String, String> copyAgentParts() {
        File src = Paths.get(CoreGlobalProperty.DATA_DIR, "feature-plugins", "agent").toFile();
        Map<String, String> before = readIndex(new File(index()));
        try {
            FileUtils.deleteDirectory(moduleDir());
            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, moduleDir());
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("cannot copy the agent parts of feature plugins from %s", src), e);
        }
        return before;
    }

    @Override
    public SshFileMd5Checker getSshFileMd5Checker(KVMHostVO host) {
        if (!new File(index()).exists()) {
            return null;
        }

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setUsername(host.getUsername());
        checker.setPassword(host.getPassword());
        checker.setSshPort(host.getPort());
        checker.setTargetIp(host.getManagementIp());
        checker.addSrcDestPair(index(), HOST_INDEX);
        return checker;
    }

    private static ApplyFeaturePluginCmd applyCmd(String pluginId, Map<String, String> index, Map<String, String> archives) {
        ApplyFeaturePluginCmd cmd = new ApplyFeaturePluginCmd();
        cmd.pluginId = pluginId;
        cmd.sha256 = index.get(pluginId);
        cmd.archive = archives.get(pluginId);
        return cmd;
    }

    // base64 of the agent archive of each plugin that changed, read once for all hosts
    private Map<String, String> archives(Set<String> pluginIds, Map<String, String> index) {
        Map<String, String> ret = new HashMap<>();
        for (String id : pluginIds) {
            if (index.get(id) == null) {
                continue;
            }
            try {
                ret.put(id, Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(moduleDir().getAbsolutePath(), id + ".tar.gz"))));
            } catch (IOException e) {
                throw new CloudRuntimeException(String.format("cannot read the agent part of feature plugin %s", id), e);
            }
        }
        return ret;
    }

    @Override
    public void agentPartsChanged() {
        Map<String, String> before = copyAgentParts();
        Map<String, String> after = readIndex(new File(index()));
        Set<String> changed = new TreeSet<>(before.keySet());
        changed.addAll(after.keySet());
        changed.removeIf(id -> Objects.equals(before.get(id), after.get(id)));
        if (changed.isEmpty()) {
            return;
        }

        List<String> connecting = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .eq(HostVO_.status, HostStatus.Connecting)
                .listValues();
        // their connections push them, until then they are not ready there
        connecting.stream().filter(destMaker::isManagedByUs).forEach(h -> {
            missedByConnectingHosts.computeIfAbsent(h, k -> ConcurrentHashMap.newKeySet()).addAll(changed);
            changed.forEach(id -> markDeploying(h, id));
        });

        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();
        List<String[]> pushes = new ArrayList<>();
        hostUuids.stream().filter(destMaker::isManagedByUs).forEach(h -> changed.forEach(id -> pushes.add(new String[]{h, id})));
        if (pushes.isEmpty()) {
            return;
        }

        Map<String, String> archives = archives(changed, after);
        logger.info(String.format("push the kvmagent parts of feature plugins %s to %s hosts", changed, pushes.size() / changed.size()));
        Map<String, Set<String>> failed = new ConcurrentHashMap<>();
        // a message holds a copy of the archive, only those in flight exist at once
        new While<>(pushes).step((push, completion) -> {
            ApplyFeaturePluginCmd cmd = applyCmd(push[1], after, archives);
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(push[0]);
            msg.setPath(KVMConstant.FEATURE_PLUGIN_APPLY_PATH);
            msg.setCommand(cmd);
            msg.setTimeout(APPLY_TIMEOUT);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, push[0]);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    String error = !reply.isSuccess() ? reply.getError().toString() : null;
                    if (error == null) {
                        ApplyFeaturePluginRsp rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(ApplyFeaturePluginRsp.class);
                        error = rsp.isSuccess() ? null : rsp.getError();
                    }
                    if (error != null) {
                        logger.warn(String.format("failed to push the kvmagent part of feature plugin %s to host[uuid:%s]: %s",
                                push[1], push[0], error));
                        failed.computeIfAbsent(push[0], k -> ConcurrentHashMap.newKeySet()).add(push[1]);
                    } else {
                        // the host has what the index lists now, a mark from an earlier failure no longer holds
                        clearDeploying(push[0], push[1]);
                    }
                    completion.done();
                }
            });
        }, HostGlobalConfig.HOST_LOAD_PARALLELISM_DEGREE.value(Integer.class)).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                failed.forEach((hostUuid, ids) -> ids.forEach(id -> markDeploying(hostUuid, id)));
                reconnect(failed.keySet());
            }
        });
    }

    private void markDeploying(String hostUuid, String pluginId) {
        SystemTagCreator creator = KVMSystemTags.FEATURE_PLUGIN_DEPLOYING.newSystemTagCreator(hostUuid);
        creator.setTagByTokens(map(e(KVMSystemTags.FEATURE_PLUGIN_DEPLOYING_TOKEN, pluginId)));
        creator.ignoreIfExisting = true;
        creator.create();
    }

    private void clearDeploying(String hostUuid, String pluginId) {
        KVMSystemTags.FEATURE_PLUGIN_DEPLOYING.delete(hostUuid, KVMSystemTags.FEATURE_PLUGIN_DEPLOYING.instantiateTag(
                map(e(KVMSystemTags.FEATURE_PLUGIN_DEPLOYING_TOKEN, pluginId))));
    }

    @Override
    public Map<String, Integer> deployingHosts() {
        List<String> tags = Q.New(SystemTagVO.class).select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, "featurePlugin::deploying::%")
                .listValues();
        Map<String, Integer> ret = new HashMap<>();
        tags.forEach(tag -> ret.merge(KVMSystemTags.FEATURE_PLUGIN_DEPLOYING.getTokenByTag(tag,
                KVMSystemTags.FEATURE_PLUGIN_DEPLOYING_TOKEN), 1, Integer::sum));
        return ret;
    }

    // the kvm playbook deployed the host's agent parts by the time its connection reaches here
    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "clear-feature-plugin-deploying-marks";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String hostUuid = context.getInventory().getUuid();
                KVMSystemTags.FEATURE_PLUGIN_DEPLOYING.delete(hostUuid);
                Map<String, String> installed = readIndex(new File(index()));
                // an agent part the host could not load is not ready on it either
                if (context.getFeaturePlugins() != null) {
                    context.getFeaturePlugins().forEach((id, status) -> {
                        if (installed.containsKey(id) && !"Loaded".equals(status.get("status"))) {
                            markDeploying(hostUuid, id);
                        }
                    });
                }

                Set<String> missed = missedByConnectingHosts.remove(hostUuid);
                if (missed == null) {
                    trigger.next();
                    return;
                }
                Map<String, String> archives = archives(missed, installed);
                String url = UriComponentsBuilder.fromHttpUrl(context.getBaseUrl()).path(KVMConstant.FEATURE_PLUGIN_APPLY_PATH).toUriString();
                new While<>(missed).each((id, completion) -> restf.asyncJsonPost(url, applyCmd(id, installed, archives),
                        new JsonAsyncRESTCallback<ApplyFeaturePluginRsp>(completion) {
                            @Override
                            public void fail(ErrorCode err) {
                                missedPushFailed(hostUuid, id, err.toString());
                                completion.done();
                            }

                            @Override
                            public void success(ApplyFeaturePluginRsp rsp) {
                                if (!rsp.isSuccess()) {
                                    missedPushFailed(hostUuid, id, rsp.getError());
                                }
                                completion.done();
                            }

                            @Override
                            public Class<ApplyFeaturePluginRsp> getReturnClass() {
                                return ApplyFeaturePluginRsp.class;
                            }
                        }, TimeUnit.MILLISECONDS, APPLY_TIMEOUT)).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    // the host stays marked, it gets the agent part when it connects again
    private void missedPushFailed(String hostUuid, String pluginId, String error) {
        logger.warn(String.format("failed to push the kvmagent part of feature plugin %s to host[uuid:%s] that was connecting when it changed: %s",
                pluginId, hostUuid, error));
        markDeploying(hostUuid, pluginId);
    }

    // the kvm playbook deploys what the push could not
    private void reconnect(Collection<String> hostUuids) {
        List<ReconnectHostMsg> msgs = hostUuids.stream().map(uuid -> {
            ReconnectHostMsg msg = new ReconnectHostMsg();
            msg.setHostUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
            return msg;
        }).collect(Collectors.toList());
        if (msgs.isEmpty()) {
            return;
        }

        logger.info(String.format("reconnect %s hosts to deploy the kvmagent parts of feature plugins", msgs.size()));
        bus.send(msgs, HostGlobalConfig.HOST_LOAD_PARALLELISM_DEGREE.value(Integer.class), new CloudBusSteppingCallback(null) {
            @Override
            public void run(NeedReplyMessage msg, MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to reconnect host[uuid:%s] for feature plugins: %s",
                            ((ReconnectHostMsg) msg).getHostUuid(), reply.getError()));
                }
            }
        });
    }

    @Override
    public void packageAdded(String pluginId, String sha256, File pkg) {
        thdf.submit(new Task<Void>() {
            @Override
            public Void call() {
                preinstall(pluginId, sha256, pkg);
                return null;
            }

            @Override
            public String getName() {
                return String.format("preinstall-feature-plugin-packages-%s", pluginId);
            }
        });
    }

    // the rpms under agent/rpms/<os>-<arch>/ of a plugin package, by "<os>-<arch>/<file name>"
    private static Map<String, byte[]> readRpms(File pkg) throws IOException {
        Map<String, byte[]> ret = new TreeMap<>();
        Path tmp = Files.createTempDirectory("feature-plugin-rpms");
        try {
            // as the management node's own user: files extracted with sudo are not readable to it
            ShellResult r = ShellUtils.runAndReturn(String.format("tar xzf '%s' -C '%s' --wildcards 'agent/rpms/*/*.rpm'",
                    pkg.getAbsolutePath(), tmp), false);
            // tar fails when the package has no rpm
            File[] folders = r.isReturnCode(0) ? tmp.resolve("agent").resolve("rpms").toFile().listFiles(File::isDirectory) : null;
            for (File folder : folders == null ? new File[0] : folders) {
                File[] rpms = folder.listFiles((d, n) -> n.endsWith(".rpm"));
                for (File rpm : rpms == null ? new File[0] : rpms) {
                    ret.put(folder.getName() + "/" + rpm.getName(), Files.readAllBytes(rpm.toPath()));
                }
            }
        } finally {
            FileUtils.deleteQuietly(tmp.toFile());
        }
        return ret;
    }

    private void preinstall(String pluginId, String sha256, File pkg) {
        Map<String, byte[]> rpms;
        try {
            rpms = readRpms(pkg);
        } catch (IOException e) {
            logger.warn(String.format("cannot read the agent packages of %s", pkg), e);
            return;
        }
        if (rpms.isEmpty()) {
            return;
        }

        // the cloud bus takes each message to the management node of the host
        List<Tuple> hosts = Q.New(HostVO.class).select(HostVO_.uuid, HostVO_.architecture)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .eq(HostVO_.status, HostStatus.Connected)
                .listTuple();
        // base64 of each rpm once; a host gets those built for its architecture
        Map<String, String> encoded = new TreeMap<>();
        rpms.forEach((name, content) -> encoded.put(name, Base64.getEncoder().encodeToString(content)));
        List<Tuple> targets = hosts.stream().filter(host -> encoded.keySet().stream()
                .anyMatch(name -> name.split("/")[0].endsWith("-" + host.get(1, String.class)))).collect(Collectors.toList());
        if (targets.isEmpty()) {
            return;
        }

        logger.info(String.format("install the agent packages of feature plugin %s on %s hosts", pluginId, targets.size()));
        new While<>(targets).step((host, completion) -> {
            FeaturePluginPackagesCmd cmd = new FeaturePluginPackagesCmd();
            cmd.pluginId = pluginId;
            cmd.sha256 = sha256;
            cmd.rpms = new TreeMap<>();
            encoded.forEach((name, content) -> {
                if (name.split("/")[0].endsWith("-" + host.get(1, String.class))) {
                    cmd.rpms.put(name, content);
                }
            });
            String hostUuid = host.get(0, String.class);
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(hostUuid);
            msg.setPath(KVMConstant.FEATURE_PLUGIN_PACKAGES_PATH);
            msg.setCommand(cmd);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    FeaturePluginPackagesRsp rsp = reply.isSuccess() ? ((KVMHostAsyncHttpCallReply) reply).toResponse(FeaturePluginPackagesRsp.class) : null;
                    if (rsp != null && rsp.isSuccess()) {
                        logger.info(String.format("installed the agent packages %s of feature plugin %s on host[uuid:%s]",
                                rsp.installed, pluginId, hostUuid));
                    } else {
                        // installing the plugin then leaves this host to the kvm playbook
                        logger.warn(String.format("failed to install the agent packages of feature plugin %s on host[uuid:%s]: %s",
                                pluginId, hostUuid, rsp == null ? reply.getError() : rsp.getError()));
                    }
                    completion.done();
                }
            });
        }, HostGlobalConfig.HOST_LOAD_PARALLELISM_DEGREE.value(Integer.class)).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
            }
        });
    }
}
