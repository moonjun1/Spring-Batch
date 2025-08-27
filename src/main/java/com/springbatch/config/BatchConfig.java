package com.springbatch.config;

import com.springbatch.dto.PersonDto;
import com.springbatch.entity.Person;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch 설정을 담당하는 Configuration 클래스
 * 
 * 이 클래스에서는 배치 처리의 핵심 컴포넌트들을 정의합니다:
 * - Job: 전체 배치 작업
 * - Step: Job을 구성하는 개별 단계
 * - ItemReader: 데이터를 읽는 컴포넌트
 * - ItemProcessor: 데이터를 가공하는 컴포넌트
 * - ItemWriter: 데이터를 저장하는 컴포넌트
 */
@Configuration
public class BatchConfig {
    
    // Spring Batch 작업 실행을 관리하는 JobRepository
    @Autowired
    private JobRepository jobRepository;
    
    // 데이터베이스 트랜잭션을 관리하는 TransactionManager
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    // JPA EntityManager를 생성하는 Factory
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    /**
     * 메인 배치 Job을 정의
     * 
     * @param importStep CSV 파일을 읽어서 데이터베이스에 저장하는 Step
     * @return 설정된 Job 인스턴스
     */
    @Bean
    public Job importPersonJob(Step importStep) {
        return new JobBuilder("importPersonJob", jobRepository)
                .start(importStep)  // importStep을 시작 단계로 설정
                .build();
    }
    
    /**
     * CSV 파일을 읽어서 데이터베이스에 저장하는 Step을 정의
     * 
     * @param reader CSV 파일을 읽는 ItemReader
     * @param processor 데이터를 변환하는 ItemProcessor
     * @param writer 데이터베이스에 저장하는 ItemWriter
     * @return 설정된 Step 인스턴스
     */
    @Bean
    public Step importStep(ItemReader<PersonDto> reader,
                          ItemProcessor<PersonDto, Person> processor,
                          ItemWriter<Person> writer) {
        return new StepBuilder("importStep", jobRepository)
                .<PersonDto, Person>chunk(3, transactionManager)  // 3개씩 묶어서 처리
                .reader(reader)     // 데이터 읽기
                .processor(processor)  // 데이터 가공
                .writer(writer)     // 데이터 저장
                .build();
    }
    
    /**
     * CSV 파일을 읽어서 PersonDto 객체로 변환하는 ItemReader
     * 
     * @return CSV 파일을 읽는 FlatFileItemReader 인스턴스
     */
    @Bean
    public FlatFileItemReader<PersonDto> reader() {
        return new FlatFileItemReaderBuilder<PersonDto>()
                .name("personReader")  // Reader의 이름 설정
                .resource(new ClassPathResource("sample-data.csv"))  // 읽을 CSV 파일 경로
                .delimited()  // 구분자로 나누어진 파일임을 명시
                .names("firstName", "lastName", "email")  // CSV 컬럼명과 DTO 필드 매핑
                .linesToSkip(1)  // 첫 번째 줄(헤더) 건너뛰기
                .targetType(PersonDto.class)  // 변환할 대상 클래스
                .build();
    }
    
    /**
     * PersonDto를 Person 엔티티로 변환하고 데이터를 가공하는 ItemProcessor
     * 
     * @return PersonDto를 Person으로 변환하는 ItemProcessor
     */
    @Bean
    public ItemProcessor<PersonDto, Person> processor() {
        return item -> {
            // 새로운 Person 엔티티 생성
            Person person = new Person();
            
            // 데이터 변환 및 가공
            person.setFirstName(item.getFirstName().toUpperCase());  // 이름을 대문자로 변환
            person.setLastName(item.getLastName().toUpperCase());    // 성을 대문자로 변환
            person.setEmail(item.getEmail().toLowerCase());          // 이메일을 소문자로 변환
            
            // 처리 중인 데이터 로그 출력
            System.out.println("데이터 처리 중: " + person.getFirstName() + " " + person.getLastName());
            
            return person;
        };
    }
    
    /**
     * Person 엔티티를 데이터베이스에 저장하는 ItemWriter
     * 
     * @return JPA를 사용하여 데이터베이스에 저장하는 JpaItemWriter
     */
    @Bean
    public JpaItemWriter<Person> writer() {
        return new JpaItemWriterBuilder<Person>()
                .entityManagerFactory(entityManagerFactory)  // JPA EntityManager 설정
                .build();
    }
}