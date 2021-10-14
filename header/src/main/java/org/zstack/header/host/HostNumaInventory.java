package org.zstack.header.host;


import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@PythonClassInventory
public class HostNumaInventory implements Serializable {
    private String name;
    private String uuid;
    private Map<String, Map<String, Object>> topology;


    public HostNumaInventory() {}

    public HostNumaInventory(HostNumaInventory numa) {
        this.name = numa.name;
        this.uuid = numa.uuid;
        this.topology = numa.topology;
    }

    public static HostNumaInventory valueOf(HostNumaInventory numa) {
        return new HostNumaInventory(numa);
    }

    public static List<HostNumaInventory> valueOf(Collection<HostNumaInventory> numas) {
        List<HostNumaInventory> invs = new ArrayList<HostNumaInventory>(numas.size());
        for (HostNumaInventory numa : invs) {
            invs.add(HostNumaInventory.valueOf(numa));
        }
        return invs;
    }

    public HostNumaInventory(Map<String, Map<String, Object>> numa ) {
        this.topology = numa;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTopology(Map<String, Map<String, Object>> topology) {
        this.topology = topology;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, Map<String, Object>> getTopology() {
        return topology;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }
}
