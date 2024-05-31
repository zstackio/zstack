package org.zstack.core.config;

import org.zstack.core.config.schema.GuestOsCharacter;

import java.util.ArrayList;
import java.util.List;

public class GuestOsCharacterInventory {
    private String architecture;
    private String platform;
    private String osRelease;
    private Boolean acpi;
    private Boolean hygonTag;
    private Boolean x2apic;
    private String cpuModel;
    private String nicDriver;

    public GuestOsCharacterInventory() {
    }

    public GuestOsCharacterInventory(GuestOsCharacter.Config config) {
        this.architecture = config.getArchitecture();
        this.platform = config.getPlatform();
        this.osRelease = config.getOsRelease();
        this.acpi = config.getAcpi();
        this.hygonTag = config.getHygonTag();
        this.x2apic = config.getX2apic();
        this.cpuModel = config.getCpuModel();
        this.nicDriver = config.getNicDriver();
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

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

    public void setAcpi(Boolean acpi) {
        this.acpi = acpi;
    }

    public Boolean getHygonTag() {
        return hygonTag;
    }

    public void setHygonTag(Boolean hygonTag) {
        this.hygonTag = hygonTag;
    }

    public Boolean getX2apic() {
        return x2apic;
    }

    public void setX2apic(Boolean x2apic) {
        this.x2apic = x2apic;
    }

    public String getCpuModel() {
        return cpuModel;
    }

    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    public String getNicDriver() {
        return nicDriver;
    }

    public void setNicDriver(String nicDriver) {
        this.nicDriver = nicDriver;
    }

    public static GuestOsCharacterInventory valueOf(GuestOsCharacter.Config config) {
        return new GuestOsCharacterInventory(config);
    }

    public static List<GuestOsCharacterInventory> valueOf(List<GuestOsCharacter.Config> configs) {
        List<GuestOsCharacterInventory> invs = new ArrayList<>();
        for (GuestOsCharacter.Config config : configs) {
            invs.add(valueOf(config));
        }
        return invs;
    }

    public static GuestOsCharacterInventory __example__() {
        GuestOsCharacterInventory inventory = new GuestOsCharacterInventory();
        inventory.setArchitecture("x86_64");
        inventory.setPlatform("Linux");
        inventory.setOsRelease("CentOS 7.0");
        inventory.setAcpi(true);
        inventory.setHygonTag(false);
        inventory.setX2apic(true);
        inventory.setCpuModel("Intel Xeon E5-2670");
        inventory.setNicDriver("e1000e");
        return inventory;
    }
}
