import org.junit.Test
import org.zstack.testlib.ApiHelperGenerator
import org.zstack.testlib.EnvDSLDocGenerator

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 {

    @Test
    void test() {
        //new EnvDSLDocGenerator().generate()
        new ApiHelperGenerator().generate("/root/ApiHelper.groovy")
    }
}
