package org.zstack.header.storage.addon.primary;

import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = ExternalPrimaryStorageVO.class)
public class ExternalPrimaryStorageInventory extends PrimaryStorageInventory {
    private String identity;

    /**
     * @example {
     * "config": "{
     * "pools": [
     * {
     * "name": "pool1",
     * "aliasName": "pool-high",
     * },
     * {
     * "name": "pool2",
     * }
     * ]
     * }
     */
    private LinkedHashMap config;

    /**
     * @example {
     * "addonInfo": {
     * "pools": [
     * {
     * "name": "pool1",
     * "availableCapacity": 100,
     * "totalCapacity": 200
     * },
     * {
     * "name": "pool2",
     * "availableCapacity": 100,
     * "totalCapacity": 200
     * }
     * ]
     * }
     */
    private LinkedHashMap addonInfo;

    private List<String> outputProtocols;

    private String defaultProtocol;

    public ExternalPrimaryStorageInventory() {
        super();
    }

    public ExternalPrimaryStorageInventory(ExternalPrimaryStorageVO lvo) {
        super(lvo);
        identity = lvo.getIdentity();
        config = JSONObjectUtil.toObject(lvo.getConfig(), LinkedHashMap.class);
        desensitizeConfig(config);
        addonInfo = JSONObjectUtil.toObject(lvo.getAddonInfo(), LinkedHashMap.class);
        outputProtocols = lvo.getOutputProtocols().stream().map(PrimaryStorageOutputProtocolRefVO::getOutputProtocol).collect(Collectors.toList());
        defaultProtocol = lvo.getDefaultProtocol();
    }

    public static ExternalPrimaryStorageInventory valueOf(ExternalPrimaryStorageVO lvo) {
        return new ExternalPrimaryStorageInventory(lvo);
    }

    private static void desensitizeConfig(Map config) {
        if (config == null) return;
        desensitizeUrlList(config, "mdsUrls");
        desensitizeUrlList(config, "mdsInfos");
    }

    private static void desensitizeUrlList(Map config, String key) {
        Object urls = config.get(key);
        if (urls instanceof List) {
            List<String> desensitized = new ArrayList<>();
            for (Object url : (List) urls) {
                desensitized.add(desensitizeUrl(String.valueOf(url)));
            }
            config.put(key, desensitized);
        }
    }

    private static String desensitizeUrl(String url) {
        int atIndex = url.lastIndexOf('@');
        if (atIndex > 0) {
            int schemeIndex = url.indexOf("://");
            if (schemeIndex >= 0 && schemeIndex < atIndex) {
                return url.substring(0, schemeIndex + 3) + "***" + url.substring(atIndex);
            }
            return "***" + url.substring(atIndex);
        }
        return url;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public LinkedHashMap getConfig() {
        return config;
    }

    public void setConfig(LinkedHashMap config) {
        this.config = config;
    }

    public List<String> getOutputProtocols() {
        return outputProtocols;
    }

    public void setOutputProtocols(List<String> outputProtocols) {
        this.outputProtocols = outputProtocols;
    }

    public String getDefaultProtocol() {
        return defaultProtocol;
    }

    public void setDefaultProtocol(String defaultProtocol) {
        this.defaultProtocol = defaultProtocol;
    }

    public LinkedHashMap getAddonInfo() {
        return addonInfo;
    }

    public void setAddonInfo(LinkedHashMap addonInfo) {
        this.addonInfo = addonInfo;
    }
}
