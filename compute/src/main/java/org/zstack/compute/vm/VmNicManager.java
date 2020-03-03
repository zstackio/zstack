package org.zstack.compute.vm;

import java.util.List;

public interface VmNicManager {

    List<String> getSupportNicDriverTypes();

    String getDefaultPVNicDriver();

    String getDefaultNicDriver();
}
