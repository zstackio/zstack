package org.zstack.kvm.memory;

import org.zstack.core.Platform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Central registry for stable preview warning codes and platform i18n keys. */
public final class MemoryPreviewWarnings {
    private static final Map<String, String> MESSAGE_KEYS;
    private static final Map<String, String> LEGACY_MESSAGES;

    static {
        Map<String, String> keys = new HashMap<>();
        keys.put("VM_EXCLUSION_GLOBAL_SWAP", "memory.preview.warning.vmExclusionGlobalSwap");
        keys.put("HOST_REVALIDATED_BEFORE_APPLY", "memory.preview.warning.hostRevalidatedBeforeApply");
        keys.put("CLOUD_LICENSE_REQUIRED", "memory.preview.warning.cloudLicenseRequired");
        keys.put("ZRAM_CAPACITY_UNVERIFIED", "memory.preview.warning.zramCapacityUnverified");
        MESSAGE_KEYS = Collections.unmodifiableMap(keys);

        Map<String, String> legacy = new HashMap<>();
        legacy.put("VM_EXCLUSION_GLOBAL_SWAP", "VM exclusion limits controlled reclamation; it does not disable kernel global swap.");
        legacy.put("HOST_REVALIDATED_BEFORE_APPLY", "Host ABI, current capacity and effective revision are rechecked before execution.");
        legacy.put("CLOUD_LICENSE_REQUIRED", "Cloud License does not permit configuration changes.");
        legacy.put("ZRAM_CAPACITY_UNVERIFIED", "Host capacity defaults could not be verified; provide explicit validated capacity before enabling ZRAM.");
        LEGACY_MESSAGES = Collections.unmodifiableMap(legacy);
    }

    private MemoryPreviewWarnings() { }

    public static MemoryPreviewWarningInventory create(String code) {
        String key = keyFor(code);
        MemoryPreviewWarningInventory warning = new MemoryPreviewWarningInventory();
        warning.setCode(code);
        warning.setMessageKey(key);
        warning.setFormatArgs(Collections.emptyList());
        warning.setMessage(Platform.toI18nString(key));
        return warning;
    }

    public static MemoryPreviewWarningInventory create(String code, Locale locale) {
        String key = keyFor(code);
        MemoryPreviewWarningInventory warning = new MemoryPreviewWarningInventory();
        warning.setCode(code);
        warning.setMessageKey(key);
        warning.setFormatArgs(Collections.emptyList());
        warning.setMessage(Platform.toI18nString(key, locale));
        return warning;
    }

    public static String legacyMessage(String code) {
        keyFor(code);
        return LEGACY_MESSAGES.get(code);
    }

    private static String keyFor(String code) {
        String key = MESSAGE_KEYS.get(code);
        if (key == null) {
            throw new IllegalArgumentException("Unknown memory preview warning code[" + code + "]");
        }
        return key;
    }
}
