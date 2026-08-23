package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.physicalserver.PhysicalServerIdentitySpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhysicalServerIdentityService {
    private static final CLogger logger = Utils.getLogger(PhysicalServerIdentityService.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public Map<String, String> resolve(Collection<PhysicalServerIdentitySpec> identities) {
        Map<String, String> requestedZones = normalize(identities);
        if (requestedZones.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, IdentityRow> servers = serversBySerial(requestedZones.keySet());
        Set<String> missing = new LinkedHashSet<>(requestedZones.keySet());
        missing.removeAll(servers.keySet());
        insertServers(missing);
        servers = serversBySerial(requestedZones.keySet());
        assignMissingZones(servers, requestedZones);
        servers = serversBySerial(requestedZones.keySet());

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> requested : requestedZones.entrySet()) {
            IdentityRow server = servers.get(requested.getKey());
            if (server == null || (requested.getValue() != null
                    && !requested.getValue().equals(server.zoneUuid))) {
                logger.warn(String.format(
                        "cannot resolve physical server identity for serialNumber[%s], " +
                                "serverZoneUuid[%s] differs from requestedZoneUuid[%s]",
                        requested.getKey(), server == null ? null : server.zoneUuid,
                        requested.getValue()));
                continue;
            }
            result.put(requested.getKey(), server.uuid);
        }
        return result;
    }

    public Map<String, String> findSerialNumbersByServerUuids(
            Collection<String> serverUuids) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Tuple server : Q.New(PhysicalServerVO.class)
                .select(PhysicalServerVO_.uuid, PhysicalServerVO_.serialNumber)
                .in(PhysicalServerVO_.uuid, serverUuids)
                .listTuple()) {
            result.put(server.get(0, String.class), server.get(1, String.class));
        }
        return result;
    }

    private Map<String, String> normalize(Collection<PhysicalServerIdentitySpec> identities) {
        if (identities == null || identities.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> requestedZones = new LinkedHashMap<>();
        Set<String> conflicts = new LinkedHashSet<>();
        for (PhysicalServerIdentitySpec identity : identities) {
            if (identity == null) {
                continue;
            }
            String serialNumber = Platform.normalizeMachineSerialNumber(identity.getSerialNumber());
            if (serialNumber == null) {
                continue;
            }
            String previousZone = requestedZones.get(serialNumber);
            String requestedZone = identity.getZoneUuid();
            if (previousZone != null && requestedZone != null && !previousZone.equals(requestedZone)) {
                conflicts.add(serialNumber);
                continue;
            }
            if (!requestedZones.containsKey(serialNumber) || previousZone == null) {
                requestedZones.put(serialNumber, requestedZone);
            }
        }
        for (String serialNumber : conflicts) {
            requestedZones.remove(serialNumber);
            logger.warn(String.format(
                    "cannot resolve physical server identity for serialNumber[%s] " +
                            "because multiple zones were reported",
                    serialNumber));
        }
        return requestedZones;
    }

    private Map<String, IdentityRow> serversBySerial(Collection<String> serialNumbers) {
        if (serialNumbers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, IdentityRow> result = new HashMap<>();
        List<Tuple> servers = Q.New(PhysicalServerVO.class)
                .select(PhysicalServerVO_.uuid, PhysicalServerVO_.serialNumber,
                        PhysicalServerVO_.zoneUuid)
                .in(PhysicalServerVO_.serialNumber, serialNumbers)
                .listTuple();
        for (Tuple server : servers) {
            IdentityRow row = new IdentityRow(
                    server.get(0, String.class), server.get(2, String.class));
            result.put(server.get(1, String.class), row);
        }
        return result;
    }

    private void insertServers(Collection<String> serialNumbers) {
        if (serialNumbers.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "INSERT IGNORE INTO PhysicalServerVO " +
                        "(uuid, serialNumber, createDate, lastOpDate) VALUES ");
        int index = 0;
        for (String ignored : serialNumbers) {
            if (index > 0) {
                sql.append(',');
            }
            sql.append("(:uuid").append(index).append(", :serial").append(index)
                    .append(", NOW(), NOW())");
            index++;
        }
        Query query = dbf.getEntityManager().createNativeQuery(sql.toString());
        index = 0;
        for (String serialNumber : serialNumbers) {
            query.setParameter("uuid" + index, Platform.getUuid());
            query.setParameter("serial" + index, serialNumber);
            index++;
        }
        query.executeUpdate();
    }

    private void assignMissingZones(
            Map<String, IdentityRow> servers,
            Map<String, String> requestedZones) {
        Map<String, String> zonesByServer = new LinkedHashMap<>();
        for (Map.Entry<String, String> requested : requestedZones.entrySet()) {
            IdentityRow server = servers.get(requested.getKey());
            if (server != null && server.zoneUuid == null
                    && requested.getValue() != null) {
                zonesByServer.put(server.uuid, requested.getValue());
            }
        }
        if (zonesByServer.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder(
                "UPDATE PhysicalServerVO SET zoneUuid = CASE uuid ");
        int index = 0;
        for (String ignored : zonesByServer.keySet()) {
            sql.append("WHEN :uuid").append(index)
                    .append(" THEN :zone").append(index).append(' ');
            index++;
        }
        sql.append("END WHERE zoneUuid IS NULL AND uuid IN (");
        for (index = 0; index < zonesByServer.size(); index++) {
            if (index > 0) {
                sql.append(',');
            }
            sql.append(":uuid").append(index);
        }
        sql.append(')');

        Query query = dbf.getEntityManager().createNativeQuery(sql.toString());
        index = 0;
        for (Map.Entry<String, String> entry : zonesByServer.entrySet()) {
            query.setParameter("uuid" + index, entry.getKey());
            query.setParameter("zone" + index, entry.getValue());
            index++;
        }
        query.executeUpdate();
    }

    private static class IdentityRow {
        private final String uuid;
        private final String zoneUuid;

        private IdentityRow(String uuid, String zoneUuid) {
            this.uuid = uuid;
            this.zoneUuid = zoneUuid;
        }
    }
}
