package org.zstack.sdnController.header;

import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by boce.wang on 06/13/2025.
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SdnControllerVO.class, myField = "sdnControllerUuid", targetField = "uuid"),
        }
)
public class H3cSdnControllerTenantVO {

        @Column
        @Id
        private String uuid;

        @Column
        @ForeignKey(parentEntityClass = SdnControllerVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
        private String sdnControllerUuid;

        @Column
        private String tenantUuid;
        @Column
        private String vdsUuid;
        @Column
        private String tenantName;
        @Column
        private String vdsName;
        @Column
        private String cloudDomainName;
        @Column
        private String state;

        @Column
        private Timestamp createDate;

        @Column
        private Timestamp lastOpDate;

        @PreUpdate
        private void preUpdate() {
                lastOpDate = null;
        }

        public String getUuid() {
                return uuid;
        }

        public void setUuid(String uuid) {
                this.uuid = uuid;
        }

        public String getSdnControllerUuid() {
                return sdnControllerUuid;
        }

        public void setSdnControllerUuid(String sdnControllerUuid) {
                this.sdnControllerUuid = sdnControllerUuid;
        }

        public String getTenantUuid() {
                return tenantUuid;
        }

        public void setTenantUuid(String tenantUuid) {
                this.tenantUuid = tenantUuid;
        }

        public String getVdsUuid() {
                return vdsUuid;
        }

        public void setVdsUuid(String vdsUuid) {
                this.vdsUuid = vdsUuid;
        }

        public String getTenantName() {
                return tenantName;
        }

        public void setTenantName(String tenantName) {
                this.tenantName = tenantName;
        }

        public String getVdsName() {
                return vdsName;
        }

        public void setVdsName(String vdsName) {
                this.vdsName = vdsName;
        }

        public String getCloudDomainName() {
                return cloudDomainName;
        }

        public void setCloudDomainName(String cloudDomainName) {
                this.cloudDomainName = cloudDomainName;
        }

        public String getState() {
                return state;
        }

        public void setState(String state) {
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
