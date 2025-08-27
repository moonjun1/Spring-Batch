# 04. Spring Batch 기초 완전 정복

## 🎯 Spring Batch란?

Spring Batch는 **대용량 데이터 처리**를 위한 경량급, 포괄적인 배치 프레임워크입니다. 엔터프라이즈 환경에서 일상적으로 필요한 견고한 배치 애플리케이션 개발을 가능하게 합니다.

### 핵심 특징
- ✅ **트랜잭션 관리**: 대용량 데이터 처리 시 안전한 트랜잭션 보장
- ✅ **청크 기반 처리**: 메모리 효율적인 대용량 데이터 처리
- ✅ **재시작/재시도**: 실패 지점부터 재시작 가능
- ✅ **스케일링**: 멀티스레드, 병렬처리, 원격처리 지원
- ✅ **모니터링**: 실행 상태 추적 및 관리

## 🏗️ Spring Batch 아키텍처

### 전체 구조도
```
JobLauncher ──┐
              ├─► Job ──┐
JobRepository ──┘      ├─► Step1 ──┐
                       │           ├─► ItemReader
                       │           ├─► ItemProcessor  
                       │           └─► ItemWriter
                       └─► Step2 ──┐
                                   ├─► ItemReader
                                   ├─► ItemProcessor
                                   └─► ItemWriter
```

### 핵심 컴포넌트 상세

#### 1. Job (작업)
**Job은 전체 배치 프로세스를 캡슐화한 엔티티**입니다.

```java
@Bean
public Job myBatchJob(Step step1, Step step2) {
    return new JobBuilder("myBatchJob", jobRepository)
        .start(step1)        // 첫 번째 Step
        .next(step2)         // 두 번째 Step
        .build();
}

// 조건부 실행 예시
@Bean
public Job conditionalJob(Step step1, Step step2, Step step3) {
    return new JobBuilder("conditionalJob", jobRepository)
        .start(step1)
        .on("FAILED").to(step3)      // step1 실패 시 step3 실행
        .from(step1).on("*").to(step2)  // step1 성공 시 step2 실행
        .build().build();
}
```

**Job의 주요 속성:**
- `JobInstance`: Job의 논리적 실행 단위
- `JobExecution`: Job의 물리적 실행 시도
- `JobParameters`: Job 실행 시 전달되는 파라미터

#### 2. Step (단계)
**Step은 Job 내에서 독립적이고 순차적인 단계**를 나타냅니다.

```java
@Bean
public Step processDataStep(ItemReader<InputData> reader,
                           ItemProcessor<InputData, OutputData> processor,
                           ItemWriter<OutputData> writer) {
    return new StepBuilder("processDataStep", jobRepository)
        .<InputData, OutputData>chunk(1000, transactionManager)  // 1000개씩 처리
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
}
```

**Step의 두 가지 유형:**

##### Chunk-oriented Step (청크 지향)
```java
// 청크 기반 처리 - 가장 일반적
.chunk(chunkSize, transactionManager)
.reader(itemReader)      // 데이터 읽기
.processor(itemProcessor) // 데이터 처리 (선택사항)
.writer(itemWriter)      // 데이터 쓰기
```

##### Tasklet-based Step (태스크릿 기반)
```java
// 단순 태스크 실행 - 파일 삭제, 정리 작업 등
@Bean
public Step cleanupStep() {
    return new StepBuilder("cleanupStep", jobRepository)
        .tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution,
                                       ChunkContext chunkContext) {
                // 정리 작업 수행
                cleanupTempFiles();
                return RepeatStatus.FINISHED;
            }
        }, transactionManager)
        .build();
}
```

#### 3. ItemReader (데이터 읽기)
**다양한 소스에서 데이터를 읽어오는 인터페이스**

```java
public interface ItemReader<T> {
    T read() throws Exception, UnexpectedInputException, ParseException;
}
```

**주요 구현체들:**

##### 파일 읽기 (CSV, TXT)
```java
@Bean
public FlatFileItemReader<Person> csvReader() {
    return new FlatFileItemReaderBuilder<Person>()
        .name("csvReader")
        .resource(new ClassPathResource("input.csv"))
        .delimited()
        .delimiter(",")
        .names("firstName", "lastName", "email", "age")
        .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
            setTargetType(Person.class);
        }})
        .linesToSkip(1)  // 헤더 행 스킵
        .build();
}
```

##### 데이터베이스 읽기 (JPA)
```java
@Bean
public JpaPagingItemReader<Customer> databaseReader() {
    return new JpaPagingItemReaderBuilder<Customer>()
        .name("customerReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT c FROM Customer c WHERE c.active = true")
        .pageSize(100)  // 페이지당 100개
        .build();
}
```

##### 데이터베이스 읽기 (JDBC)
```java
@Bean
public JdbcCursorItemReader<Person> jdbcReader() {
    return new JdbcCursorItemReaderBuilder<Person>()
        .name("jdbcReader")
        .dataSource(dataSource)
        .sql("SELECT first_name, last_name, email FROM person WHERE active = 1")
        .rowMapper(new BeanPropertyRowMapper<>(Person.class))
        .build();
}
```

##### JSON 파일 읽기
```java
@Bean
public JsonItemReader<Person> jsonReader() {
    return new JsonItemReaderBuilder<Person>()
        .name("jsonReader")
        .resource(new ClassPathResource("input.json"))
        .jsonObjectReader(new JacksonJsonObjectReader<>(Person.class))
        .build();
}
```

##### XML 파일 읽기
```java
@Bean
public StaxEventItemReader<Person> xmlReader() {
    return new StaxEventItemReaderBuilder<Person>()
        .name("xmlReader")
        .resource(new ClassPathResource("input.xml"))
        .addFragmentRootElements("person")
        .unmarshaller(personUnmarshaller())
        .build();
}
```

#### 4. ItemProcessor (데이터 처리)
**읽어온 데이터를 비즈니스 로직에 따라 변환하는 인터페이스**

```java
public interface ItemProcessor<I, O> {
    O process(I item) throws Exception;
}
```

**실제 구현 예시:**

##### 단순 변환
```java
@Bean
public ItemProcessor<Person, Person> validatingProcessor() {
    return new ItemProcessor<Person, Person>() {
        @Override
        public Person process(Person person) throws Exception {
            // 데이터 검증
            if (person.getEmail() == null || !person.getEmail().contains("@")) {
                return null;  // null 반환 시 해당 아이템은 writer로 전달되지 않음
            }
            
            // 데이터 변환
            person.setFirstName(person.getFirstName().toUpperCase());
            person.setLastName(person.getLastName().toUpperCase());
            
            return person;
        }
    };
}
```

##### 복합 프로세서 (여러 프로세서 연결)
```java
@Bean
public CompositeItemProcessor<Person, Person> compositeProcessor() {
    List<ItemProcessor<?, ?>> processors = new ArrayList<>();
    processors.add(validationProcessor());
    processors.add(transformationProcessor());
    processors.add(enrichmentProcessor());
    
    CompositeItemProcessor<Person, Person> processor = new CompositeItemProcessor<>();
    processor.setDelegates(processors);
    return processor;
}
```

##### 외부 API 호출 프로세서
```java
@Bean
public ItemProcessor<String, WeatherData> apiCallProcessor() {
    return cityName -> {
        try {
            // 외부 API 호출
            WeatherApiResponse response = webClient.get()
                .uri("/weather?q=" + cityName)
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .timeout(Duration.ofSeconds(5))  // 5초 타임아웃
                .block();
            
            // 응답을 엔티티로 변환
            return convertToWeatherData(response, cityName);
            
        } catch (Exception e) {
            log.error("API call failed for city: {}", cityName, e);
            return null;  // 실패 시 null 반환
        }
    };
}
```

#### 5. ItemWriter (데이터 쓰기)
**처리된 데이터를 최종 목적지에 저장하는 인터페이스**

```java
public interface ItemWriter<T> {
    void write(List<? extends T> items) throws Exception;
}
```

**주요 구현체들:**

##### 파일 쓰기
```java
@Bean
public FlatFileItemWriter<Person> csvWriter() {
    return new FlatFileItemWriterBuilder<Person>()
        .name("csvWriter")
        .resource(new FileSystemResource("output.csv"))
        .delimited()
        .delimiter(",")
        .names("firstName", "lastName", "email")
        .headerCallback(writer -> writer.write("FirstName,LastName,Email"))  // 헤더
        .build();
}
```

##### 데이터베이스 쓰기 (JPA)
```java
@Bean
public JpaItemWriter<Customer> jpaWriter() {
    JpaItemWriter<Customer> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(entityManagerFactory);
    return writer;
}
```

##### 데이터베이스 쓰기 (JDBC)
```java
@Bean
public JdbcBatchItemWriter<Person> jdbcWriter() {
    return new JdbcBatchItemWriterBuilder<Person>()
        .dataSource(dataSource)
        .sql("INSERT INTO person (first_name, last_name, email) VALUES (:firstName, :lastName, :email)")
        .beanMapped()  // Bean 속성을 SQL 파라미터로 매핑
        .build();
}
```

##### 복합 Writer (여러 Writer에 동시 저장)
```java
@Bean
public CompositeItemWriter<Person> compositeWriter() {
    List<ItemWriter<? super Person>> writers = new ArrayList<>();
    writers.add(databaseWriter());
    writers.add(fileWriter());
    writers.add(emailNotificationWriter());
    
    CompositeItemWriter<Person> writer = new CompositeItemWriter<>();
    writer.setDelegates(writers);
    return writer;
}
```

## 🔄 청크 기반 처리의 이해

### 청크 처리 과정
```
[Reader] ──► Item1 ──┐
[Reader] ──► Item2 ──┤
[Reader] ──► Item3 ──┤ Chunk (Size=3)
                     ├──► [Processor] ──► ProcessedChunk ──► [Writer]
                     │                                      │
                     └──► Transaction Commit ◄──────────────┘
```

### 청크 크기 결정 기준

```java
// 작은 청크 크기 (10-100)
.chunk(50, transactionManager)
// 장점: 빠른 피드백, 적은 메모리 사용, 롤백 범위 최소화
// 단점: 트랜잭션 오버헤드 증가

// 큰 청크 크기 (1000-10000)
.chunk(5000, transactionManager)
// 장점: 높은 처리 성능, 트랜잭션 오버헤드 감소
// 단점: 메모리 사용량 증가, 롤백 시 재처리 데이터 많음
```

### 청크 처리 최적화 팁

```java
@Bean
public Step optimizedStep() {
    return new StepBuilder("optimizedStep", jobRepository)
        .<InputData, OutputData>chunk(1000, transactionManager)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        
        // 성능 최적화 설정
        .faultTolerant()
        .skipLimit(100)                    // 최대 100개 아이템 스킵 허용
        .skip(ValidationException.class)   // 특정 예외는 스킵
        .retryLimit(3)                     // 최대 3번 재시도
        .retry(TransientException.class)   // 일시적 예외는 재시도
        
        // 멀티스레드 처리
        .taskExecutor(taskExecutor())
        .throttleLimit(4)                  // 동시 실행 스레드 수
        
        build();
}
```

## 💾 메타데이터 관리

### JobRepository
Spring Batch는 실행 중인 Job의 상태 정보를 JobRepository에 저장합니다.

```sql
-- 주요 메타데이터 테이블들
BATCH_JOB_INSTANCE        -- Job 인스턴스 정보
BATCH_JOB_EXECUTION       -- Job 실행 정보  
BATCH_JOB_EXECUTION_PARAMS -- Job 파라미터
BATCH_STEP_EXECUTION      -- Step 실행 정보
BATCH_STEP_EXECUTION_CONTEXT -- Step 실행 컨텍스트
```

### JobParameters 사용법
```java
// JobParameters 생성
JobParameters jobParameters = new JobParametersBuilder()
    .addString("inputFile", "/path/to/input.csv")
    .addLong("timestamp", System.currentTimeMillis())
    .addDate("processDate", new Date())
    .toJobParameters();

// Job 실행
JobExecution execution = jobLauncher.run(job, jobParameters);
```

## 🚨 에러 처리 전략

### 1. Skip (건너뛰기)
```java
@Bean
public Step skipStep() {
    return new StepBuilder("skipStep", jobRepository)
        .<String, String>chunk(10, transactionManager)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .faultTolerant()
        .skip(ValidationException.class)     // ValidationException 발생 시 스킵
        .skip(DataAccessException.class)     // DB 접근 오류 시 스킵
        .skipLimit(100)                      // 최대 100개까지 스킵 허용
        .skipPolicy(customSkipPolicy())      // 커스텀 스킵 정책
        .build();
}
```

### 2. Retry (재시도)
```java
@Bean
public Step retryStep() {
    return new StepBuilder("retryStep", jobRepository)
        .<String, String>chunk(10, transactionManager)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .faultTolerant()
        .retry(ConnectException.class)       // 연결 오류 시 재시도
        .retry(TimeoutException.class)       // 타임아웃 시 재시도
        .retryLimit(3)                       // 최대 3번 재시도
        .retryPolicy(customRetryPolicy())    // 커스텀 재시도 정책
        .build();
}
```

### 3. 커스텀 예외 처리
```java
@Component
public class CustomSkipPolicy implements SkipPolicy {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomSkipPolicy.class);
    
    @Override
    public boolean shouldSkip(Throwable exception, int skipCount) throws SkipLimitExceededException {
        
        if (exception instanceof ValidationException) {
            logger.warn("Validation error occurred. Skipping item. Skip count: {}", skipCount);
            return skipCount < 50;  // ValidationException은 50개까지 스킵
        }
        
        if (exception instanceof DataAccessException) {
            logger.error("Database error occurred. Skipping item. Skip count: {}", skipCount);
            return skipCount < 10;  // DB 오류는 10개까지만 스킵
        }
        
        return false;  // 다른 예외는 스킵하지 않음
    }
}
```

## 🎯 실전 예제: 완전한 배치 시스템

### 고객 데이터 처리 배치
```java
@Configuration
@EnableBatchProcessing
public class CustomerProcessingBatchConfig {
    
    @Bean
    public Job customerProcessingJob() {
        return new JobBuilder("customerProcessingJob", jobRepository)
            .start(readCustomersStep())          // 1. 고객 데이터 읽기
            .next(validateCustomersStep())       // 2. 데이터 검증
            .next(enrichCustomersStep())         // 3. 데이터 보강
            .next(calculateScoreStep())          // 4. 점수 계산
            .next(sendNotificationStep())        // 5. 알림 발송
            .next(cleanupStep())                 // 6. 정리 작업
            .build();
    }
    
    // Step 1: 고객 데이터 읽기
    @Bean
    public Step readCustomersStep() {
        return new StepBuilder("readCustomersStep", jobRepository)
            .<CustomerCsv, Customer>chunk(500, transactionManager)
            .reader(customerCsvReader())
            .processor(csvToCustomerProcessor())
            .writer(customerDatabaseWriter())
            .build();
    }
    
    // Step 2: 데이터 검증
    @Bean
    public Step validateCustomersStep() {
        return new StepBuilder("validateCustomersStep", jobRepository)
            .<Customer, Customer>chunk(1000, transactionManager)
            .reader(customerDatabaseReader())
            .processor(validationProcessor())
            .writer(validatedCustomerWriter())
            .faultTolerant()
            .skip(ValidationException.class)
            .skipLimit(100)
            .listener(validationSkipListener())
            .build();
    }
    
    // Step 3: 외부 API를 통한 데이터 보강
    @Bean
    public Step enrichCustomersStep() {
        return new StepBuilder("enrichCustomersStep", jobRepository)
            .<Customer, Customer>chunk(100, transactionManager)
            .reader(validatedCustomerReader())
            .processor(enrichmentProcessor())
            .writer(enrichedCustomerWriter())
            .faultTolerant()
            .retry(ConnectException.class)
            .retryLimit(3)
            .taskExecutor(taskExecutor())
            .throttleLimit(5)  // API 호출 제한
            .build();
    }
}
```

이제 Spring Batch의 모든 기본 개념을 이해했습니다! 다음 단계에서는 실제로 배치를 어떻게 구현하고 운영하는지 더 자세히 알아보겠습니다.