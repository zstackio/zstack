//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Lists;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class DeviceImage extends ApiObjectBase {
    private String device_image_file_name;
    private String device_image_vendor_name;
    private String device_image_device_family;
    private DevicePlatformListType device_image_supported_platforms;
    private String device_image_os_version;
    private String device_image_file_uri;
    private Integer device_image_size;
    private String device_image_md5;
    private String device_image_sha1;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> hardware_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;

    @Override
    public String getObjectType() {
        return "device-image";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config");
    }

    @Override
    public String getDefaultParentType() {
        return "global-system-config";
    }

    public void setParent(GlobalSystemConfig parent) {
        super.setParent(parent);
    }
    
    public String getFileName() {
        return device_image_file_name;
    }
    
    public void setFileName(String device_image_file_name) {
        this.device_image_file_name = device_image_file_name;
    }
    
    
    public String getVendorName() {
        return device_image_vendor_name;
    }
    
    public void setVendorName(String device_image_vendor_name) {
        this.device_image_vendor_name = device_image_vendor_name;
    }
    
    
    public String getDeviceFamily() {
        return device_image_device_family;
    }
    
    public void setDeviceFamily(String device_image_device_family) {
        this.device_image_device_family = device_image_device_family;
    }
    
    
    public DevicePlatformListType getSupportedPlatforms() {
        return device_image_supported_platforms;
    }
    
    public void setSupportedPlatforms(DevicePlatformListType device_image_supported_platforms) {
        this.device_image_supported_platforms = device_image_supported_platforms;
    }
    
    
    public String getOsVersion() {
        return device_image_os_version;
    }
    
    public void setOsVersion(String device_image_os_version) {
        this.device_image_os_version = device_image_os_version;
    }
    
    
    public String getFileUri() {
        return device_image_file_uri;
    }
    
    public void setFileUri(String device_image_file_uri) {
        this.device_image_file_uri = device_image_file_uri;
    }
    
    
    public Integer getSize() {
        return device_image_size;
    }
    
    public void setSize(Integer device_image_size) {
        this.device_image_size = device_image_size;
    }
    
    
    public String getMd5() {
        return device_image_md5;
    }
    
    public void setMd5(String device_image_md5) {
        this.device_image_md5 = device_image_md5;
    }
    
    
    public String getSha1() {
        return device_image_sha1;
    }
    
    public void setSha1(String device_image_sha1) {
        this.device_image_sha1 = device_image_sha1;
    }
    
    
    public IdPermsType getIdPerms() {
        return id_perms;
    }
    
    public void setIdPerms(IdPermsType id_perms) {
        this.id_perms = id_perms;
    }
    
    
    public PermType2 getPerms2() {
        return perms2;
    }
    
    public void setPerms2(PermType2 perms2) {
        this.perms2 = perms2;
    }
    
    
    public KeyValuePairs getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(KeyValuePairs annotations) {
        this.annotations = annotations;
    }
    
    
    public String getDisplayName() {
        return display_name;
    }
    
    public void setDisplayName(String display_name) {
        this.display_name = display_name;
    }
    

    public List<ObjectReference<ApiPropertyBase>> getHardware() {
        return hardware_refs;
    }

    public void setHardware(Hardware obj) {
        hardware_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        hardware_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addHardware(Hardware obj) {
        if (hardware_refs == null) {
            hardware_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        hardware_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeHardware(Hardware obj) {
        if (hardware_refs != null) {
            hardware_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearHardware() {
        if (hardware_refs != null) {
            hardware_refs.clear();
            return;
        }
        hardware_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getTag() {
        return tag_refs;
    }

    public void setTag(Tag obj) {
        tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addTag(Tag obj) {
        if (tag_refs == null) {
            tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeTag(Tag obj) {
        if (tag_refs != null) {
            tag_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearTag() {
        if (tag_refs != null) {
            tag_refs.clear();
            return;
        }
        tag_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }
}