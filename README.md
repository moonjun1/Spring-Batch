# Spring Batch 프로젝트

## 📋 프로젝트 개요

Spring Boot 3.5.5와 Spring Batch를 활용한 **CSV 파일 처리 및 실시간 날씨 데이터 수집** 배치 시스템입니다.
<img width="1356" height="937" alt="스크린샷 2025-08-27 234348" src="https://github.com/user-attachments/assets/727967ac-77da-46c6-8635-c81df53591d3" />
<img width="650" height="790" alt="스크린샷 2025-08-27 225313" src="https://github.com/user-attachments/assets/41a48c82-3d0c-498e-9090-0096382546d9" />
<img width="1868" height="347" alt="스크린샷 2025-08-27 225241" src="https://github.com/user-attachments/assets/883d2b9f-1c87-4065-be7e-edf3e66a236f" />
<img width="667" height="175" alt="스크린샷 2025-08-27 225236" src="https://github.com/user-attachments/assets/cf6c57e0-6426-48c9-81d1-c370dd3a4c9c" />
<img width="1437" height="926" alt="스크린샷 2025-08-27 225231" src="https://github.com/user-attachments/assets/f7edd7cf-8410-4fe4-a3e6-0d349125645b" />
<img width="1414" height="810" alt="스크린샷 2025-08-27 234402" src="https://github.com/user-attachments/assets/ec8c0123-52bd-490c-916d-0b6b2a0d0f26" />
<img width="1338" height="763" alt="스크린샷 2025-08-27 234352" src="https://github.com/user-attachments/assets/63910937-6934-4b00-97e1-d5ab62e86f9c" />

### 주요 기능

- 📄 **CSV 배치 처리**: 사용자 데이터를 CSV 파일에서 읽어 데이터베이스에 저장
- 🌦️ **날씨 데이터 수집**: OpenWeatherMap API를 통한 전국 주요 도시 날씨 정보 수집
- ⚠️ **이상 기후 탐지**: 전날 대비 20도 이상 온도 변화 감지
- 📊 **실시간 대시보드**: Thymeleaf 기반 웹 UI 제공
- 🗄️ **H2 데이터베이스**: 개발 및 테스트용 인메모리 데이터베이스

## 🛠️ 기술 스택

- **Java 17**
- **Spring Boot 3.5.5**
- **Spring Batch**
- **Spring Data JPA**
- **Spring WebFlux**
- **H2 Database**
- **Thymeleaf**
- **Lombok**
- **Gradle**

## 🚀 시작하기

### 1. 프로젝트 클론
```bash
git clone https://github.com/moonjun1/Spring-Batch.git
cd Spring-Batch
```

### 2. 환경 설정
`.env` 파일에 날씨 API 키 설정:
```properties
WEATHER_API_KEY=your_openweathermap_api_key
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 4. 웹 브라우저 접속
- **메인 페이지**: http://localhost:8080
- **날씨 대시보드**: http://localhost:8080/weather/dashboard
- **H2 콘솔**: http://localhost:8080/h2-console

## 📊 주요 화면

### 메인 대시보드
- CSV 배치 실행
- 저장된 데이터 조회
- H2 데이터베이스 관리

### 날씨 대시보드
- 전국 8개 도시 실시간 날씨
- 온도별 도시 순위
- 이상 기후 알림
- 수집 통계

## 🗄️ 데이터베이스

### H2 콘솔 접속 정보
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **사용자명**: `sa`
- **비밀번호**: (비워두세요)

### 주요 테이블
- `PERSON`: CSV에서 읽은 사용자 정보
- `WEATHER_DATA`: 수집된 날씨 데이터

## 🔧 개발 가이드

자세한 학습 가이드와 개발 문서는 `docs/` 폴더를 참고하세요:

1. [현재 프로젝트 구조 분석](docs/01-현재-프로젝트-구조-분석.md)
2. [시스템 사용법](docs/02-현재-시스템-사용법.md)
3. [Spring Batch 기초](docs/04-Spring-Batch-기초-완전-정복.md)

## 📂 프로젝트 구조

```
src/main/java/com/springbatch/
├── SpringBatchApplication.java          # 메인 애플리케이션
├── config/
│   ├── BatchConfig.java                 # CSV 배치 설정
│   └── WeatherBatchConfig.java          # 날씨 배치 설정
├── controller/
│   ├── BatchController.java             # 배치 실행 컨트롤러
│   └── WeatherController.java           # 날씨 API 컨트롤러
├── entity/
│   ├── Person.java                      # 사용자 엔티티
│   └── WeatherData.java                 # 날씨 데이터 엔티티
├── dto/
│   └── WeatherApiResponse.java          # 날씨 API 응답 DTO
├── repository/
│   ├── PersonRepository.java            # 사용자 리포지토리
│   └── WeatherDataRepository.java       # 날씨 데이터 리포지토리
└── service/
    └── WeatherApiService.java           # 날씨 API 서비스
```

## 🎯 주요 기능 소개

### CSV 배치 처리
- 청크 기반 처리 (3개씩 묶어서 처리)
- 데이터 검증 및 변환
- 트랜잭션 안전성 보장

### 날씨 데이터 수집
- 8개 주요 도시 (서울, 부산, 인천, 대구, 대전, 광주, 울산, 수원)
- 실시간 온도, 습도, 기압, 날씨 상태 수집
- 전날 대비 온도 변화량 계산
- 이상 기후 자동 탐지

## 🤝 기여하기

1. 이 저장소를 Fork 합니다
2. 새로운 기능 브랜치를 생성합니다 (`git checkout -b feat/new-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'feat: 새로운 기능 추가'`)
4. 브랜치에 푸시합니다 (`git push origin feat/new-feature`)
5. Pull Request를 생성합니다

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 📞 문의

프로젝트에 대한 질문이나 제안사항이 있으시면 이슈를 생성해 주세요.
