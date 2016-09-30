package org.zstack.core.timeout;

import java.util.Set;

/**
 * Created by frank on 2/18/2016.
 */
public class ApiTimeout {
    Set<Class> relatives;
    long timeout;

    public Set<Class> getRelatives() {
        return relatives;
    }

    public void setRelatives(Set<Class> relatives) {
        this.relatives = relatives;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
