package org.zstack.zql;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;

import javax.management.MXBean;
import java.util.Map;

@MXBean
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZQLJMX implements ZQLMXBean {
    @Override
    public Map<String, ZQLStatistic> getZQLStatistic() {
        return ZQL.getZQLStatistic();
    }
}
