//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class NamespaceValue extends ApiPropertyBase {
    SubnetListType ipv4_cidr;
    AutonomousSystemsType asn;
    MacAddressesType mac_addr;
    List<AsnRangeType> asn_ranges;
    List<String> serial_nums;
    public NamespaceValue() {
    }
    public NamespaceValue(SubnetListType ipv4_cidr, AutonomousSystemsType asn, MacAddressesType mac_addr, List<AsnRangeType> asn_ranges, List<String> serial_nums) {
        this.ipv4_cidr = ipv4_cidr;
        this.asn = asn;
        this.mac_addr = mac_addr;
        this.asn_ranges = asn_ranges;
        this.serial_nums = serial_nums;
    }
    public NamespaceValue(SubnetListType ipv4_cidr) {
        this(ipv4_cidr, null, null, null, null);    }
    public NamespaceValue(SubnetListType ipv4_cidr, AutonomousSystemsType asn) {
        this(ipv4_cidr, asn, null, null, null);    }
    public NamespaceValue(SubnetListType ipv4_cidr, AutonomousSystemsType asn, MacAddressesType mac_addr) {
        this(ipv4_cidr, asn, mac_addr, null, null);    }
    public NamespaceValue(SubnetListType ipv4_cidr, AutonomousSystemsType asn, MacAddressesType mac_addr, List<AsnRangeType> asn_ranges) {
        this(ipv4_cidr, asn, mac_addr, asn_ranges, null);    }
    
    public SubnetListType getIpv4Cidr() {
        return ipv4_cidr;
    }
    
    public void setIpv4Cidr(SubnetListType ipv4_cidr) {
        this.ipv4_cidr = ipv4_cidr;
    }
    
    
    public AutonomousSystemsType getAsn() {
        return asn;
    }
    
    public void setAsn(AutonomousSystemsType asn) {
        this.asn = asn;
    }
    
    
    public MacAddressesType getMacAddr() {
        return mac_addr;
    }
    
    public void setMacAddr(MacAddressesType mac_addr) {
        this.mac_addr = mac_addr;
    }
    
    
    public List<AsnRangeType> getAsnRanges() {
        return asn_ranges;
    }
    
    
    public void addAsnRanges(AsnRangeType obj) {
        if (asn_ranges == null) {
            asn_ranges = new ArrayList<AsnRangeType>();
        }
        asn_ranges.add(obj);
    }
    public void clearAsnRanges() {
        asn_ranges = null;
    }
    
    
    public void addAsnRanges(Integer asn_min, Integer asn_max) {
        if (asn_ranges == null) {
            asn_ranges = new ArrayList<AsnRangeType>();
        }
        asn_ranges.add(new AsnRangeType(asn_min, asn_max));
    }
    
    
    public List<String> getSerialNums() {
        return serial_nums;
    }
    
    
    public void addSerialNums(String obj) {
        if (serial_nums == null) {
            serial_nums = new ArrayList<String>();
        }
        serial_nums.add(obj);
    }
    public void clearSerialNums() {
        serial_nums = null;
    }
    
}
