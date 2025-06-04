package org.zstack.header.image;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ImageGroupVO.class, collectionValueOfMethod = "valueOf1")
public class ImageGroupInventory implements Serializable {
    private Integer imageCount;
    private String name;
    private String description;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;

    protected ImageGroupInventory(ImageGroupVO vo) {
        this.setImageCount(vo.getImageCount());
        this.setName(vo.getName());
        this.setStatus(vo.getStatus().toString());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
    }

    public static ImageGroupInventory valueOf(ImageGroupVO vo) {
        return new ImageGroupInventory(vo);
    }

    public static List<ImageGroupInventory> valueOf1(Collection<ImageGroupVO> vos) {
        List<ImageGroupInventory> invs = new ArrayList<ImageGroupInventory>(vos.size());
        for (ImageGroupVO vo : vos) {
            invs.add(ImageGroupInventory.valueOf(vo));
        }
        return invs;
    }

    public ImageGroupInventory() {
    }


    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
