//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SflowParameters extends ApiPropertyBase {
    StatsCollectionFrequency stats_collection_frequency;
    String agent_id;
    Integer adaptive_sample_rate;
    String enabled_interface_type;
    List<EnabledInterfaceParams> enabled_interface_params;
    public SflowParameters() {
    }
    public SflowParameters(StatsCollectionFrequency stats_collection_frequency, String agent_id, Integer adaptive_sample_rate, String enabled_interface_type, List<EnabledInterfaceParams> enabled_interface_params) {
        this.stats_collection_frequency = stats_collection_frequency;
        this.agent_id = agent_id;
        this.adaptive_sample_rate = adaptive_sample_rate;
        this.enabled_interface_type = enabled_interface_type;
        this.enabled_interface_params = enabled_interface_params;
    }
    public SflowParameters(StatsCollectionFrequency stats_collection_frequency) {
        this(stats_collection_frequency, null, 300, null, null);    }
    public SflowParameters(StatsCollectionFrequency stats_collection_frequency, String agent_id) {
        this(stats_collection_frequency, agent_id, 300, null, null);    }
    public SflowParameters(StatsCollectionFrequency stats_collection_frequency, String agent_id, Integer adaptive_sample_rate) {
        this(stats_collection_frequency, agent_id, adaptive_sample_rate, null, null);    }
    public SflowParameters(StatsCollectionFrequency stats_collection_frequency, String agent_id, Integer adaptive_sample_rate, String enabled_interface_type) {
        this(stats_collection_frequency, agent_id, adaptive_sample_rate, enabled_interface_type, null);    }
    
    public StatsCollectionFrequency getStatsCollectionFrequency() {
        return stats_collection_frequency;
    }
    
    public void setStatsCollectionFrequency(StatsCollectionFrequency stats_collection_frequency) {
        this.stats_collection_frequency = stats_collection_frequency;
    }
    
    
    public String getAgentId() {
        return agent_id;
    }
    
    public void setAgentId(String agent_id) {
        this.agent_id = agent_id;
    }
    
    
    public Integer getAdaptiveSampleRate() {
        return adaptive_sample_rate;
    }
    
    public void setAdaptiveSampleRate(Integer adaptive_sample_rate) {
        this.adaptive_sample_rate = adaptive_sample_rate;
    }
    
    
    public String getEnabledInterfaceType() {
        return enabled_interface_type;
    }
    
    public void setEnabledInterfaceType(String enabled_interface_type) {
        this.enabled_interface_type = enabled_interface_type;
    }
    
    
    public List<EnabledInterfaceParams> getEnabledInterfaceParams() {
        return enabled_interface_params;
    }
    
    
    public void addEnabledInterfaceParams(EnabledInterfaceParams obj) {
        if (enabled_interface_params == null) {
            enabled_interface_params = new ArrayList<EnabledInterfaceParams>();
        }
        enabled_interface_params.add(obj);
    }
    public void clearEnabledInterfaceParams() {
        enabled_interface_params = null;
    }
    
    
    public void addEnabledInterfaceParams(String name, StatsCollectionFrequency stats_collection_frequency) {
        if (enabled_interface_params == null) {
            enabled_interface_params = new ArrayList<EnabledInterfaceParams>();
        }
        enabled_interface_params.add(new EnabledInterfaceParams(name, stats_collection_frequency));
    }
    
}
