package com.springbatch.controller;

import com.springbatch.entity.WeatherAlert;
import com.springbatch.entity.WeatherStatistics;
import com.springbatch.repository.WeatherAlertRepository;
import com.springbatch.repository.WeatherStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

/**
 * 배치 실행 결과를 확인하는 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/batch-results")
public class BatchResultController {
    
    @Autowired
    private WeatherStatisticsRepository weatherStatisticsRepository;
    
    @Autowired
    private WeatherAlertRepository weatherAlertRepository;
    
    /**
     * 배치 결과 메인 페이지
     */
    @GetMapping
    public String batchResultsPage(Model model) {
        try {
            // 최근 7일간의 통계 데이터 조회
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            List<WeatherStatistics> recentStatistics = weatherStatisticsRepository
                    .findRecentStatistics(sevenDaysAgo);
            
            // 최근 24시간 내 알림 조회
            List<WeatherAlert> recentAlerts = weatherAlertRepository
                    .findByIsResolvedFalseOrderByAlertTimeDesc();
            
            // 총 통계 개수
            long totalStatistics = weatherStatisticsRepository.count();
            long totalAlerts = weatherAlertRepository.count();
            
            // 오늘 날짜의 통계
            LocalDate today = LocalDate.now();
            List<WeatherStatistics> todayStatistics = weatherStatisticsRepository
                    .findByStatisticsDateOrderByCityNameAsc(today);
            
            // 발송되지 않은 알림
            List<WeatherAlert> unsentAlerts = weatherAlertRepository
                    .findByIsSentFalseOrderByAlertTimeAsc();
            
            model.addAttribute("recentStatistics", recentStatistics);
            model.addAttribute("recentAlerts", recentAlerts);
            model.addAttribute("totalStatistics", totalStatistics);
            model.addAttribute("totalAlerts", totalAlerts);
            model.addAttribute("todayStatistics", todayStatistics);
            model.addAttribute("unsentAlerts", unsentAlerts);
            
            log.info("배치 결과 페이지 로드 완료: 통계 {}개, 알림 {}개", totalStatistics, totalAlerts);
            
        } catch (Exception e) {
            log.error("배치 결과 페이지 로드 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "배치 결과를 불러오는 중 오류가 발생했습니다.");
        }
        
        return "batch-results";
    }
    
    /**
     * 통계 상세 페이지
     */
    @GetMapping("/statistics")
    public String statisticsDetailPage(Model model) {
        try {
            // 최근 30일간의 모든 통계
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            List<WeatherStatistics> allStatistics = weatherStatisticsRepository
                    .findRecentStatistics(thirtyDaysAgo);
            
            // 이상 기후 발생이 많은 도시 순위
            List<WeatherStatistics> abnormalWeatherStats = weatherStatisticsRepository
                    .findCitiesByAbnormalWeatherCount(thirtyDaysAgo, LocalDate.now());
            
            // 전국 평균 온도 (최근 7일)
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            Double nationalAvgTemp = weatherStatisticsRepository
                    .calculateNationalAverageTemperature(sevenDaysAgo, LocalDate.now());
            
            model.addAttribute("allStatistics", allStatistics);
            model.addAttribute("abnormalWeatherStats", abnormalWeatherStats);
            model.addAttribute("nationalAvgTemp", nationalAvgTemp);
            
        } catch (Exception e) {
            log.error("통계 상세 페이지 로드 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "통계 데이터를 불러오는 중 오류가 발생했습니다.");
        }
        
        return "statistics-detail";
    }
    
    /**
     * 알림 상세 페이지
     */
    @GetMapping("/alerts")
    public String alertsDetailPage(Model model) {
        try {
            // 모든 알림 조회 (최근 순)
            List<WeatherAlert> allAlerts = weatherAlertRepository
                    .findByAlertTimeBetweenOrderByAlertTimeDesc(
                            LocalDate.now().minusDays(30).atStartOfDay(),
                            LocalDate.now().atTime(23, 59, 59)
                    );
            
            // 알림 유형별 통계
            List<Object[]> alertTypeStats = weatherAlertRepository
                    .findAlertStatisticsByType(
                            LocalDate.now().minusDays(30).atStartOfDay(),
                            LocalDate.now().atTime(23, 59, 59)
                    );
            
            // 도시별 알림 빈도
            List<Object[]> cityAlertFrequency = weatherAlertRepository
                    .findAlertFrequencyByCity(
                            LocalDate.now().minusDays(30).atStartOfDay(),
                            LocalDate.now().atTime(23, 59, 59)
                    );
            
            model.addAttribute("allAlerts", allAlerts);
            model.addAttribute("alertTypeStats", alertTypeStats);
            model.addAttribute("cityAlertFrequency", cityAlertFrequency);
            
        } catch (Exception e) {
            log.error("알림 상세 페이지 로드 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "알림 데이터를 불러오는 중 오류가 발생했습니다.");
        }
        
        return "alerts-detail";
    }
}