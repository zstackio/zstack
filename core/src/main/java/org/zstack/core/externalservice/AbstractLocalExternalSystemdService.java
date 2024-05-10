package org.zstack.core.externalservice;

import org.zstack.utils.Bash;

public abstract class AbstractLocalExternalSystemdService extends AbstractLocalExternalService{
    abstract public String getSystemdServiceName();

    public void sysctl(String ctl) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();
                run("systemctl %s %s", ctl, getSystemdServiceName());
            }
        }.execute();
    }
}
