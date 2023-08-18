//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class StatsCollectionFrequency extends ApiPropertyBase {
    Integer sample_rate;
    Integer polling_interval;
    String direction;
    public StatsCollectionFrequency() {
    }
    public StatsCollectionFrequency(Integer sample_rate, Integer polling_interval, String direction) {
        this.sample_rate = sample_rate;
        this.polling_interval = polling_interval;
        this.direction = direction;
    }
    public StatsCollectionFrequency(Integer sample_rate) {
        this(sample_rate, 0, null);    }
    public StatsCollectionFrequency(Integer sample_rate, Integer polling_interval) {
        this(sample_rate, polling_interval, null);    }
    
    public Integer getSampleRate() {
        return sample_rate;
    }
    
    public void setSampleRate(Integer sample_rate) {
        this.sample_rate = sample_rate;
    }
    
    
    public Integer getPollingInterval() {
        return polling_interval;
    }
    
    public void setPollingInterval(Integer polling_interval) {
        this.polling_interval = polling_interval;
    }
    
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
}
