package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.backup.BackupStoragePathMaker;
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SftpBackupStorage extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(SftpBackupStorage.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private ErrorFacade errf;

    private String agentPackageName = SftpBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;

    public SftpBackupStorage(SftpBackupStorageVO vo) {
        super(vo);
    }

    private String buildUrl(String subPath) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(SftpBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(getSelf().getHostname());
        }

        ub.port(SftpBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private SftpBackupStorageVO getSelf() {
        return (SftpBackupStorageVO) self;
    }

    protected BackupStorageInventory getSelfInventory() {
        return SftpBackupStorageInventory.valueOf(getSelf());
    }

    private class DownloadResult {
        String md5sum;
        long size;
    }

    private void download(String url, String installPath, final ReturnValueCompletion<DownloadResult> completion) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!SftpBackupStorageFactory.type.getSupportedSchemes().contains(scheme)) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("SftpBackupStorage doesn't support scheme[%s] in url[%s]", scheme, url)
                ));
            }

            DownloadCmd cmd = new DownloadCmd();
            cmd.setUrl(url);
            cmd.setUrlScheme(scheme);
            cmd.setInstallPath(installPath);
            cmd.setTimeout(SftpBackupStorageGlobalProperty.DOWNLOAD_CMD_TIMEOUT);

            restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<DownloadResponse>() {
                @Override
                public void fail(ErrorCode err) {
                    completion.fail(err);
                }

                @Override
                public void success(DownloadResponse ret) {
                    if (ret.isSuccess()) {
                        DownloadResult res = new DownloadResult();
                        res.md5sum = ret.getMd5Sum();
                        res.size = ret.getSize();

                        updateCapacity(ret.getTotalCapacity(), ret.getAvailableCapacity());

                        completion.success(res);
                    } else {
                        completion.fail(errf.stringToOperationError(ret.getError()));
                    }
                }

                @Override
                public Class<DownloadResponse> getReturnClass() {
                    return DownloadResponse.class;
                }
            }, TimeUnit.SECONDS, SftpBackupStorageGlobalProperty.DOWNLOAD_CMD_TIMEOUT);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        final DownloadImageReply reply = new DownloadImageReply();
        final ImageInventory iinv = msg.getImageInventory();
        final String installPath = PathUtil.join(getSelf().getUrl(), BackupStoragePathMaker.makeImageInstallPath(iinv));
        download(iinv.getUrl(), installPath, new ReturnValueCompletion<DownloadResult>(msg) {
            @Override
            public void success(DownloadResult res) {
                reply.setInstallPath(installPath);
                reply.setSize(res.size);
                reply.setMd5sum(res.md5sum);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        final DownloadVolumeReply reply = new DownloadVolumeReply();
        final String installPath = PathUtil.join(getSelf().getUrl(), BackupStoragePathMaker.makeVolumeInstallPath(msg.getUrl(), msg.getVolume()));
        download(msg.getUrl(), installPath, new ReturnValueCompletion<DownloadResult>() {
            @Override
            public void success(DownloadResult res) {
                reply.setInstallPath(installPath);
                reply.setSize(res.size);
                reply.setMd5sum(res.md5sum);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void connectHook(Completion completion) {
        connect(completion);
    }

    private void continueConnect(final Completion complete) {
        restf.echo(buildUrl(SftpBackupStorageConstant.ECHO_PATH), new Completion(complete) {
            @Override
            public void success() {
                String url = buildUrl(SftpBackupStorageConstant.CONNECT_PATH);
                ConnectCmd cmd = new ConnectCmd();
                cmd.setStoragePath(getSelf().getUrl());
                ConnectResponse rsp = restf.syncJsonPost(url, cmd, ConnectResponse.class);
                if (!rsp.isSuccess()) {
                    String err = String.format("unable to connect to SimpleHttpBackupStorage[url:%s], because %s", url, rsp.getError());
                    logger.warn(err);
                    complete.fail(errf.stringToOperationError(err));
                    return;
                }

                updateCapacity(rsp.getTotalCapacity(), rsp.getAvailableCapacity());
                logger.debug(String.format("connected to backup storage[uuid:%s, name:%s, total capacity:%sG, available capacity: %sG",
                        getSelf().getUuid(), getSelf().getName(), rsp.getTotalCapacity(), rsp.getAvailableCapacity()));
                complete.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }
    
    private void connect(final Completion complete) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(complete);
            return;
        }

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(getSelf().getHostname());
        checker.setUsername(getSelf().getUsername());
        checker.setPassword(getSelf().getPassword());
        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/sftpbackupstorage/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/sftpbackupstorage/%s", agentPackageName), true).getAbsolutePath(),
                String.format("/var/lib/zstack/sftpbackupstorage/%s", agentPackageName));

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setPassword(getSelf().getPassword());
        runner.setUsername(getSelf().getUsername());
        runner.setTargetIp(getSelf().getHostname());
        runner.setAgentPort(SftpBackupStorageGlobalProperty.AGENT_PORT);
        runner.setPlayBookName(SftpBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);
        runner.putArgument("pkg_sftpbackupstorage", agentPackageName);
        runner.run(new Completion(complete) {
            @Override
            public void success() {
                continueConnect(complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectSftpBackupStorageMsg) {
            handle((APIReconnectSftpBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) throws URISyntaxException {
        if (msg instanceof GetSftpBackupStorageDownloadCredentialMsg) {
            handle((GetSftpBackupStorageDownloadCredentialMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();

        DeleteCmd cmd = new DeleteCmd();
        cmd.setInstallUrl(msg.getInstallPath());
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.DELETE_PATH), cmd, new JsonAsyncRESTCallback<DeleteResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteResponse ret) {
                if (!ret.isSuccess()) {
                    logger.warn(String.format("failed to delete bits[%s], schedule clean up, %s",
                            msg.getInstallPath(), ret.getError()));
                    //TODO: schedule cleanup
                } else {
                    updateCapacity(ret.getTotalCapacity(), ret.getAvailableCapacity());
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteResponse> getReturnClass() {
                return DeleteResponse.class;
            }
        });
    }

    protected void handle(final PingBackupStorageMsg msg) {
        final PingBackupStorageReply reply = new PingBackupStorageReply();
        PingCmd cmd = new PingCmd();
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.PING_PATH), cmd, new JsonAsyncRESTCallback<PingResponse>() {
            @Override
            public void fail(ErrorCode err) {
                reply.setAvailable(false);
                bus.reply(msg, reply);
            }

            @Override
            public void success(PingResponse ret) {
                reply.setAvailable(ret.isSuccess());
                bus.reply(msg, reply);
            }

            @Override
            public Class<PingResponse> getReturnClass() {
                return PingResponse.class;
            }
        });
    }

    private void handle(final GetSftpBackupStorageDownloadCredentialMsg msg) {
        final GetSftpBackupStorageDownloadCredentialReply reply = new GetSftpBackupStorageDownloadCredentialReply();

        String key = gcf.getConfigValue(ConfigurationConstant.GlobalConfig.privateKey.getCategory(), ConfigurationConstant.GlobalConfig.privateKey.toString(), String.class);
        reply.setHostname(getSelf().getHostname());
        reply.setSshKey(key);
        bus.reply(msg, reply);
    }

    private void handle(final APIReconnectSftpBackupStorageMsg msg) {
        final APIReconnectSftpBackupStorageEvent evt = new APIReconnectSftpBackupStorageEvent(msg.getId());
        connect(new Completion(msg) {
            @Override
            public void success() {
                changeStatus(BackupStorageStatus.Connected, new NoErrorCompletion(msg) {
                    @Override
                    public void done() {
                        self = dbf.reload(self);
                        evt.setInventory(SftpBackupStorageInventory.valueOf(getSelf()));
                        bus.publish(evt);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errf.instantiateErrorCode(SftpBackupStorageErrors.RECONNECT_ERROR, errorCode));
                bus.publish(evt);
            }
        });
    }

    @Override
    protected BackupStorageVO updateBackupStorage(APIUpdateBackupStorageMsg msg) {
        SftpBackupStorageVO vo = (SftpBackupStorageVO) super.updateBackupStorage(msg);
        vo = vo == null ? getSelf() : vo;

        APIUpdateSftpBackupStorageMsg umsg = (APIUpdateSftpBackupStorageMsg) msg;
        if (umsg.getUsername() != null) {
            vo.setUsername(umsg.getUsername());
        }
        if (umsg.getPassword() != null) {
            vo.setPassword(umsg.getPassword());
        }

        return vo;
    }
}
