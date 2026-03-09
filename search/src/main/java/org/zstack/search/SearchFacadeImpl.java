package org.zstack.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.search.SearchConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Hibernate Search 5 removed — incompatible with Jakarta namespace.
 * Full-text search is disabled until upgrade to Hibernate Search 7.x.
 * This stub keeps the service registered so the bus can route messages.
 */
public class SearchFacadeImpl extends AbstractService implements SearchFacade {
    private static CLogger logger = Utils.getLogger(SearchFacadeImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

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
        APIRefreshSearchIndexesReply reply = new APIRefreshSearchIndexesReply();
        logger.warn("Hibernate Search is disabled (pending upgrade to 7.x). Ignoring refresh request.");
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SearchConstant.SEARCH_FACADE_SERVICE_ID);
    }

    @Override
    public boolean start() {
        logger.info("SearchFacade started (Hibernate Search disabled — pending 7.x upgrade)");
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
