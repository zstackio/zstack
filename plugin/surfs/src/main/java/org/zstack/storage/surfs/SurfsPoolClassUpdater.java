package org.zstack.storage.surfs;

import java.util.ArrayList;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.Platform;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.db.GLock;
import org.zstack.header.storage.primary.ImageCacheVO;

import java.util.*;

import javax.persistence.TypedQuery;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SurfsPoolClassUpdater {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    public static List<SurfsPoolClassVO> poolclss =new ArrayList<SurfsPoolClassVO>();
    private static final CLogger logger = Utils.getLogger(SurfsPoolClassUpdater.class);
    @Transactional
    private void addpoolcls(String fsid,String[] plmsg){
		SurfsPoolClassVO spcvo=new SurfsPoolClassVO();
		spcvo.setUuid(Platform.getUuid());
		spcvo.setFsid(fsid);
		spcvo.setClsname(plmsg[0]);
		spcvo.setDisplayName(plmsg[0]);
		if (plmsg[3].equals("true")){
			spcvo.setIsActive(true);
		}else{
			spcvo.setIsActive(false);
		}
		try{
			spcvo.setTotalCapacity(Long.parseLong(plmsg[1]));
			spcvo.setAvailableCapacity(Long.parseLong(plmsg[2]));
		}catch(Exception ex){
			spcvo.setTotalCapacity(0);
			spcvo.setAvailableCapacity(0);
			logger.warn(String.format(
					"PoolClass[%s] msg  error:TotalCapacity[%s] and AvailableCapacity[%s]",plmsg[0],plmsg[1],plmsg[2]
					));		
		}
		try{
			poolclss.add(spcvo);
		    dbf.getEntityManager().persist(spcvo);
		}catch(Exception ex){
			logger.debug(ex.getMessage());
		}
	    	
    }
    @Transactional
    private void updatepoolcls(SurfsPoolClassVO spcvo,String[] plmsg){
		if (plmsg[3].equals("true")){
			spcvo.setIsActive(true);
		}else{
			spcvo.setIsActive(false);
		}
		try{
			spcvo.setTotalCapacity(Long.parseLong(plmsg[1]));
			spcvo.setAvailableCapacity(Long.parseLong(plmsg[2]));
		}catch(Exception ex){
			spcvo.setTotalCapacity(0);
			spcvo.setAvailableCapacity(0);
			logger.warn(String.format(
					"PoolClass[%s] msg  error:TotalCapacity[%s] and AvailableCapacity[%s]",plmsg[0],plmsg[1],plmsg[2]
					));		
		}
		try{
			dbf.getEntityManager().merge(spcvo);
		}catch(Exception ex){
			logger.debug(ex.getMessage());
		}
	    
	    
    }
    
    @Transactional
    private void refreshspc(){
    	String sql = "select c from SurfsPoolClassVO c";
    	TypedQuery<SurfsPoolClassVO> fq = dbf.getEntityManager().createQuery(sql, SurfsPoolClassVO.class);
    	List<SurfsPoolClassVO> spclist =fq.getResultList();
    	if (spclist == null || spclist.isEmpty()){
    		return;
    	}
    	for(final SurfsPoolClassVO spc : spclist ){
    		poolclss.add(spc);
    	}
    }
    public void update(String fsid,String clsmsg){
    	String[] pools=clsmsg.split(",");
    	if (pools.length == 0){
    		return;
    	}
    	
    	GLock lock = new GLock(String.format("surfs-p-%s", fsid), 240);
    	
    	lock.lock();
    	try{
        	String poolist="";
        	for (int i=0;i < pools.length;i++){
    			String[] plmsg=pools[i].split(":");
    			poolist=poolist + "," + plmsg[0];
    			if (plmsg.length !=4){
    				
    				continue;
    			}
    			
        		if (poolclss.isEmpty()){
        			refreshspc();
        			if (poolclss.isEmpty()) {
        				addpoolcls(fsid,plmsg);
        			}	
        		}else{
        			Iterator<SurfsPoolClassVO> it = poolclss.iterator();
        			int dosign=0;
        			while (it.hasNext()){
        				SurfsPoolClassVO svo=it.next();
        				if (svo.getClsname().equals(plmsg[0])){
        					if (Long.parseLong(plmsg[1]) !=svo.getTotalCapacity() || Long.parseLong(plmsg[2]) !=svo.getAvailableCapacity()){
        						updatepoolcls(svo,plmsg);
        					}
        					dosign=1;
        				}
        			}
        			if (dosign==0){
        				addpoolcls(fsid,plmsg);
        			}
        		}
        	}
        	for (int j=0;j< poolclss.size();j++){
        		if (poolist.indexOf(poolclss.get(j).getClsname()) !=-1){
        			continue;
        		}
        		try{
            		dbf.getEntityManager().remove(poolclss.get(j));
            		poolclss.remove(j);
            		
        		}catch(Exception ex){
        			logger.warn(String.format("Failed to remove the pool type [%s] for out of date",poolclss.get(j).getClsname()));
        		}
        		break;
        	}
    	}finally {
        	lock.unlock();
    	}


    }
    
}