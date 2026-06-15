package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.backup.BackupStorageEndpointCandidate;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.EndpointAddressFamilyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

public class KvmOperationEndpointSelector {
    private static final String ROLE_PRIMARY_STORAGE = "primaryStorage";
    private static final String ROLE_BACKUP_STORAGE = "backupStorage";
    private static final String ROLE_RESOURCE = "resource";
    private static final String NETWORK_PLANE_MANAGEMENT = "management";
    private static final String NETWORK_PLANE_STORAGE_COPY = "storage-copy";
    private static final String FAILURE_NO_CANDIDATE_HOST = "NO_CANDIDATE_HOST";
    private static final String FAILURE_ADDRESS_FAMILY_MISMATCH = "ADDRESS_FAMILY_MISMATCH";

    public static class Endpoint {
        private final String resourceRole;
        private final String resourceType;
        private final String resourceIdentity;
        private final String address;
        private final String endpointSource;
        private final String networkPlane;

        private Endpoint(String resourceType, String resourceIdentity, String address) {
            this(inferResourceRole(resourceType), resourceType, resourceIdentity, address,
                    inferEndpointSource(resourceType), inferNetworkPlane(resourceType));
        }

        private Endpoint(String resourceRole, String resourceType, String resourceIdentity,
                         String address, String endpointSource, String networkPlane) {
            this.resourceRole = resourceRole;
            this.resourceType = resourceType;
            this.resourceIdentity = resourceIdentity;
            this.address = address;
            this.endpointSource = endpointSource;
            this.networkPlane = networkPlane;
        }

        public static Endpoint of(String resourceType, String resourceIdentity, String address) {
            return new Endpoint(resourceType, resourceIdentity, address);
        }

        public static Endpoint primaryStorage(String resourceType, String resourceIdentity, String address) {
            return new Endpoint(ROLE_PRIMARY_STORAGE, resourceType, resourceIdentity, address,
                    "primaryStorageInventory.url", NETWORK_PLANE_STORAGE_COPY);
        }

        public static Endpoint backupStorage(String resourceType, String resourceIdentity, String address) {
            return new Endpoint(ROLE_BACKUP_STORAGE, resourceType, resourceIdentity, address,
                    "backupStorageCredential.hostname", NETWORK_PLANE_STORAGE_COPY);
        }

        public static Endpoint backupStorage(String resourceType, String resourceIdentity, String address, String endpointSource) {
            return new Endpoint(ROLE_BACKUP_STORAGE, resourceType, resourceIdentity, address,
                    endpointSource, NETWORK_PLANE_STORAGE_COPY);
        }

        public String getResourceRole() {
            return resourceRole;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getResourceIdentity() {
            return resourceIdentity;
        }

        public String getAddress() {
            return address;
        }

        public String getAddressFamily() {
            return EndpointAddressFamilyUtils.getEndpointAddressFamily(address);
        }

        public String getEndpointSource() {
            return endpointSource;
        }

        public String getNetworkPlane() {
            return networkPlane;
        }

        @Override
        public String toString() {
            return String.format("{resourceRole:%s,resourceType:%s,resourceIdentity:%s,address:%s,addressFamily:%s,endpointSource:%s,networkPlane:%s}",
                    resourceRole, resourceType, resourceIdentity, address, getAddressFamily(), endpointSource, networkPlane);
        }
    }

    public static class Selection {
        private final List<HostInventory> selectedHosts;
        private final Endpoint selectedBackupStorageEndpoint;

        private Selection(List<HostInventory> selectedHosts, Endpoint selectedBackupStorageEndpoint) {
            this.selectedHosts = selectedHosts;
            this.selectedBackupStorageEndpoint = selectedBackupStorageEndpoint;
        }

        public List<HostInventory> getSelectedHosts() {
            return selectedHosts;
        }

        public HostInventory getSelectedHost() {
            return selectedHosts.get(0);
        }

        public List<String> getSelectedHostUuids() {
            List<String> ret = new ArrayList<String>();
            for (HostInventory host : selectedHosts) {
                ret.add(host.getUuid());
            }
            return ret;
        }

        public Endpoint getSelectedBackupStorageEndpoint() {
            return selectedBackupStorageEndpoint;
        }

        public String getSelectedBackupStorageAddress() {
            return selectedBackupStorageEndpoint == null ? null : selectedBackupStorageEndpoint.getAddress();
        }
    }

    public static HostInventory selectOne(String operation, List<HostInventory> candidates,
                                          List<Endpoint> endpoints, String errorCode) {
        List<HostInventory> selected = filter(operation, candidates, endpoints, errorCode);
        return selected.get(0);
    }

    public static List<HostInventory> filter(String operation, List<HostInventory> candidates,
                                             List<Endpoint> endpoints, String errorCode) {
        if (candidates == null || candidates.isEmpty()) {
            throw new OperationFailureException(buildError(operation, candidates, endpoints, errorCode, null));
        }

        Set<String> requiredFamilies = getRequiredFamilies(endpoints);
        if (requiredFamilies.isEmpty()) {
            return candidates;
        }

        List<HostInventory> ret = new ArrayList<HostInventory>();
        for (HostInventory candidate : candidates) {
            if (hostMatches(candidate.getUuid(), candidate.getManagementIp(), requiredFamilies)) {
                ret.add(candidate);
            }
        }

        if (ret.isEmpty()) {
            throw new OperationFailureException(buildError(operation, candidates, endpoints, errorCode, null));
        }

        return ret;
    }

    public static List<String> filterHostUuids(String operation, List<String> candidateHostUuids,
                                               List<Endpoint> endpoints, String errorCode) {
        if (candidateHostUuids == null || candidateHostUuids.isEmpty()) {
            throw new OperationFailureException(buildError(operation, new ArrayList<HostInventory>(), endpoints, errorCode, null));
        }

        List<HostVO> vos = Q.New(HostVO.class)
                .in(HostVO_.uuid, candidateHostUuids)
                .list();
        Map<String, HostInventory> hosts = new LinkedHashMap<String, HostInventory>();
        for (HostVO vo : vos) {
            hosts.put(vo.getUuid(), HostInventory.valueOf(vo));
        }

        List<HostInventory> ordered = new ArrayList<HostInventory>();
        for (String hostUuid : candidateHostUuids) {
            HostInventory host = hosts.get(hostUuid);
            if (host != null) {
                ordered.add(host);
            }
        }

        List<HostInventory> selected = filter(operation, ordered, endpoints, errorCode);
        List<String> ret = new ArrayList<String>();
        for (HostInventory host : selected) {
            ret.add(host.getUuid());
        }

        return ret;
    }

    public static void validateFixedHost(String operation, String hostUuid, List<Endpoint> endpoints, String errorCode) {
        HostVO vo = Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .find();
        if (vo == null) {
            throw new OperationFailureException(buildError(operation, new ArrayList<HostInventory>(), endpoints, errorCode, hostUuid));
        }

        HostInventory host = HostInventory.valueOf(vo);
        Set<String> requiredFamilies = getRequiredFamilies(endpoints);
        if (!requiredFamilies.isEmpty() && !hostMatches(host.getUuid(), host.getManagementIp(), requiredFamilies)) {
            throw new OperationFailureException(buildError(operation, Arrays.asList(host), endpoints, errorCode, hostUuid));
        }
    }

    public static Selection selectForTargetEndpoint(String operation, List<HostInventory> candidates,
                                                    List<Endpoint> requiredEndpoints,
                                                    List<Endpoint> backupStorageEndpointCandidates,
                                                    String errorCode) {
        if (candidates == null || candidates.isEmpty()) {
            throw new OperationFailureException(buildError(operation, candidates,
                    mergeEndpoints(requiredEndpoints, backupStorageEndpointCandidates), errorCode, null));
        }

        if (backupStorageEndpointCandidates == null || backupStorageEndpointCandidates.isEmpty()) {
            throw new OperationFailureException(buildError(operation, candidates,
                    requiredEndpoints, errorCode, null));
        }

        for (HostInventory candidate : candidates) {
            Endpoint targetEndpoint = selectTargetEndpointForHost(candidate, requiredEndpoints, backupStorageEndpointCandidates);
            if (targetEndpoint != null) {
                List<Endpoint> endpoints = mergeEndpoints(requiredEndpoints, Arrays.asList(targetEndpoint));
                return new Selection(filter(operation, candidates, endpoints, errorCode), targetEndpoint);
            }
        }

        throw new OperationFailureException(buildError(operation, candidates,
                mergeEndpoints(requiredEndpoints, backupStorageEndpointCandidates), errorCode, null));
    }

    public static Endpoint selectTargetEndpointForFixedHost(String operation, String hostUuid,
                                                           List<Endpoint> requiredEndpoints,
                                                           List<Endpoint> backupStorageEndpointCandidates,
                                                           String errorCode) {
        HostVO vo = Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .find();
        if (vo == null) {
            throw new OperationFailureException(buildError(operation, new ArrayList<HostInventory>(),
                    mergeEndpoints(requiredEndpoints, backupStorageEndpointCandidates), errorCode, hostUuid));
        }

        HostInventory host = HostInventory.valueOf(vo);
        if (backupStorageEndpointCandidates == null || backupStorageEndpointCandidates.isEmpty()) {
            throw new OperationFailureException(buildError(operation, Arrays.asList(host),
                    requiredEndpoints, errorCode, hostUuid));
        }

        Endpoint targetEndpoint = selectTargetEndpointForHost(host, requiredEndpoints, backupStorageEndpointCandidates);
        if (targetEndpoint != null) {
            return targetEndpoint;
        }

        throw new OperationFailureException(buildError(operation, Arrays.asList(host),
                mergeEndpoints(requiredEndpoints, backupStorageEndpointCandidates), errorCode, hostUuid));
    }

    public static List<Endpoint> backupStorageEndpoints(String resourceType, String resourceIdentity,
                                                        List<BackupStorageEndpointCandidate> candidates) {
        List<Endpoint> ret = new ArrayList<Endpoint>();
        if (candidates == null) {
            return ret;
        }

        for (BackupStorageEndpointCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.getAddress())) {
                continue;
            }
            if (StringUtils.isNotBlank(candidate.getRole()) &&
                    !BackupStorageEndpointCandidate.ROLE_STORAGE_IMAGE_TRANSFER.equals(candidate.getRole())) {
                continue;
            }
            if (StringUtils.isNotBlank(candidate.getProtocol()) && !"ssh".equals(candidate.getProtocol())) {
                continue;
            }

            ret.add(Endpoint.backupStorage(resourceType, resourceIdentity, candidate.getAddress(), candidate.getSource()));
        }

        return ret;
    }

    public static List<Endpoint> backupStorageEndpoints(String resourceType, String resourceIdentity,
                                                        List<BackupStorageEndpointCandidate> candidates,
                                                        String legacyAddress) {
        List<Endpoint> ret = backupStorageEndpoints(resourceType, resourceIdentity, candidates);
        if (ret.isEmpty() && StringUtils.isNotBlank(legacyAddress)) {
            ret.add(Endpoint.backupStorage(resourceType, resourceIdentity, legacyAddress));
        }
        return ret;
    }

    private static List<Endpoint> mergeEndpoints(List<Endpoint> requiredEndpoints, List<Endpoint> targetEndpoints) {
        List<Endpoint> ret = new ArrayList<Endpoint>();
        if (requiredEndpoints != null) {
            ret.addAll(requiredEndpoints);
        }
        if (targetEndpoints != null) {
            ret.addAll(targetEndpoints);
        }
        return ret;
    }

    private static Endpoint selectTargetEndpointForHost(HostInventory host, List<Endpoint> requiredEndpoints,
                                                        List<Endpoint> targetEndpoints) {
        if (host == null || targetEndpoints == null) {
            return null;
        }

        Set<String> preferredFamilies = getRequiredFamilies(requiredEndpoints);
        Endpoint selected = null;
        int selectedScore = Integer.MAX_VALUE;
        for (Endpoint targetEndpoint : targetEndpoints) {
            if (targetEndpoint == null) {
                continue;
            }

            List<Endpoint> endpoints = mergeEndpoints(requiredEndpoints, Arrays.asList(targetEndpoint));
            Set<String> requiredFamilies = getRequiredFamilies(endpoints);
            if (!requiredFamilies.isEmpty() && !hostMatches(host.getUuid(), host.getManagementIp(), requiredFamilies)) {
                continue;
            }

            int score = getTargetEndpointPreferenceScore(targetEndpoint, preferredFamilies);
            if (selected == null || score < selectedScore) {
                selected = targetEndpoint;
                selectedScore = score;
            }
        }

        return selected;
    }

    private static int getTargetEndpointPreferenceScore(Endpoint targetEndpoint, Set<String> preferredFamilies) {
        String family = targetEndpoint.getAddressFamily();
        if (family != null && preferredFamilies != null && preferredFamilies.contains(family)) {
            return 0;
        }

        if (family != null) {
            return 1;
        }

        return 2;
    }

    private static Set<String> getRequiredFamilies(List<Endpoint> endpoints) {
        Set<String> requiredFamilies = new LinkedHashSet<String>();
        if (endpoints == null) {
            return requiredFamilies;
        }

        for (Endpoint endpoint : endpoints) {
            if (endpoint == null) {
                continue;
            }

            String family = endpoint.getAddressFamily();
            if (family != null) {
                requiredFamilies.add(family);
            }
        }

        return requiredFamilies;
    }

    private static boolean hostMatches(String hostUuid, String managementIp, Set<String> requiredFamilies) {
        Set<String> hostFamilies = getHostAddressFamilies(hostUuid, managementIp);
        if (hostFamilies.contains("unknown")) {
            return true;
        }

        return hostFamilies.containsAll(requiredFamilies);
    }

    private static Set<String> getHostAddressFamilies(String hostUuid, String managementIp) {
        Set<String> families = new LinkedHashSet<String>();
        addAddressFamily(families, managementIp);

        if (StringUtils.isNotBlank(hostUuid)) {
            String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(hostUuid, HostSystemTags.EXTRA_IPS_TOKEN);
            if (StringUtils.isNotBlank(extraIps)) {
                for (String ip : extraIps.split(",")) {
                    addAddressFamily(families, ip);
                }
            }
        }

        return families;
    }

    private static void addAddressFamily(Set<String> families, String address) {
        if (StringUtils.isBlank(address)) {
            return;
        }

        String trimmed = address.trim();
        String family = EndpointAddressFamilyUtils.getEndpointAddressFamily(trimmed);
        if (family == null && EndpointAddressFamilyUtils.isHostnameEndpoint(trimmed)) {
            families.add("unknown");
            return;
        }

        if (family != null && EndpointAddressFamilyUtils.isRemoteUsableIp(trimmed)) {
            families.add(family);
        }
    }

    private static ErrorCode buildError(String operation, List<HostInventory> candidates,
                                        List<Endpoint> endpoints, String errorCode, String fixedHostUuid) {
        return operr(errorCode, JSONObjectUtil.toJsonString(buildDiagnosis(operation, candidates, endpoints, fixedHostUuid)));
    }

    private static Map<String, Object> buildDiagnosis(String operation, List<HostInventory> candidates,
                                                      List<Endpoint> endpoints, String fixedHostUuid) {
        Map<String, Object> diagnosis = new LinkedHashMap<String, Object>();
        Set<String> requiredFamilies = getRequiredFamilies(endpoints);
        diagnosis.put("diagnosisType", "operationReachability");
        diagnosis.put("operation", operation);
        diagnosis.put("networkPlane", inferNetworkPlane(endpoints));
        diagnosis.put("probeTtl", 0);
        diagnosis.put("failureClass", candidates == null || candidates.isEmpty() ?
                FAILURE_NO_CANDIDATE_HOST : FAILURE_ADDRESS_FAMILY_MISMATCH);
        diagnosis.put("candidateHostCount", candidates == null ? 0 : candidates.size());
        diagnosis.put("fixedHostUuid", fixedHostUuid);
        diagnosis.put("selectedHostUuid", null);
        diagnosis.put("inferredOnly", true);
        diagnosis.put("endpoints", describeEndpoints(endpoints));
        putPrimaryStorageFields(diagnosis, endpoints);
        putBackupStorageFields(diagnosis, endpoints);
        diagnosis.put("candidateHosts", describeCandidates(candidates, endpoints, requiredFamilies));
        diagnosis.put("failedEdges", describeFailedEdges(candidates, endpoints, requiredFamilies));
        diagnosis.put("recommendations", Arrays.asList(
                "use a host managementIp/extraIps address family that matches every literal endpoint",
                "change the endpoint to a reachable address or hostname"));
        return diagnosis;
    }

    private static List<Map<String, Object>> describeEndpoints(List<Endpoint> endpoints) {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        if (endpoints == null) {
            return ret;
        }

        for (Endpoint endpoint : endpoints) {
            if (endpoint == null) {
                continue;
            }

            Map<String, Object> fact = new LinkedHashMap<String, Object>();
            fact.put("resourceRole", endpoint.getResourceRole());
            fact.put("resourceType", endpoint.getResourceType());
            fact.put("resourceIdentity", endpoint.getResourceIdentity());
            fact.put("address", endpoint.getAddress());
            fact.put("addressFamily", endpoint.getAddressFamily());
            fact.put("endpointSource", endpoint.getEndpointSource());
            fact.put("networkPlane", endpoint.getNetworkPlane());
            ret.add(fact);
        }

        return ret;
    }

    private static List<Map<String, Object>> describeFailedEdges(List<HostInventory> candidates,
                                                                List<Endpoint> endpoints,
                                                                Set<String> requiredFamilies) {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        if (endpoints == null) {
            return ret;
        }

        if (candidates == null || candidates.isEmpty()) {
            for (Endpoint endpoint : endpoints) {
                if (endpoint == null || endpoint.getAddressFamily() == null) {
                    continue;
                }
                ret.add(buildFailedEdge(null, endpoint, FAILURE_NO_CANDIDATE_HOST, "no candidate host"));
            }
            return ret;
        }

        if (requiredFamilies == null || requiredFamilies.isEmpty()) {
            return ret;
        }

        for (HostInventory candidate : candidates) {
            Set<String> missingFamilies = getMissingFamilies(candidate, requiredFamilies);
            if (missingFamilies.isEmpty()) {
                continue;
            }

            for (Endpoint endpoint : endpoints) {
                if (endpoint == null || endpoint.getAddressFamily() == null || !missingFamilies.contains(endpoint.getAddressFamily())) {
                    continue;
                }
                ret.add(buildFailedEdge(candidate, endpoint, FAILURE_ADDRESS_FAMILY_MISMATCH,
                        "missing host address family: " + endpoint.getAddressFamily()));
            }
        }

        return ret;
    }

    private static Map<String, Object> buildFailedEdge(HostInventory candidate, Endpoint endpoint,
                                                       String failureClass, String reason) {
        Map<String, Object> edge = new LinkedHashMap<String, Object>();
        edge.put("actorType", "kvmHost");
        edge.put("actorUuid", candidate == null ? null : candidate.getUuid());
        edge.put("actorManagementIp", candidate == null ? null : candidate.getManagementIp());
        edge.put("actorAddressFamilies", candidate == null ? new LinkedHashSet<String>() :
                getHostAddressFamilies(candidate.getUuid(), candidate.getManagementIp()));
        edge.put("targetRole", endpoint.getResourceRole());
        edge.put("targetType", endpoint.getResourceType());
        edge.put("targetIdentity", endpoint.getResourceIdentity());
        edge.put("targetAddress", endpoint.getAddress());
        edge.put("targetAddressFamily", endpoint.getAddressFamily());
        edge.put("targetEndpointSource", endpoint.getEndpointSource());
        edge.put("networkPlane", endpoint.getNetworkPlane());
        edge.put("failureClass", failureClass);
        edge.put("reason", reason);
        return edge;
    }

    private static List<Map<String, Object>> describeCandidates(List<HostInventory> candidates,
                                                                List<Endpoint> endpoints,
                                                                Set<String> requiredFamilies) {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        if (candidates == null) {
            return ret;
        }

        for (HostInventory candidate : candidates) {
            String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(candidate.getUuid(), HostSystemTags.EXTRA_IPS_TOKEN);
            Map<String, Object> fact = new LinkedHashMap<String, Object>();
            fact.put("uuid", candidate.getUuid());
            fact.put("hostUuid", candidate.getUuid());
            fact.put("managementIp", candidate.getManagementIp());
            fact.put("managementIpFamily", EndpointAddressFamilyUtils.getEndpointAddressFamily(candidate.getManagementIp()));
            fact.put("extraIps", extraIps);
            fact.put("extraIpFamilies", getExtraIpFamilies(extraIps));
            fact.put("addressFamilies", getHostAddressFamilies(candidate.getUuid(), candidate.getManagementIp()));
            fact.put("excludedReason", describeExcludedReason(candidate, requiredFamilies));
            fact.put("failedEdges", describeFailedEdges(Arrays.asList(candidate), endpoints, requiredFamilies));
            ret.add(fact);
        }

        return ret;
    }

    private static Set<String> getExtraIpFamilies(String extraIps) {
        Set<String> families = new LinkedHashSet<String>();
        if (StringUtils.isBlank(extraIps)) {
            return families;
        }

        for (String extraIp : extraIps.split(",")) {
            addAddressFamily(families, extraIp);
        }

        return families;
    }

    private static Set<String> getMissingFamilies(HostInventory candidate, Set<String> requiredFamilies) {
        Set<String> hostFamilies = getHostAddressFamilies(candidate.getUuid(), candidate.getManagementIp());
        if (hostFamilies.contains("unknown")) {
            return new LinkedHashSet<String>();
        }

        Set<String> missing = new LinkedHashSet<String>(requiredFamilies);
        missing.removeAll(hostFamilies);
        return missing;
    }

    private static String describeExcludedReason(HostInventory candidate, Set<String> requiredFamilies) {
        if (requiredFamilies == null || requiredFamilies.isEmpty()) {
            return null;
        }

        Set<String> missingFamilies = getMissingFamilies(candidate, requiredFamilies);
        if (missingFamilies.isEmpty()) {
            return null;
        }

        return FAILURE_ADDRESS_FAMILY_MISMATCH + ": missing host address family " + missingFamilies;
    }

    private static void putPrimaryStorageFields(Map<String, Object> diagnosis, List<Endpoint> endpoints) {
        Endpoint endpoint = findEndpointByRole(endpoints, ROLE_PRIMARY_STORAGE);
        diagnosis.put("primaryStorageUuid", endpoint == null ? null : endpoint.getResourceIdentity());
        diagnosis.put("primaryStorageType", endpoint == null ? null : endpoint.getResourceType());
        diagnosis.put("primaryStorageEndpoint", endpoint == null ? null : endpoint.getAddress());
        diagnosis.put("primaryStorageEndpointFamily", endpoint == null ? null : endpoint.getAddressFamily());
    }

    private static void putBackupStorageFields(Map<String, Object> diagnosis, List<Endpoint> endpoints) {
        Endpoint endpoint = findEndpointByRole(endpoints, ROLE_BACKUP_STORAGE);
        diagnosis.put("backupStorageUuid", endpoint == null ? null : endpoint.getResourceIdentity());
        diagnosis.put("backupStorageType", endpoint == null ? null : endpoint.getResourceType());
        diagnosis.put("backupStorageEndpoint", endpoint == null ? null : endpoint.getAddress());
        diagnosis.put("backupStorageEndpointFamily", endpoint == null ? null : endpoint.getAddressFamily());
        diagnosis.put("backupStorageEndpointSource", endpoint == null ? null : endpoint.getEndpointSource());
    }

    private static Endpoint findEndpointByRole(List<Endpoint> endpoints, String role) {
        if (endpoints == null) {
            return null;
        }

        for (Endpoint endpoint : endpoints) {
            if (endpoint != null && role.equals(endpoint.getResourceRole())) {
                return endpoint;
            }
        }

        return null;
    }

    private static String inferNetworkPlane(List<Endpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return NETWORK_PLANE_MANAGEMENT;
        }

        Set<String> planes = new LinkedHashSet<String>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint != null && StringUtils.isNotBlank(endpoint.getNetworkPlane())) {
                planes.add(endpoint.getNetworkPlane());
            }
        }

        if (planes.isEmpty()) {
            return NETWORK_PLANE_MANAGEMENT;
        }

        return planes.size() == 1 ? planes.iterator().next() : "mixed";
    }

    private static String inferResourceRole(String resourceType) {
        if (StringUtils.containsIgnoreCase(resourceType, "primary storage")) {
            return ROLE_PRIMARY_STORAGE;
        }
        if (StringUtils.containsIgnoreCase(resourceType, "backup storage")) {
            return ROLE_BACKUP_STORAGE;
        }
        return ROLE_RESOURCE;
    }

    private static String inferEndpointSource(String resourceType) {
        String role = inferResourceRole(resourceType);
        if (ROLE_PRIMARY_STORAGE.equals(role)) {
            return "primaryStorageInventory.url";
        }
        if (ROLE_BACKUP_STORAGE.equals(role)) {
            return "backupStorageCredential.hostname";
        }
        return "endpoint.address";
    }

    private static String inferNetworkPlane(String resourceType) {
        String role = inferResourceRole(resourceType);
        if (ROLE_PRIMARY_STORAGE.equals(role) || ROLE_BACKUP_STORAGE.equals(role)) {
            return NETWORK_PLANE_STORAGE_COPY;
        }
        return NETWORK_PLANE_MANAGEMENT;
    }
}
