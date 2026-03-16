package org.zstack.header.core.progress;

import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses TaskProgressVO.opaque (free-form JSON) into a typed LongJobProgressDetail.
 *
 * All agents now send a standardized camelCase format:
 *   {"processed":N, "total":N, "percent":N, "stage":"migrating",
 *    "speed":N, "estimatedRemainingSeconds":N, "state":"running",
 *    "stateReason":"...", "processedItems":N, "totalItems":N, "unit":"bytes"}
 *
 * The "unit" field tells the UI how to format processed/total/speed:
 *   "bytes" — byte quantities (format as KB/MB/GB)
 *   "items" — discrete items (format as count)
 *   "steps" — workflow steps
 *
 * Unknown keys are preserved in LongJobProgressDetail.extra so no data is silently dropped.
 */
public class LongJobProgressDetailBuilder {
    private static final CLogger logger = Utils.getLogger(LongJobProgressDetailBuilder.class);

    private static final String[] KNOWN_KEYS = {
            "processed", "total", "percent", "stage", "state", "stateReason",
            "speed", "estimatedRemainingSeconds", "processedItems", "totalItems", "unit"
    };

    private LongJobProgressDetailBuilder() {}

    /**
     * Build a LongJobProgressDetail from a TaskProgressVO.
     * Returns null if opaque is null/empty or not valid JSON.
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

        try {
            LongJobProgressDetail detail = new LongJobProgressDetail();

            Number processed = toNumber(raw.get("processed"));
            if (processed != null) {
                detail.setProcessed(processed.longValue());
            }

            Number total = toNumber(raw.get("total"));
            if (total != null) {
                detail.setTotal(total.longValue());
            }

            Number percent = toNumber(raw.get("percent"));
            if (percent != null) {
                detail.setPercent(Math.max(0, Math.min(100, (int) Math.round(percent.doubleValue()))));
            }

            Object stage = raw.get("stage");
            if (stage instanceof String) {
                detail.setStage((String) stage);
            }

            Object state = raw.get("state");
            if (state instanceof String) {
                detail.setState((String) state);
            }

            Object stateReason = raw.get("stateReason");
            if (stateReason instanceof String) {
                detail.setStateReason((String) stateReason);
            }

            Number speed = toNumber(raw.get("speed"));
            if (speed != null) {
                detail.setSpeed(speed.longValue());
            }

            Number eta = toNumber(raw.get("estimatedRemainingSeconds"));
            if (eta != null) {
                detail.setEstimatedRemainingSeconds(eta.longValue());
            }

            Number processedItems = toNumber(raw.get("processedItems"));
            if (processedItems != null) {
                detail.setProcessedItems(processedItems.longValue());
            }

            Number totalItems = toNumber(raw.get("totalItems"));
            if (totalItems != null) {
                detail.setTotalItems(totalItems.longValue());
            }

            Object unit = raw.get("unit");
            if (unit instanceof String) {
                detail.setUnit((String) unit);
            }

            // Carry over any unrecognized keys into extra
            Map<String, Object> extra = new HashMap<>(raw);
            for (String key : KNOWN_KEYS) {
                extra.remove(key);
            }
            if (!extra.isEmpty()) {
                detail.setExtra(extra);
            }

            return detail;
        } catch (Exception e) {
            logger.trace("LongJobProgressDetailBuilder: failed to parse standard format", e);
            return null;
        }
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
