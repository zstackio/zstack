import org.junit.Test
import org.zstack.testlib.ApiHelperGenerator

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 {

    @Test
    void test() {
        new ApiHelperGenerator().generate("/root/CreationSpec.groovy")
    }
}
