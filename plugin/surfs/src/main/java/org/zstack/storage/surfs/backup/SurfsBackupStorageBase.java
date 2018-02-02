package org.zstack.storage.surfs.backup;

import static org.zstack.utils.CollectionDSL.list;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.surfs.SurfsNodeBase.PingResult;
import org.zstack.storage.surfs.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by zhouhaiping 2017-08-23
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SurfsBackupStorageBase extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(SurfsBackupStorageBase.class);

    class ReconnectNodeLock {
        AtomicBoolean hold = new AtomicBoolean(false);

        boolean lock() {
            return hold.compareAndSet(false, true);
        }

        void unlock() {
            hold.set(false);
        }
    }

    ReconnectNodeLock reconnectNodeLock = new ReconnectNodeLock();

    @Autowired
    protected RESTFacade restf;

    public static class AgentCommand {
    	String fsid;
        String uuid;

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
        } 
        
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class AgentResponse {
        String error;
        boolean success = true;
        Long totalCapacity;
        Long availableCapacity;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public static class Pool {
        String name;
        boolean predefined;
    }
    
    public static class InitCmd extends AgentCommand {
        List<Pool> pools;
    }

    public static class InitRsp extends AgentResponse {
        String fsid;

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
        }
    }

    @ApiTimeout(apiClasses = {APIAddImageMsg.class})
    public static class DownloadCmd extends AgentCommand {
        String url;
        String installPath;
        String imageUuid;
        String imageFormat;
        
        public String getImageFormat() {
            return imageFormat;
        }

        public void setImageFormat(String imageFormat) {
            this.imageFormat = imageFormat;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DownloadRsp extends AgentResponse {
        long size;
        Long actualSize;

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class DeleteCmd extends AgentCommand {
        String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteRsp extends AgentResponse {
    }

    public static class PingCmd extends AgentCommand {
    }

    public static class PingRsp extends AgentResponse {

    }

    public static class GetImageSizeCmd extends AgentCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
    }

    public static class GetFactsCmd extends AgentCommand {
        public String nodeUuid;
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
    }

    public static class GetLocalFileSizeCmd extends AgentCommand {
        public String path ;
    }

    public static class GetLocalFileSizeRsp extends AgentResponse {
        public long size;
    }


    public static final String INIT_PATH = "/surfs/backupstorage/init";
    public static final String DOWNLOAD_IMAGE_PATH = "/surfs/backupstorage/image/download";
    public static final String DELETE_IMAGE_PATH = "/surfs/backupstorage/image/delete";
    public static final String GET_IMAGE_SIZE_PATH = "/surfs/backupstorage/image/getsize";
    public static final String PING_PATH = "/surfs/backupstorage/ping";
    public static final String GET_FACTS = "/surfs/backupstorage/facts";
    public static final String GET_LOCAL_FILE_SIZE = "/surfs/backupstorage/getlocalfilesize";

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("surfs://%s/%s", getSelf().getPoolName(), imageUuid);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        cmd.setUuid(self.getUuid());

        final List<SurfsBackupStorageNodeBase> nodes = new ArrayList<SurfsBackupStorageNodeBase>();
        for (SurfsBackupStorageNodeVO nodevo : getSelf().getNodes()) {
            if (nodevo.getStatus() == NodeStatus.Connected) {
                nodes.add(new SurfsBackupStorageNodeBase(nodevo));
            }
        }

        if (nodes.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("all surfs mons are Disconnected in surfs backup storage[uuid:%s]", self.getUuid())
            ));
        }

        Collections.shuffle(nodes);

        class HttpCaller {
            Iterator<SurfsBackupStorageNodeBase> it = nodes.iterator();
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

            void call() {
                if (!it.hasNext()) {
                    callback.fail(errf.stringToOperationError(
                            String.format("all nodes failed to execute http call[%s], errors are %s", path, JSONObjectUtil.toJsonString(errorCodes))
                    ));

                    return;
                }

                SurfsBackupStorageNodeBase base = it.next();
                base.httpCall(path, cmd, retClass, new ReturnValueCompletion<T>(callback) {
                    @Override
                    public void success(T ret) {
                        if (!ret.success) {
                            // not an IO error but an operation error, return it
                            String details = String.format("[node:%s], %s", base.getSelf().getHostname(), ret.error);
                            callback.fail(errf.stringToOperationError(details));
                        } else {
                            if (!(cmd instanceof InitCmd)) {
                                updateCapacityIfNeeded(ret);
                            }

                            callback.success(ret);
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        String details = String.format("[node:%s], %s", base.getSelf().getHostname(), errorCode.getDetails());
                        errorCode.setDetails(details);
                        errorCodes.add(errorCode);
                        call();
                    }
                });
            }
        }

        new HttpCaller().call();    	

    }

    public SurfsBackupStorageBase(BackupStorageVO self) {
        super(self);
    }

    protected SurfsBackupStorageVO getSelf() {
        return (SurfsBackupStorageVO) self;
    }

    protected SurfsBackupStorageInventory getInventory() {
        return SurfsBackupStorageInventory.valueOf(getSelf());
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
            new SurfsCapacityUpdater().update(getSelf().getFsid(), rsp.totalCapacity, rsp.availableCapacity);
        }
    }
    
    @Override
    @Transactional
    protected void handle(final DownloadImageMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());
        cmd.imageUuid = msg.getImageInventory().getUuid();
        cmd.imageFormat = msg.getImageInventory().getFormat();
        
        String sql = "update ImageBackupStorageRefVO set installPath = :installPath " +
                "where backupStorageUuid = :bsUuid and imageUuid = :imageUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("installPath", cmd.installPath);
        q.setParameter("bsUuid", msg.getBackupStorageUuid());
        q.setParameter("imageUuid", msg.getImageInventory().getUuid());
        q.executeUpdate();

        final DownloadImageReply reply = new DownloadImageReply();
        logger.debug("---------localfile----------100--------");
        if (cmd.url.startsWith("file://")){
        	logger.debug("---------localfile----------101--------");
        	checklocalfilenode(cmd.url);
        }
        if (snd.getIsLocalFile()){
        	logger.debug("---------localfile----------003--------");
        	logger.debug(snd.getNodeIp());
        	snd.setIsLocalFile(false);
        	snd.setSign(0);
        	singleCall(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class,snd.getNodeIp(),new ReturnValueCompletion<DownloadRsp>(msg){
                @Override
                public void success(DownloadRsp ret) {
                	snd.setSign(1);
                    reply.setInstallPath(cmd.installPath);
                    reply.setSize(ret.size);
                    long asize = ret.actualSize == null ? ret.size : ret.actualSize;
                    reply.setActualSize(asize);
                    reply.setMd5sum("not calculated");
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {                
                	reply.setError(errorCode);
                	snd.setSign(1);
                	bus.reply(msg, reply);
                }
                
            });
        	return;
        }
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);

                // current surfs has no way to get the actual size
                // if we cannot get the actual size from HTTP, use the virtual size
                long asize = ret.actualSize == null ? ret.size : ret.actualSize;
                reply.setActualSize(asize);
                reply.setMd5sum("not calculated");
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getUrl();
        cmd.installPath = makeImageInstallPath(msg.getVolume().getUuid());
        cmd.imageFormat = msg.getVolume().getFormat();

        final DownloadVolumeReply reply = new DownloadVolumeReply();
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);
                reply.setMd5sum("not calculated");
                bus.reply(msg, reply);
            }
        });
    }

    @Transactional(readOnly = true)
    private boolean canDelete(String installPath) {
        String sql = "select count(c)" +
                " from ImageBackupStorageRefVO img, ImageCacheVO c" +
                " where img.imageUuid = c.imageUuid" +
                " and img.backupStorageUuid = :bsUuid" +
                " and img.installPath = :installPath";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("bsUuid", self.getUuid());
        q.setParameter("installPath", installPath);
        return q.getSingleResult() == 0;
    }

    @Override
    protected void handle(final GetImageSizeOnBackupStorageMsg msg){
        //TODO
        throw new CloudRuntimeException(String.format("not implemented"));
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();
        if (!canDelete(msg.getInstallPath())) {
            //TODO: GC, the image is still referred, need to cleanup
            bus.reply(msg, reply);
            return;
        }
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        httpCall(DELETE_IMAGE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                //TODO GC, instead of error
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        reply.setInstallPath(makeImageInstallPath(msg.getImageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = msg.getImage().getUuid();

        ImageBackupStorageRefInventory ref = CollectionUtils.find(msg.getImage().getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                return self.getUuid().equals(arg.getBackupStorageUuid()) ? arg : null;
            }
        });

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefInventory of image[uuid:%s] for" +
                    " the backup storage[uuid:%s]", msg.getImage().getUuid(), self.getUuid()));
        }

        final SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();
        cmd.installPath = ref.getInstallPath();
        httpCall(GET_IMAGE_SIZE_PATH, cmd, GetImageSizeRsp.class, new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void success(GetImageSizeRsp rsp) {
                reply.setSize(rsp.size);

                // current surfs cannot get actual size
                long asize = rsp.actualSize == null ? msg.getImage().getActualSize() : rsp.actualSize;
                reply.setActualSize(asize);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
    private static class SelectedNode{
    	private String nodeip;
    	private long size;
    	private int sign;
    	private boolean isLocalFile=false;
    	public boolean getIsLocalFile(){
    		return this.isLocalFile;
    	}
    	public void setIsLocalFile(boolean islfile){
    	    this.isLocalFile=islfile;
    	}
    	public int getSign()
    	{
    		return this.sign;
    	}
    	public void setSign(int csign){
    		this.sign=csign;
    	}
    	public String getNodeIp(){
    		return this.nodeip;
    	}
    	public void setNodeIp(String hostip){
    		this.nodeip=hostip;
    	}
    	public long getSize(){
    		return this.size;
    	}
    	public void setSize(long fsize){
    		this.size=fsize;
    	}
    }
    private static SelectedNode snd=new SelectedNode();
    private void checklocalfilenode(String fileurl){
        GetLocalFileSizeCmd cmd = new GetLocalFileSizeCmd();
        cmd.path = fileurl;
        snd.setSize(0);
        for (SurfsBackupStorageNodeVO node :getSelf().getNodes()){
        	snd.setSign(0);
            singleCall(GET_LOCAL_FILE_SIZE, cmd,GetLocalFileSizeRsp.class,node.getHostname(),new ReturnValueCompletion<GetLocalFileSizeRsp>(null){
                @Override
                public void success(GetLocalFileSizeRsp ret) {
                	logger.debug(String.valueOf(ret.size));
                	snd.setSize(ret.size);
                	snd.setSign(1);
                }

                @Override
                public void fail(ErrorCode errorCode) {                
                	snd.setSign(1);
                }
                
            }); 
            int sk=0;
            while (true) {
            	if (snd.getSign() >0){
            		break;
            	}else{
            		try{
            	        Thread.sleep(500);
            		}catch(Exception ex){
            			logger.debug(String.format("Error to sleep for getlocalfilesize from[%s]",node.getHostname()));
            		}
            	}
            	if (sk > 20){
            		break;
            	}
            	sk = sk + 1;
           }
           if (snd.getSize() >0){
        	   logger.debug(node.getHostname());
        	   snd.setNodeIp(node.getHostname());
               break;
            }
        }
        if (snd.getSize() >0){
        	logger.debug(snd.getNodeIp());
        	snd.setIsLocalFile(true);
        }
    }
    @Override
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        GetLocalFileSizeOnBackupStorageReply reply = new GetLocalFileSizeOnBackupStorageReply();
        GetLocalFileSizeCmd cmd = new GetLocalFileSizeCmd();
        cmd.path = msg.getUrl();
        class EndSign{
        	int sign;
        	long size;
        	public void setSign(int ss){
        		this.sign=ss;
        	}
        	public int getSign(){
        		return this.sign;
        	}
        	public void setSize(long sz){
        		this.size=sz;
        	}
        	public long getSize(){
        		return this.size;
        	}
        }
        EndSign mysign=new EndSign();
        for (SurfsBackupStorageNodeVO node :getSelf().getNodes()){
        	mysign.setSign(0);
            singleCall(GET_LOCAL_FILE_SIZE, cmd,GetLocalFileSizeRsp.class,node.getHostname(),new ReturnValueCompletion<GetLocalFileSizeRsp>(null){
                @Override
                public void success(GetLocalFileSizeRsp ret) {
                	mysign.setSize(ret.size);
                	reply.setSuccess(true);
                	mysign.setSign(1);
                }

                @Override
                public void fail(ErrorCode errorCode) {                
                	reply.setError(errorCode);
                	mysign.setSize(0);
                	mysign.setSign(1);
                }
                
            }); 
            int sk=0;
            while (true) {
            	if (mysign.getSign() >0){
            		break;
            	}else{
            		try{
            	        Thread.sleep(500);
            		}catch(Exception ex){
            			logger.debug(String.format("Error to sleep for getlocalfilesize from[%s]",node.getHostname()));
            		}
            	}
            	if (sk > 20){
            		break;
            	}
            	sk = sk + 1;
           }
           if (mysign.getSize() >0){
               break;
            }
        }
        if (mysign.getSize() >0){
        	reply.setSize(mysign.getSize());
        	bus.reply(msg,reply);
        }else{
        	bus.reply(msg, reply);
        }

    }

    @Override
    protected void connectHook(final boolean newAdded, final Completion completion) {
        final List<SurfsBackupStorageNodeBase> nodes = CollectionUtils.transformToList(getSelf().getNodes(), new Function<SurfsBackupStorageNodeBase, SurfsBackupStorageNodeVO>() {
            @Override
            public SurfsBackupStorageNodeBase call(SurfsBackupStorageNodeVO arg) {
                return new SurfsBackupStorageNodeBase(arg);
            }
        });

        class Connector {
            List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
            Iterator<SurfsBackupStorageNodeBase> it = nodes.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == nodes.size()) {
                        trigger.fail(errf.stringToOperationError(
                                String.format("unable to connect to the surfs backup storage[uuid:%s]. Failed to connect all surfs nodes. Errors are %s",
                                        self.getUuid(), JSONObjectUtil.toJsonString(errorCodes))
                        ));
                    } else {
                        // reload because node status changed
                        self = dbf.reload(self);
                        trigger.next();
                    }
                    return;
                }

                final SurfsBackupStorageNodeBase base = it.next();
                base.connect(new Completion(completion) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);

                        if (newAdded) {
                            // the node fails to connect, remove it
                            dbf.remove(base.getSelf());
                        }

                        connect(trigger);
                    }
                });
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-surfs-backup-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-monitor";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Connector().connect(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final Map<String, String> fsids = new HashMap<String, String>();

                        final List<SurfsBackupStorageNodeBase> nodes = CollectionUtils.transformToList(getSelf().getNodes(), new Function<SurfsBackupStorageNodeBase, SurfsBackupStorageNodeVO>() {
                            @Override
                            public SurfsBackupStorageNodeBase call(SurfsBackupStorageNodeVO arg) {
                                return arg.getStatus() == NodeStatus.Connected ? new SurfsBackupStorageNodeBase(arg) : null;
                            }
                        });

                        DebugUtils.Assert(!nodes.isEmpty(), "how can be no connected node!!! ???");

                        final AsyncLatch latch = new AsyncLatch(nodes.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                Set<String> set = new HashSet<String>();
                                set.addAll(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb =  new StringBuilder("the fsid returned by nodes are mismatching, it seems the nodes belong to different surfs clusters:\n");
                                    for (SurfsBackupStorageNodeBase node : nodes) {
                                        String fsid = fsids.get(node.getSelf().getUuid());
                                        sb.append(String.format("%s (node ip) --> %s (fsid)\n", node.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(errf.stringToOperationError(sb.toString()));
                                }

                                // check if there is another surfs setup having the same fsid
                                String fsId = set.iterator().next();

                                SimpleQuery<SurfsBackupStorageVO>  q = dbf.createQuery(SurfsBackupStorageVO.class);
                                q.add(SurfsBackupStorageVO_.fsid, Op.EQ, fsId);
                                q.add(SurfsBackupStorageVO_.uuid, Op.NOT_EQ, self.getUuid());
                                SurfsBackupStorageVO othersurfs = q.find();
                                if (othersurfs != null) {
                                    throw new OperationFailureException(errf.stringToOperationError(
                                            String.format("there is another surfs backup storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same surfs setup as two different backup storage",
                                                    othersurfs.getName(), othersurfs.getUuid(), fsId)
                                    ));
                                }

                                trigger.next();
                            }
                        });

                        for (final SurfsBackupStorageNodeBase node : nodes) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.nodeUuid = node.getSelf().getUuid();
                            node.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.success) {
                                        // one node cannot get the facts, directly error out
                                        trigger.fail(errf.stringToOperationError(rsp.error));
                                        return;
                                    }

                                    fsids.put(node.getSelf().getUuid(), rsp.fsid);
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one node cannot get the facts, directly error out
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        InitCmd cmd = new InitCmd();
                        Pool p = new Pool();
                        p.name = getSelf().getPoolName();
                        p.predefined = SurfsSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());
                        cmd.pools = list(p);

                        httpCall(INIT_PATH, cmd, InitRsp.class, new ReturnValueCompletion<InitRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (getSelf().getFsid() == null) {
                                    getSelf().setFsid(ret.fsid);
                                    self = dbf.updateAndRefresh(self);
                                }

                                SurfsCapacityUpdater updater = new SurfsCapacityUpdater();
                                updater.update(ret.fsid, ret.totalCapacity, ret.availableCapacity, true);
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (newAdded) {
                            self = dbf.reload(self);
                            if (!getSelf().getNodes().isEmpty()) {
                                dbf.removeCollection(getSelf().getNodes(), SurfsBackupStorageNodeVO.class);
                            }
                        }

                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void pingHook(final Completion completion) {
        final List<SurfsBackupStorageNodeBase> mons = CollectionUtils.transformToList(getSelf().getNodes(), new Function<SurfsBackupStorageNodeBase, SurfsBackupStorageNodeVO>() {
            @Override
            public SurfsBackupStorageNodeBase call(SurfsBackupStorageNodeVO arg) {
                return new SurfsBackupStorageNodeBase(arg);
            }
        });

        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

        class Ping {
            private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final SurfsBackupStorageNodeBase mon, boolean delay) {
                if (!SurfsGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the surfs backup storage node[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), SurfsGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
                    return;
                }

                // there has been a reconnect in process
                if (!reconnectNodeLock.lock()) {
                    return;
                }

                final NoErrorCompletion releaseLock = new NoErrorCompletion() {
                    @Override
                    public void done() {
                        reconnectNodeLock.unlock();
                    }
                };

                try {
                    if (delay) {
                        try {
                            TimeUnit.SECONDS.sleep(SurfsGlobalConfig.BACKUP_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mon.connect(new Completion(releaseLock) {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the node[uuid:%s] of the surfs backup" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to reconnect the node[uuid:%s] of the surfs backup" +
                                    " storage[uuid:%s, name:%s], %s", mon.getSelf().getUuid(), self.getUuid(), self.getName(), errorCode));
                            releaseLock.done();
                        }
                    });
                } catch (Throwable t) {
                    releaseLock.done();
                    logger.warn(t.getMessage(), t);
                }
            }

            void ping() {
                // this is only called when all mons are disconnected
                final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (!replied.compareAndSet(false, true)) {
                            return;
                        }

                        ErrorCode err =  errf.stringToOperationError(String.format("failed to ping the surfs backup storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final SurfsBackupStorageNodeBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping node[uuid:%s] of the surfs backup storage[uuid:%s, name:%s], %s",
                                    mon.getSelf().getUuid(), self.getUuid(), self.getName(), err));
                            errors.add(err);
                            mon.changeStatus(NodeStatus.Disconnected);
                            reconnectMon(mon, true);
                            latch.ack();
                        }

                        @Override
                        public void success(PingResult res) {
                            if (res.success) {
                                // as long as there is one mon working, the backup storage works
                                pingSuccess();

                                if (mon.getSelf().getStatus() == NodeStatus.Disconnected) {
                                    reconnectMon(mon, false);
                                }

                            } else if (res.operationFailure) {
                                // as long as there is one mon saying the surfs not working, the backup storage goes down
                                logger.warn(String.format("the surfs backup storage[uuid:%s, name:%s] is down, as one node[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error));
                                backupStorageDown();
                            } else  {
                                // this mon is down(success == false, operationFailure == false), but the backup storage may still work as other mons may work
                                ErrorCode errorCode = errf.stringToOperationError(res.error);
                                thisMonIsDown(errorCode);
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            thisMonIsDown(errorCode);
                        }
                    });
                }
            }

            // this is called once a mon return an operation failure
            private void backupStorageDown() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                // set all mons to be disconnected
                for (SurfsBackupStorageNodeBase base : mons) {
                    base.changeStatus(NodeStatus.Disconnected);
                }

                ErrorCode err = errf.stringToOperationError(String.format("failed to ping the backup primary storage[uuid:%s, name:%s]",
                        self.getUuid(), self.getName()), errors);
                completion.fail(err);
            }

            private void pingSuccess() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                completion.success();
            }
        }

        new Ping().ping();
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddNodeToSurfsBackupStorageMsg) {
            handle((APIAddNodeToSurfsBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateSurfsBackupStorageNodeMsg){
            handle((APIUpdateSurfsBackupStorageNodeMsg) msg);
        } else if (msg instanceof APIRemoveNodeFromSurfsBackupStorageMsg) {
            handle((APIRemoveNodeFromSurfsBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    public void deleteHook() {
    	dbf.removeCollection(getSelf().getNodes(), SurfsBackupStorageNodeVO.class);
    }


    private void handle(final APIUpdateSurfsBackupStorageNodeMsg msg) {
        final APIUpdateNodeToSurfsBackupStorageEvent evt = new APIUpdateNodeToSurfsBackupStorageEvent(msg.getId());
        SurfsBackupStorageNodeVO monvo = dbf.findByUuid(msg.getNodeUuid(), SurfsBackupStorageNodeVO.class);
        if (msg.getHostname() != null) {
            monvo.setHostname(msg.getHostname());
        }
        if (msg.getNodePort() != null && msg.getNodePort() > 0 && msg.getNodePort() <= 65535) {
            monvo.setNodePort(msg.getNodePort());
        }
        if (msg.getSshPort() != null && msg.getSshPort() > 0 && msg.getSshPort() <= 65535) {
            monvo.setSshPort(msg.getSshPort());
        }
        if (msg.getSshUsername() != null) {
            monvo.setSshUsername(msg.getSshUsername());
        }
        if (msg.getSshPassword() != null) {
            monvo.setSshPassword(msg.getSshPassword());
        }
        dbf.update(monvo);
        evt.setInventory(SurfsBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void handle(APIRemoveNodeFromSurfsBackupStorageMsg msg) {
        SimpleQuery<SurfsBackupStorageNodeVO> q = dbf.createQuery(SurfsBackupStorageNodeVO.class);
        q.add(SurfsBackupStorageNodeVO_.hostname, Op.IN, msg.getNodeHostnames());
        q.add(SurfsBackupStorageNodeVO_.backupStorageUuid, Op.EQ, self.getUuid());
        List<SurfsBackupStorageNodeVO> vos = q.list();

        if (!vos.isEmpty()) {
            dbf.removeCollection(vos, SurfsBackupStorageNodeVO.class);
        }

        APIRemoveNodeFromSurfsBackupStorageEvent evt = new APIRemoveNodeFromSurfsBackupStorageEvent(msg.getId());
        evt.setInventory(SurfsBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }
    
    
    private void handle(final APIAddNodeToSurfsBackupStorageMsg msg) {
        final APIAddNodeToSurfsBackupStorageEvent evt = new APIAddNodeToSurfsBackupStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-node-Surfs-backup-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            List<SurfsBackupStorageNodeVO> monVOs = new ArrayList<SurfsBackupStorageNodeVO>();

            @Override
            public void setup() {
            	flow(new NoRollbackFlow() {
                    String __name__ = "node-if-exist";
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                    	for (String url : msg.getNodeUrls()){
                    		NodeUri uri = new NodeUri(url);
                    	    for (SurfsBackupStorageNodeVO nov :getSelf().getNodes()){
                    	    	if (nov.getHostname().equals(uri.getHostname())){
                    	    		trigger.fail(errf.stringToInternalError(
                    	    				String.format("the node[%s] is exists in surfsbackupstorage[%s]",nov.getHostname(),getSelf().getUuid())
                    	    				));
                    	    	}                    	    		
                    	    }
                    	}
                        trigger.next();
                    }
            	});
                flow(new Flow() {
                    String __name__ = "create-node-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getNodeUrls()) {
                            SurfsBackupStorageNodeVO monvo = new SurfsBackupStorageNodeVO();
                            NodeUri uri = new NodeUri(url);
                            monvo.setUuid(Platform.getUuid());
                            monvo.setStatus(NodeStatus.Connecting);
                            monvo.setHostname(uri.getHostname());
                            monvo.setNodeAddr(uri.getHostname());
                            monvo.setNodePort(uri.getNodePort());
                            monvo.setSshPort(uri.getSshPort());
                            monvo.setSshUsername(uri.getSshUsername());
                            monvo.setSshPassword(uri.getSshPassword());
                            monvo.setBackupStorageUuid(self.getUuid());
                            monVOs.add(monvo);
                        }

                        dbf.persistCollection(monVOs);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeCollection(monVOs, SurfsBackupStorageNodeVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<SurfsBackupStorageNodeBase> bases = CollectionUtils.transformToList(monVOs, new Function<SurfsBackupStorageNodeBase, SurfsBackupStorageNodeVO>() {
                            @Override
                            public SurfsBackupStorageNodeBase call(SurfsBackupStorageNodeVO arg) {
                                return new SurfsBackupStorageNodeBase(arg);
                            }
                        });

                        final List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (!errorCodes.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to connect mons", errorCodes));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (SurfsBackupStorageNodeBase base : bases) {
                            base.connect(new Completion(trigger) {
                                @Override
                                public void success() {
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errorCodes.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<SurfsBackupStorageNodeBase> bases = CollectionUtils.transformToList(monVOs, new Function<SurfsBackupStorageNodeBase, SurfsBackupStorageNodeVO>() {
                            @Override
                            public SurfsBackupStorageNodeBase call(SurfsBackupStorageNodeVO arg) {
                                return new SurfsBackupStorageNodeBase(arg);
                            }
                        });

                        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, "unable to add mon to Surfs backup storage", errors));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final SurfsBackupStorageNodeBase base : bases) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.nodeUuid = base.getSelf().getUuid();
                            base.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    if (!rsp.isSuccess()) {
                                        errors.add(errf.stringToOperationError(rsp.getError()));
                                    } else {
                                        String fsid = rsp.fsid;
                                        if (!getSelf().getFsid().equals(fsid)) {
                                            errors.add(errf.stringToOperationError(
                                                    String.format("the mon[ip:%s] returns a fsid[%s] different from the current fsid[%s] of the cep cluster," +
                                                            "are you adding a mon not belonging to current cluster mistakenly?", base.getSelf().getHostname(), fsid, getSelf().getFsid())
                                            ));
                                        }
                                    }

                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errors.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(SurfsBackupStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }
    
    private <T> void singleCall(String path, final AgentCommand cmd, final Class<T> rspClass,String dsthost, final ReturnValueCompletion<T> completion) {
        restf.asyncJsonPost(String.format("http://%s:%s%s", dsthost, SurfsGlobalProperty.BACKUP_STORAGE_AGENT_PORT, path),
                cmd, new JsonAsyncRESTCallback<T>(completion) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(T ret) {
                        completion.success(ret);
                    }

                    @Override
                    public Class<T> getReturnClass() {
                        return rspClass;
                    }
                });
    }

}
