package org.zstack.test;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.appliancevm.ApplianceVmGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.kvm.KVMGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.storage.backup.sftp.SftpBackupStorageGlobalProperty;

import java.io.File;
import java.io.IOException;

public class WebBeanConstructor extends BeanConstructor {
    private Tomcat tomcat;
    private static final String BASE_DIR = "target/test-classes/tomcat";
    private static final String APP_NAME = "zstack";
    private int port = 8989;
    private UriComponentsBuilder ub;
    private String siteUrl;

    public WebBeanConstructor() {
        // initialize static block in Platform
        Platform.getUuid();
        KVMGlobalProperty.AGENT_PORT = port;
        VirtualRouterGlobalProperty.AGENT_PORT = port;
        SftpBackupStorageGlobalProperty.AGENT_PORT = port;
        ApplianceVmGlobalProperty.AGENT_PORT = port;
    }
    
    private void generateWarFile() {
        WarBuilder wbuilder = new WarBuilder();
        wbuilder.setSpringConfigPath(springConfigPath);
        wbuilder.setWarExportedToPath(BASE_DIR);
        wbuilder.build();
    }

    private void prepareTomcat() throws IOException {
        File dir = new File(BASE_DIR);
        FileUtils.deleteDirectory(dir);
        FileUtils.forceMkdir(dir);

        generateWarFile();
        tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(BASE_DIR);
        tomcat.getHost().setAppBase(BASE_DIR);
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);

        final Connector nioConnector = new Connector(Http11NioProtocol.class.getName());
        nioConnector.setPort(port);
        nioConnector.setSecure(false);
        nioConnector.setScheme("http");
        nioConnector.setProtocol("HTTP/1.1");
        try {
            //nioConnector.setProperty("address", InetAddress.getByName("localhost").getHostAddress());
            nioConnector.setProperty("address", "0.0.0.0");
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        tomcat.getService().removeConnector(tomcat.getConnector());
        tomcat.getService().addConnector(nioConnector);
        tomcat.setConnector(nioConnector);

        tomcat.addWebapp(tomcat.getHost(), "/", new File(BASE_DIR, APP_NAME + ".war").getAbsolutePath());

    }

    public void startTomcat() {
        try {
            prepareTomcat();
            tomcat.start();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void stopTomcat() {
        try {
            if (tomcat != null && tomcat.getServer() != null && tomcat.getServer().getState() != LifecycleState.DESTROYED) {
                if (tomcat.getServer().getState() != LifecycleState.STOPPED) {
                    tomcat.stop();
                }
                tomcat.destroy();
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
    
    @Override
    public ComponentLoader build() {
        generateSpringConfig();
        startTomcat();
        return Platform.getComponentLoader();
    }
    
    public String getSiteUrl() {
        if (siteUrl == null) {
            ub = UriComponentsBuilder.fromHttpUrl("http://localhost");
            ub.port(getPort());
            siteUrl = ub.build().toUriString();
        }
        return siteUrl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public String buildUrl(String...path) {
        UriComponentsBuilder ubb = UriComponentsBuilder.fromHttpUrl(getSiteUrl());
        for (String p : path) {
            ubb.path(p);
        }
        return ubb.build().toUriString();
    }
}
