import javassist.ClassPool
import javassist.CtClass
import org.zstack.header.core.workflow.Flow
import org.zstack.header.core.workflow.FlowRollback
import org.zstack.header.core.workflow.FlowTrigger

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 {

    @org.junit.Test
    void test() {
        ClassPool pool = ClassPool.getDefault()
        Flow flow = new Flow() {
            @Override
            void run(FlowTrigger trigger, Map data) {

            }

            @Override
            void rollback(FlowRollback trigger, Map data) {

            }
        }

        CtClass cc = pool.get(flow.class.name)
        cc.stopPruning(true)
        def m = cc.getDeclaredMethod("run")
        println("xxx ${m.getMethodInfo().getLineNumber(0)}")
    }
}
