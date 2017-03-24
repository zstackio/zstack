package org.zstack.network.l2.vxlan.vtep;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;

/**
 * Created by weiwang on 06/03/2017.
 */
public interface Vtep {
    void setup(VtepVO vo);


}
