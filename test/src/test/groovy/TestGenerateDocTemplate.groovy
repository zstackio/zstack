import org.junit.Test
import org.zstack.rest.RestServer
import org.zstack.rest.sdk.DocumentGenerator

import java.nio.file.Paths

/**
 * Created by xing5 on 2016/12/21.
 */
class TestGenerateDocTemplate {

    @Test
    void test() {
        if (System.getProperty("re") != null) {
            RestServer.generateDocTemplate(Paths.get("../").toAbsolutePath().normalize().toString(), DocumentGenerator.DocMode.RECREATE_ALL)
        } else {
            RestServer.generateDocTemplate(Paths.get("../").toAbsolutePath().normalize().toString(), DocumentGenerator.DocMode.CREATE_MISSING)
        }
    }
}
