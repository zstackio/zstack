package org.zstack.storage.primary;

import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.Ts;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaCoefficients;
import com.google.common.collect.EvictingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private static final long MAX_FORECAST_STEPS = 31 * 6;
    private static final long START_FORECAST_DAYS = 15;
    private static final long FORECAST_INTERVAL = 1;
    private static final int THREE_DAYS_CACHE = 3;

    private static class HistoricalUsage {
        EvictingQueue<Long> historicalUsedPhysicalCapacities;
        EvictingQueue<Long> historicalUsedPhysicalCapacityForecasts;
        EvictingQueue<Long> totalPhysicalCapacities;
        EvictingQueue<Long> recordDates;
        String resourceUuid;
        String resourceType;

        public EvictingQueue<Long> getHistoricalUsedPhysicalCapacities() {
            return historicalUsedPhysicalCapacities;
        }

        public void setHistoricalUsedPhysicalCapacities(EvictingQueue<Long> historicalUsedPhysicalCapacities) {
            this.historicalUsedPhysicalCapacities = historicalUsedPhysicalCapacities;
        }

        public EvictingQueue<Long> getHistoricalUsedPhysicalCapacityForecasts() {
            return historicalUsedPhysicalCapacityForecasts;
        }

        public void setHistoricalUsedPhysicalCapacityForecasts(EvictingQueue<Long> historicalUsedPhysicalCapacityForecasts) {
            this.historicalUsedPhysicalCapacityForecasts = historicalUsedPhysicalCapacityForecasts;
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

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }
    }

    private static class usedPhysicalCapacityForecasts {
        List<Long> forecasts = new ArrayList<>();
        List<Double> forecastsInPercent = new ArrayList<>();
        String resourceUuid;

        public usedPhysicalCapacityForecasts(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public List<Long> getForecasts() {
            return forecasts;
        }

        public void setForecasts(List<Long> forecasts) {
            this.forecasts = forecasts;
        }

        public List<Double> getForecastsInPercent() {
            return forecastsInPercent;
        }

        public void setForecastsInPercent(List<Double> forecastsInPercent) {
            this.forecastsInPercent = forecastsInPercent;
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

    protected final Map<String, HistoricalUsage> historicalUsageMap = new HashMap<>();

    protected final Map<String, usedPhysicalCapacityForecasts> usedPhysicalCapacityForecastsMap = new HashMap<>();

    public List<Double> getUsedPhysicalCapacityForecastsInPercent(String resourceUuid) {
        return usedPhysicalCapacityForecastsMap.get(resourceUuid).getForecastsInPercent();
    }

    private List<ResourceUsage> getResourceUsagesUsages() {
        List<K> capacityVOs = Q.New(capacityClass).list();
        List<ResourceUsage> resourceUsages = new ArrayList<>();
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

    private Map<String, T> getCurrentDayUsage(Timestamp recordDate) {
        Map<String, T> usageMap = new HashMap<>();

        List<T> usageVOs = Q.New(usageClass)
                .eq(HistoricalUsageAO_.totalPhysicalCapacity, 0)
                .eq(HistoricalUsageAO_.recordDate, recordDate)
                .list();

        usageVOs.forEach(vo -> usageMap.put(vo.getResourceUuid(), vo));
        return usageMap;
    }

    private T convertUsage(ResourceUsage usage, Timestamp recordDate, T existsHistoryAO) {
        if (existsHistoryAO == null) {
            T vo;
            try {
                vo = usageClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            vo.setResourceUuid(usage.getResourceUuid());
            vo.setTotalPhysicalCapacity(usage.getTotalPhysicalCapacity());
            vo.setUsedPhysicalCapacity(usage.getUsedPhysicalCapacity());
            vo.setRecordDate(recordDate);
            return vo;
        } else {
            existsHistoryAO.setTotalPhysicalCapacity(usage.getTotalPhysicalCapacity());
            existsHistoryAO.setUsedPhysicalCapacity(usage.getUsedPhysicalCapacity());
            return existsHistoryAO;
        }
    }

    private void deleteExpiredHistoricalUsageFromDatabase() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        SQL.New(usageClass).lt(HistoricalUsageAO_.recordDate, calendar.getTime()).delete();
    }

    private void deleteExpiredHistoricalUsageFromMap(List<String> resourceUuids) {
        historicalUsageMap.keySet().removeIf(resourceUuid -> !resourceUuids.contains(resourceUuid));
    }

    private class HistoricalUsageLoader {
        List<T> usages = new ArrayList<>();
        List<T> usagesToPersist = new ArrayList<>();
        List<T> usagesToUpdate = new ArrayList<>();

        public List<T> getUsages() {
            return usages;
        }

        public void setUsages(List<T> usages) {
            this.usages = usages;
        }

        public List<T> getUsagesToPersist() {
            return usagesToPersist;
        }

        public void setUsagesToPersist(List<T> usagesToPersist) {
            this.usagesToPersist = usagesToPersist;
        }

        public List<T> getUsagesToUpdate() {
            return usagesToUpdate;
        }

        public void setUsagesToUpdate(List<T> usagesToUpdate) {
            this.usagesToUpdate = usagesToUpdate;
        }
    }

    private Map<String, HistoricalUsageLoader> getHistoricalUsagesFromDatabase() {
        Map<String, HistoricalUsageLoader> usageMap = new HashMap<>();

        List<T> allUsageVOs = Q.New(usageClass).orderBy(HistoricalUsageAO_.recordDate, SimpleQuery.Od.ASC).list();

        List<String> resourceUuids = allUsageVOs.stream().map(HistoricalUsageAO::getResourceUuid)
                .distinct().collect(Collectors.toList());

        resourceUuids.forEach(resourceUuid -> {
            List<T> usageVOs = allUsageVOs.stream()
                    .filter(vo -> Objects.equals(vo.getResourceUuid(), resourceUuid) && vo.getTotalPhysicalCapacity() > 0)
                    .collect(Collectors.toList());

            List<T> cacheUsageVOs = allUsageVOs.stream()
                    .filter(vo -> Objects.equals(vo.getResourceUuid(), resourceUuid) && vo.getTotalPhysicalCapacity() == 0)
                    .collect(Collectors.toList());

            HistoricalUsageLoader historicalUsageLoader = new HistoricalUsageLoader();
            historicalUsageLoader.setUsages(usageVOs);
            addMissingDataIfNeeded(historicalUsageLoader, cacheUsageVOs);

            usageMap.put(resourceUuid, historicalUsageLoader);
        });
        return usageMap;
    }

    private void addMissingDataIfNeeded(HistoricalUsageLoader historicalUsageLoader, List<T> cacheUsageVOs) {
        T usage = historicalUsageLoader.getUsages().get(historicalUsageLoader.getUsages().size() - 1);

        long nowTime = Timestamp.valueOf(LocalDate.now().atStartOfDay()).getTime();
        long usageTime = usage.getRecordDate().getTime();
        long days = (nowTime - usageTime) / (TimeUnit.DAYS.toMillis(1));

        if (days == 0) {
            return;
        }

        if (!cacheUsageVOs.isEmpty()) {
            List<T> usagesToUpdate = new ArrayList<>();
            IntStream.range(0, days < cacheUsageVOs.size() ? (int) days : cacheUsageVOs.size()).forEach(i -> {
                cacheUsageVOs.get(i).setTotalPhysicalCapacity(usage.getTotalPhysicalCapacity());
                cacheUsageVOs.get(i).setUsedPhysicalCapacity(usage.getUsedPhysicalCapacity());
                usagesToUpdate.add(cacheUsageVOs.get(i));
            });

            historicalUsageLoader.getUsages().addAll(usagesToUpdate);
            historicalUsageLoader.setUsagesToUpdate(usagesToUpdate);
        }

        if (days - cacheUsageVOs.size() <= 0) {
            return;
        }

        List<T> usagesToPersist = new ArrayList<>();
        IntStream.range(1, (int) days - cacheUsageVOs.size()).forEach(i -> {
            T vo;
            try {
                vo = usageClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            T usageVO = historicalUsageLoader.getUsages().get(historicalUsageLoader.getUsages().size() - 1);
            vo.setResourceUuid(usageVO.getResourceUuid());
            vo.setTotalPhysicalCapacity(usageVO.getTotalPhysicalCapacity());
            vo.setUsedPhysicalCapacity(usageVO.getUsedPhysicalCapacity());
            vo.setUsedPhysicalCapacity(usageVO.getUsedPhysicalCapacity());
            vo.setHistoricalForecast(usageVO.getHistoricalForecast());
            vo.setRecordDate(Timestamp.from(usageVO.getRecordDate().toInstant().plusMillis(TimeUnit.DAYS.toMillis(i))));

            usagesToPersist.add(vo);
        });
        historicalUsageLoader.setUsagesToPersist(usagesToPersist);
        historicalUsageLoader.getUsages().addAll(usagesToPersist);
    }

    protected void loadHistoricalUsageFromDatabase() {
        List<T> usagesToPersist = new ArrayList<>();
        List<T> usagesToUpdate = new ArrayList<>();

        Map<String, HistoricalUsageLoader> historicalUsageVOsMap = getHistoricalUsagesFromDatabase();

        historicalUsageVOsMap.forEach((resourceUuid, historicalUsageLoader) -> {
            HistoricalUsage usage = new HistoricalUsage();
            usage.setResourceUuid(resourceUuid);

            List<T> usageAOs = historicalUsageLoader.getUsages();

            EvictingQueue<Long> queue = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
            queue.addAll(usageAOs.stream().map(HistoricalUsageAO::getUsedPhysicalCapacity).collect(Collectors.toList()));
            usage.setHistoricalUsedPhysicalCapacities(queue);

            queue = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
            queue.addAll(usageAOs.stream().map(HistoricalUsageAO::getHistoricalForecast).collect(Collectors.toList()));
            usage.setHistoricalUsedPhysicalCapacityForecasts(queue);

            queue = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
            queue.addAll(usageAOs.stream().map(HistoricalUsageAO::getTotalPhysicalCapacity).collect(Collectors.toList()));
            usage.setTotalPhysicalCapacities(queue);

            queue = EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY);
            queue.addAll(usageAOs.stream().map(ao -> ao.getRecordDate().getTime()).collect(Collectors.toList()));
            usage.setRecordDates(queue);

            historicalUsageMap.put(resourceUuid, usage);
            usagesToPersist.addAll(historicalUsageLoader.getUsagesToPersist());
            usagesToUpdate.addAll(historicalUsageLoader.getUsagesToUpdate());
        });

        if (!usagesToPersist.isEmpty()) {
            dbf.persistCollection(usagesToPersist);
        }
        if (!usagesToUpdate.isEmpty()) {
            dbf.updateCollection(usagesToUpdate);
        }
    }

    public Map<String, UsageReport> getUsageReportByResourceUuids(List<String> resourceUuids) {
        Map<String, UsageReport> map = new HashMap<>();

        resourceUuids.forEach(resourceUuid -> {
            UsageReport usageReport = new UsageReport();

            usageReport.setUsedPhysicalCapacitiesHistory(new ArrayList<>(historicalUsageMap.get(resourceUuid).getHistoricalUsedPhysicalCapacities()));
            usageReport.setTotalPhysicalCapacitiesHistory(new ArrayList<>(historicalUsageMap.get(resourceUuid).getTotalPhysicalCapacities()));

            List<Long> usedPhysicalCapacitiesForecast = new ArrayList<>(historicalUsageMap.get(resourceUuid).getHistoricalUsedPhysicalCapacityForecasts());
            usedPhysicalCapacitiesForecast.addAll(usedPhysicalCapacityForecastsMap.get(resourceUuid).getForecasts());
            usageReport.setUsedPhysicalCapacitiesForecast(usedPhysicalCapacitiesForecast);

            usageReport.setStartTime(new ArrayList<>(historicalUsageMap.get(resourceUuid).getRecordDates()).get(0));
            usageReport.setInterval(FORECAST_INTERVAL);

            map.put(resourceUuid, usageReport);
        });

        return map;
    }

    private List<String> getResourceUuids() {
        List<String> resourceUuids = new ArrayList<>();
        List<K> vos = Q.New(capacityClass).list();
        vos.forEach(vo -> resourceUuids.add(vo.getResourceUuid()));
        return resourceUuids;
    }

    private List<T> recordForecastForFutureThreeDays(String resourceUuid, Long lastHistoricalUsageDate,
                                                     List<Long> forecastList, List<T> cacheUsageVOs) {
        List<T> usageVOs = new ArrayList<>();
        long lastHistoricalCacheRecordDate = CollectionUtils.isEmpty(cacheUsageVOs) ? lastHistoricalUsageDate :
                cacheUsageVOs.get(cacheUsageVOs.size() - 1).getRecordDate().getTime();

        int persistHistoricalCacheSize = THREE_DAYS_CACHE - cacheUsageVOs.size();
        IntStream.range(1, persistHistoricalCacheSize + 1).forEach(i -> {
            T vo;
            try {
                vo = usageClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            vo.setResourceUuid(resourceUuid);
            vo.setHistoricalForecast(forecastList.get(cacheUsageVOs.size() + i - 1));
            LocalDate localDate = Instant.ofEpochMilli(lastHistoricalCacheRecordDate).atOffset(ZoneOffset.UTC).toLocalDate();
            vo.setRecordDate(Timestamp.valueOf(localDate.plusDays(i).atStartOfDay()));
            usageVOs.add(vo);
        });

        return new ArrayList<>(usageVOs);
    }

    private Map<String, List<T>> getAllExitedCacheUsage(Timestamp recordDate) {
        Map<String, List<T>> usageMap = new HashMap<>();

        getResourceUuids().forEach(resourceUuid -> usageMap.put(resourceUuid, new ArrayList<>()));

        List<T> allExitedCacheUsageVOs = Q.New(usageClass)
                .eq(HistoricalUsageAO_.totalPhysicalCapacity, 0)
                .gt(HistoricalUsageAO_.recordDate, recordDate)
                .orderBy(HistoricalUsageAO_.recordDate, SimpleQuery.Od.ASC)
                .list();

        allExitedCacheUsageVOs.forEach(vo -> usageMap.get(vo.getResourceUuid()).add(vo));
        return usageMap;
    }

    private Arima getArimaModel(List<Long> historicalUsedPhysicalCapacities) {
        TimeSeries timeSeries = Ts.newMonthlySeries(2023, 1,
                historicalUsedPhysicalCapacities.stream().mapToDouble(Long::doubleValue).toArray());
        Arima.FittingStrategy fittingStrategy = Arima.FittingStrategy.CSSML;
        ArimaCoefficients coefficients = ArimaCoefficients.builder()
                .setMACoeffs(-0.68)
                .setDifferences(1)
                .setSeasonalDifferences(1)
                .build();
        return Arima.model(timeSeries, coefficients, fittingStrategy);
    }

    private List<Long> getForecastFromModelOutputs(Forecast forecast) {
        String[] forecastSplits = forecast.toString().split("\n");
        List<Long> forecastList = new ArrayList<>();
        for (int i = 3; i < forecastSplits.length - 1; i++) {
            String[] row = forecastSplits[i].split("\\|");
            long value = (long) Double.parseDouble(row[2].trim());
            forecastList.add((long) Math.round(value));
        }
        return forecastList;
    }

    private List<Long> doForecastUsedPhysicalCapacity(List<Long> historicalUsedPhysicalCapacities) {
        long historicalUsageSize = historicalUsedPhysicalCapacities.size();
        if (historicalUsageSize < START_FORECAST_DAYS) {
            return new ArrayList<>();
        }

        Arima model = getArimaModel(historicalUsedPhysicalCapacities);
        return getForecastFromModelOutputs(model.forecast((int) MAX_FORECAST_STEPS));
    }

    private void deleteExpiredForecastsFromMap(List<String> resourceUuids) {
        usedPhysicalCapacityForecastsMap.keySet().removeIf(resourceUuid -> !resourceUuids.contains(resourceUuid));
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
                collectUsage();
            }
        });

        collectUsage();
    }

    protected void collectUsage() {
        List<T> usagesToPersist = new ArrayList<>();
        List<T> usagesToUpdate = new ArrayList<>();

        Timestamp recordDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Map<String, T> existsHistoryUsages = getCurrentDayUsage(recordDate);

        List<ResourceUsage> resourceUsages = getResourceUsagesUsages();
        resourceUsages.forEach(usage -> {
            T existHistoryAO = existsHistoryUsages.get(usage.getResourceUuid());
            T usageAO = convertUsage(usage, recordDate, existHistoryAO);
            if (usageAO == existHistoryAO) {
                usagesToUpdate.add(usageAO);
            } else {
                usagesToPersist.add(usageAO);
            }

            if (!historicalUsageMap.containsKey(usage.getResourceUuid())) {
                HistoricalUsage historicalUsage = new HistoricalUsage();
                historicalUsage.setResourceUuid(usage.resourceUuid);
                historicalUsage.setHistoricalUsedPhysicalCapacityForecasts(EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY));
                historicalUsage.setHistoricalUsedPhysicalCapacities(EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY));
                historicalUsage.setTotalPhysicalCapacities(EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY));
                historicalUsage.setRecordDates(EvictingQueue.create(MAX_CACHE_LENGTHS_BY_DAY));
                historicalUsageMap.put(usage.getResourceUuid(), historicalUsage);
            }
            historicalUsageMap.get(usage.getResourceUuid()).getHistoricalUsedPhysicalCapacityForecasts().add(usageAO.getHistoricalForecast());
            historicalUsageMap.get(usage.getResourceUuid()).getHistoricalUsedPhysicalCapacities().add(usageAO.getUsedPhysicalCapacity());
            historicalUsageMap.get(usage.getResourceUuid()).getTotalPhysicalCapacities().add(usageAO.getTotalPhysicalCapacity());
            historicalUsageMap.get(usage.getResourceUuid()).getRecordDates().add(usageAO.getRecordDate().getTime());
        });

        deleteExpiredHistoricalUsageFromDatabase();
        deleteExpiredHistoricalUsageFromMap(resourceUsages.stream().map(ResourceUsage::getResourceUuid).collect(Collectors.toList()));

        if (!usagesToUpdate.isEmpty()) {
            dbf.updateCollection(usagesToUpdate);
        }
        if (!usagesToPersist.isEmpty()) {
            dbf.persistCollection(usagesToPersist);
        }
    }

    private void startForecast() {
        logger.debug(String.format("%s starts with the interval %s year", this.getClass().getSimpleName(), FORECAST_INTERVAL));

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
        List<T> cacheUsagesToPersist = new ArrayList<>();
        Map<String, List<T>> allExitedCacheUsages = getAllExitedCacheUsage(Timestamp.valueOf(LocalDate.now().atStartOfDay()));

        List<String> resourceUuids = getResourceUuids();
        resourceUuids.forEach(resourceUuid -> {
            // get used physical capacity list and total physical capacity list
            List<Long> historicalUsedPhysicalCapacities = new ArrayList<>(historicalUsageMap.get(resourceUuid).getHistoricalUsedPhysicalCapacities());
            List<Long> totalPhysicalCapacities = new ArrayList<>(historicalUsageMap.get(resourceUuid).getTotalPhysicalCapacities());
            Long totalPhysicalCapacity = totalPhysicalCapacities.get(totalPhysicalCapacities.size() - 1);

            // forecast used physical capacity
            List<Long> forecasts = doForecastUsedPhysicalCapacity(historicalUsedPhysicalCapacities);
            if (forecasts.isEmpty()) {
                return;
            }
            List<Double> forecastsInPercent = forecasts.stream().mapToDouble(forecast -> forecast.doubleValue() / totalPhysicalCapacity.doubleValue())
                    .boxed().collect(Collectors.toList());

            // record forecast for future 3 days
            List<Long> recordDate = new ArrayList<>(historicalUsageMap.get(resourceUuid).getRecordDates());
            cacheUsagesToPersist.addAll(recordForecastForFutureThreeDays(
                    resourceUuid, recordDate.get(recordDate.size() - 1), forecasts, allExitedCacheUsages.get(resourceUuid)));

            if (!usedPhysicalCapacityForecastsMap.containsKey(resourceUuid)) {
                usedPhysicalCapacityForecastsMap.put(resourceUuid, new usedPhysicalCapacityForecasts(resourceUuid));
            }
            usedPhysicalCapacityForecastsMap.get(resourceUuid).setForecasts(forecasts);
            usedPhysicalCapacityForecastsMap.get(resourceUuid).setForecastsInPercent(forecastsInPercent);
        });

        if (!cacheUsagesToPersist.isEmpty()) {
            dbf.persistCollection(cacheUsagesToPersist);
        }

        deleteExpiredForecastsFromMap(resourceUuids);
    }

    protected void start() {
        loadHistoricalUsageFromDatabase();

        startCollect();
        startForecast();
    }
}
