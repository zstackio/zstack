package org.zstack.storage.surfs.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.Message;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.surfs.SurfsConstants;
import org.zstack.storage.surfs.SurfsGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhouhaiping 2017-09-14
 */


public class SurfsPrimaryStorageVmMigrationExtension implements VmInstanceMigrateExtensionPoint {
    public static final String KVM_SURFS_QUERY_PATH = "/surfs/query";
    public static final String SURFS_MIGRATE_PREPARE = "/surfs/primarystorage/migrateprepare";
    public static final String SURFS_MIGRATE_AFTER = "/surfs/primarystorage/migrateafter";

    public static class SurfsQueryRsp extends AgentResponse {
        public String rsp;
    }

    public static class SurfsQueryCmd extends AgentCommand {
        public String query;
    }

    private CLogger logger = Utils.getLogger(SurfsPrimaryStorageVmMigrationExtension.class);
    private Map<String, List<VolumeInventory>> vmVolumes = new ConcurrentHashMap<String, List<VolumeInventory>>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    protected RESTFacade restf;
    private String dsthostname;

    public static class AgentCommand {
        String fsId;
        String uuid;

        public String getFsId() {
            return fsId;
        }

        public void setFsId(String fsId) {
            this.fsId = fsId;
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
    public static class MigrateVmBeforeCmd extends AgentCommand{
    	String rootinstallPath;
    	String datainstallPath;
    }
    public static class MigrateVmBeforeRsp extends AgentResponse{
    	
    }

    @Transactional(readOnly = true)
    private boolean needLink(VmInstanceInventory inv) {
        String sql = "select ps.type from PrimaryStorageVO ps, VolumeVO vol where ps.uuid = vol.primaryStorageUuid" +
                " and vol.uuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", inv.getRootVolumeUuid());
        List<String> res = q.getResultList();
        if (res.isEmpty()) {
            return false;
        }

        String type = res.get(0);
        return SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE.equals(type);
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        
        /*
        SurfsQueryCmd cmd = new SurfsQueryCmd();
        cmd.query = "query";

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(KVM_SURFS_QUERY_PATH);
        msg.setHostUuid(destHostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHostUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        KVMHostAsyncHttpCallReply r = reply.castReply();
        SurfsQueryRsp rsp = r.toResponse(SurfsQueryRsp.class);
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
        }
        */
       
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (!needLink(inv)) {
            return;
        }
    	this.dsthostname="";    	
        String sql = "select ssv.hostname from SurfsPrimaryStorageNodeVO ssv , HostVO hv " +
                      "where ssv.hostname=hv.managementIp and hv.uuid= :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", destHostUuid);
        List<String> res = q.getResultList();
        if (res.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("node[uuid:%s]  is not in surfs primary nodes", destHostUuid)));
        }
        this.dsthostname=res.get(0);
        MigrateVmBeforeCmd cmd=new MigrateVmBeforeCmd();
        cmd.rootinstallPath=inv.getRootVolume().getInstallPath();
        String datavols="";
        for(VolumeInventory dvl :inv.getAllVolumes()){
        	if (cmd.rootinstallPath.equals(dvl.getInstallPath())){
        		continue;
        	}
    		if (datavols.equals("")){
    			datavols= dvl.getInstallPath().split("/")[2] + ":" + dvl.getUuid();   			
    		}else{
    			datavols= datavols + "," + dvl.getInstallPath().split("/")[2] + ":" + dvl.getUuid();
    		}    	  
        };
        class NoMsg extends Message{
        	String smsg;
        	public String getSmsg(){
        		return smsg;
        	}
        	public void setSmsg(String msg){
        		this.smsg=msg;
        	}        	
        }
        NoMsg nomsg=new NoMsg();
        nomsg.setSmsg("start");
        cmd.datainstallPath=datavols;
        httpCall(SURFS_MIGRATE_PREPARE, cmd,MigrateVmBeforeRsp.class,new ReturnValueCompletion<MigrateVmBeforeRsp>(null){
            @Override
            public void success(MigrateVmBeforeRsp ret) {
            	nomsg.setSmsg("end");
                logger.debug(String.format("Success to prepare for vm[%s] before migrate",inv.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {                
            	nomsg.setSmsg("end");
            	logger.warn(String.format("Error to prepare for vm[%s] before migrate",inv.getUuid()));
            }
            
        }); 
        int intevel=30;
        try{
        	int sk=0;
        	while (true){
        		if (nomsg.getSmsg().equals("end")){
        			break;
        		}
        		if (sk > intevel){
        			break;
        		}
            	Thread.sleep(500);
            	sk =sk + 1;
        	}
        }catch(Exception ex){
        	logger.debug(String.format("Error to sleep for migrate[%s] befroe",inv.getUuid()));
        }
                          	
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        if (!needLink(inv)) {
            return;
        }
        this.dsthostname="";    	
        String sql = "select ssv.hostname from SurfsPrimaryStorageNodeVO ssv , HostVO hv " +
                      "where ssv.hostname=hv.managementIp and hv.uuid= :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", srcHostUuid);
        List<String> res = q.getResultList();
        if (res.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
            		
                    String.format("node[uuid:%s]  is not in surfs primary nodes", res.get(0))
            ));           
        }
        this.dsthostname=res.get(0);
        MigrateVmBeforeCmd cmd=new MigrateVmBeforeCmd();
        cmd.rootinstallPath=inv.getRootVolume().getInstallPath();
        String datavols="";
        for(VolumeInventory dvl :inv.getAllVolumes()){
        	if (cmd.rootinstallPath.equals(dvl.getInstallPath())){
        		continue;
        	}
    		if (datavols.equals("")){
    			datavols= dvl.getInstallPath().split("/")[2] + ":" + dvl.getUuid();   			
    		}else{
    			datavols= datavols + "," + dvl.getInstallPath().split("/")[2] + ":" + dvl.getUuid();
    		}    	  
        };
 
        cmd.datainstallPath=datavols;       
        httpCall(SURFS_MIGRATE_AFTER, cmd,MigrateVmBeforeRsp.class,new ReturnValueCompletion<MigrateVmBeforeRsp>(null){
            @Override
            public void success(MigrateVmBeforeRsp ret) {
            	logger.debug(String.format("Success to clean for vm[%s] after migrate",inv.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {                
            	
            	logger.warn(String.format("Error to clean for vm[%s] after migrate",inv.getUuid()));
            }
            
        }); 
   	
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
    }
    
    private <T> void httpCall(String path, MigrateVmBeforeCmd cmd, final Class<T> rspClass, final ReturnValueCompletion<T> completion) {
        restf.asyncJsonPost(String.format("http://%s:%s%s", this.dsthostname, SurfsGlobalProperty.PRIMARY_STORAGE_AGENT_PORT, path),
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
