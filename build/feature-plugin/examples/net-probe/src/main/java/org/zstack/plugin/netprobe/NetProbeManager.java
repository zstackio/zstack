package org.zstack.plugin.netprobe;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.plugin.sdk.PluginUsageChecker;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.plugin.netprobe.NetProbeCommands.ProbeCmd;
import org.zstack.plugin.netprobe.NetProbeCommands.ProbeResult;
import org.zstack.plugin.netprobe.NetProbeCommands.ProbeRsp;
import org.zstack.plugin.netprobe.NetProbeCommands.ProbeSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

/**
 * Runs probe tasks on hosts. Every management node checks for due tasks; a node runs a task only after it moved
 * the task's nextRunAt on, so that each task runs once per interval however many nodes there are.
 */
public class NetProbeManager extends AbstractService implements PluginUsageChecker {
    private static final CLogger logger = Utils.getLogger(NetProbeManager.class);

    public static final String SERVICE_ID = "netprobe";
    private static final String PROBE_PATH = "/plugin/net-probe/probe";
    private static final int CHECK_INTERVAL_SECONDS = 5;
    private static final int DEFAULT_INTERVAL_SECONDS = 60;
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final int MAX_TASKS = 100;
    private static final int KEPT_RESULTS = 100;
    // an IPv4 address or a host name; the kvmagent part checks the same before running anything
    private static final Pattern TARGET = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9.-]{0,252}$");

    static final String OK = "OK";
    static final String FAILED = "FAILED";
    static final String HOST_UNAVAILABLE = "HOST_UNAVAILABLE";
    static final String NOT_RUN = "NOT_RUN";

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private Future<Void> scheduler;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APICreateNetProbeTaskMsg) {
            handle((APICreateNetProbeTaskMsg) msg);
        } else if (msg instanceof APIDeleteNetProbeTaskMsg) {
            handle((APIDeleteNetProbeTaskMsg) msg);
        } else if (msg instanceof APIGetNetProbeTasksMsg) {
            handle((APIGetNetProbeTasksMsg) msg);
        } else if (msg instanceof APIGetNetProbeResultsMsg) {
            handle((APIGetNetProbeResultsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private ErrorCode validate(APICreateNetProbeTaskMsg msg, int interval, int timeout) {
        if (!"HTTP".equals(msg.getType()) && (msg.getTarget() == null || !TARGET.matcher(msg.getTarget()).matches())) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10000",
                    "the probe target %s must be an IPv4 address or a host name", msg.getTarget());
        }
        if ("TCP".equals(msg.getType()) && msg.getPort() == null) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10001", "a TCP probe needs a port between 1 and 65535");
        }
        if ("HTTP".equals(msg.getType()) && (msg.getUrl() == null
                || !(msg.getUrl().startsWith("http://") || msg.getUrl().startsWith("https://"))
                || msg.getUrl().chars().anyMatch(Character::isWhitespace))) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10002",
                    "the URL of an HTTP probe must start with http:// or https://: %s", msg.getUrl());
        }
        if (timeout >= interval) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10003",
                    "the timeout %d seconds must be less than the interval %d seconds", timeout, interval);
        }
        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        if (!"KVM".equals(host.getHypervisorType())) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10004", "host[uuid:%s] is not a KVM host", msg.getHostUuid());
        }
        if (host.getStatus() != HostStatus.Connected) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10005", "host[uuid:%s] is not connected", msg.getHostUuid());
        }
        long count = Q.New(NetProbeTaskVO.class).count();
        if (count >= MAX_TASKS) {
            return argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10006",
                    "there are already %d probe tasks, the most allowed", count);
        }
        return null;
    }

    private void handle(APICreateNetProbeTaskMsg msg) {
        APICreateNetProbeTaskEvent evt = new APICreateNetProbeTaskEvent(msg.getId());
        int interval = msg.getIntervalSeconds() == null ? DEFAULT_INTERVAL_SECONDS : msg.getIntervalSeconds();
        int timeout = msg.getTimeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : msg.getTimeoutSeconds();
        ErrorCode err = validate(msg, interval, timeout);
        if (err != null) {
            evt.setError(err);
            bus.publish(evt);
            return;
        }

        Timestamp now = dbf.getCurrentSqlTime();
        NetProbeTaskVO vo = new NetProbeTaskVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(msg.getName());
        vo.setHostUuid(msg.getHostUuid());
        vo.setType(msg.getType());
        vo.setTarget("HTTP".equals(msg.getType()) ? null : msg.getTarget());
        vo.setPort("TCP".equals(msg.getType()) ? msg.getPort() : null);
        vo.setUrl("HTTP".equals(msg.getType()) ? msg.getUrl() : null);
        vo.setIntervalSeconds(interval);
        vo.setTimeoutSeconds(timeout);
        // due right away, the next check of any management node runs it
        vo.setNextRunAt(now);
        vo.setLastStatus(NOT_RUN);
        vo.setCreateDate(now);
        vo.setLastOpDate(now);
        evt.setInventory(NetProbeTaskInventory.valueOf(dbf.persistAndRefresh(vo)));
        bus.publish(evt);
    }

    private void handle(APIDeleteNetProbeTaskMsg msg) {
        // its results go with it through the foreign key
        SQL.New("delete from NetProbeTaskVO t where t.uuid = :uuid").param("uuid", msg.getUuid()).execute();
        bus.publish(new APIDeleteNetProbeTaskEvent(msg.getId()));
    }

    private void handle(APIGetNetProbeTasksMsg msg) {
        List<NetProbeTaskVO> tasks = msg.getHostUuid() == null ? Q.New(NetProbeTaskVO.class).list()
                : Q.New(NetProbeTaskVO.class).eq(NetProbeTaskVO_.hostUuid, msg.getHostUuid()).list();
        APIGetNetProbeTasksReply reply = new APIGetNetProbeTasksReply();
        reply.setInventories(tasks.stream()
                .sorted(Comparator.comparing(NetProbeTaskVO::getCreateDate).thenComparing(NetProbeTaskVO::getUuid))
                .map(NetProbeTaskInventory::valueOf)
                .collect(Collectors.toList()));
        bus.reply(msg, reply);
    }

    private void handle(APIGetNetProbeResultsMsg msg) {
        APIGetNetProbeResultsReply reply = new APIGetNetProbeResultsReply();
        if (!Q.New(NetProbeTaskVO.class).eq(NetProbeTaskVO_.uuid, msg.getTaskUuid()).isExists()) {
            reply.setError(argerr("ORG_ZSTACK_PLUGIN_NET_PROBE_10007",
                    "probe task[uuid:%s] is not found", msg.getTaskUuid()));
            bus.reply(msg, reply);
            return;
        }
        List<NetProbeResultVO> results = Q.New(NetProbeResultVO.class)
                .eq(NetProbeResultVO_.taskUuid, msg.getTaskUuid()).list();
        int limit = msg.getLimit() == null ? KEPT_RESULTS : msg.getLimit();
        reply.setInventories(results.stream()
                .sorted(Comparator.comparingLong(NetProbeResultVO::getId).reversed())
                .limit(limit)
                .map(NetProbeResultInventory::valueOf)
                .collect(Collectors.toList()));
        bus.reply(msg, reply);
    }

    private void runDueTasks() {
        Timestamp now = dbf.getCurrentSqlTime();
        List<NetProbeTaskVO> due = Q.New(NetProbeTaskVO.class).lte(NetProbeTaskVO_.nextRunAt, now).list();
        Map<String, List<NetProbeTaskVO>> byHost = new LinkedHashMap<>();
        for (NetProbeTaskVO t : due) {
            Timestamp next = new Timestamp(now.getTime() + t.getIntervalSeconds() * 1000L);
            int claimed = SQL.New("update NetProbeTaskVO t set t.nextRunAt = :next where t.uuid = :uuid and t.nextRunAt = :seen")
                    .param("next", next).param("uuid", t.getUuid()).param("seen", t.getNextRunAt()).execute();
            if (claimed == 1) {
                byHost.computeIfAbsent(t.getHostUuid(), k -> new ArrayList<>()).add(t);
            }
        }
        byHost.forEach(this::probeOnHost);
    }

    private void probeOnHost(String hostUuid, List<NetProbeTaskVO> tasks) {
        HostStatus status = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, hostUuid).findValue();
        if (status != HostStatus.Connected) {
            tasks.forEach(t -> record(t.getUuid(), HOST_UNAVAILABLE, false, null, null, null, "the host is unavailable"));
            return;
        }

        ProbeCmd cmd = new ProbeCmd();
        cmd.probes = tasks.stream().map(t -> {
            ProbeSpec s = new ProbeSpec();
            s.taskUuid = t.getUuid();
            s.type = t.getType();
            s.target = t.getTarget();
            s.port = t.getPort();
            s.url = t.getUrl();
            s.timeout = t.getTimeoutSeconds();
            return s;
        }).collect(Collectors.toList());
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(PROBE_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    String error = String.valueOf(r.getError().getDetails() != null ? r.getError().getDetails() : r.getError().getDescription());
                    // a host that has not reconnected since the plugin was installed has no such path yet
                    String reason = error.contains("404") ? "the net-probe part on the host is not ready" : error;
                    tasks.forEach(t -> record(t.getUuid(), FAILED, false, null, null, null, reason));
                    return;
                }
                ProbeRsp rsp = ((KVMHostAsyncHttpCallReply) r).toResponse(ProbeRsp.class);
                if (!rsp.isSuccess() || rsp.results == null) {
                    tasks.forEach(t -> record(t.getUuid(), FAILED, false, null, null, null, rsp.getError()));
                    return;
                }
                for (ProbeResult res : rsp.results) {
                    record(res.taskUuid, res.success ? OK : FAILED, res.success, res.latencyMs, res.lossPercent,
                            res.httpStatus, res.error);
                }
            }
        });
    }

    private void record(String taskUuid, String status, boolean success, Double latencyMs, Double lossPercent,
                        Integer httpStatus, String error) {
        try {
            Timestamp now = dbf.getCurrentSqlTime();
            NetProbeResultVO res = new NetProbeResultVO();
            res.setTaskUuid(taskUuid);
            res.setRunAt(now);
            res.setSuccess(success);
            res.setLatencyMs(latencyMs);
            res.setLossPercent(lossPercent);
            res.setHttpStatus(httpStatus);
            res.setError(truncate(error));
            dbf.persist(res);
            SQL.New("update NetProbeTaskVO t set t.lastStatus = :status, t.lastLatencyMs = :latency, t.lastRunAt = :at,"
                    + " t.lastError = :error where t.uuid = :uuid")
                    .param("status", status).param("latency", latencyMs).param("at", now)
                    .param("error", truncate(error)).param("uuid", taskUuid).execute();
            pruneResults(taskUuid);
        } catch (Throwable t) {
            // the task may be deleted while its probe ran
            logger.warn(String.format("cannot record the result of probe task[uuid:%s]: %s", taskUuid, t.getMessage()));
        }
    }

    private static String truncate(String error) {
        return error == null || error.length() <= 1024 ? error : error.substring(0, 1024);
    }

    private void pruneResults(String taskUuid) {
        List<Long> ids = Q.New(NetProbeResultVO.class).select(NetProbeResultVO_.id)
                .eq(NetProbeResultVO_.taskUuid, taskUuid).listValues();
        if (ids.size() <= KEPT_RESULTS) {
            return;
        }
        ids.sort(Collections.reverseOrder());
        SQL.New("delete from NetProbeResultVO r where r.id in (:ids)")
                .param("ids", new ArrayList<>(ids.subList(KEPT_RESULTS, ids.size()))).execute();
    }

    @Override
    public List<String> inUseReasons() {
        long count = Q.New(NetProbeTaskVO.class).count();
        return count == 0 ? Collections.emptyList() : Collections.singletonList(String.format("%d probe tasks exist", count));
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        scheduler = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return CHECK_INTERVAL_SECONDS;
            }

            @Override
            public String getName() {
                return "net-probe-scheduler";
            }

            @Override
            public void run() {
                try {
                    runDueTasks();
                } catch (Throwable t) {
                    logger.warn(String.format("failed to run due probe tasks: %s", t.getMessage()), t);
                }
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        if (scheduler != null) {
            scheduler.cancel(true);
        }
        return true;
    }
}
