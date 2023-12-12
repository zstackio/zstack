package org.zstack.header.ccs;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Wenhao.Zhang on 21/10/15
 */
@Inventory(mappingVOClass = CCSCertificateUserRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "userCertificateRefs", inventoryClass = CCSCertificateInventory.class,
                foreignKey = "certificateUuid", expandedInventoryKey = "uuid"),
})
public class CCSCertificateUserRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String userUuid;
    private String certificateUuid;
    private CCSCertificateUserState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public CCSCertificateUserRefInventory() {
    }

    public CCSCertificateUserRefInventory(CCSCertificateUserRefVO vo) {
        this.id = vo.getId();
        this.userUuid = vo.getUserUuid();
        this.certificateUuid = vo.getCertificateUuid();
        this.state = vo.getState();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static CCSCertificateUserRefInventory valueOf(CCSCertificateUserRefVO vo) {
        return new CCSCertificateUserRefInventory(vo);
    }

    public static List<CCSCertificateUserRefInventory> valueOf(Collection<CCSCertificateUserRefVO> vos) {
        return vos.stream().map(CCSCertificateUserRefInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }

    public CCSCertificateUserState getState() {
        return state;
    }

    public void setState(CCSCertificateUserState state) {
        this.state = state;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
