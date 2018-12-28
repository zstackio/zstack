import org.junit.Test
import org.zstack.rest.RestServer

import java.nio.file.Paths

/**
 * Created by mingjian.deng on 2018/12/28.*/
class TestGenerateErrorCodeDoc {
    @Test
    void test() {
        RestServer.generateErrorCodeDoc(Paths.get("src/test/resources/elaborations/").toAbsolutePath().normalize().toString())
    }
}
