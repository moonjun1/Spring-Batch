# 스프링 배치 MVP 구현 가이드

## 📋 프로젝트 개요
CSV 파일의 사용자 데이터를 읽어서 데이터베이스에 저장하는 간단한 배치 작업

## 🏗️ 아키텍처 다이어그램

```
[CSV 파일] → [ItemReader] → [ItemProcessor] → [ItemWriter] → [Database]
     ↓              ↓              ↓              ↓
   Input         읽기           가공           저장
```

### 핵심 컴포넌트
- **Job**: 배치 작업 전체를 관리
- **Step**: Job 내의 개별 처리 단위
- **ItemReader**: 데이터 읽기 (CSV → Java Object)
- **ItemProcessor**: 데이터 가공/변환 (선택사항)
- **ItemWriter**: 데이터 저장 (Database)

## 📝 구현 태스크

### Task 1: 프로젝트 셋업
**목표**: 스프링 부트 + 배치 환경 구성

```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.h2database:h2'
}
```

```yaml
# application.yml
spring:
  batch:
    initialize-schema: always
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

### Task 2: 도메인 모델 생성
**목표**: 처리할 데이터 구조 정의

```java
// Person.java (Entity)
@Entity
public class Person {
    @Id @GeneratedValue
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    // getter, setter, constructor
}

// PersonDto.java (CSV 매핑용)
public class PersonDto {
    private String firstName;
    private String lastName;
    private String email;
    // getter, setter, constructor
}
```

### Task 3: 배치 Job 설정
**목표**: 배치 작업의 핵심 로직 구현

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Bean
    public Job importPersonJob(Step importStep) {
        return new JobBuilder("importPersonJob", jobRepository)
                .start(importStep)
                .build();
    }
    
    @Bean
    public Step importStep(ItemReader<PersonDto> reader,
                          ItemProcessor<PersonDto, Person> processor,
                          ItemWriter<Person> writer) {
        return new StepBuilder("importStep", jobRepository)
                .<PersonDto, Person>chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
```

### Task 4: ItemReader 구현
**목표**: CSV 파일 읽기

```java
@Bean
public ItemReader<PersonDto> reader() {
    return new FlatFileItemReaderBuilder<PersonDto>()
            .name("personReader")
            .resource(new ClassPathResource("sample-data.csv"))
            .delimited()
            .names("firstName", "lastName", "email")
            .targetType(PersonDto.class)
            .build();
}
```

### Task 5: ItemProcessor 구현
**목표**: 데이터 변환 처리

```java
@Bean
public ItemProcessor<PersonDto, Person> processor() {
    return item -> {
        Person person = new Person();
        person.setFirstName(item.getFirstName().toUpperCase());
        person.setLastName(item.getLastName().toUpperCase());
        person.setEmail(item.getEmail().toLowerCase());
        
        System.out.println("Processing: " + person.getFirstName());
        return person;
    };
}
```

### Task 6: ItemWriter 구현
**목표**: 데이터베이스 저장

```java
@Bean
public ItemWriter<Person> writer(PersonRepository repository) {
    return new RepositoryItemWriterBuilder<Person>()
            .repository(repository)
            .methodName("save")
            .build();
}

// 또는 더 간단하게 JpaItemWriter 사용
@Bean 
public ItemWriter<Person> writer(EntityManagerFactory entityManagerFactory) {
    return new JpaItemWriterBuilder<Person>()
            .entityManagerFactory(entityManagerFactory)
            .build();
}

// PersonRepository.java
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
}
```

### Task 7: 테스트 데이터 준비
**목표**: CSV 파일 생성

```csv
# src/main/resources/sample-data.csv
firstName,lastName,email
김,철수,kim@example.com
이,영희,lee@example.com
박,민수,park@example.com
```

### Task 8: 실행 및 테스트
**목표**: 배치 작업 실행

```java
@SpringBootApplication
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}

// 또는 CommandLineRunner로 자동 실행
@Component
public class BatchRunner implements CommandLineRunner {
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job importPersonJob;
    
    @Override
    public void run(String... args) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        
        jobLauncher.run(importPersonJob, params);
    }
}
```

## 🎯 실행 결과 확인

1. **콘솔 로그**: "Processing: 김철수" 등의 로그 확인
2. **H2 콘솔**: `http://localhost:8080/h2-console`에서 데이터 확인
3. **배치 메타데이터**: `BATCH_JOB_EXECUTION` 테이블에서 실행 이력 확인

```sql
-- 저장된 데이터 확인
SELECT * FROM PERSON;

-- 배치 실행 이력 확인  
SELECT * FROM BATCH_JOB_EXECUTION;
```

## 🔧 확장 포인트

1. **에러 처리**: `skip()`, `retry()` 설정
2. **청크 사이즈 조정**: 성능 최적화
3. **여러 Step**: 복잡한 워크플로우 구성
4. **스케줄링**: `@Scheduled`로 정기 실행
5. **파라미터 처리**: JobParameter 활용

이 MVP를 통해 스프링 배치의 핵심 개념과 구현 방법을 익힐 수 있습니다!