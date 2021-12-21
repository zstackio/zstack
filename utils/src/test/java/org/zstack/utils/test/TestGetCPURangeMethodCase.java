package org.zstack.utils.test;

import org.apache.commons.collections4.Get;
import org.junit.Test;
import org.zstack.utils.string.GetCPURangeMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestGetCPURangeMethodCase {
    String betweenCPURangeString = "0-5";
    String eachCPURangeString = "6,8,9,11";
    String allString = "0-9,^7,11";
    String errorString = "1,^1";
    Integer maxCPUID = 11;
    static List<Integer> betweenCPURange = IntStream.rangeClosed(0, 5).boxed().sorted().collect(Collectors.toList());
    static List<Integer> eachCPURange =Stream.of(6,8,9,11).sorted().collect(Collectors.toList());
    static List<Integer> all = Stream.of(0,1,2,3,4,5,6,8,9,11).sorted().collect(Collectors.toList());

    @Test
    public void testBetweenCPURange() {
        List<Integer> result = GetCPURangeMethod.getCPURange(betweenCPURangeString);
        result = result.stream().sorted().collect(Collectors.toList());
        assert betweenCPURange.size() == result.size();
        assert betweenCPURange.toString().equals(result.toString());
    }

    @Test
    public void testEachCPURange() {
        List<Integer> result = GetCPURangeMethod.getCPURange(eachCPURangeString);
        result = result.stream().sorted().collect(Collectors.toList());
        assert eachCPURange.size() == result.size();
        assert eachCPURange.toString().equals(result.toString());
    }

    @Test
    public void testGetCPURange() {
        List<Integer> result = GetCPURangeMethod.getCPURange(allString);
        result = result.stream().sorted().collect(Collectors.toList());

        assert all.size() == result.size();
        assert all.toString().equals(result.toString());
    }

    @Test
    public void testGetMaxCPUID() {
        int maxID = GetCPURangeMethod.getMaxCPUIDInString(eachCPURangeString);
        assert maxID == maxCPUID;

        maxID = GetCPURangeMethod.getMaxCPUIDInString(allString);
        assert maxID == maxCPUID;


        maxID = GetCPURangeMethod.getMaxCPUIDInString(betweenCPURangeString);
        assert maxID == 5;

    }

    @Test
    public void testGetErrorCPURange() {
        List<Integer> result = GetCPURangeMethod.getCPURange(errorString);
        assert result.isEmpty();

        int maxID = GetCPURangeMethod.getMaxCPUIDInString(errorString);
        assert maxID == -1;
    }
}
