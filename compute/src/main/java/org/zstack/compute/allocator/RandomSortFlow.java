package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostSortorFlow;

import java.util.Collections;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RandomSortFlow extends AbstractHostSortorFlow {
    @Override
    public void sort() {
        Collections.shuffle(candidates);
    }
}
