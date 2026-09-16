package org.zstack.test.unittest.core

import org.junit.Test
import org.zstack.utils.BeanUtils

class CloudBusSchemaRestoreCase {
    // A message sent to another management node carries a schema header, and the
    // receiver restores every nested path before delivering it to the local
    // service. Plain public fields must be resolvable, otherwise the whole
    // message is dropped.
    @Test
    void plainFieldDtoIsRestoredThroughTheSchema() {
        def msg = new SchemaRestoreProbe.HolderMsg()
        msg.payload = [name: 'segment', phase: 'PREPARE']
        msg.putHeaderEntry('schema', [
                'payload'      : SchemaRestoreProbe.PlainDto.class.name,
                'payload.phase': SchemaRestoreProbe.Phase.class.name])

        msg.restoreFromSchema([payload: [name: 'segment', phase: 'PREPARE']])

        assert msg.payload instanceof SchemaRestoreProbe.PlainDto
        assert msg.payload.name == 'segment'
        assert msg.payload.phase == SchemaRestoreProbe.Phase.PREPARE
    }

    @Test
    void plainFieldIsReadableAndWritableWithoutJavaBeanAccessors() {
        def dto = new SchemaRestoreProbe.PlainDto()
        dto.phase = SchemaRestoreProbe.Phase.QUERY

        assert BeanUtils.getPropertyOrField(dto, 'phase') == SchemaRestoreProbe.Phase.QUERY
        assert BeanUtils.setPropertyOrField(dto, 'phase', SchemaRestoreProbe.Phase.PREPARE)
        assert dto.phase == SchemaRestoreProbe.Phase.PREPARE
    }

    @Test
    void unresolvablePathIsReportedInsteadOfThrowing() {
        def dto = new SchemaRestoreProbe.PlainDto()

        assert BeanUtils.getPropertyOrField(dto, 'missing') == null
        assert !BeanUtils.setPropertyOrField(dto, 'missing', 'value')
    }
}
