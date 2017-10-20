package org.zstack.utils;

import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by shixin 2017/10/06
 */
public class VipUseForList {

    public static final String EIP_NETWORK_SERVICE_TYPE = "Eip";
    public static final String LB_NETWORK_SERVICE_TYPE = "LoadBalancer";
    public static final String PORTFORWARDING_NETWORK_SERVICE_TYPE = "PortForwarding";
    public static final String IPSEC_NETWORK_SERVICE_TYPE = "IPsec";
    public static final String SNAT_NETWORK_SERVICE_TYPE = "SNAT";

    /* vip usefor field will be encoded like 'PortForwarding,LoadBalancer' */
    private static final String delimiter = ",";
    private List<String> useForList;
    private static CLogger logger = Utils.getLogger(VipUseForList.class);

    public VipUseForList() {
        this.useForList = new ArrayList<String>();
    }

    public VipUseForList(String useFor) {
        this.useForList = new ArrayList<String>();

        if(useFor != null) {
            String[] itemArray = useFor.split(delimiter);
            for(String item : itemArray){
                useForList.add(item);
            }
        }
    }

    public List<String> getUseForList() {
        return this.useForList;
    }

    public String add(String newItem){
        boolean exist = false;
        Iterator<String> it = this.useForList.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if (item.equals(newItem)) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            this.useForList.add(newItem);
        }

        return this.toString();
    }

    public String del(String delItem) {
        boolean exist = false;
        Iterator<String> it = this.useForList.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if (item.equals(delItem)) {
                it.remove();
            }
        }

        return this.toString();
    }

    public boolean isIncluded(String newItem){
        Iterator<String> it = this.useForList.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if (item.equals(newItem)) {
                return true;
            }
        }

        return false;
    }

    /* true -- sucess, false -- failed*/
    public boolean validate(){
        /* eip and other services(SNAT,LB,IPSec,PF) can not be attached to same vip */
        if(isIncluded(EIP_NETWORK_SERVICE_TYPE) && this.useForList.size() > 1){
            return false;
        }
        else{
            return true;
        }
    }

    public boolean validateNewAdded(String newItem){
        /* eip and other services(SNAT,LB,IPSec,PF) can not be attached to same vip */
        if(newItem.equals(EIP_NETWORK_SERVICE_TYPE)){
            if (this.useForList.isEmpty()){
                return true;
            }
            else {
                return false;
            }
        }
        else{
            if (isIncluded(EIP_NETWORK_SERVICE_TYPE)){
                return false;
            }
            else {
                return true;
            }
        }
    }

    public boolean validateNewAdded(VipUseForList newList){
        Iterator<String> it = newList.getUseForList().iterator();
        while (it.hasNext()){
            String useFor = it.next();
            if(!validateNewAdded(useFor)){
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        if (this.useForList.isEmpty()){
            return null;
        }
        else {
            return String.join(delimiter, this.useForList);
        }
    }
}

