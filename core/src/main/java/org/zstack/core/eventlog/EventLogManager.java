package org.zstack.core.eventlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventLogManager implements EventLogger, Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(EventLogManager.class);

    private final BlockingQueue<EventLogBuilder> eventLogQueue = new LinkedBlockingQueue<>();
    private boolean running = true;

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void accept(EventLogBuilder builder) {
        try {
            eventLogQueue.offer(builder, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn(String.format("unable to consume event log: %s", JSONObjectUtil.toJsonString(builder)), e);
        }
    }

    @ExceptionSafe
    private void writeEventLogsToDB() {
        List<EventLogBuilder> builders = new ArrayList<>();
        eventLogQueue.drainTo(builders);

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (EventLogBuilder builder: builders) {
                    EventLogVO vo = new EventLogVO();
                    vo.setTime(System.currentTimeMillis());
                    vo.setCategory(builder.category);
                    vo.setResourceUuid(builder.resourceUuid);
                    vo.setResourceType(builder.resourceType);
                    vo.setTrackingId(builder.trackingId);
                    vo.setContent(String.format(builder.content, builder.arguments.toArray()));
                    vo.setType(builder.type);
                    dbf.getEntityManager().persist(vo);
                }
            }
        }.execute();
    }

    private void start_consumer() {
        thdf.submit(new Task<Void>() {
            @Override
            public String getName() {
                return "event-log consumer";
            }

            @Override
            public Void call() {
                while (running) {
                    writeEventLogsToDB();
                }
                return null;
            }
        });
    }

    private void stop_consumer() {
        running = false;
    }

    private void start_log_cleaner() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.HOURS;
            }

            @Override
            public long getInterval() {
                return 1;
            }

            @Override
            public String getName() {
                return "clean-expired-log-entries";
            }

            @Override
            @ExceptionSafe
            public void run() {
                cleanup();
            }

            private Timestamp getCurrentSqlTime() {
                Query q = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) q.getSingleResult();
            }

            @Transactional
            private void cleanup() {
                Integer expireInDay = EventLogGlobalConfig.EXPIRE_TIME_IN_DAY.value(Integer.class);
                if (expireInDay == null  || expireInDay == 0) {
                    return;
                }

                long milliseconds = TimeUnit.DAYS.toMillis(expireInDay);
                Timestamp deadline = new Timestamp(getCurrentSqlTime().getTime() - milliseconds);
                UpdateQuery.New(EventLogVO.class)
                        .lt(EventLogVO_.createDate, deadline)
                        .hardDelete();
            }
        });
    }

    @Override
    public void managementNodeReady() {
        start_log_cleaner();
    }

    @Override
    public boolean start() {
        start_consumer();
        return true;
    }

    @Override
    public boolean stop() {
        stop_consumer();
        return true;
    }
}
