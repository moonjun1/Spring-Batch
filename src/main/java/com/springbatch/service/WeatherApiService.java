package com.springbatch.service;

import com.springbatch.dto.WeatherApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * OpenWeatherMap API와 연동하여 날씨 데이터를 수집하는 서비스
 * 
 * 주요 기능:
 * - 도시별 현재 날씨 정보 조회
 * - 전국 주요 도시 날씨 데이터 일괄 수집
 * - API 호출 및 응답 처리
 */
@Slf4j
@Service
public class WeatherApiService {
    
    private final WebClient webClient;
    
    // OpenWeatherMap API 키 (application.properties에서 설정)
    @Value("${weather.api.key:demo_key}")
    private String apiKey;
    
    // OpenWeatherMap API URL
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    // 전국 주요 도시 목록 (도시명, 도시코드)
    private static final Map<String, String> MAJOR_CITIES = Map.of(
        "Seoul", "서울",
        "Busan", "부산", 
        "Incheon", "인천",
        "Daegu", "대구",
        "Daejeon", "대전",
        "Gwangju", "광주",
        "Ulsan", "울산",
        "Suwon", "수원"
    );
    
    public WeatherApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(API_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
    
    /**
     * 특정 도시의 현재 날씨 정보 조회
     * 
     * @param cityName 도시명 (영문)
     * @return 날씨 API 응답 데이터
     */
    public Mono<WeatherApiResponse> getCurrentWeather(String cityName) {
        log.info("Fetching weather data for city: {}", cityName);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("q", cityName + ",KR")  // 한국 도시로 제한
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")      // 섭씨 온도 사용
                        .queryParam("lang", "kr")           // 한국어 설명
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .doOnSuccess(response -> log.info("Successfully fetched weather data for {}", cityName))
                .doOnError(error -> log.error("Failed to fetch weather data for {}: {}", cityName, error.getMessage()));
    }
    
    /**
     * 전국 주요 도시의 날씨 정보 일괄 조회
     * 
     * @return 모든 도시의 날씨 데이터 리스트
     */
    public Mono<List<WeatherApiResponse>> getAllMajorCitiesWeather() {
        log.info("Fetching weather data for all major cities: {}", MAJOR_CITIES.keySet());
        
        List<Mono<WeatherApiResponse>> weatherRequests = MAJOR_CITIES.keySet().stream()
                .map(this::getCurrentWeather)
                .toList();
        
        return Mono.zip(weatherRequests, responses -> {
            List<WeatherApiResponse> results = List.of((WeatherApiResponse[]) responses);
            log.info("Successfully fetched weather data for {} cities", results.size());
            return results;
        }).onErrorResume(error -> {
            log.error("Failed to fetch weather data for some cities: {}", error.getMessage());
            return Mono.empty();
        });
    }
    
    /**
     * 도시 코드(영문)를 한글명으로 변환
     */
    public String getCityNameInKorean(String englishCityName) {
        return MAJOR_CITIES.getOrDefault(englishCityName, englishCityName);
    }
    
    /**
     * 지원하는 주요 도시 목록 반환
     */
    public Map<String, String> getMajorCities() {
        return MAJOR_CITIES;
    }
    
    /**
     * API 키가 설정되어 있는지 확인
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.equals("demo_key") && !apiKey.trim().isEmpty();
    }
}