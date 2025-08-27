package com.springbatch.controller;

import com.springbatch.entity.Person;
import com.springbatch.repository.PersonRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Spring Batch 작업을 웹에서 제어하고 결과를 확인할 수 있는 MVC 컨트롤러
 * 
 * 주요 기능:
 * - 배치 작업 수동 실행
 * - 저장된 데이터 조회
 * - Thymeleaf 템플릿 기반 웹 UI 제공
 */
@Controller
public class BatchController {
    
    // 배치 작업을 실행하는 JobLauncher
    @Autowired
    private JobLauncher jobLauncher;
    
    // 실행할 배치 Job (CSV → 데이터베이스 작업)
    @Autowired
    private Job importPersonJob;
    
    // 저장된 Person 데이터를 조회하기 위한 Repository
    @Autowired
    private PersonRepository personRepository;
    
    /**
     * 배치 작업을 수동으로 실행하는 엔드포인트
     * 
     * POST /run-batch 요청 시 CSV 파일을 읽어서 데이터베이스에 저장하는 작업을 시작합니다.
     * 
     * @return 배치 작업 실행 결과 메시지
     */
    @PostMapping("/run-batch")
    @ResponseBody
    public String runBatch() {
        try {
            // 매번 다른 파라미터로 배치를 실행하기 위해 현재 시간을 파라미터로 사용
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())  // 실행 시간을 파라미터로 추가
                    .toJobParameters();
            
            // 배치 작업 실행
            JobExecution execution = jobLauncher.run(importPersonJob, params);
            
            return "배치 작업이 시작되었습니다. 상태: " + execution.getStatus();
        } catch (Exception e) {
            return "배치 실행 중 오류 발생: " + e.getMessage();
        }
    }
    
    /**
     * 데이터베이스에 저장된 모든 Person 데이터를 조회하는 엔드포인트
     * 
     * GET /persons 요청 시 배치 작업으로 저장된 데이터를 JSON 형태로 반환합니다.
     * 
     * @return 저장된 모든 Person 객체 리스트
     */
    @GetMapping("/persons")
    @ResponseBody
    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
    
    /**
     * 메인 페이지 - Thymeleaf 템플릿을 사용한 웹 UI
     * 
     * GET / 요청 시 templates/index.html 템플릿을 렌더링합니다.
     * 
     * @return 템플릿 파일명 (templates/index.html)
     */
    @GetMapping("/")
    public String home() {
        return "index";  // templates/index.html 템플릿을 렌더링
    }
}