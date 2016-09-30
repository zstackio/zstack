package org.zstack.core.inventory;

import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface InventoryFacade {
    public Object valueOf(Object vo);

    public List valueOf(Collection vos);
}
