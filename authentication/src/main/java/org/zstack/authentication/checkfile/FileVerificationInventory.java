package org.zstack.authentication.checkfile;
import org.zstack.header.search.Inventory;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@Inventory(mappingVOClass = FileVerificationVO.class)
public class FileVerificationInventory {
    private String uuid;
    private String path;
    private String node;
    private String type;
    private String digest;
    private String state;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public static FileVerificationInventory valueOf(FileVerificationVO vo) {
        FileVerificationInventory inv = new FileVerificationInventory();
        inv.setUuid(vo.getUuid());
        inv.setPath(vo.getPath());
        inv.setNode(vo.getNode());
        inv.setType(vo.getType());
        inv.setDigest(vo.getDigest());
        inv.setState(vo.getState());
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
