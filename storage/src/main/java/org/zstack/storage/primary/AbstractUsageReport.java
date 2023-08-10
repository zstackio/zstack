package org.zstack.storage.primary;

import com.google.common.collect.EvictingQueue;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractUsageReport<T extends HistoricalUsageAO, K extends StorageCapacityAO> {
    private static final CLogger logger = Utils.getLogger(AbstractUsageReport.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    protected Class<T> usageClass;
    protected Class<K> capacityClass;

    private static final int MAX_CACHE_LENGTHS_BY_DAY = 366;
    private static final long COLLECT_INTERVAL = 1;
    private static final long FORECAST_INTERVAL = 1;
    private static final String HISTORICAL_USED_PHYSICAL_CAPACITIES_PATH = "/tmp/historicalUsedPhysicalCapacities";
    private static final String FORECAST_RESULTS_PATH = "/tmp/forecast";
    private static final String CAPACITY_FORECAST_BIN_FILENAME = "capacity_forecast";
    private static final String CAPACITY_FORECAST_BIN_PATH = "/usr/local/zstack/algorithm/capacity-forecast";


    private static class HistoricalUsage {
        EvictingQueue<Long> historicalUsedPhysicalCapacities = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
        EvictingQueue<Long> totalPhysicalCapacities = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
        EvictingQueue<Long> recordDates = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
        String resourceUuid;

        public EvictingQueue<Long> getHistoricalUsedPhysicalCapacities() {
            return historicalUsedPhysicalCapacities;
        }

        public void setHistoricalUsedPhysicalCapacities(EvictingQueue<Long> historicalUsedPhysicalCapacities) {
            this.historicalUsedPhysicalCapacities = historicalUsedPhysicalCapacities;
        }

        public EvictingQueue<Long> getTotalPhysicalCapacities() {
            return totalPhysicalCapacities;
        }

        public void setTotalPhysicalCapacities(EvictingQueue<Long> totalPhysicalCapacities) {
            this.totalPhysicalCapacities = totalPhysicalCapacities;
        }

        public EvictingQueue<Long> getRecordDates() {
            return recordDates;
        }

        public void setRecordDates(EvictingQueue<Long> recordDates) {
            this.recordDates = recordDates;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }
    }

    private static class usedPhysicalCapacityForecasts {
        List<Long> allForecasts = new ArrayList<>();
        List<Double> futureForecastsInPercent = new ArrayList<>();
        String resourceUuid;

        public usedPhysicalCapacityForecasts(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public List<Long> getAllForecasts() {
            return allForecasts;
        }

        public void setAllForecasts(List<Long> allForecasts) {
            this.allForecasts = allForecasts;
        }

        public List<Double> getFutureForecastsInPercent() {
            return futureForecastsInPercent;
        }

        public void setFutureForecastsInPercent(List<Double> futureForecastsInPercent) {
            this.futureForecastsInPercent = futureForecastsInPercent;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }
    }

    private static class ResourceUsage {
        String resourceUuid;
        Long totalPhysicalCapacity;
        Long usedPhysicalCapacity;

        public ResourceUsage(String resourceUuid, Long totalPhysicalCapacity, Long usedPhysicalCapacity) {
            this.resourceUuid = resourceUuid;
            this.totalPhysicalCapacity = totalPhysicalCapacity;
            this.usedPhysicalCapacity = usedPhysicalCapacity;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public Long getTotalPhysicalCapacity() {
            return totalPhysicalCapacity;
        }

        public void setTotalPhysicalCapacity(Long totalPhysicalCapacity) {
            this.totalPhysicalCapacity = totalPhysicalCapacity;
        }

        public Long getUsedPhysicalCapacity() {
            return usedPhysicalCapacity;
        }

        public void setUsedPhysicalCapacity(Long usedPhysicalCapacity) {
            this.usedPhysicalCapacity = usedPhysicalCapacity;
        }
    }

    private class HistoricalUsageLoader {
        List<T> allHistoricalUsageVOs = new ArrayList<>();
        List<T> usagesToPersist = new ArrayList<>();

        public List<T> getAllHistoricalUsageVOs() {
            return allHistoricalUsageVOs;
        }

        public void setAllHistoricalUsageVOs(List<T> allHistoricalUsageVOs) {
            this.allHistoricalUsageVOs = allHistoricalUsageVOs;
        }

        public List<T> getUsagesToPersist() {
            return usagesToPersist;
        }

        public void setUsagesToPersist(List<T> usagesToPersist) {
            this.usagesToPersist = usagesToPersist;
        }
    }

    protected final Map<String, HistoricalUsage> historicalUsageMap = new HashMap<>();

    protected final Map<String, usedPhysicalCapacityForecasts> usedPhysicalCapacityForecastsMap = new HashMap<>();

    public List<Double> getFutureForecastsInPercent(String resourceUuid) {
        if (!usedPhysicalCapacityForecastsMap.containsKey(resourceUuid)) {
            return new ArrayList<>();
        }
        return usedPhysicalCapacityForecastsMap.get(resourceUuid).getFutureForecastsInPercent();
    }

    private List<ResourceUsage> getResourceUsages() {
        List<ResourceUsage> resourceUsages = new ArrayList<>();

        List<K> capacityVOs = Q.New(capacityClass).list();
        capacityVOs.forEach(capacityVO -> {
            String resourceUuid = capacityVO.getResourceUuid();
            long totalPhysicalCapacity;
            long usedPhysicalCapacity;
            if (capacityVO.getTotalPhysicalCapacity() == 0) {
                int usageLength = historicalUsageMap.get(resourceUuid).getTotalPhysicalCapacities().size() - 1;
                totalPhysicalCapacity = new ArrayList<>(historicalUsageMap.get(resourceUuid).getTotalPhysicalCapacities()).get(usageLength);
                usedPhysicalCapacity = new ArrayList<>(historicalUsageMap.get(resourceUuid).getHistoricalUsedPhysicalCapacities()).get(usageLength);
            } else {
                totalPhysicalCapacity = capacityVO.getTotalPhysicalCapacity() / SizeUnit.GIGABYTE.toByte(1);
                usedPhysicalCapacity = (capacityVO.getTotalPhysicalCapacity() - capacityVO.getAvailablePhysicalCapacity()) / SizeUnit.GIGABYTE.toByte(1);
            }
            ResourceUsage resourceUsage = new ResourceUsage(resourceUuid, totalPhysicalCapacity, usedPhysicalCapacity);
            resourceUsages.add(resourceUsage);
        });
        return resourceUsages;
    }

    private List<String> getResourceUuids() {
        List<String> resourceUuids = new ArrayList<>();
        List<K> vos = Q.New(capacityClass).list();
        vos.forEach(vo -> resourceUuids.add(vo.getResourceUuid()));
        return resourceUuids;
    }

    public Map<String, UsageReport> getUsageReportByResourceUuids(List<String> resourceUuids) {
        Map<String, UsageReport> map = new HashMap<>();

        resourceUuids.forEach(resourceUuid -> {
            UsageReport usageReport = new UsageReport();

            usageReport.setUsedPhysicalCapacitiesForecast(usedPhysicalCapacityForecastsMap.get(resourceUuid).getAllForecasts());
            usageReport.setUsedPhysicalCapacitiesHistory(new ArrayList<>(historicalUsageMap.get(resourceUuid).getHistoricalUsedPhysicalCapacities()));
            usageReport.setTotalPhysicalCapacitiesHistory(new ArrayList<>(historicalUsageMap.get(resourceUuid).getTotalPhysicalCapacities()));
            usageReport.setStartTime(new ArrayList<>(historicalUsageMap.get(resourceUuid).getRecordDates()).get(0));
            usageReport.setInterval(FORECAST_INTERVAL);

            map.put(resourceUuid, usageReport);
        });

        return map;
    }

    private void deleteExpiredHistoricalUsageFromDatabase() {
        SQL.New(usageClass).lt(HistoricalUsageAO_.recordDate,
                Timestamp.valueOf(LocalDate.now().minusDays(366).atStartOfDay())).delete();
    }

    private void deleteExpiredHistoricalUsageFromMap(List<String> resourceUuids) {
        historicalUsageMap.keySet().removeIf(resourceUuid -> !resourceUuids.contains(resourceUuid));
    }

    private void deleteExpiredForecastsFromMap(List<String> resourceUuids) {
        usedPhysicalCapacityForecastsMap.keySet().removeIf(resourceUuid -> !resourceUuids.contains(resourceUuid));
    }

    protected void loadHistoricalUsageFromDatabase() {
        List<T> usagesToPersist = new ArrayList<>();

        Map<String, HistoricalUsageLoader> historicalUsageVOsMap = getHistoricalUsagesFromDatabase();

        historicalUsageVOsMap.forEach((resourceUuid, historicalUsageLoader) -> {
            HistoricalUsage usage = new HistoricalUsage();
            usage.setResourceUuid(resourceUuid);

            List<T> usageAOs = historicalUsageLoader.getAllHistoricalUsageVOs();

            usage.getHistoricalUsedPhysicalCapacities()
                    .addAll(usageAOs.stream().map(HistoricalUsageAO::getUsedPhysicalCapacity).collect(Collectors.toList()));
            usage.getTotalPhysicalCapacities()
                    .addAll(usageAOs.stream().map(HistoricalUsageAO::getTotalPhysicalCapacity).collect(Collectors.toList()));
            usage.getRecordDates()
                    .addAll(usageAOs.stream().map(ao -> ao.getRecordDate().getTime()).collect(Collectors.toList()));

            historicalUsageMap.put(resourceUuid, usage);
            usagesToPersist.addAll(historicalUsageLoader.getUsagesToPersist());
        });

        if (!usagesToPersist.isEmpty()) {
            dbf.persistCollection(usagesToPersist);
        }
    }

    private Map<String, HistoricalUsageLoader> getHistoricalUsagesFromDatabase() {
        Map<String, HistoricalUsageLoader> usageMap = new HashMap<>();

        List<T> allUsageVOs = Q.New(usageClass).orderBy(HistoricalUsageAO_.recordDate, SimpleQuery.Od.ASC).list();

        List<String> resourceUuids = allUsageVOs.stream().map(HistoricalUsageAO::getResourceUuid).distinct().collect(Collectors.toList());
        resourceUuids.forEach(resourceUuid -> {
            List<T> usageVOs = allUsageVOs.stream().filter(vo -> Objects.equals(vo.getResourceUuid(), resourceUuid))
                    .collect(Collectors.toList());

            HistoricalUsageLoader historicalUsageLoader = new HistoricalUsageLoader();
            historicalUsageLoader.setAllHistoricalUsageVOs(usageVOs);

            addMissingDataIfNeeded(usageVOs, historicalUsageLoader);
            usageMap.put(resourceUuid, historicalUsageLoader);
        });
        return usageMap;
    }

    private void addMissingDataIfNeeded(List<T> existedUsageVOs, HistoricalUsageLoader historicalUsageLoader) {
        if (existedUsageVOs.isEmpty()) {
            return;
        }

        T usage = existedUsageVOs.get(existedUsageVOs.size() - 1);

        long nowTime = Timestamp.valueOf(LocalDate.now().atStartOfDay()).getTime();
        long usageTime = usage.getRecordDate().getTime();
        long days = (nowTime - usageTime) / (TimeUnit.DAYS.toMillis(1));

        if (days <= 0) {
            return;
        }

        List<T> usagesToPersist = new ArrayList<>();
        IntStream.range(1, (int) days).forEach(i -> {
            T vo;
            try {
                vo = usageClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            vo.setResourceUuid(usage.getResourceUuid());
            vo.setTotalPhysicalCapacity(usage.getTotalPhysicalCapacity());
            vo.setUsedPhysicalCapacity(usage.getUsedPhysicalCapacity());
            vo.setUsedPhysicalCapacity(usage.getUsedPhysicalCapacity());
            vo.setRecordDate(Timestamp.from(usage.getRecordDate().toInstant().plusMillis(TimeUnit.DAYS.toMillis(i))));

            usagesToPersist.add(vo);
        });
        historicalUsageLoader.getAllHistoricalUsageVOs().addAll(usagesToPersist);
        historicalUsageLoader.setUsagesToPersist(usagesToPersist);
    }

    private void startCollect() {
        logger.debug(String.format("[%s] starts with the interval %s days", this.getClass().getSimpleName(), COLLECT_INTERVAL));

        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.DAYS;
            }

            @Override
            public long getInterval() {
                return COLLECT_INTERVAL;
            }

            @Override
            public String getName() {
                return "collect-primaryStorage-used-physicalCapacity";
            }

            @Override
            public void run() {
                collectUsage(LocalDate.now());
            }
        });

        collectUsage(LocalDate.now());
    }

    private Map<String, Timestamp> getHistoricalLastUsageRecordDateMap() {
        Map<String, Timestamp> recordDateMap = new HashMap<>();

        List<T> allUsageVOs = Q.New(usageClass).orderBy(HistoricalUsageAO_.recordDate, SimpleQuery.Od.ASC).list();

        List<String> resourceUuids = allUsageVOs.stream().map(HistoricalUsageAO::getResourceUuid).distinct().collect(Collectors.toList());
        resourceUuids.forEach(resourceUuid -> {
            List<T> usageVOs = allUsageVOs.stream().filter(vo -> Objects.equals(vo.getResourceUuid(), resourceUuid))
                    .collect(Collectors.toList());
            recordDateMap.put(resourceUuid, usageVOs.get(usageVOs.size() - 1).getRecordDate());
        });
        return recordDateMap;
    }

    private boolean skipCollectUsage(String resourceUuid, LocalDate nowRecordDate) {
        Map<String, Timestamp> historicalLastUsageRecordDateMap = getHistoricalLastUsageRecordDateMap();
        if (!historicalLastUsageRecordDateMap.containsKey(resourceUuid)) {
            return false;
        }

        long nowRecordDateToEpochMilli = Timestamp.valueOf(nowRecordDate.atStartOfDay()).getTime();
        long lastUsageRecordDateMilli = historicalLastUsageRecordDateMap.get(resourceUuid).getTime();
        return nowRecordDateToEpochMilli <= lastUsageRecordDateMilli;
    }

    protected void collectUsage(LocalDate nowRecordDate) {
        List<T> usagesToPersist = new ArrayList<>();
        List<ResourceUsage> resourceUsages = getResourceUsages();
        resourceUsages.forEach(usage -> {
            if (skipCollectUsage(usage.getResourceUuid(), nowRecordDate)) {
                return;
            }

            T vo;
            try {
                vo = usageClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            vo.setResourceUuid(usage.getResourceUuid());
            vo.setTotalPhysicalCapacity(usage.getTotalPhysicalCapacity());
            vo.setUsedPhysicalCapacity(usage.getUsedPhysicalCapacity());
            vo.setRecordDate(Timestamp.valueOf(nowRecordDate.atStartOfDay()));
            usagesToPersist.add(vo);

            if (!historicalUsageMap.containsKey(usage.getResourceUuid())) {
                HistoricalUsage historicalUsage = new HistoricalUsage();
                historicalUsage.setResourceUuid(usage.resourceUuid);
                historicalUsageMap.put(usage.getResourceUuid(), historicalUsage);
            }
            historicalUsageMap.get(usage.getResourceUuid()).getHistoricalUsedPhysicalCapacities().add(usage.getUsedPhysicalCapacity());
            historicalUsageMap.get(usage.getResourceUuid()).getTotalPhysicalCapacities().add(usage.getTotalPhysicalCapacity());
            historicalUsageMap.get(usage.getResourceUuid()).getRecordDates().add(Timestamp.valueOf(nowRecordDate.atStartOfDay()).getTime());
        });

        deleteExpiredHistoricalUsageFromDatabase();
        deleteExpiredHistoricalUsageFromMap(resourceUsages.stream().map(ResourceUsage::getResourceUuid).collect(Collectors.toList()));

        if (!usagesToPersist.isEmpty()) {
            dbf.persistCollection(usagesToPersist);
        }
    }

    private void startForecast() {
        logger.debug(String.format("%s starts with the interval %s day", this.getClass().getSimpleName(), FORECAST_INTERVAL));

        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.DAYS;
            }

            @Override
            public long getInterval() {
                return FORECAST_INTERVAL;
            }

            @Override
            public String getName() {
                return "forecast-used-physical-capacity";
            }

            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
                forecastUsage();
            }
        });

        forecastUsage();
    }

    protected void forecastUsage() {
        List<String> resourceUuids = getResourceUuids();

        resourceUuids.forEach(resourceUuid -> {
            // get used physical capacity and total physical capacity
            List<Long> historicalUsedPhysicalCapacities = new ArrayList<>(historicalUsageMap.get(resourceUuid).getHistoricalUsedPhysicalCapacities());
            List<Long> totalPhysicalCapacities = new ArrayList<>(historicalUsageMap.get(resourceUuid).getTotalPhysicalCapacities());
            Long totalPhysicalCapacity = totalPhysicalCapacities.get(totalPhysicalCapacities.size() - 1);

            // forecast used physical capacity
            List<Long> forecastResults;
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                forecastResults = new Random().longs(historicalUsedPhysicalCapacities.size() + 185).boxed().collect(Collectors.toList());
            } else {
                forecastResults = getForecastResults(resourceUuid, historicalUsedPhysicalCapacities);
                if (forecastResults.isEmpty()) {
                    return;
                }
            }
            List<Double> futureForecastsInPercent = new ArrayList<>();
            IntStream.range(historicalUsedPhysicalCapacities.size(), forecastResults.size()).forEach(i -> {
                futureForecastsInPercent.add(forecastResults.get(i).doubleValue() / totalPhysicalCapacity.doubleValue());
            });

            if (!usedPhysicalCapacityForecastsMap.containsKey(resourceUuid)) {
                usedPhysicalCapacityForecastsMap.put(resourceUuid, new usedPhysicalCapacityForecasts(resourceUuid));
            }
            usedPhysicalCapacityForecastsMap.get(resourceUuid).setAllForecasts(forecastResults);
            usedPhysicalCapacityForecastsMap.get(resourceUuid).setFutureForecastsInPercent(futureForecastsInPercent);
        });

        deleteExpiredForecastsFromMap(resourceUuids);
    }

    private List<Long> getForecastResults(String resourceUuid, List<Long> historicalUsedPhysicalCapacities) {
        String historicalCapacitiesPath = String.format("%s-%s", HISTORICAL_USED_PHYSICAL_CAPACITIES_PATH, resourceUuid);
        String forecastResultsPath = String.format("%s-%s", FORECAST_RESULTS_PATH, resourceUuid);
        String capacityForecastBinPath = CAPACITY_FORECAST_BIN_PATH;
        if (!isCapacityForecastBinExisted(capacityForecastBinPath)) {
            return new ArrayList<>();
        }

        writeHistoricalCapacities(historicalCapacitiesPath, historicalUsedPhysicalCapacities);

        String cmd = String.format("`%s %s %s`", capacityForecastBinPath, historicalCapacitiesPath, forecastResultsPath);
        doForecast(cmd);

        String forecasts = readForecastResults(forecastResultsPath);
        if (forecasts == null) {
            return new ArrayList<>();
        }

        deleteFile(historicalCapacitiesPath);
        deleteFile(forecastResultsPath);

        return Arrays.stream(forecasts.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }

    private void writeHistoricalCapacities(String historicalCapacitiesPath, List<Long> historicalUsedPhysicalCapacities) {
        StringJoiner capacitiesJoiner = new StringJoiner(",");
        historicalUsedPhysicalCapacities.forEach(capacity -> capacitiesJoiner.add(capacity.toString()));
        try {
            File file = new File(historicalCapacitiesPath);
            FileUtils.writeStringToFile(file, capacitiesJoiner.toString(), "UTF-8");
        } catch (IOException e) {
            logger.warn(String.format("failed to write historicalUsedPhysicalCapacities : %s", e.getMessage()));
        }
    }

    private boolean isCapacityForecastBinExisted(String capacityForecastBinPath) {
        boolean isExisted = true;
        File file = new File(capacityForecastBinPath);
        if (!file.exists()) {
            logger.warn(String.format("can not find %s", capacityForecastBinPath));
            isExisted = false;
        }
        return isExisted;
    }

    private void doForecast(String cmd) {
        ShellUtils.run(cmd, false);
    }

    private String readForecastResults(String forecastResultsPath) {
        String forecasts = null;
        try {
            forecasts = FileUtils.readFileToString(new File(forecastResultsPath), "UTF-8");
        } catch (IOException e) {
            logger.warn(String.format("failed to read capacity forecast results : %s", e.getMessage()));
        }
        return forecasts;
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            logger.warn(String.format("Failed to delete file[%s]: %s", filePath, e.getMessage()));
        }
    }

    protected void start() {
        loadHistoricalUsageFromDatabase();

        startCollect();
        startForecast();
    }
}
