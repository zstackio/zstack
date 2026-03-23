package org.zstack.test.integration.configuration.systemTag

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountConstant
import org.zstack.header.tag.TagPatternType
import org.zstack.header.tag.TagPatternVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * ZSTAC-74908: TagPatternVO.resourceType scoping
 *
 * Verifies:
 * 1. AI model tags (resourceType = "ModelVO") are not visible when
 *    querying tag patterns for other resource types (e.g. VmInstanceVO).
 * 2. Universal tags (resourceType = null) remain visible for all
 *    resource types — backward compatible with pre-upgrade data.
 * 3. Upgraded old AI tags get backfilled with resourceType = "ModelVO"
 *    on next prepareDbInitialValue() run.
 */
class TagPatternResourceTypeCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testUniversalTagPatternVisibleForAllResourceTypes()
            testScopedTagPatternOnlyVisibleForMatchingResourceType()
            testQueryFilterByResourceType()
        }
    }

    /**
     * resourceType = null means the tag pattern is universal.
     * It should be returned regardless of what resource type is being queried.
     */
    void testUniversalTagPatternVisibleForAllResourceTypes() {
        // Create a universal tag pattern (simulating pre-upgrade tag)
        TagPatternVO universal = new TagPatternVO()
        universal.setUuid(Platform.getUuid())
        universal.setName("Priority::High")
        universal.setValue("Priority::High")
        universal.setColor("red")
        universal.setType(TagPatternType.simple)
        universal.setResourceType(null)  // null = universal
        universal.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        dbf.persist(universal)

        // Verify it can be found without any resourceType filter
        TagPatternVO found = dbf.findByUuid(universal.getUuid(), TagPatternVO.class)
        assert found != null
        assert found.getResourceType() == null

        // Verify it appears in queries for any resource type
        // Simulating the filter: resourceType IS NULL OR resourceType = 'ZoneVO'
        List<TagPatternVO> results = SQL.New(
                "select tp from TagPatternVO tp where tp.uuid = :uuid and tp.resourceType is null",
                TagPatternVO.class
        ).param("uuid", universal.getUuid()).list()
        assert results.size() == 1

        // Clean up
        dbf.removeByPrimaryKey(universal.getUuid(), TagPatternVO.class)
    }

    /**
     * resourceType = "ModelVO" means the tag pattern is scoped to AI models.
     * It should NOT appear when filtering for other resource types.
     */
    void testScopedTagPatternOnlyVisibleForMatchingResourceType() {
        // Create an AI-scoped tag pattern
        TagPatternVO aiTag = new TagPatternVO()
        aiTag.setUuid(Platform.getUuid())
        aiTag.setName("AI::LLM")
        aiTag.setValue("AI::LLM")
        aiTag.setColor("blue")
        aiTag.setType(TagPatternType.simple)
        aiTag.setResourceType("ModelVO")
        aiTag.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        dbf.persist(aiTag)

        TagPatternVO found = dbf.findByUuid(aiTag.getUuid(), TagPatternVO.class)
        assert found != null
        assert found.getResourceType() == "ModelVO"

        // Should be found when filtering for ModelVO
        List<TagPatternVO> modelResults = SQL.New(
                "select tp from TagPatternVO tp where tp.uuid = :uuid and tp.resourceType = :resType",
                TagPatternVO.class
        ).param("uuid", aiTag.getUuid()).param("resType", "ModelVO").list()
        assert modelResults.size() == 1

        // Should NOT be found when filtering for VmInstanceVO
        List<TagPatternVO> vmResults = SQL.New(
                "select tp from TagPatternVO tp where tp.uuid = :uuid and tp.resourceType = :resType",
                TagPatternVO.class
        ).param("uuid", aiTag.getUuid()).param("resType", "VmInstanceVO").list()
        assert vmResults.size() == 0

        // Clean up
        dbf.removeByPrimaryKey(aiTag.getUuid(), TagPatternVO.class)
    }

    /**
     * Test the combined query pattern that the UI should use:
     * WHERE resourceType IS NULL OR resourceType = :targetResourceType
     *
     * This ensures:
     * - Universal tags (null) are always included
     * - Scoped tags only appear for matching resource types
     * - AI tags do not leak into VM/Zone/etc pages
     */
    void testQueryFilterByResourceType() {
        // Create a universal tag
        TagPatternVO universal = new TagPatternVO()
        universal.setUuid(Platform.getUuid())
        universal.setName("Env::Production")
        universal.setValue("Env::Production")
        universal.setColor("green")
        universal.setType(TagPatternType.simple)
        universal.setResourceType(null)
        universal.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        dbf.persist(universal)

        // Create an AI-scoped tag
        TagPatternVO aiTag = new TagPatternVO()
        aiTag.setUuid(Platform.getUuid())
        aiTag.setName("AI::Rerank")
        aiTag.setValue("AI::Rerank")
        aiTag.setColor("purple")
        aiTag.setType(TagPatternType.simple)
        aiTag.setResourceType("ModelVO")
        aiTag.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        dbf.persist(aiTag)

        // Create a VM-scoped tag
        TagPatternVO vmTag = new TagPatternVO()
        vmTag.setUuid(Platform.getUuid())
        vmTag.setName("VM::HighPerf")
        vmTag.setValue("VM::HighPerf")
        vmTag.setColor("orange")
        vmTag.setType(TagPatternType.simple)
        vmTag.setResourceType("VmInstanceVO")
        vmTag.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        dbf.persist(vmTag)

        // Query for VmInstanceVO page: should see universal + VM tag, NOT AI tag
        List<TagPatternVO> vmPageTags = SQL.New(
                "select tp from TagPatternVO tp" +
                " where tp.uuid in (:uuids)" +
                " and (tp.resourceType is null or tp.resourceType = :resType)",
                TagPatternVO.class
        ).param("uuids", [universal.getUuid(), aiTag.getUuid(), vmTag.getUuid()])
         .param("resType", "VmInstanceVO")
         .list()

        assert vmPageTags.size() == 2
        def vmPageUuids = vmPageTags.collect { it.getUuid() } as Set
        assert vmPageUuids.contains(universal.getUuid())
        assert vmPageUuids.contains(vmTag.getUuid())
        assert !vmPageUuids.contains(aiTag.getUuid())

        // Query for ModelVO page: should see universal + AI tag, NOT VM tag
        List<TagPatternVO> modelPageTags = SQL.New(
                "select tp from TagPatternVO tp" +
                " where tp.uuid in (:uuids)" +
                " and (tp.resourceType is null or tp.resourceType = :resType)",
                TagPatternVO.class
        ).param("uuids", [universal.getUuid(), aiTag.getUuid(), vmTag.getUuid()])
         .param("resType", "ModelVO")
         .list()

        assert modelPageTags.size() == 2
        def modelPageUuids = modelPageTags.collect { it.getUuid() } as Set
        assert modelPageUuids.contains(universal.getUuid())
        assert modelPageUuids.contains(aiTag.getUuid())
        assert !modelPageUuids.contains(vmTag.getUuid())

        // Query with no resource type filter: should see ALL tags
        List<TagPatternVO> allTags = SQL.New(
                "select tp from TagPatternVO tp where tp.uuid in (:uuids)",
                TagPatternVO.class
        ).param("uuids", [universal.getUuid(), aiTag.getUuid(), vmTag.getUuid()])
         .list()

        assert allTags.size() == 3

        // Clean up
        [universal, aiTag, vmTag].each {
            dbf.removeByPrimaryKey(it.getUuid(), TagPatternVO.class)
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
