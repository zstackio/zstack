package org.zstack.core.scim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SQL;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.header.scim.ScimConstant;
import org.zstack.header.scim.ScimEventVO;
import org.zstack.header.scim.ScimException;
import org.zstack.header.scim.ScimOperation;
import org.zstack.header.scim.ScimPayload;
import org.zstack.header.scim.ScimResourceHandler;
import org.zstack.header.scim.ScimResult;
import org.zstack.header.resource.ResourceSourceConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScimService {
    private static final CLogger logger = Utils.getLogger(ScimService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_CLIENT_ID_LENGTH = 128;
    private static final int MAX_EVENT_ID_LENGTH = 255;
    private static final int MAX_RESOURCE_TYPE_LENGTH = 64;
    private static final int MAX_RESOURCE_ID_LENGTH = 255;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1024;
    private static final long LOCK_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(2);
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final Set<String> SENSITIVE_PAYLOAD_KEYS = new HashSet<>(Arrays.asList(
            "password", "refreshtoken", "scimtoken", "token", "providersecret", "clientsecret", "secret"
    ));

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ScimResourceHandler resourceHandler;
    private final GlobalConfigUpdateExtensionPoint cleanupWhenDisabledExtension = this::cleanupWhenDisabled;
    private final ReentrantReadWriteLock receiverStateLock = new ReentrantReadWriteLock();
    private volatile GlobalConfig registeredReceiverEnabledConfig;

    public void init() {
        ensureReceiverEnabledExtensionRegistered();
    }

    private void ensureReceiverEnabledExtensionRegisteredIfNeeded() {
        GlobalConfig receiverEnabled = ScimGlobalConfig.RECEIVER_ENABLED;
        // Lock-free fast path: concurrent misses are harmless because the synchronized slow path de-duplicates registration.
        if (registeredReceiverEnabledConfig == receiverEnabled
                && receiverEnabled.getLocalUpdateExtensions().contains(cleanupWhenDisabledExtension)
                && receiverEnabled.getUpdateExtensions().contains(cleanupWhenDisabledExtension)) {
            return;
        }
        ensureReceiverEnabledExtensionRegistered();
    }

    private synchronized void ensureReceiverEnabledExtensionRegistered() {
        GlobalConfig receiverEnabled = ScimGlobalConfig.RECEIVER_ENABLED;
        if (registeredReceiverEnabledConfig == receiverEnabled
                && receiverEnabled.getLocalUpdateExtensions().contains(cleanupWhenDisabledExtension)
                && receiverEnabled.getUpdateExtensions().contains(cleanupWhenDisabledExtension)) {
            return;
        }

        if (!receiverEnabled.getLocalUpdateExtensions().contains(cleanupWhenDisabledExtension)) {
            receiverEnabled.installLocalUpdateExtension(cleanupWhenDisabledExtension);
        }
        if (!receiverEnabled.getUpdateExtensions().contains(cleanupWhenDisabledExtension)) {
            receiverEnabled.installUpdateExtension(cleanupWhenDisabledExtension);
        }
        registeredReceiverEnabledConfig = receiverEnabled;
    }

    private void cleanupWhenDisabled(GlobalConfig oldConfig, GlobalConfig newConfig) {
        if (isEnabled(oldConfig.value()) && !isEnabled(newConfig.value())) {
            cleanupScimResourcesIfPresent();
        }
    }

    private void cleanupScimResourcesIfPresent() {
        receiverStateLock.writeLock().lock();
        try {
            Long count = SQL.New("select count(vo) from ResourceSourceRefVO vo where vo.syncType = :syncType", Long.class)
                    .param("syncType", ResourceSourceConstant.SYNC_TYPE_SCIM)
                    .find();
            if (count != null && count > 0) {
                resourceHandler.cleanupResources(ResourceSourceConstant.SYNC_TYPE_SCIM);
            }
        } finally {
            receiverStateLock.writeLock().unlock();
        }
    }

    public ScimResult apply(HttpServletRequest request, String resourceType, String pathResourceId, String rawBody) {
        ensureReceiverEnabledExtensionRegisteredIfNeeded();
        receiverStateLock.readLock().lock();
        try {
            String body = rawBody == null ? "" : rawBody;
            verifyEnabled();
            verifyBearer(request);
            String method = normalizeMethod(request);
            validateMaxLength(resourceType, "resource type", MAX_RESOURCE_TYPE_LENGTH);
            validateMaxLength(pathResourceId, "resource uuid", MAX_RESOURCE_ID_LENGTH);
            String clientId = validateMaxLength(optionalHeader(request, ScimConstant.HEADER_CLIENT_ID,
                    ScimConstant.DEFAULT_CLIENT_ID), ScimConstant.HEADER_CLIENT_ID, MAX_CLIENT_ID_LENGTH);
            String eventId = validateMaxLength(requiredHeader(request, ScimConstant.HEADER_EVENT_ID),
                    ScimConstant.HEADER_EVENT_ID, MAX_EVENT_ID_LENGTH);
            String timestamp = requiredHeader(request, ScimConstant.HEADER_TIMESTAMP);
            verifySignature(request, clientId, eventId, timestamp, method, resourceType, pathResourceId, body);

            String canonicalType = resourceHandler.normalizeResourceType(resourceType);
            validateMaxLength(canonicalType, "canonical resource type", MAX_RESOURCE_TYPE_LENGTH);
            Map<String, Object> bodyMap = parseBody(body);
            rejectSensitivePayloadKeys(bodyMap);
            ScimPayload payload = mapper.convertValue(bodyMap, ScimPayload.class);
            rejectSensitivePayloadKeys(payload.attributes);
            String pathResourceUuid = normalizeOptionalUuid(pathResourceId, "path resource uuid");
            String payloadUuid = normalizeOptionalUuid(payload.uuid, "payload uuid");
            if (!isBlank(pathResourceUuid) && !isBlank(payloadUuid) && !pathResourceUuid.equals(payloadUuid)) {
                throw new ScimException(400, "path resource uuid and payload uuid must match");
            }
            String resourceId = validateMaxLength(firstNotBlank(pathResourceUuid, payloadUuid),
                    "resource uuid", MAX_RESOURCE_ID_LENGTH);
            if (isBlank(resourceId)) {
                throw new ScimException(400, "resource uuid is required");
            }

            ScimOperation operation = toOperation(method);
            GLock lock = new GLock(lockName(clientId, canonicalType, resourceId), LOCK_TIMEOUT_SECONDS);
            lock.lock();
            try {
                return applyLocked(clientId, eventId, canonicalType, resourceId, operation, body, payload);
            } finally {
                lock.unlock();
            }
        } finally {
            receiverStateLock.readLock().unlock();
        }
    }

    private ScimResult applyLocked(String clientId, String eventId, String resourceType, String resourceId,
                                   ScimOperation operation, String body, ScimPayload payload) {
        ScimEventVO existing = findEvent(clientId, eventId);
        if (existing != null) {
            return resultForExistingEvent(existing);
        }

        ScimEventVO event = reserveEvent(clientId, eventId, resourceType, resourceId, operation, body);
        if (!ScimConstant.EVENT_STATUS_PENDING.equals(event.getStatus())) {
            return resultForExistingEvent(event);
        }

        try {
            resourceHandler.applyResource(operation, resourceType, resourceId, payload);
            markEventApplied(event);
            return ScimResult.applied(clientId, eventId, resourceType, resourceId, operation.name());
        } catch (RuntimeException e) {
            markEventFailed(event, e);
            throw e;
        }
    }

    private void verifyBearer(HttpServletRequest request) {
        String expected = ScimGlobalConfig.RECEIVER_TOKEN.value();
        if (isBlank(expected)) {
            throw new ScimException(401, "SCIM receiver token is not configured");
        }
        String auth = request.getHeader("Authorization");
        if (isBlank(auth) || !auth.startsWith("Bearer ")) {
            throw new ScimException(401, "missing bearer token");
        }
        String token = auth.substring("Bearer ".length()).trim();
        if (!constantTimeEquals(expected, token)) {
            throw new ScimException(401, "invalid bearer token");
        }
    }

    private void verifyEnabled() {
        if (!isEnabled(ScimGlobalConfig.RECEIVER_ENABLED.value())) {
            throw new ScimException(403, "SCIM receiver is disabled");
        }
    }

    private boolean isEnabled(String value) {
        return Boolean.parseBoolean(value);
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (isBlank(value)) {
            throw new ScimException(400, String.format("missing header %s", name));
        }
        return value.trim();
    }

    private String optionalHeader(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        return isBlank(value) ? defaultValue : value.trim();
    }

    private String normalizeMethod(HttpServletRequest request) {
        String method = request.getMethod();
        if (isBlank(method)) {
            throw new ScimException(400, "missing HTTP method");
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private ScimOperation toOperation(String method) {
        switch (method) {
            case "POST":
                return ScimOperation.CREATE;
            case "PUT":
                return ScimOperation.UPDATE;
            case "PATCH":
                return ScimOperation.PATCH;
            case "DELETE":
                return ScimOperation.DELETE;
            default:
                throw new ScimException(405, String.format("unsupported SCIM method[%s]", method));
        }
    }

    private void verifySignature(HttpServletRequest request, String clientId, String eventId,
                                 String timestamp, String method, String resourceType, String pathResourceId, String body) {
        String secret = ScimGlobalConfig.RECEIVER_SIGNATURE_SECRET.value();
        if (isBlank(secret)) {
            throw new ScimException(401, "SCIM signature secret is not configured");
        }
        String actual = request.getHeader(ScimConstant.HEADER_SIGNATURE);
        if (isBlank(actual)) {
            throw new ScimException(401, "missing SCIM signature");
        }
        String base = signatureBase(clientId, method, resourceType, pathResourceId, eventId, timestamp, body);
        String expected = "sha256=" + hmacSha256(secret, base);
        if (!constantTimeEquals(expected, actual.trim())) {
            throw new ScimException(401, "invalid SCIM signature");
        }
    }

    private void rejectSensitivePayloadKeys(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        for (String key : values.keySet()) {
            if (key != null && SENSITIVE_PAYLOAD_KEYS.contains(normalizePayloadKey(key))) {
                throw new ScimException(400, String.format("SCIM payload must not contain sensitive field[%s]", key));
            }
        }
    }

    private String normalizePayloadKey(String key) {
        return key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> parseBody(String body) {
        if (isBlank(body)) {
            return new java.util.HashMap<>();
        }
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ScimException(400, "invalid JSON payload");
        }
    }

    private ScimEventVO findEvent(String clientId, String eventId) {
        return SQL.New("select vo from ScimEventVO vo where vo.clientId = :clientId and vo.eventId = :eventId",
                ScimEventVO.class)
                .param("clientId", clientId)
                .param("eventId", eventId)
                .find();
    }

    private ScimEventVO reserveEvent(String clientId, String eventId, String resourceType, String resourceId,
                                     ScimOperation operation, String body) {
        ScimEventVO vo = new ScimEventVO();
        vo.setUuid(Platform.getUuid());
        vo.setClientId(clientId);
        vo.setEventId(eventId);
        vo.setResourceType(resourceType);
        vo.setResourceId(resourceId);
        vo.setOperation(operation.name());
        vo.setStatus(ScimConstant.EVENT_STATUS_PENDING);
        vo.setPayloadHash(sha256(body));
        Timestamp now = new Timestamp(System.currentTimeMillis());
        vo.setCreateDate(now);
        vo.setLastOpDate(now);
        try {
            return dbf.persist(vo);
        } catch (RuntimeException e) {
            ScimEventVO existing = findEvent(clientId, eventId);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    private void markEventApplied(ScimEventVO event) {
        event.setStatus(ScimConstant.EVENT_STATUS_APPLIED);
        event.setErrorMessage(null);
        dbf.update(event);
    }

    private void markEventFailed(ScimEventVO event, RuntimeException e) {
        try {
            event.setStatus(ScimConstant.EVENT_STATUS_FAILED);
            event.setErrorMessage(trimError(e));
            dbf.update(event);
        } catch (RuntimeException ignored) {
        }
    }

    private ScimResult resultForExistingEvent(ScimEventVO event) {
        if (ScimConstant.EVENT_STATUS_APPLIED.equals(event.getStatus())) {
            return ScimResult.duplicate(event.getClientId(), event.getEventId(), event.getResourceType(),
                    event.getResourceId());
        }
        if (ScimConstant.EVENT_STATUS_PENDING.equals(event.getStatus())) {
            throw new ScimException(409, String.format("SCIM event[%s] is being processed", event.getEventId()));
        }
        throw new ScimException(409, String.format("SCIM event[%s] failed before: %s",
                event.getEventId(), firstNotBlank(event.getErrorMessage(), "unknown error")));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String hmacSha256(String secret, String base) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signatureKey(secret), "HmacSHA256"));
            return hex(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ScimException(500, "failed to calculate signature");
        }
    }

    private byte[] signatureKey(String secret) {
        return sha256Bytes("ziam-scim-signature\n" + secret);
    }

    private String sha256(String value) {
        try {
            return hex(sha256Bytes(value));
        } catch (Exception e) {
            throw new ScimException(500, "failed to calculate payload hash");
        }
    }

    private byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ScimException(500, "failed to calculate sha256");
        }
    }

    private String hex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        int index = 0;
        for (byte b : bytes) {
            int value = b & 0xff;
            chars[index++] = HEX[value >>> 4];
            chars[index++] = HEX[value & 0x0f];
        }
        return new String(chars);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String validateMaxLength(String value, String name, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ScimException(400, String.format("%s length must be <= %s", name, maxLength));
        }
        return value;
    }

    private String normalizeOptionalUuid(String value, String name) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        String compact = trimmed.replace("-", "");
        if (!compact.matches("[0-9a-fA-F]{32}")) {
            throw new ScimException(400, String.format("%s must be a UUID", name));
        }
        if (trimmed.indexOf('-') >= 0 &&
                !trimmed.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ScimException(400, String.format("%s must be a UUID", name));
        }
        return compact.toLowerCase(Locale.ROOT);
    }

    private String trimError(RuntimeException e) {
        String message = e.getMessage();
        if (isBlank(message)) {
            message = e.getClass().getName();
        }
        return message.length() > MAX_ERROR_MESSAGE_LENGTH ? message.substring(0, MAX_ERROR_MESSAGE_LENGTH) : message;
    }

    private String signatureBase(String clientId, String method, String resourceType, String pathResourceId,
                                 String eventId, String timestamp, String body) {
        return clientId + "\n" +
                method.toUpperCase(Locale.ROOT) + "\n" +
                nullToEmpty(resourceType) + "\n" +
                signaturePathResourceId(method, pathResourceId) + "\n" +
                eventId + "\n" +
                timestamp.trim() + "\n" +
                body;
    }

    private String signaturePathResourceId(String method, String pathResourceId) {
        if ("POST".equalsIgnoreCase(method)) {
            return "";
        }
        return nullToEmpty(pathResourceId);
    }

    private String lockName(String clientId, String resourceType, String resourceId) {
        String key = clientId + "\n" + resourceType + "\n" + resourceId;
        return "scim-" + sha256(key).substring(0, 58);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
