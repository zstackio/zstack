package org.zstack.authentication.checkfile;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@Inventory(mappingVOClass = FileVerificationVO.class)
public class FileVerificationInventory {
    private String uuid;
    private String path;
    private String node;
    private String category;
    private String hexType;
    private String digest;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getHexType() {
        return hexType;
    }

    public void setHexType(String hexType) {
        this.hexType = hexType;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public static FileVerificationInventory valueOf(FileVerificationVO vo) {
        FileVerificationInventory inv = new FileVerificationInventory();
        inv.setUuid(vo.getUuid());
        inv.setPath(vo.getPath());
        inv.setNode(vo.getNode());
        inv.setHexType(vo.getHexType());
        inv.setDigest(vo.getDigest());
        inv.setState(vo.getState());
        inv.setCategory(vo.getCategory());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<FileVerificationInventory> valueOf(Collection<FileVerificationVO> vos) {
        List<FileVerificationInventory> invs = new ArrayList<>();
        for (FileVerificationVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }
}
