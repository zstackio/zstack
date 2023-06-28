//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class TimerType extends ApiPropertyBase {
    volatile java.util.Date start_time;
    Long on_interval;
    Long off_interval;
    volatile java.util.Date end_time;
    public TimerType() {
    }
    public TimerType(java.util.Date start_time, Long on_interval, Long off_interval, java.util.Date end_time) {
        this.start_time = start_time;
        this.on_interval = on_interval;
        this.off_interval = off_interval;
        this.end_time = end_time;
    }
    public TimerType(java.util.Date start_time) {
        this(start_time, null, null, null);    }
    public TimerType(java.util.Date start_time, Long on_interval) {
        this(start_time, on_interval, null, null);    }
    public TimerType(java.util.Date start_time, Long on_interval, Long off_interval) {
        this(start_time, on_interval, off_interval, null);    }
    
    public java.util.Date getStartTime() {
        return start_time;
    }
    
    public void setStartTime(java.util.Date start_time) {
        this.start_time = start_time;
    }
    
    
    public Long getOnInterval() {
        return on_interval;
    }
    
    public void setOnInterval(Long on_interval) {
        this.on_interval = on_interval;
    }
    
    
    public Long getOffInterval() {
        return off_interval;
    }
    
    public void setOffInterval(Long off_interval) {
        this.off_interval = off_interval;
    }
    
    
    public java.util.Date getEndTime() {
        return end_time;
    }
    
    public void setEndTime(java.util.Date end_time) {
        this.end_time = end_time;
    }
    
}
