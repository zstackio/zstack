package org.zstack.test.integration.core

import org.zstack.core.asyncbatch.While
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.workflow.WhileCompletion
import org.zstack.testlib.SubCase

/**
 * Created by Administrator on 2017-05-02.
 */
class WhileCase extends SubCase{
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
        int a = 0
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
}
