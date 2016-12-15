package org.zstack.tag;

import java.util.List;

/**
 * Created by mingjian.deng on 16/12/15.
 */
public interface TagManagerExtensionFactory {
    TagMangerService getTagManager();
    List<Class> getMessageClasses();
}
