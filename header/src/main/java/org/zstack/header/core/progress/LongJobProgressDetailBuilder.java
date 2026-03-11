package org.zstack.header.core.progress;

import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses TaskProgressVO.opaque (free-form JSON) into a typed LongJobProgressDetail.
 *
 * Three known opaque formats are handled:
 *   Format 1 — VM migration:   {"remain":N, "total":N, "speed":N, "remaining_migration_time":N}
 *   Format 2 — AI download:    {"data": "<JSON string>"}  where the inner JSON has
 *                               {state, progress:{percent,downloaded_bytes,total_bytes,
 *                                speed_bytes_per_second,estimated_remaining_seconds,
 *                                downloaded_files,total_files,stage}, state_reason}
 *   Format 3 — unknown:        entire map goes into LongJobProgressDetail.extra
 *
 * Each format is tried independently. Failures in one format don't affect others.
 */
public class LongJobProgressDetailBuilder {
    private static final CLogger logger = Utils.getLogger(LongJobProgressDetailBuilder.class);

    private LongJobProgressDetailBuilder() {}

    /**
     * Build a LongJobProgressDetail from a TaskProgressVO.
     * Returns null if opaque is null/empty or all parsers fail.
     */
    public static LongJobProgressDetail fromTaskProgressVO(TaskProgressVO vo) {
        if (vo == null || vo.getOpaque() == null || vo.getOpaque().isEmpty()) {
            return null;
        }

        Map<String, Object> raw;
        try {
            raw = JSONObjectUtil.toObject(vo.getOpaque(), HashMap.class);
        } catch (Exception e) {
            logger.trace("LongJobProgressDetailBuilder: opaque is not a JSON object, skipping: " + vo.getOpaque(), e);
            return null;
        }

        if (raw == null || raw.isEmpty()) {
            return null;
        }

        // Try Format 2 first: AI download wraps everything under "data" key
        if (raw.containsKey("data")) {
            LongJobProgressDetail detail = tryParseAiDownloadFormat(raw);
            if (detail != null) {
                return detail;
            }
        }

        // Try Format 1: VM migration with remain/total/speed keys
        if (raw.containsKey("remain") && raw.containsKey("total")) {
            LongJobProgressDetail detail = tryParseVmMigrationFormat(raw);
            if (detail != null) {
                return detail;
            }
        }

        // Format 3: unknown — put everything into extra
        return parseAsExtra(raw);
    }

    /**
     * Format 1: VM migration opaque
     * {"remain": 1234567, "total": 9999999, "speed": 102400, "remaining_migration_time": 30}
     * remain = bytes still to transfer; processed = total - remain
     */
    private static LongJobProgressDetail tryParseVmMigrationFormat(Map<String, Object> raw) {
        try {
            LongJobProgressDetail detail = new LongJobProgressDetail();
            detail.setStage("migrating");

            Number total = toNumber(raw.get("total"));
            Number remain = toNumber(raw.get("remain"));
            Number speed = toNumber(raw.get("speed"));
            Number remainingTime = toNumber(raw.get("remaining_migration_time"));

            if (total != null) {
                detail.setTotalBytes(total.longValue());
            }
            if (total != null && remain != null) {
                long processed = Math.max(0L, total.longValue() - remain.longValue());
                detail.setProcessedBytes(processed);
                if (total.longValue() > 0) {
                    detail.setPercent((int) Math.min(100, Math.round(processed * 100.0 / total.longValue())));
                }
            }
            if (speed != null) {
                detail.setSpeedBytesPerSecond(speed.longValue());
            }
            if (remainingTime != null) {
                detail.setEstimatedRemainingSeconds(remainingTime.longValue());
            }

            // Carry over any unrecognized keys into extra
            Map<String, Object> extra = new HashMap<>(raw);
            extra.remove("remain");
            extra.remove("total");
            extra.remove("speed");
            extra.remove("remaining_migration_time");
            if (!extra.isEmpty()) {
                detail.setExtra(extra);
            }

            return detail;
        } catch (Exception e) {
            logger.trace("LongJobProgressDetailBuilder: failed to parse VM migration format", e);
            return null;
        }
    }

    /**
     * Format 2: AI download opaque
     * {"data": "{\"state\":\"downloading\", \"progress\":{\"percent\":42, \"processedBytes\":N, ...}}"}
     * The "data" value is a JSON string (double-encoded).
     */
    private static LongJobProgressDetail tryParseAiDownloadFormat(Map<String, Object> raw) {
        try {
            Object dataVal = raw.get("data");
            if (dataVal == null) {
                return null;
            }

            Map<String, Object> inner;
            if (dataVal instanceof String) {
                // double-encoded JSON string
                inner = JSONObjectUtil.toObject((String) dataVal, HashMap.class);
            } else if (dataVal instanceof Map) {
                inner = (Map<String, Object>) dataVal;
            } else {
                return null;
            }

            if (inner == null) {
                return null;
            }

            LongJobProgressDetail detail = new LongJobProgressDetail();
            Map<String, Object> extra = new HashMap<>();

            // state field
            Object stateVal = inner.get("state");
            if (stateVal instanceof String) {
                detail.setState((String) stateVal);
            }

            // progress sub-object
            Object progressVal = inner.get("progress");
            if (progressVal instanceof Map) {
                Map<String, Object> progress = (Map<String, Object>) progressVal;

                Number percent = toNumber(progress.get("percent"));
                if (percent != null) {
                    detail.setPercent(Math.max(0, Math.min(100, (int) Math.round(percent.doubleValue()))));
                }

                // AI agent uses snake_case field names
                Number processedBytes = toNumber(progress.get("downloaded_bytes"));
                if (processedBytes != null) {
                    detail.setProcessedBytes(processedBytes.longValue());
                }

                Number totalBytes = toNumber(progress.get("total_bytes"));
                if (totalBytes != null) {
                    detail.setTotalBytes(totalBytes.longValue());
                }

                Number speed = toNumber(progress.get("speed_bytes_per_second"));
                if (speed != null) {
                    detail.setSpeedBytesPerSecond(speed.longValue());
                }

                Number eta = toNumber(progress.get("estimated_remaining_seconds"));
                if (eta != null) {
                    detail.setEstimatedRemainingSeconds(eta.longValue());
                }

                Number processedFiles = toNumber(progress.get("downloaded_files"));
                if (processedFiles != null) {
                    detail.setProcessedItems(processedFiles.longValue());
                }

                Number totalFiles = toNumber(progress.get("total_files"));
                if (totalFiles != null) {
                    detail.setTotalItems(totalFiles.longValue());
                }

                Object stage = progress.get("stage");
                if (stage instanceof String) {
                    detail.setStage((String) stage);
                }

                // remaining progress fields go into extra
                Map<String, Object> extraProgress = new HashMap<>(progress);
                extraProgress.remove("percent");
                extraProgress.remove("downloaded_bytes");
                extraProgress.remove("total_bytes");
                extraProgress.remove("speed_bytes_per_second");
                extraProgress.remove("estimated_remaining_seconds");
                extraProgress.remove("downloaded_files");
                extraProgress.remove("total_files");
                extraProgress.remove("stage");
                extra.putAll(extraProgress);
            }

            // stateReason field — can be String or Map (structured reason with code/description)
            Object stateReason = inner.get("state_reason");
            if (stateReason instanceof String) {
                detail.setStateReason((String) stateReason);
            } else if (stateReason instanceof Map) {
                detail.setStateReason(JSONObjectUtil.toJsonString(stateReason));
            }

            // preserve unknown keys from inner top-level
            Map<String, Object> extraInner = new HashMap<>(inner);
            extraInner.remove("state");
            extraInner.remove("progress");
            extraInner.remove("state_reason");
            extra.putAll(extraInner);

            // preserve unknown keys from raw outer-level
            Map<String, Object> extraRaw = new HashMap<>(raw);
            extraRaw.remove("data");
            extra.putAll(extraRaw);

            if (!extra.isEmpty()) {
                detail.setExtra(extra);
            }

            return detail;
        } catch (Exception e) {
            logger.trace("LongJobProgressDetailBuilder: failed to parse AI download format", e);
            return null;
        }
    }

    /**
     * Format 3: unknown — preserve the entire map as extra for UI passthrough.
     */
    private static LongJobProgressDetail parseAsExtra(Map<String, Object> raw) {
        LongJobProgressDetail detail = new LongJobProgressDetail();
        detail.setExtra(new HashMap<>(raw));
        return detail;
    }

    private static Number toNumber(Object val) {
        if (val instanceof Number) {
            return (Number) val;
        }
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
