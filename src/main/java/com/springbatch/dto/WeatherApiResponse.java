package com.springbatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenWeatherMap API 응답을 매핑하는 DTO 클래스
 * API에서 받은 JSON 데이터를 Java 객체로 변환합니다.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherApiResponse {
    
    // 날씨 정보 배열
    private List<Weather> weather;
    
    // 주요 날씨 데이터 (온도, 습도 등)
    private Main main;
    
    // 바람 정보
    private Wind wind;
    
    // 구름 정보
    private Clouds clouds;
    
    // 강수량 정보 (있을 경우)
    private Rain rain;
    
    // 적설량 정보 (있을 경우)
    private Snow snow;
    
    // 가시거리
    private Integer visibility;
    
    // 데이터 계산 시간 (유닉스 타임스탬프)
    private Long dt;
    
    // 도시 이름
    private String name;
    
    // 날씨 상태 정보
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        private Integer id;
        private String main;        // 날씨 그룹 (Rain, Snow, Clouds 등)
        private String description; // 날씨 설명
        private String icon;        // 날씨 아이콘 ID
    }
    
    // 주요 날씨 데이터
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        private Double temp;        // 온도
        @JsonProperty("feels_like")
        private Double feelsLike;   // 체감온도
        @JsonProperty("temp_min")
        private Double tempMin;     // 최저온도
        @JsonProperty("temp_max")
        private Double tempMax;     // 최고온도
        private Integer pressure;   // 기압
        private Integer humidity;   // 습도
    }
    
    // 바람 정보
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        private Double speed;       // 바람 속도 (m/s)
        private Integer deg;        // 바람 방향 (도)
        private Double gust;        // 돌풍 속도
    }
    
    // 구름 정보
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Clouds {
        private Integer all;        // 구름량 (%)
    }
    
    // 강수량 정보
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rain {
        @JsonProperty("1h")
        private Double oneHour;     // 지난 1시간 강수량
        @JsonProperty("3h")  
        private Double threeHour;   // 지난 3시간 강수량
    }
    
    // 적설량 정보
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snow {
        @JsonProperty("1h")
        private Double oneHour;     // 지난 1시간 적설량
        @JsonProperty("3h")
        private Double threeHour;   // 지난 3시간 적설량
    }
}