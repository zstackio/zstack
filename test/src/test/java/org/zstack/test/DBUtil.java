package org.zstack.test;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Properties;

public class DBUtil {
    private static final CLogger logger = Utils.getLogger(DBUtil.class);
    
    public static void reDeployDB() {
        logger.info("Deploying database ...");
        String home = System.getProperty("user.dir");
        String baseDir = Utils.getPathUtil().join(home, "../");
        Properties prop = new Properties();
        
        try {
            prop.load(DBUtil.class.getClassLoader().getResourceAsStream("zstack.properties"));
            String shellcmd = String.format("build/deploydb.sh %s %s", prop.getProperty("DbFacadeDataSource.user"), prop.getProperty("DbFacadeDataSource.password"));
            ShellUtils.run(shellcmd, baseDir);
            logger.info("Deploying database successfully");
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy zstack database for testing", e);
        }
    }
}

