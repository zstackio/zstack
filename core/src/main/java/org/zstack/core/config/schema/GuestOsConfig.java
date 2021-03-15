package org.zstack.core.config.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "osInfo"
})
@XmlRootElement(name = "category")
public class GuestOsConfig {
    protected List<Config> osInfo;

    public List<Config> getOsInfo() {
        if (osInfo == null) {
            osInfo = new ArrayList<Config>();
        }
        return this.osInfo;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "platform",
            "name",
            "version",
            "osRelease"
    })
    public static class Config {
        @XmlElement(required = true)
        protected String name;

        @XmlElement(required = true)
        protected String platform;

        @XmlElement(required = true)
        protected String version;

        @XmlElement(required = true)
        protected String osRelease;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOsRelease() {
            return osRelease;
        }

        public void setOsRelease(String osRelease) {
            this.osRelease = osRelease;
        }
    }
}
