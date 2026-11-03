package org.zstack.search;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.search.SearchErrorHandler;
import org.zstack.core.search.SearchGlobalProperty;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.search.SearchConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.stopwatch.StopWatch;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:20 2020/11/20
 */
public class SearchFacadeImpl extends AbstractService implements SearchFacade {
    private static CLogger logger = Utils.getLogger(SearchFacadeImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

    private Future<Void> recoveryTask;
    private boolean indexesBuiltOnVip;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIRefreshSearchIndexesMsg) {
            handle((APIRefreshSearchIndexesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRefreshSearchIndexesMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                APIRefreshSearchIndexesReply reply = new APIRefreshSearchIndexesReply();
                refreshSearchIndexes(false);
                bus.reply(msg, reply);
                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return "refresh-search-indexs";
            }

            @Override
            public int getSyncLevel() {
                return 5;
            }
        });
    }

    @Transactional(readOnly = true)
    private boolean createSearchIndexes() {
        try {
            if (!Platform.isVIPNode()) {
                logger.info("current managementNode is not vip node, skip refresh search indexes");
                return false;
            }

            if (!SearchGlobalProperty.SearchAutoRegister) {
                logger.info("search module has been disabled, skip refresh search indexes");
                return false;
            }

            StopWatch watch = Utils.getStopWatch();
            watch.start();
            logger.info("start refresh search indexes");
            FullTextEntityManager textEntityManager = getFullTextEntityManager();
            long errorsBeforeIndexing = SearchErrorHandler.getErrorCount();
            //required typesToIndexInParallel * (threadsToLoadObjects + 1) jdbc connections
            textEntityManager.createIndexer()
                    //.typesToIndexInParallel(2), default 1
                    .batchSizeToLoadObjects(SearchGlobalProperty.massIndexerBatchSizeToLoadObjects)
                    .threadsToLoadObjects(SearchGlobalProperty.massIndexerThreadsToLoadObjects)
                    .progressMonitor(new MassIndexerProgressMonitor() {
                        @Override
                        public void documentsBuilt(int number) {

                        }

                        @Override
                        public void entitiesLoaded(int size) {

                        }

                        @Override
                        public void addToTotalCount(long count) {
                            logger.debug(String.format("indexing is going to fetch %d primary keys", count));
                        }

                        @Override
                        public void indexingCompleted() {
                            logger.debug("indexing completed");
                        }

                        @Override
                        public void documentsAdded(long increment) {
                            
                        }
                    })
                    .idFetchSize(150)
                    .startAndWait();
            watch.stop();
            if (SearchErrorHandler.getErrorCount() != errorsBeforeIndexing) {
                logger.warn("refresh search indexes failed: Hibernate Search reported indexing errors");
                return false;
            }
            logger.info(String.format("refresh search indexes success, cost %d ms", watch.getLapse()));
            return true;
        } catch (Throwable e) {
            logger.warn("a unhandled exception happened", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void refreshSearchIndexes(boolean onlyOnVipChange) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public String getName() {
                return "refresh-search-indexes";
            }

            @Override
            public Void call() throws Exception {
                if (!Platform.isVIPNode()) {
                    indexesBuiltOnVip = false;
                    return null;
                }
                if (SearchGlobalProperty.SearchAutoRegister && (!onlyOnVipChange || !indexesBuiltOnVip)) {
                    indexesBuiltOnVip = createSearchIndexes();
                }
                return null;
            }

            @Override
            public String getSyncSignature() {
                return "refresh-search-indexex";
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SearchConstant.SEARCH_FACADE_SERVICE_ID);
    }

    @Override
    public boolean start() {
        refreshSearchIndexes(false);
        recoveryTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 30;
            }

            @Override
            public String getName() {
                return "recover-search-indexes-on-vip";
            }

            @Override
            public void run() {
                refreshSearchIndexes(true);
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        if (recoveryTask != null) {
            recoveryTask.cancel(true);
        }
        return true;
    }

    @Override
    public FullTextEntityManager getFullTextEntityManager() {
        return Search.getFullTextEntityManager(dbf.getEntityManager());
    }
}
