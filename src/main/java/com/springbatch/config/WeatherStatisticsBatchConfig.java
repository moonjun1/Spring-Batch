package com.springbatch.config;

import com.springbatch.entity.WeatherData;
import com.springbatch.entity.WeatherStatistics;
import com.springbatch.repository.WeatherDataRepository;
import com.springbatch.repository.WeatherStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 날씨 통계 생성 배치 설정
 */
@Slf4j
@Configuration
public class WeatherStatisticsBatchConfig {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private WeatherDataRepository weatherDataRepository;
    
    @Autowired
    private WeatherStatisticsRepository weatherStatisticsRepository;
    
    /**
     * 일일 날씨 통계 생성 Job
     */
    @Bean
    public Job generateDailyWeatherStatisticsJob(Step dailyStatisticsStep) {
        return new JobBuilder("generateDailyWeatherStatisticsJob", jobRepository)
                .start(dailyStatisticsStep)
                .build();
    }
    
    /**
     * 일일 통계 생성 Step
     */
    @Bean
    public Step dailyStatisticsStep(ItemReader<String> cityListReader,
                                   ItemProcessor<String, WeatherStatistics> statisticsProcessor,
                                   ItemWriter<WeatherStatistics> statisticsWriter) {
        return new StepBuilder("dailyStatisticsStep", jobRepository)
                .<String, WeatherStatistics>chunk(3, transactionManager)
                .reader(cityListReader)
                .processor(statisticsProcessor)
                .writer(statisticsWriter)
                .build();
    }
    
    /**
     * 도시 목록을 읽어오는 ItemReader
     */
    @Bean
    public ItemReader<String> cityListReader() {
        List<String> cities = List.of("Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan", "Suwon");
        
        return new ItemReader<String>() {
            private int index = 0;
            
            @Override
            public String read() {
                if (index < cities.size()) {
                    return cities.get(index++);
                }
                return null;
            }
        };
    }
    
    /**
     * 도시별 일일 통계를 계산하는 ItemProcessor
     */
    @Bean
    public ItemProcessor<String, WeatherStatistics> statisticsProcessor() {
        return cityCode -> {
            try {
                log.info("Generating daily statistics for city: {}", cityCode);
                
                // 어제 날짜 기준 (통계는 전날 데이터 기준) - 테스트를 위해 오늘 포함
                LocalDate targetDate = LocalDate.now();
                LocalDateTime startOfDay = targetDate.atStartOfDay();
                LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);
                
                // 해당 날짜의 날씨 데이터 조회
                List<WeatherData> dailyWeatherData = weatherDataRepository
                        .findByCityCodeAndCollectedAtBetweenOrderByCollectedAtDesc(cityCode, startOfDay, endOfDay);
                
                if (dailyWeatherData.isEmpty()) {
                    log.warn("No weather data found for city: {} on date: {}", cityCode, targetDate);
                    return null;
                }
                
                // 기존 통계가 있는지 확인
                var existingStats = weatherStatisticsRepository
                        .findByStatisticsDateAndCityCode(targetDate, cityCode);
                
                WeatherStatistics statistics = existingStats.orElse(new WeatherStatistics());
                statistics.setStatisticsDate(targetDate);
                statistics.setCityCode(cityCode);
                statistics.setCityName(dailyWeatherData.get(0).getCityName());
                
                // 온도 통계 계산
                calculateTemperatureStatistics(statistics, dailyWeatherData);
                
                // 습도 및 기압 통계 계산
                calculateHumidityAndPressureStatistics(statistics, dailyWeatherData);
                
                // 날씨 상태 통계 계산
                calculateWeatherConditionStatistics(statistics, dailyWeatherData);
                
                // 이상 기후 통계 계산
                calculateAbnormalWeatherStatistics(statistics, dailyWeatherData);
                
                // 데이터 수집 통계 계산
                calculateDataCollectionStatistics(statistics, dailyWeatherData);
                
                log.info("Successfully generated statistics for {}: Avg temp {}, Records: {}", 
                        statistics.getCityName(), statistics.getAvgTemperature(), statistics.getTotalRecords());
                
                return statistics;
                
            } catch (Exception e) {
                log.error("Failed to generate statistics for city {}: {}", cityCode, e.getMessage(), e);
                return null;
            }
        };
    }
    
    /**
     * 온도 통계 계산
     */
    private void calculateTemperatureStatistics(WeatherStatistics statistics, List<WeatherData> data) {
        List<Double> temperatures = data.stream()
                .filter(w -> w.getTemperature() != null)
                .map(WeatherData::getTemperature)
                .collect(Collectors.toList());
        
        if (!temperatures.isEmpty()) {
            // 평균 온도
            double avgTemp = temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            statistics.setAvgTemperature(BigDecimal.valueOf(avgTemp).setScale(2, RoundingMode.HALF_UP));
            
            // 최고/최저 온도
            double maxTemp = temperatures.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minTemp = temperatures.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            statistics.setMaxTemperature(BigDecimal.valueOf(maxTemp).setScale(2, RoundingMode.HALF_UP));
            statistics.setMinTemperature(BigDecimal.valueOf(minTemp).setScale(2, RoundingMode.HALF_UP));
            
            // 온도 범위 계산
            statistics.calculateTemperatureRange();
        }
    }
    
    /**
     * 습도 및 기압 통계 계산
     */
    private void calculateHumidityAndPressureStatistics(WeatherStatistics statistics, List<WeatherData> data) {
        // 평균 습도
        double avgHumidity = data.stream()
                .filter(w -> w.getHumidity() != null)
                .mapToInt(WeatherData::getHumidity)
                .average().orElse(0.0);
        statistics.setAvgHumidity((int) Math.round(avgHumidity));
        
        // 평균 기압
        double avgPressure = data.stream()
                .filter(w -> w.getPressure() != null)
                .mapToInt(WeatherData::getPressure)
                .average().orElse(0.0);
        statistics.setAvgPressure((int) Math.round(avgPressure));
    }
    
    /**
     * 날씨 상태 통계 계산
     */
    private void calculateWeatherConditionStatistics(WeatherStatistics statistics, List<WeatherData> data) {
        // 날씨 상태별 빈도 계산
        Map<String, Long> weatherCounts = data.stream()
                .filter(w -> w.getWeatherMain() != null)
                .collect(Collectors.groupingBy(WeatherData::getWeatherMain, Collectors.counting()));
        
        // 가장 많이 나타난 날씨 상태
        String dominantWeather = weatherCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
        statistics.setDominantWeather(dominantWeather);
        
        // 날씨 상태별 시간 계산
        statistics.setClearHours(weatherCounts.getOrDefault("Clear", 0L).intValue());
        statistics.setCloudyHours(weatherCounts.getOrDefault("Clouds", 0L).intValue());
        statistics.setRainyHours(weatherCounts.getOrDefault("Rain", 0L).intValue());
    }
    
    /**
     * 이상 기후 통계 계산
     */
    private void calculateAbnormalWeatherStatistics(WeatherStatistics statistics, List<WeatherData> data) {
        // 이상 기후 건수
        long abnormalCount = data.stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsAbnormal()))
                .count();
        statistics.setAbnormalWeatherCount((int) abnormalCount);
        
        // 최대 온도 변화량
        double maxTempChange = data.stream()
                .filter(w -> w.getTemperatureChange() != null)
                .mapToDouble(w -> Math.abs(w.getTemperatureChange()))
                .max().orElse(0.0);
        statistics.setMaxTemperatureChange(BigDecimal.valueOf(maxTempChange).setScale(2, RoundingMode.HALF_UP));
    }
    
    /**
     * 데이터 수집 통계 계산
     */
    private void calculateDataCollectionStatistics(WeatherStatistics statistics, List<WeatherData> data) {
        statistics.setTotalRecords(data.size());
        
        // 24시간 기준 예상 데이터 수집 횟수 (매시간 1회 수집 가정)
        int expectedRecords = 24;
        statistics.calculateDataCollectionRate(expectedRecords);
    }
    
    /**
     * 통계 데이터를 저장하는 ItemWriter
     */
    @Bean
    public ItemWriter<WeatherStatistics> statisticsWriter() {
        return chunk -> {
            List<? extends WeatherStatistics> statistics = chunk.getItems();
            List<WeatherStatistics> validStats = statistics.stream()
                    .filter(stat -> stat != null)
                    .map(stat -> (WeatherStatistics) stat)
                    .collect(Collectors.toList());
            
            if (!validStats.isEmpty()) {
                weatherStatisticsRepository.saveAll(validStats);
                log.info("Saved {} weather statistics records", validStats.size());
                
                // 저장된 통계 요약 로깅
                validStats.forEach(stat -> 
                    log.debug("Statistics for {}: Avg={}°C, Max={}°C, Min={}°C, Records={}", 
                            stat.getCityName(), stat.getAvgTemperature(), 
                            stat.getMaxTemperature(), stat.getMinTemperature(), stat.getTotalRecords())
                );
            }
        };
    }
}