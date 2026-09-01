package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhysicalServerIdentityService {
    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public Map<String, String> resolveBySerialNumbers(
            Collection<String> serialNumbers) {
        Set<String> normalized = normalize(serialNumbers);
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        insertServers(normalized);
        return serversBySerial(normalized);
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

    private Set<String> normalize(Collection<String> serialNumbers) {
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String serialNumber : serialNumbers) {
            String normalized = Platform.normalizeMachineSerialNumber(serialNumber);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private Map<String, String> serversBySerial(
            Collection<String> serialNumbers) {
        Map<String, String> result = new LinkedHashMap<>();
        List<Tuple> servers = Q.New(PhysicalServerVO.class)
                .select(PhysicalServerVO_.uuid, PhysicalServerVO_.serialNumber)
                .in(PhysicalServerVO_.serialNumber, serialNumbers)
                .listTuple();
        for (Tuple server : servers) {
            result.put(
                    server.get(1, String.class),
                    server.get(0, String.class));
        }
        return result;
    }

    private void insertServers(Collection<String> serialNumbers) {
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
}
