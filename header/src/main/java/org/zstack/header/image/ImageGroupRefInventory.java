package org.zstack.header.image;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ImageGroupRefVO.class, collectionValueOfMethod = "valueOf1")
public class ImageGroupRefInventory implements Serializable {
    private String imageUuid;
    private String imageGroupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected ImageGroupRefInventory(ImageGroupRefVO vo) {
        this.setImageUuid(vo.getImageUuid());
        this.setImageGroupUuid(vo.getImageGroupUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public ImageGroupRefInventory() {
    }

    public static ImageGroupRefInventory valueOf(ImageGroupRefVO vo) {
        return new ImageGroupRefInventory(vo);
    }

    public static List<ImageGroupRefInventory> valueOf1(Collection<ImageGroupRefVO> vos) {
        List<ImageGroupRefInventory> invs = new ArrayList<ImageGroupRefInventory>(vos.size());
        for (ImageGroupRefVO vo : vos) {
            invs.add(ImageGroupRefInventory.valueOf(vo));
        }
        return invs;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getImageGroupUuid() {
        return imageGroupUuid;
    }

    public void setImageGroupUuid(String imageGroupUuid) {
        this.imageGroupUuid = imageGroupUuid;
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
