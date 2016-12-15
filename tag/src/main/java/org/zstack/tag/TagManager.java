package org.zstack.tag;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.TagInventory;

import java.util.Collection;
import java.util.List;

/**
 */
public interface TagManager {
    SystemTagInventory createNonInherentSystemTag(String resourceUuid, String tag, String resourceType);

    SystemTagInventory createInherentSystemTag(String resourceUuid, String tag, String resourceType);

    void createInherentSystemTags(List<String> sysTags, String resourceUuid, String resourceType);

    void createNonInherentSystemTags(List<String> sysTags, String resourceUuid, String resourceType);

    void createTagsFromAPICreateMessage(APICreateMessage msg, String resourceUuid, String resourceType);

    TagInventory createSysTag(String resourceUuid, String tag, String resourceType);

    TagInventory createUserTag(String resourceUuid, String tag, String resourceType);

    TagInventory createSysTag(String resourceUuid, Enum tag, String resourceType);

    TagInventory createUserTag(String resourceUuid, Enum tag, String resourceType);

    void copySystemTag(String srcResourceUuid, String srcResourceType, String dstResourceUuid, String dstResourceType);

    SystemTagInventory updateSystemTag(String tagUuid, String newTag);

    List<String> findSystemTags(String resourceUuid);

    List<String> findUserTags(String resourceUuid);

    boolean hasSystemTag(String resourceUuid, String tag);

    boolean hasSystemTag(String resourceUuid, Enum tag);

    void deleteSystemTag(String tag, String resourceUuid, String resourceType, Boolean inherit);

    void deleteSystemTagUseLike(String tag, String resourceUuid, String resourceType, Boolean inherit);

    Collection<String> getManagedEntityNames();

    void validateSystemTag(String resourceUuid, String resourceType, String tag);

    void installCreateMessageValidator(String resourceType, SystemTagCreateMessageValidator validator);
}
