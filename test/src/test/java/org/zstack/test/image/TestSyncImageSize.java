package org.zstack.test.image;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.SyncImageSizeOnBackupStorageMsg;
import org.zstack.header.storage.backup.SyncImageSizeOnBackupStorageReply;
import org.zstack.image.ImageBase;

import java.util.Collections;

/**
 * Created by david on 2/9/17.
 */
public class TestSyncImageSize {
    @InjectMocks
    private ImageBase imageBase;

    @Mock
    private CloudBus mockBus;

    private final ImageVO vo = new ImageVO();

    @Before
    public void setUp() {
        vo.setUuid(Platform.getUuid());
        vo.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate);
        vo.setStatus(ImageStatus.Ready);
        vo.setState(ImageState.Enabled);
        vo.setBackupStorageRefs(Collections.emptySet());
        imageBase = new ImageBase(vo);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSyncImageSize() {
        SyncImageSizeMsg msg = new SyncImageSizeMsg();
        msg.setImageUuid(vo.getUuid());
        msg.setBackupStorageUuid(Platform.getUuid());

        SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();
        reply.setError(new ErrorCode("1000", "unexpected error"));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                CloudBusCallBack comp = (CloudBusCallBack)args[1];
                comp.run(reply);
                return null;
            }
        }).when(mockBus).send(Matchers.any(SyncImageSizeOnBackupStorageMsg.class), Matchers.anyObject());

        imageBase.handleMessage(msg);

        ArgumentCaptor<MessageReply> argument = ArgumentCaptor.forClass(MessageReply.class);
        Mockito.verify(mockBus).reply(Mockito.any(Message.class), argument.capture());
        Assert.assertEquals(reply.getError().getCode(), argument.getValue().getError().getCode());
        Assert.assertEquals(reply.getError().getDescription(), argument.getValue().getError().getDescription());
    }
}
