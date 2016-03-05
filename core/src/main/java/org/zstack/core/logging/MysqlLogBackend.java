package org.zstack.core.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 */
public class MysqlLogBackend implements LogBackend {
    private static final CLogger logger = Utils.getLogger(MysqlLogBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private volatile boolean isStarted;
    private BlockingQueue queue = new LinkedBlockingQueue();
    private QuitToken quitToken =new QuitToken();

    class QuitToken {
    }

    @Override
    public void write(LogVO log) {
        if (!isStarted) {
            return;
        }

        try {
            queue.offer(log, 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn(String.format("unable to write log %s", JSONObjectUtil.toJsonString(log)), e);
        }
    }

    @Override
    public String getLogBackendType() {
        return LogConstant.MYSQL_BACKEND_TYPE;
    }

    @Transactional
    private void batchWrite(List lst) {
        for (Object obj : lst) {
            LogVO vo = (LogVO) obj;
            dbf.getEntityManager().persist(vo);
        }
    }

    private void consume() throws InterruptedException {
        while (true) {
            List lst = new ArrayList();
            lst.add(queue.take());
            queue.drainTo(lst);
            if (lst.contains(quitToken)) {
                List ret = CollectionUtils.transformToList(lst, new Function() {
                    @Override
                    public Object call(Object arg) {
                        return arg instanceof LogVO ? arg : null;
                    }
                });
                batchWrite(ret);
                return;
            }

            batchWrite(lst);
        }
    }

    private void startLogThread() {
        thdf.submit(new Task<Void>() {
            @Override
            public String getName() {
                return "log-thread";
            }

            @Override
            public Void call() throws Exception {
                consume();
                return null;
            }
        });
    }

    @Override
    public void start() {
        isStarted = true;
        startLogThread();
    }

    private void quit() {
        try {
            queue.offer(quitToken, 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        isStarted = false;
        quit();
    }
}
