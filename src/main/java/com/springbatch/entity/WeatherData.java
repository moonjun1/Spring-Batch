package com.springbatch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 날씨 데이터를 저장하는 JPA 엔티티 클래스
 * OpenWeatherMap API에서 수집한 날씨 정보를 데이터베이스에 저장합니다.
 */
@Entity
@Table(name = "weather_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    
    // 기본키 - 자동으로 증가하는 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 도시명 (한글)
    @Column(nullable = false)
    private String cityName;
    
    // 도시 코드 (영문)
    @Column(nullable = false)
    private String cityCode;
    
    // 현재 온도 (섭씨)
    @Column(nullable = false)
    private Double temperature;
    
    // 체감 온도 (섭씨)
    private Double feelsLike;
    
    // 최저 온도 (섭씨)
    private Double tempMin;
    
    // 최고 온도 (섭씨)
    private Double tempMax;
    
    // 습도 (%)
    private Integer humidity;
    
    // 기압 (hPa)
    private Integer pressure;
    
    // 날씨 상태 (맑음, 흐림, 비, 눈 등)
    private String weatherMain;
    
    // 날씨 상세 설명
    private String weatherDescription;
    
    // 구름량 (%)
    private Integer cloudiness;
    
    // 바람 속도 (m/s)
    private Double windSpeed;
    
    // 바람 방향 (도)
    private Integer windDirection;
    
    // 강수량 (mm) - 지난 1시간
    private Double rainfall;
    
    // 적설량 (mm) - 지난 1시간  
    private Double snowfall;
    
    // 가시거리 (m)
    private Integer visibility;
    
    // 데이터 수집 시간
    @Column(nullable = false)
    private LocalDateTime collectedAt;
    
    // API에서 제공하는 날씨 데이터 시간
    private LocalDateTime weatherTime;
    
    // 이상 기후 플래그 (전날 대비 급격한 변화)
    private Boolean isAbnormal = false;
    
    // 온도 변화량 (전날 동시간 대비)
    private Double temperatureChange;
    
    /**
     * 날씨 데이터 생성자 (필수 필드만)
     */
    public WeatherData(String cityName, String cityCode, Double temperature, 
                      String weatherMain, LocalDateTime collectedAt) {
        this.cityName = cityName;
        this.cityCode = cityCode;
        this.temperature = temperature;
        this.weatherMain = weatherMain;
        this.collectedAt = collectedAt;
        this.weatherTime = collectedAt;
    }
}