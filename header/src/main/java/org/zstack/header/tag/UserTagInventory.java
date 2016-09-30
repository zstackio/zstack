package org.zstack.header.tag;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = UserTagVO.class)
public class UserTagInventory extends TagInventory {
    public static UserTagInventory valueOf(UserTagVO vo) {
        UserTagInventory inv = new UserTagInventory();
        inv.setUuid(vo.getUuid());
        inv.setType(vo.getType().toString());
        inv.setTag(vo.getTag());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setResourceType(vo.getResourceType());
        return inv;
    }

    public static List<UserTagInventory> valueOf(Collection<UserTagVO> vos) {
        List<UserTagInventory> invs = new ArrayList<UserTagInventory>();
        for (UserTagVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }
}
