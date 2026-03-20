package org.zstack.header.host;

import java.io.Serializable;
import java.util.Objects;

public class HostBlockDeviceStruct implements Serializable {
    private String name;
    private String wwid;
    private String vendor;
    private String model;
    private String wwn;
    private String serial;
    private String hctl;
    private String type;
    private String path;
    private Long size;
    private String source;
    private String transport;
    private String targetIdentifier;

    public HostBlockDeviceStruct() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HostBlockDeviceStruct that = (HostBlockDeviceStruct) o;
        if (wwid != null) {
            return wwid.equals(that.wwid);
        }
        if (that.wwid != null) {
            return false;
        }
        return Objects.equals(path, that.path) && Objects.equals(hctl, that.hctl);


    }

    @Override
    public int hashCode() {
        return wwid != null ? wwid.hashCode() : Objects.hash(path, hctl);
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getWwid() {
        return wwid;
    }

    public void setWwid(String wwid) {
        this.wwid = wwid;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }
}
