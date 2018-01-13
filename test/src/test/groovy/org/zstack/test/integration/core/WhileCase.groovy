package org.zstack.test.integration.core

import org.zstack.core.asyncbatch.While
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.workflow.WhileCompletion
import org.zstack.test.core.rest.RESTBeanForTest
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by MaJin on 2017-05-02.
 */
class WhileCase extends SubCase{
    private static final CLogger logger = Utils.getLogger(WhileCase.class)

    @Override
    void clean() {

    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        testRunAllWhenItemsEmpty()
        testRunAllCompletionAllDone()
        testRunStepWhenItemsEmpty()
        testRunStepCompletionDone()
        testRunStepComletionAllDone()
    }

    static void testRunAllWhenItemsEmpty(){
        FutureCompletion future = new FutureCompletion(null)

        new While<>(new ArrayList<String>()).all(new While.Do<String>() {
            @Override
            void accept(String item, WhileCompletion completion) {
                completion.done()
            }
        }).run(new NoErrorCompletion(){

            @Override
            void done() {
                future.success()
            }
        })

        future.await(10)

        assert future.success
    }

    static void testRunAllCompletionAllDone(){
        FutureCompletion future = new FutureCompletion(null)
        AtomicInteger count = new AtomicInteger(0)

        new While<>(["1", "2"]).all({item, completion ->
            logger.debug(String.format("item %s allDone", item))
            completion.allDone()
        }).run(new NoErrorCompletion(){
            @Override
            void done() {
                count.addAndGet(1)
                logger.debug("While is done")
                future.success()
            }
        })

        future.await(10)
        assert future.success
        assert count.get() == 1
    }

    static void testRunStepWhenItemsEmpty(){
        FutureCompletion future = new FutureCompletion(null)

        new While<>(new ArrayList<String>()).step({item, completion ->
            completion.done()
        }, 1).run(new NoErrorCompletion(){
            @Override
            void done() {
                future.success()
            }
        })

        future.await(10)
        assert future.success
    }

    static void testRunStepCompletionDone(){
        FutureCompletion future = new FutureCompletion(null)
        AtomicInteger count = new AtomicInteger(0)

        new While<>(["1", "2"]).step({item, completion ->
            logger.debug("step " + item)
            completion.done()
        }, 2).run(new NoErrorCompletion(){
            @Override
            void done() {
                count.addAndGet(1)
                logger.debug("While is done")
                future.success()
            }
        })

        future.await(10)
        assert future.success
        assert count.get() == 1
    }

    static void testRunStepComletionAllDone(){
        FutureCompletion future = new FutureCompletion(null)
        AtomicInteger count = new AtomicInteger(0)

        new While<>(["1", "2"]).step({item, completion ->
            logger.debug(String.format("step %s allDone", item))
            completion.allDone()
        }, 2).run(new NoErrorCompletion(){
            @Override
            void done() {
                count.addAndGet(1)
                logger.debug("While is done")
                future.success()
            }
        })

        future.await(10)
        assert future.success
        assert count.get() == 1
    }
}
