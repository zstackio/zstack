package org.zstack.test.core.db;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.test.core.db.PersonVO.Sex;

import javax.persistence.Tuple;
import java.util.Date;
import java.util.List;

public class TestSimpleQuery {
    ComponentLoader loader;
    DatabaseFacade dbf;
    int personNum = 100;

    @Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
    class Persist {
        @Transactional
        void persist() {
            for (int i = 0; i < personNum; i++) {
                PersonVO vo = new PersonVO();
                vo.setAge(i);
                vo.setDate(new Date());
                vo.setDescription("This is person " + String.valueOf(i));
                vo.setMarriage((i % 5 == 0));
                vo.setName("Person" + String.valueOf(i));
                if (i % 2 == 0) {
                    vo.setSex(PersonVO.Sex.FEMALE);
                } else {
                    vo.setSex(PersonVO.Sex.MALE);
                }
                vo.setTitle(String.valueOf(i) + "Person");
                dbf.getEntityManager().persist(vo);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
        new Persist().persist();
    }

    @Test
    public void test() {
        SimpleQuery<PersonVO> query = dbf.createQuery(PersonVO.class);
        query.add(PersonVO_.sex, Op.EQ, Sex.FEMALE);
        List<PersonVO> females = query.list();
        Assert.assertEquals(personNum / 2, females.size());

        query = dbf.createQuery(PersonVO.class);
        query.select(PersonVO_.age);
        query.add(PersonVO_.sex, Op.EQ, Sex.FEMALE);
        List<Integer> femaleAges = query.listValue();
        for (Integer age : femaleAges) {
            Assert.assertEquals(age % 2, 0);
        }

        query = dbf.createQuery(PersonVO.class);
        query.select(PersonVO_.age, PersonVO_.marriage, PersonVO_.name);
        query.add(PersonVO_.marriage, Op.EQ, true);
        query.add(PersonVO_.sex, Op.EQ, Sex.FEMALE);
        List<Tuple> tuples = query.listTuple();
        for (Tuple t : tuples) {
            int age = t.get(0, Integer.class);
            /* all married female should be at age which can divide 10 exactly*/
            Assert.assertEquals(age % 10, 0);
        }

        query = dbf.createQuery(PersonVO.class);
        query.orderBy(PersonVO_.age, Od.DESC);
        List<PersonVO> ps = query.list();
        Assert.assertEquals(ps.get(0).getAge(), this.personNum - 1);

        query = dbf.createQuery(PersonVO.class);
        query.orderBy(PersonVO_.age, Od.ASC);
        ps = query.list();
        Assert.assertEquals(ps.get(0).getAge(), 0);

        query = dbf.createQuery(PersonVO.class);
        long count = query.count();
        Assert.assertEquals(count, this.personNum);
    }

}
