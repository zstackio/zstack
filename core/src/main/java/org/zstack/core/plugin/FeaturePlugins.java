package org.zstack.core.plugin;

import org.apache.commons.io.FileUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.external.plugin.FeaturePluginInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.YamlUtils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The feature plugins installed on this management node, from the records zstack-ctl keeps outside the war in
 * &lt;dataDir&gt;/feature-plugins/records, and how loading them went.
 */
public class FeaturePlugins {
    private static final CLogger logger = Utils.getLogger(FeaturePlugins.class);
    private static final String ROOT = "feature-plugins";

    private static List<FeaturePluginInventory> records;
    // ids of the recorded plugins with a management node part; drivers and agent parts have nothing to load here
    private static volatile Set<String> mnParts = Collections.emptySet();
    // plugin id -> {status, error} of loading it; a record's own status (left by zstack-ctl) comes first
    private static final Map<String, String[]> states = new ConcurrentHashMap<>();
    // class loader -> id of the plugin loaded by it
    private static final Map<ClassLoader, String> classLoaders = new ConcurrentHashMap<>();

    public static File recordDir() {
        return Paths.get(CoreGlobalProperty.DATA_DIR, ROOT, "records").toFile();
    }

    /**
     * The folder zstack-ctl lays the management node part of a plugin package out in.
     */
    public static File pluginDir(String id, String sha256) {
        return Paths.get(CoreGlobalProperty.DATA_DIR, ROOT, "mn", id, sha256.substring(0, 12)).toFile();
    }

    public static synchronized List<FeaturePluginInventory> installed() {
        if (records == null) {
            records = load();
        }
        List<FeaturePluginInventory> ret = new ArrayList<>();
        for (FeaturePluginInventory r : records) {
            FeaturePluginInventory inv = new FeaturePluginInventory();
            inv.setId(r.getId());
            inv.setName(r.getName());
            inv.setCategory(r.getCategory());
            inv.setVersion(r.getVersion());
            inv.setSha256(r.getSha256());
            inv.setLicenseAddon(r.getLicenseAddon());
            inv.setStatus(r.getStatus());
            inv.setError(r.getError());
            String[] state = states.get(r.getId());
            if (inv.getStatus() == null && state != null) {
                inv.setStatus(state[0]);
                inv.setError(state[1]);
            }
            ret.add(inv);
        }
        return ret;
    }

    /**
     * Reads the records again after zstack-ctl changed them.
     */
    public static synchronized void refresh() {
        records = load();
    }

    /**
     * The records as zstack-ctl wrote them; a status here is zstack-ctl's (the plugin has no files to load).
     */
    static synchronized List<FeaturePluginInventory> records() {
        if (records == null) {
            records = load();
        }
        return new ArrayList<>(records);
    }

    static boolean hasMnPart(String id) {
        return mnParts.contains(id);
    }

    static void setState(String id, String status, String error) {
        states.put(id, new String[]{status, error});
    }

    static void clearState(String id) {
        states.remove(id);
    }

    /**
     * @param loaded plugin id -> the class loader the plugin is loaded by now
     */
    static void setClassLoaders(Map<String, ClassLoader> loaded) {
        classLoaders.clear();
        loaded.forEach((id, cl) -> classLoaders.put(cl, id));
    }

    /**
     * @return the id of the feature plugin whose class loader defined the class, null for a platform class
     */
    public static String pluginIdOf(Class<?> clz) {
        ClassLoader cl = clz.getClassLoader();
        return cl == null ? null : classLoaders.get(cl);
    }

    private static List<FeaturePluginInventory> load() {
        List<FeaturePluginInventory> ret = new ArrayList<>();
        Set<String> withMn = new HashSet<>();
        File[] files = recordDir().listFiles((d, n) -> n.endsWith(".yaml"));
        if (files != null) {
            for (File f : files) {
                ret.add(loadRecord(f, withMn));
            }
        }
        ret.sort(Comparator.comparing(FeaturePluginInventory::getId));
        mnParts = withMn;
        return ret;
    }

    @SuppressWarnings("unchecked")
    private static FeaturePluginInventory loadRecord(File file, Set<String> withMn) {
        FeaturePluginInventory inv = new FeaturePluginInventory();
        inv.setId(file.getName().replaceAll("\\.yaml$", ""));
        try {
            Map<String, Object> desc = (Map<String, Object>) YamlUtils.load(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            inv.setId(String.valueOf(desc.get("id")));
            inv.setName(String.valueOf(desc.get("name")));
            inv.setCategory(String.valueOf(desc.get("category")));
            inv.setVersion(String.valueOf(desc.get("version")));
            inv.setSha256((String) desc.get("sha256"));
            if (desc.get("mn") != null) {
                withMn.add(inv.getId());
            }
            Map<String, Object> license = (Map<String, Object>) desc.get("license");
            inv.setLicenseAddon(license == null ? null : (String) license.get("addon"));

            // zstack-ctl leaves a record without files when a plugin cannot be brought to its desired state
            Map<String, Object> skip = (Map<String, Object>) desc.get("skip");
            if (skip != null) {
                inv.setStatus(String.valueOf(skip.get("status")));
                inv.setError(String.valueOf(skip.get("reason")));
            }
        } catch (Throwable t) {
            inv.setStatus(FeaturePluginInventory.STATUS_FAILED);
            inv.setError(t.toString());
            logger.warn(String.format("failed to read the record of feature plugin %s", file.getAbsolutePath()), t);
        }
        return inv;
    }
}
