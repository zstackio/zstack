package org.zstack.rest;

import org.apache.commons.collections.map.LRUMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/12/8.
 */
public class MysqlAsyncRestStore implements AsyncRestApiStore, Component {
    private static final CLogger logger = Utils.getLogger(MysqlAsyncRestStore.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;

    // cache 2000 API results
    private Map<String, APIEvent> results = Collections.synchronizedMap(new LRUMap(RestGlobalProperty.MAX_CACHED_API_RESULTS));
    private Future cleanupThread;

    @Override
    public String save(APIMessage msg) {
        AsyncRestVO vo = new AsyncRestVO();
        vo.setUuid(msg.getId());
        vo.setApiMessage(JSONObjectUtil.toJsonString(map(e(msg.getClass().getName(), msg))));
        vo.setState(AsyncRestState.processing);
        dbf.persist(vo);

        return vo.getUuid();
    }

    @Override
    public void complete(APIEvent evt) {
        if (destinationMaker.isManagedByUs(evt.getApiId())) {
            UpdateQuery q = UpdateQuery.New();
            q.entity(AsyncRestVO.class);
            q.condAnd(AsyncRestVO_.uuid, SimpleQuery.Op.EQ, evt.getApiId());
            q.set(AsyncRestVO_.result, JSONObjectUtil.toJsonString(map(e(evt.getClass().getName(), evt))));
            q.set(AsyncRestVO_.state, AsyncRestState.done);
            q.update();
        }

        results.put(evt.getApiId(), evt);
    }

    @Override
    public AsyncRestQueryResult query(String uuid) {
        AsyncRestQueryResult result = new AsyncRestQueryResult();
        result.setUuid(uuid);

        APIEvent evt = results.get(uuid);
        if (evt != null) {
            result.setState(AsyncRestState.done);
            result.setResult(evt);
            return result;
        }

        AsyncRestVO vo = dbf.findByUuid(uuid, AsyncRestVO.class);
        if (vo == null) {
            result.setState(AsyncRestState.expired);
            return result;
        }

        if (vo.getState() != AsyncRestState.done) {
            result.setState(vo.getState());
            return result;
        }

        try {
            Map m = JSONObjectUtil.toObject(vo.getResult(), LinkedHashMap.class);
            String apiEventName = (String) m.keySet().iterator().next();
            Class<APIEvent> apiEventClass = (Class<APIEvent>) Class.forName(apiEventName);
            evt = JSONObjectUtil.rehashObject(m.get(apiEventName), apiEventClass);
            result.setState(AsyncRestState.done);
            result.setResult(evt);

            results.put(uuid, evt);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        return result;
    }

    @Override
    public boolean start() {
        startExpiredApiCleanupThread();
        RestGlobalConfig.SCAN_EXPIRED_API_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpiredApiCleanupThread();
            }
        });

        return true;
    }

    private void startExpiredApiCleanupThread() {
        if (cleanupThread != null) {
            cleanupThread.cancel(true);
        }

        cleanupThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return RestGlobalConfig.SCAN_EXPIRED_API_INTERVAL.value(Integer.class).longValue();
            }

            @Override
            public String getName() {
                return "scan-expired-api-records";
            }

            @Override
            public void run() {
                try {
                    cleanup();
                } catch (Throwable t) {
                    logger.warn("unhandled error", t);
                }
            }

            @Transactional
            private void cleanup() {
                String sql = "DELETE FROM AsyncRestVO vo WHERE vo.state = :state and vo.createDate < (NOW() - INTERVAL :period SECOND)";
                Query q = dbf.getEntityManager().createQuery(sql);
                q.setParameter("state", AsyncRestState.done);
                q.setParameter("period", RestGlobalConfig.COMPLETED_API_EXPIRED_PERIOD.value(Integer.class));
                q.executeUpdate();
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
