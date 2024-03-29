package org.zstack.core.config.schema;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "osInfo"
})
@XmlRootElement(name = "character")
public class GuestOsCharacter {
    protected List<GuestOsCharacter.Config> osInfo;

    public List<Config> getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(List<Config> osInfo) {
        this.osInfo = osInfo;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "architecture",
            "platform",
            "osRelease",
            "acpi",
            "x2apic"
    })
    public static class Config {
        @XmlElement(required = true)
        protected String architecture;

        @XmlElement(required = true)
        protected String platform;

        @XmlElement(required = true)
        protected String osRelease;

        @XmlElement(required = false)
        protected Boolean acpi;

        @XmlElement(required = false)
        protected Boolean x2apic;

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getOsRelease() {
            return osRelease;
        }

        public void setOsRelease(String osRelease) {
            this.osRelease = osRelease;
        }

        public Boolean getAcpi() {
            return acpi;
        }

        public Boolean getX2apic() {
            return x2apic;
        }

        public void setAcpi(Boolean acpi) {
            this.acpi = acpi;
        }

        public void setX2apic(Boolean x2apic) {
            this.x2apic = x2apic;
        }

        public String getArchitecture() {
            return architecture;
        }

        public void setArchitecture(String architecture) {
            this.architecture = architecture;
        }
    }
}
