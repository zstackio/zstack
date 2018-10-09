package org.zstack.utils;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IpRangeSet {
    private static Pattern rangePattern = Pattern.compile("^([\\d.]+)(-[\\d.]+)?$");
    private RangeSet rangeSet = new RangeSet();

    private static String IP_SET_SEPARATOR = ",";
    private static String IP_SET_INVERT_PREFIX = "^";

    private void closed(String origin){
        Matcher matcher = rangePattern.matcher(origin);
        if (matcher.matches()) {
            Long start = NetworkUtils.ipv4StringToLong(matcher.group(1));
            Long end = Optional.ofNullable(matcher.group(2)).map(it -> NetworkUtils.ipv4StringToLong(it.replaceFirst("-", ""))).orElse(start);
            rangeSet.closed(start, end);
        } else {
            throw new IllegalArgumentException(String.format("illegal word[%s] for ip range", origin));
        }
    }

    public static IpRangeSet valueOf(String word){
        IpRangeSet result = new IpRangeSet();
        Set<Long> values = originLongValueOf(word);
        result.rangeSet = RangeSet.valueOf(values);
        return result;
    }

    public static IpRangeSet valueOf(Collection<Long> numbers){
        IpRangeSet result = new IpRangeSet();
        result.rangeSet = RangeSet.valueOf(numbers);
        return result;
    }

    public static Set<String> listAllIps(String word) {
        Set<Long> ipLongValue = originLongValueOf(word);
        return ipLongValue.stream().map(NetworkUtils::longToIpv4String).collect(Collectors.toSet());
    }

    public static Set<Long> originLongValueOf(String word){
        word = StringUtils.deleteWhitespace(word);
        String[] sets = word.split(IP_SET_SEPARATOR);
        IpRangeSet contain = new IpRangeSet();
        IpRangeSet exclude = new IpRangeSet();
        for (String set : sets) {
            if (set.startsWith(IP_SET_INVERT_PREFIX)) {
                exclude.closed(set.substring(1));
            } else {
                contain.closed(set);
            }
        }

        Set<Long> values = contain.rangeSet.values();
        values.removeAll(exclude.rangeSet.values());
        if (values.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid ipset [%s]", word));
        }

        return values;
    }

    @Override
    public String toString(){
        return String.join(IP_SET_SEPARATOR, rangeSet.getRanges().stream().map(range ->
                range.getStart() == range.getEnd() ? String.valueOf(range.getStart()) :
                        String.format("%s-%s", range.getStart(), range.getEnd()))
                .collect(Collectors.toList()));
    }
}