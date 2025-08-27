package com.springbatch.config;

import com.springbatch.dto.WeatherApiResponse;
import com.springbatch.entity.WeatherData;
import com.springbatch.repository.WeatherDataRepository;
import com.springbatch.service.WeatherApiService;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 날씨 데이터 수집을 위한 Spring Batch 설정
 * 
 * 배치 프로세스:
 * 1. ItemReader: 전국 주요 도시 목록 생성
 * 2. ItemProcessor: 각 도시의 날씨 API 호출 및 데이터 변환
 * 3. ItemWriter: 날씨 데이터를 데이터베이스에 저장
 */
@Slf4j
@Configuration
public class WeatherBatchConfig {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private WeatherApiService weatherApiService;
    
    @Autowired
    private WeatherDataRepository weatherDataRepository;
    
    /**
     * 날씨 데이터 수집 Job 정의
     */
    @Bean
    public Job collectWeatherDataJob(Step weatherCollectionStep) {
        return new JobBuilder("collectWeatherDataJob", jobRepository)
                .start(weatherCollectionStep)
                .build();
    }
    
    /**
     * 날씨 데이터 수집 Step 정의
     */
    @Bean
    public Step weatherCollectionStep(ItemReader<String> cityReader,
                                     ItemProcessor<String, WeatherData> weatherProcessor,
                                     ItemWriter<WeatherData> weatherWriter) {
        return new StepBuilder("weatherCollectionStep", jobRepository)
                .<String, WeatherData>chunk(3, transactionManager)
                .reader(cityReader)
                .processor(weatherProcessor)
                .writer(weatherWriter)
                .build();
    }
    
    /**
     * 도시 목록을 읽어오는 ItemReader
     * 전국 주요 도시 리스트를 순차적으로 반환
     */
    @Bean
    public ItemReader<String> cityReader() {
        List<String> cities = List.of("Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan", "Suwon");
        
        return new ItemReader<String>() {
            private int index = 0;
            
            @Override
            public String read() {
                if (index < cities.size()) {
                    return cities.get(index++);
                }
                return null; // 더 이상 읽을 데이터가 없음을 나타냄
            }
        };
    }
    
    /**
     * 각 도시의 날씨 데이터를 API에서 가져와서 WeatherData 엔티티로 변환하는 ItemProcessor
     */
    @Bean
    public ItemProcessor<String, WeatherData> weatherProcessor() {
        return cityCode -> {
            try {
                log.info("Processing weather data for city: {}", cityCode);
                
                // API 호출하여 날씨 데이터 가져오기
                WeatherApiResponse response = weatherApiService.getCurrentWeather(cityCode).block();
                
                if (response == null) {
                    log.warn("No weather data received for city: {}", cityCode);
                    return null;
                }
                
                // WeatherData 엔티티로 변환
                WeatherData weatherData = convertToWeatherData(response, cityCode);
                
                // 이상 기후 탐지
                detectAbnormalWeather(weatherData);
                
                log.info("Successfully processed weather data for {}: {}°C, {}", 
                        weatherData.getCityName(), weatherData.getTemperature(), weatherData.getWeatherMain());
                
                return weatherData;
                
            } catch (Exception e) {
                log.error("Failed to process weather data for city {}: {}", cityCode, e.getMessage());
                return null; // 실패한 데이터는 건너뛰기
            }
        };
    }
    
    /**
     * WeatherData를 데이터베이스에 저장하는 ItemWriter
     */
    @Bean
    public ItemWriter<WeatherData> weatherWriter() {
        return chunk -> {
            List<? extends WeatherData> weatherDataList = chunk.getItems();
            
            // null이 아닌 데이터만 필터링
            List<WeatherData> validData = weatherDataList.stream()
                    .filter(data -> data != null)
                    .map(data -> (WeatherData) data)
                    .toList();
            
            if (!validData.isEmpty()) {
                weatherDataRepository.saveAll(validData);
                log.info("Saved {} weather data records to database", validData.size());
            }
        };
    }
    
    /**
     * WeatherApiResponse를 WeatherData 엔티티로 변환
     */
    private WeatherData convertToWeatherData(WeatherApiResponse response, String cityCode) {
        WeatherData weatherData = new WeatherData();
        LocalDateTime now = LocalDateTime.now();
        
        // 기본 정보
        weatherData.setCityCode(cityCode);
        weatherData.setCityName(weatherApiService.getCityNameInKorean(cityCode));
        weatherData.setCollectedAt(now);
        weatherData.setWeatherTime(now);
        
        // 온도 정보
        if (response.getMain() != null) {
            weatherData.setTemperature(response.getMain().getTemp());
            weatherData.setFeelsLike(response.getMain().getFeelsLike());
            weatherData.setTempMin(response.getMain().getTempMin());
            weatherData.setTempMax(response.getMain().getTempMax());
            weatherData.setHumidity(response.getMain().getHumidity());
            weatherData.setPressure(response.getMain().getPressure());
        }
        
        // 날씨 상태
        if (response.getWeather() != null && !response.getWeather().isEmpty()) {
            WeatherApiResponse.Weather weather = response.getWeather().get(0);
            weatherData.setWeatherMain(weather.getMain());
            weatherData.setWeatherDescription(weather.getDescription());
        }
        
        // 바람 정보
        if (response.getWind() != null) {
            weatherData.setWindSpeed(response.getWind().getSpeed());
            weatherData.setWindDirection(response.getWind().getDeg());
        }
        
        // 구름 정보
        if (response.getClouds() != null) {
            weatherData.setCloudiness(response.getClouds().getAll());
        }
        
        // 강수량 정보
        if (response.getRain() != null && response.getRain().getOneHour() != null) {
            weatherData.setRainfall(response.getRain().getOneHour());
        }
        
        // 적설량 정보
        if (response.getSnow() != null && response.getSnow().getOneHour() != null) {
            weatherData.setSnowfall(response.getSnow().getOneHour());
        }
        
        // 가시거리
        weatherData.setVisibility(response.getVisibility());
        
        return weatherData;
    }
    
    /**
     * 이상 기후 탐지 로직
     * 전날 동시간 대비 온도 변화량을 계산하여 20도 이상 차이나면 이상 기후로 판단
     */
    private void detectAbnormalWeather(WeatherData currentData) {
        try {
            LocalDateTime yesterday = currentData.getCollectedAt().minusDays(1);
            LocalDateTime yesterdayStart = yesterday.toLocalDate().atStartOfDay();
            LocalDateTime yesterdayEnd = yesterday.toLocalDate().atTime(23, 59, 59);
            
            var yesterdayDataList = weatherDataRepository.findByCityCodeAndCollectedAtBetweenOrderByCollectedAtDesc(
                    currentData.getCityCode(), yesterdayStart, yesterdayEnd
            );
            
            if (!yesterdayDataList.isEmpty() && currentData.getTemperature() != null) {
                WeatherData yesterdayData = yesterdayDataList.get(0); // 가장 최근 데이터 사용
                double temperatureChange = currentData.getTemperature() - yesterdayData.getTemperature();
                currentData.setTemperatureChange(temperatureChange);
                
                // 전날 대비 20도 이상 변화 시 이상 기후로 판단
                if (Math.abs(temperatureChange) >= 20.0) {
                    currentData.setIsAbnormal(true);
                    log.warn("Abnormal weather detected in {}: Temperature changed by {:.1f}°C from yesterday", 
                            currentData.getCityName(), temperatureChange);
                }
            }
        } catch (Exception e) {
            log.debug("Could not detect abnormal weather for {}: {}", currentData.getCityName(), e.getMessage());
        }
    }
}