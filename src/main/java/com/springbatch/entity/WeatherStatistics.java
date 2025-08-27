package com.springbatch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 날씨 통계 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "weather_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "statistics_date", nullable = false)
    private LocalDate statisticsDate;
    
    @Column(name = "city_code", nullable = false, length = 50)
    private String cityCode;
    
    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;
    
    // 온도 통계
    @Column(name = "avg_temperature", precision = 5, scale = 2)
    private BigDecimal avgTemperature;
    
    @Column(name = "max_temperature", precision = 5, scale = 2)
    private BigDecimal maxTemperature;
    
    @Column(name = "min_temperature", precision = 5, scale = 2)
    private BigDecimal minTemperature;
    
    @Column(name = "temperature_range", precision = 5, scale = 2)
    private BigDecimal temperatureRange;
    
    // 습도 및 기압 통계
    @Column(name = "avg_humidity")
    private Integer avgHumidity;
    
    @Column(name = "avg_pressure")
    private Integer avgPressure;
    
    // 날씨 상태 통계
    @Column(name = "dominant_weather", length = 50)
    private String dominantWeather;
    
    @Column(name = "clear_hours")
    private Integer clearHours;
    
    @Column(name = "cloudy_hours")
    private Integer cloudyHours;
    
    @Column(name = "rainy_hours")
    private Integer rainyHours;
    
    // 이상 기후 통계
    @Column(name = "abnormal_weather_count")
    private Integer abnormalWeatherCount = 0;
    
    @Column(name = "max_temperature_change", precision = 5, scale = 2)
    private BigDecimal maxTemperatureChange;
    
    // 데이터 수집 통계
    @Column(name = "total_records")
    private Integer totalRecords;
    
    @Column(name = "data_collection_rate", precision = 5, scale = 2)
    private BigDecimal dataCollectionRate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 온도 범위 계산
     */
    public void calculateTemperatureRange() {
        if (maxTemperature != null && minTemperature != null) {
            this.temperatureRange = maxTemperature.subtract(minTemperature);
        }
    }
    
    /**
     * 데이터 수집률 계산 (24시간 기준)
     */
    public void calculateDataCollectionRate(int expectedRecords) {
        if (totalRecords != null && expectedRecords > 0) {
            this.dataCollectionRate = BigDecimal.valueOf(totalRecords)
                    .divide(BigDecimal.valueOf(expectedRecords), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
}