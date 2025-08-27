package com.springbatch.controller;

import com.springbatch.entity.WeatherData;
import com.springbatch.repository.WeatherDataRepository;
import com.springbatch.service.WeatherApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 날씨 데이터 관련 웹 컨트롤러
 * 
 * 주요 기능:
 * - 날씨 데이터 수집 배치 실행
 * - 수집된 날씨 데이터 조회
 * - 날씨 통계 및 분석 결과 제공
 * - 웹 UI를 통한 날씨 데이터 시각화
 */
@Slf4j
@Controller
@RequestMapping("/weather")
public class WeatherController {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job collectWeatherDataJob;
    
    @Autowired
    private WeatherDataRepository weatherDataRepository;
    
    @Autowired
    private WeatherApiService weatherApiService;
    
    /**
     * 날씨 데이터 수집 배치를 수동으로 실행하는 엔드포인트
     */
    @PostMapping("/collect")
    @ResponseBody
    public String collectWeatherData() {
        try {
            // API 키 설정 확인
            if (!weatherApiService.isApiKeyConfigured()) {
                return "날씨 API 키가 설정되지 않았습니다. application.properties를 확인하세요.";
            }
            
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            JobExecution execution = jobLauncher.run(collectWeatherDataJob, params);
            
            return "날씨 데이터 수집 배치가 시작되었습니다. 상태: " + execution.getStatus();
            
        } catch (Exception e) {
            log.error("Weather data collection failed", e);
            return "날씨 데이터 수집 중 오류 발생: " + e.getMessage();
        }
    }
    
    /**
     * 수집된 모든 날씨 데이터를 JSON 형태로 반환
     */
    @GetMapping("/data")
    @ResponseBody
    public List<WeatherData> getAllWeatherData() {
        return weatherDataRepository.findAll();
    }
    
    /**
     * 도시별 최신 날씨 데이터 조회
     */
    @GetMapping("/current")
    @ResponseBody
    public List<WeatherData> getCurrentWeatherData() {
        return weatherDataRepository.findLatestWeatherDataForAllCities();
    }
    
    /**
     * 이상 기후 데이터 조회
     */
    @GetMapping("/abnormal")
    @ResponseBody
    public List<WeatherData> getAbnormalWeatherData() {
        return weatherDataRepository.findByIsAbnormalTrueOrderByCollectedAtDesc();
    }
    
    /**
     * 날씨 대시보드 메인 페이지
     */
    @GetMapping("/dashboard")
    public String weatherDashboard(Model model) {
        // 최신 날씨 데이터
        List<WeatherData> currentWeather = weatherDataRepository.findLatestWeatherDataForAllCities();
        
        // 온도 순으로 정렬
        List<WeatherData> sortedByTemp = weatherDataRepository.findCitiesOrderByTemperatureDesc();
        
        // 이상 기후 데이터
        List<WeatherData> abnormalWeather = weatherDataRepository.findByIsAbnormalTrueOrderByCollectedAtDesc();
        
        // 전체 데이터 개수
        long totalRecords = weatherDataRepository.count();
        
        // 도시별 통계
        Map<String, Long> cityStats = weatherDataRepository.findAll().stream()
                .collect(Collectors.groupingBy(WeatherData::getCityName, Collectors.counting()));
        
        model.addAttribute("currentWeather", currentWeather);
        model.addAttribute("sortedByTemp", sortedByTemp);
        model.addAttribute("abnormalWeather", abnormalWeather);
        model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("cityStats", cityStats);
        model.addAttribute("supportedCities", weatherApiService.getMajorCities());
        
        return "weather-dashboard";
    }
    
    /**
     * 특정 도시의 날씨 통계 조회
     */
    @GetMapping("/statistics")
    @ResponseBody
    public Map<String, Object> getWeatherStatistics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
        
        // 오늘 수집된 데이터 개수
        List<WeatherData> todayData = weatherDataRepository.findAll().stream()
                .filter(w -> w.getCollectedAt().isAfter(startOfToday) && w.getCollectedAt().isBefore(endOfToday))
                .toList();
        
        // 통계 계산
        double avgTemp = todayData.stream()
                .filter(w -> w.getTemperature() != null)
                .mapToDouble(WeatherData::getTemperature)
                .average()
                .orElse(0.0);
        
        double maxTemp = todayData.stream()
                .filter(w -> w.getTemperature() != null)
                .mapToDouble(WeatherData::getTemperature)
                .max()
                .orElse(0.0);
        
        double minTemp = todayData.stream()
                .filter(w -> w.getTemperature() != null)
                .mapToDouble(WeatherData::getTemperature)
                .min()
                .orElse(0.0);
        
        return Map.of(
            "todayRecords", todayData.size(),
            "averageTemperature", Math.round(avgTemp * 10.0) / 10.0,
            "maxTemperature", maxTemp,
            "minTemperature", minTemp,
            "abnormalCount", todayData.stream().filter(w -> Boolean.TRUE.equals(w.getIsAbnormal())).count()
        );
    }
}