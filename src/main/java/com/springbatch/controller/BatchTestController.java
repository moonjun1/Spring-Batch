package com.springbatch.controller;

import com.springbatch.service.TestDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 배치 작업 테스트를 위한 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/batch-test")
public class BatchTestController {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    @Qualifier("generateDailyWeatherStatisticsJob")
    private Job weatherStatisticsJob;
    
    @Autowired
    @Qualifier("generateWeatherAlertsJob")
    private Job weatherAlertsJob;
    
    @Autowired
    private TestDataService testDataService;
    
    /**
     * 배치 테스트 페이지
     */
    @GetMapping
    public String batchTestPage(Model model) {
        long weatherDataCount = testDataService.getWeatherDataCount();
        model.addAttribute("weatherDataCount", weatherDataCount);
        return "batch-test";
    }
    
    /**
     * 날씨 통계 배치 실행
     */
    @PostMapping("/statistics")
    public String runStatisticsBatch(RedirectAttributes redirectAttributes) {
        try {
            log.info("🚀 Starting Weather Statistics Batch Job...");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            var jobExecution = jobLauncher.run(weatherStatisticsJob, jobParameters);
            
            String status = jobExecution.getStatus().toString();
            String message = String.format("날씨 통계 배치가 완료되었습니다. 상태: %s", status);
            
            log.info("✅ Weather Statistics Batch completed with status: {}", status);
            redirectAttributes.addFlashAttribute("successMessage", message);
            
        } catch (Exception e) {
            log.error("❌ Failed to run Weather Statistics Batch: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "날씨 통계 배치 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/batch-test";
    }
    
    /**
     * 날씨 알림 배치 실행
     */
    @PostMapping("/alerts")
    public String runAlertsBatch(RedirectAttributes redirectAttributes) {
        try {
            log.info("🚀 Starting Weather Alerts Batch Job...");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            var jobExecution = jobLauncher.run(weatherAlertsJob, jobParameters);
            
            String status = jobExecution.getStatus().toString();
            String message = String.format("날씨 알림 배치가 완료되었습니다. 상태: %s", status);
            
            log.info("✅ Weather Alerts Batch completed with status: {}", status);
            redirectAttributes.addFlashAttribute("successMessage", message);
            
        } catch (Exception e) {
            log.error("❌ Failed to run Weather Alerts Batch: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "날씨 알림 배치 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/batch-test";
    }
    
    /**
     * 두 배치 모두 실행
     */
    @PostMapping("/run-all")
    public String runAllBatches(RedirectAttributes redirectAttributes) {
        try {
            log.info("🚀 Starting All Batch Jobs...");
            
            // 1. 통계 배치 실행
            JobParameters statsParams = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("type", "statistics")
                    .toJobParameters();
            
            var statsExecution = jobLauncher.run(weatherStatisticsJob, statsParams);
            log.info("📊 Statistics Batch Status: {}", statsExecution.getStatus());
            
            // 2. 알림 배치 실행
            Thread.sleep(1000); // 잠시 대기
            
            JobParameters alertsParams = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("type", "alerts")
                    .toJobParameters();
            
            var alertsExecution = jobLauncher.run(weatherAlertsJob, alertsParams);
            log.info("📧 Alerts Batch Status: {}", alertsExecution.getStatus());
            
            String message = String.format("모든 배치가 완료되었습니다. 통계: %s, 알림: %s", 
                    statsExecution.getStatus(), alertsExecution.getStatus());
            
            log.info("✅ All batches completed successfully");
            redirectAttributes.addFlashAttribute("successMessage", message);
            
        } catch (Exception e) {
            log.error("❌ Failed to run batch jobs: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "배치 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/batch-test";
    }
    
    /**
     * 테스트 데이터 생성
     */
    @PostMapping("/generate-data")
    public String generateTestData(RedirectAttributes redirectAttributes) {
        try {
            log.info("🔧 Generating test weather data...");
            testDataService.generateTestWeatherData();
            
            long count = testDataService.getWeatherDataCount();
            String message = String.format("테스트 날씨 데이터가 성공적으로 생성되었습니다! (총 %d개)", count);
            
            log.info("✅ Test data generated successfully. Total records: {}", count);
            redirectAttributes.addFlashAttribute("successMessage", message);
            
        } catch (Exception e) {
            log.error("❌ Failed to generate test data: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "테스트 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/batch-test";
    }
    
    /**
     * 테스트 데이터 삭제
     */
    @PostMapping("/clear-data")
    public String clearTestData(RedirectAttributes redirectAttributes) {
        try {
            log.info("🗑️ Clearing test weather data...");
            testDataService.clearTestData();
            
            String message = "모든 날씨 데이터가 삭제되었습니다.";
            
            log.info("✅ All test data cleared successfully");
            redirectAttributes.addFlashAttribute("successMessage", message);
            
        } catch (Exception e) {
            log.error("❌ Failed to clear test data: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "데이터 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/batch-test";
    }
}