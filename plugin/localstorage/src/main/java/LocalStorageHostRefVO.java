import org.zstack.header.host.HostEO;
import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by frank on 6/30/2015.
 */
@Entity
@Table
public class LocalStorageHostRefVO {
    @Column
    @Id
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @Index
    private long totalCapacity;

    @Column
    @Index
    private long availableCapacity;

    @Column
    @Index
    private long totalPhysicalCapacity;

    @Column
    @Index
    private long availablePhysicalCapacity;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
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
