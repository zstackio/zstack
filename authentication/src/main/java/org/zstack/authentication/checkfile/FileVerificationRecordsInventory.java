package org.zstack.authentication.checkfile;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = FileVerificationRecordsVO.class)
public class FileVerificationRecordsInventory {
    private long id;
    private String fileVerificationUuid;
    private String path;
    private String node;
    private String currentDigest;
    private String targetDigest;
    private String reason;
    private boolean recoverFlag;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    public static FileVerificationRecordsInventory valueOf(FileVerificationRecordsVO vo) {
        FileVerificationRecordsInventory inv = new FileVerificationRecordsInventory();
        inv.setId(vo.getId());
        inv.setFileVerificationUuid(vo.getFileVerificationUuid());
        inv.setPath(vo.getPath());
        inv.setNode(vo.getNode());
        inv.setCurrentDigest(vo.getCurrentDigest());
        inv.setTargetDigest(vo.getTargetDigest());
        inv.setReason(vo.getReason());
        inv.setRecoverFlag(vo.isRecoverFlag());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<FileVerificationRecordsInventory> valueOf(Collection<FileVerificationRecordsVO> vos) {
        List<FileVerificationRecordsInventory> invs = new ArrayList<>();
        for (FileVerificationRecordsVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileVerificationUuid() {
        return fileVerificationUuid;
    }

    public void setFileVerificationUuid(String fileVerificationUuid) {
        this.fileVerificationUuid = fileVerificationUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getCurrentDigest() {
        return currentDigest;
    }

    public void setCurrentDigest(String currentDigest) {
        this.currentDigest = currentDigest;
    }

    public String getTargetDigest() {
        return targetDigest;
    }

    public void setTargetDigest(String targetDigest) {
        this.targetDigest = targetDigest;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isRecoverFlag() {
        return recoverFlag;
    }

    public void setRecoverFlag(boolean recoverFlag) {
        this.recoverFlag = recoverFlag;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
