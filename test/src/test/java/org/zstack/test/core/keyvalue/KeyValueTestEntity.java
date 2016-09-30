package org.zstack.test.core.keyvalue;

import org.zstack.header.core.keyvalue.KeyValueEntity;
import org.zstack.header.vo.Uuid;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
public class KeyValueTestEntity implements KeyValueEntity {
    int a1 = 10;
    int b1 = 100;
    String c2 = "hello world";
    Integer d = 9;
    Map<String, String> dict = map(e("last name", "zhang"), e("first name", "xin"));
    List<Integer> list1 = new ArrayList<Integer>();
    KeyValueTestEntity child;
    Map<String, KeyValueTestEntity> dict2;
    List<KeyValueTestEntity> list2;
    @Uuid
    String uuid;
    Date date = new Date();
    Timestamp timestamp = new Timestamp(date.getTime());

    public Integer getD() {
        return d;
    }

    public void setD(Integer d) {
        this.d = d;
    }

    public int getA1() {
        return a1;
    }

    public void setA1(int a1) {
        this.a1 = a1;
    }

    public int getB1() {
        return b1;
    }

    public void setB1(int b1) {
        this.b1 = b1;
    }

    public String getC2() {
        return c2;
    }

    public void setC2(String c2) {
        this.c2 = c2;
    }

    public Map<String, String> getDict() {
        return dict;
    }

    public void setDict(Map<String, String> dict) {
        this.dict = dict;
    }

    public List<Integer> getList1() {
        return list1;
    }

    public void setList1(List<Integer> list1) {
        this.list1 = list1;
    }

    public KeyValueTestEntity getChild() {
        return child;
    }

    public void setChild(KeyValueTestEntity child) {
        this.child = child;
    }

    public Map<String, KeyValueTestEntity> getDict2() {
        return dict2;
    }

    public void setDict2(Map<String, KeyValueTestEntity> dict2) {
        this.dict2 = dict2;
    }

    public List<KeyValueTestEntity> getList2() {
        return list2;
    }

    public void setList2(List<KeyValueTestEntity> list2) {
        this.list2 = list2;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getUuid() {
        return uuid;
    }
}
