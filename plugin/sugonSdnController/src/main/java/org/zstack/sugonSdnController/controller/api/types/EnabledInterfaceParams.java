//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class EnabledInterfaceParams extends ApiPropertyBase {
    String name;
    StatsCollectionFrequency stats_collection_frequency;
    public EnabledInterfaceParams() {
    }
    public EnabledInterfaceParams(String name, StatsCollectionFrequency stats_collection_frequency) {
        this.name = name;
        this.stats_collection_frequency = stats_collection_frequency;
    }
    public EnabledInterfaceParams(String name) {
        this(name, null);    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    
    public StatsCollectionFrequency getStatsCollectionFrequency() {
        return stats_collection_frequency;
    }
    
    public void setStatsCollectionFrequency(StatsCollectionFrequency stats_collection_frequency) {
        this.stats_collection_frequency = stats_collection_frequency;
    }
    
}
