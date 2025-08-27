package com.springbatch.config;

import com.springbatch.entity.WeatherAlert;
import com.springbatch.entity.WeatherData;
import com.springbatch.entity.AlertType;
import com.springbatch.entity.AlertLevel;
import com.springbatch.repository.WeatherDataRepository;
import com.springbatch.repository.WeatherAlertRepository;
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
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 날씨 경고 및 알림 배치 설정
 */
@Slf4j
@Configuration
public class WeatherAlertBatchConfig {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private WeatherDataRepository weatherDataRepository;
    
    @Autowired
    private WeatherAlertRepository weatherAlertRepository;
    
    // 임계값 설정
    private static final double HEAT_WAVE_THRESHOLD = 35.0;
    private static final double COLD_WAVE_THRESHOLD = -10.0;
    private static final double HEAVY_RAIN_THRESHOLD = 50.0;
    private static final double ABNORMAL_TEMP_CHANGE_THRESHOLD = 20.0;
    
    /**
     * 날씨 알림 생성 Job
     */
    @Bean
    public Job generateWeatherAlertsJob(Step weatherAlertStep) {
        return new JobBuilder("generateWeatherAlertsJob", jobRepository)
                .start(weatherAlertStep)
                .build();
    }
    
    /**
     * 날씨 알림 생성 Step
     */
    @Bean
    public Step weatherAlertStep(ItemReader<WeatherData> recentWeatherDataReader,
                                ItemProcessor<WeatherData, List<WeatherAlert>> alertProcessor,
                                ItemWriter<List<WeatherAlert>> alertWriter) {
        return new StepBuilder("weatherAlertStep", jobRepository)
                .<WeatherData, List<WeatherAlert>>chunk(10, transactionManager)
                .reader(recentWeatherDataReader)
                .processor(alertProcessor)
                .writer(alertWriter)
                .build();
    }
    
    /**
     * 최근 날씨 데이터를 읽어오는 ItemReader
     */
    @Bean
    public ItemReader<WeatherData> recentWeatherDataReader() {
        return new ItemReader<WeatherData>() {
            private List<WeatherData> weatherDataList;
            private int index = 0;
            
            @Override
            public WeatherData read() {
                if (weatherDataList == null) {
                    // 최근 24시간 내 데이터 조회 (테스트를 위해 확장)
                    LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(24);
                    weatherDataList = weatherDataRepository
                            .findByCollectedAtAfterOrderByCollectedAtDesc(twoHoursAgo);
                    log.info("Loaded {} recent weather data records for alert processing", weatherDataList.size());
                }
                
                if (index < weatherDataList.size()) {
                    return weatherDataList.get(index++);
                }
                return null;
            }
        };
    }
    
    /**
     * 날씨 데이터를 분석하여 알림을 생성하는 ItemProcessor
     */
    @Bean
    public ItemProcessor<WeatherData, List<WeatherAlert>> alertProcessor() {
        return weatherData -> {
            List<WeatherAlert> alerts = new ArrayList<>();
            
            try {
                log.debug("Processing weather data for alerts: {} - {}°C", 
                         weatherData.getCityName(), weatherData.getTemperature());
                
                // 폭염 경보 확인 (35도 이상)
                if (weatherData.getTemperature() != null && weatherData.getTemperature() >= HEAT_WAVE_THRESHOLD) {
                    if (!hasRecentSimilarAlert(weatherData.getCityCode(), AlertType.HEAT_WAVE)) {
                        WeatherAlert heatWaveAlert = WeatherAlert.createHeatWaveAlert(
                                weatherData.getCityCode(), 
                                weatherData.getCityName(), 
                                weatherData.getTemperature()
                        );
                        heatWaveAlert.setWeatherDataId(weatherData.getId());
                        alerts.add(heatWaveAlert);
                        log.info("Heat wave alert created for {}: {}°C", 
                                weatherData.getCityName(), weatherData.getTemperature());
                    }
                }
                
                // 한파 주의보 확인 (-10도 이하)
                if (weatherData.getTemperature() != null && weatherData.getTemperature() <= COLD_WAVE_THRESHOLD) {
                    if (!hasRecentSimilarAlert(weatherData.getCityCode(), AlertType.COLD_WAVE)) {
                        WeatherAlert coldWaveAlert = WeatherAlert.createColdWaveAlert(
                                weatherData.getCityCode(), 
                                weatherData.getCityName(), 
                                weatherData.getTemperature()
                        );
                        coldWaveAlert.setWeatherDataId(weatherData.getId());
                        alerts.add(coldWaveAlert);
                        log.info("Cold wave alert created for {}: {}°C", 
                                weatherData.getCityName(), weatherData.getTemperature());
                    }
                }
                
                // 호우 경보 확인 (시간당 50mm 이상 강수량)
                if (isHeavyRainCondition(weatherData)) {
                    if (!hasRecentSimilarAlert(weatherData.getCityCode(), AlertType.HEAVY_RAIN)) {
                        // 강수량을 시뮬레이션 (실제로는 API에서 제공되어야 함)
                        double simulatedRainfall = calculateHourlyRainfall(weatherData);
                        WeatherAlert heavyRainAlert = WeatherAlert.createHeavyRainAlert(
                                weatherData.getCityCode(), 
                                weatherData.getCityName(), 
                                simulatedRainfall
                        );
                        heavyRainAlert.setWeatherDataId(weatherData.getId());
                        alerts.add(heavyRainAlert);
                        log.info("Heavy rain alert created for {}: {}mm/h", 
                                weatherData.getCityName(), simulatedRainfall);
                    }
                }
                
                // 이상 기후 알림 확인 (급격한 기온 변화)
                if (weatherData.getTemperatureChange() != null && 
                    Math.abs(weatherData.getTemperatureChange()) >= ABNORMAL_TEMP_CHANGE_THRESHOLD) {
                    if (!hasRecentSimilarAlert(weatherData.getCityCode(), AlertType.ABNORMAL_WEATHER)) {
                        WeatherAlert abnormalWeatherAlert = WeatherAlert.createAbnormalWeatherAlert(
                                weatherData.getCityCode(), 
                                weatherData.getCityName(), 
                                weatherData.getTemperatureChange()
                        );
                        abnormalWeatherAlert.setWeatherDataId(weatherData.getId());
                        alerts.add(abnormalWeatherAlert);
                        log.info("Abnormal weather alert created for {}: {:+.1f}°C change", 
                                weatherData.getCityName(), weatherData.getTemperatureChange());
                    }
                }
                
                return alerts.isEmpty() ? null : alerts;
                
            } catch (Exception e) {
                log.error("Failed to process weather data for alerts: {}", e.getMessage(), e);
                return null;
            }
        };
    }
    
    /**
     * 알림 목록을 저장하는 ItemWriter
     */
    @Bean
    public ItemWriter<List<WeatherAlert>> alertWriter() {
        return chunk -> {
            List<? extends List<WeatherAlert>> alertLists = chunk.getItems();
            List<WeatherAlert> allAlerts = alertLists.stream()
                    .filter(alertList -> alertList != null)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            
            if (!allAlerts.isEmpty()) {
                weatherAlertRepository.saveAll(allAlerts);
                log.info("Saved {} new weather alerts", allAlerts.size());
                
                // 알림별 상세 로깅
                allAlerts.forEach(alert -> {
                    log.info("Alert saved - {}: {} (Level: {}, Trigger: {})", 
                            alert.getCityName(), 
                            alert.getAlertTitle(),
                            alert.getAlertLevel(),
                            alert.getTriggerValue());
                });
                
                // 알림 발송 처리 (여기서는 로깅으로 시뮬레이션)
                processAlertNotifications(allAlerts);
            }
        };
    }
    
    /**
     * 최근 유사한 알림이 있는지 확인 (중복 방지)
     */
    private boolean hasRecentSimilarAlert(String cityCode, AlertType alertType) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<WeatherAlert> recentAlerts = weatherAlertRepository
                .findRecentSimilarAlerts(cityCode, alertType, oneHourAgo);
        return !recentAlerts.isEmpty();
    }
    
    /**
     * 호우 조건 확인
     */
    private boolean isHeavyRainCondition(WeatherData weatherData) {
        // 비 관련 날씨 상태 확인
        return weatherData.getWeatherMain() != null && 
               (weatherData.getWeatherMain().equals("Rain") || 
                weatherData.getWeatherMain().equals("Thunderstorm"));
    }
    
    /**
     * 시간당 강수량 계산 (시뮬레이션)
     */
    private double calculateHourlyRainfall(WeatherData weatherData) {
        // 실제로는 API에서 제공하는 강수량 데이터를 사용해야 함
        // 여기서는 습도와 날씨 상태를 기반으로 시뮬레이션
        if (weatherData.getWeatherMain().equals("Thunderstorm")) {
            return 60.0 + (Math.random() * 40.0); // 60-100mm
        } else if (weatherData.getWeatherMain().equals("Rain")) {
            return 50.0 + (Math.random() * 20.0); // 50-70mm
        }
        return 0.0;
    }
    
    /**
     * 알림 발송 처리 (시뮬레이션)
     */
    private void processAlertNotifications(List<WeatherAlert> alerts) {
        alerts.forEach(alert -> {
            try {
                // 실제로는 이메일, SMS, 푸시 알림 등을 발송
                log.info("📢 ALERT NOTIFICATION: {} - {}", alert.getAlertTitle(), alert.getAlertMessage());
                
                // 발송 상태 업데이트
                alert.markAsSent();
                weatherAlertRepository.save(alert);
                
            } catch (Exception e) {
                log.error("Failed to send alert notification for {}: {}", alert.getAlertTitle(), e.getMessage());
            }
        });
    }
}