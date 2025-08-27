package com.springbatch.repository;

import com.springbatch.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WeatherData 엔티티에 대한 데이터베이스 접근을 담당하는 JPA 리포지토리
 * 
 * 날씨 데이터 조회, 통계, 분석을 위한 다양한 쿼리 메서드를 제공합니다.
 */
@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {
    
    /**
     * 특정 도시의 최신 날씨 데이터 조회
     */
    Optional<WeatherData> findFirstByCityCodeOrderByCollectedAtDesc(String cityCode);
    
    /**
     * 특정 도시의 특정 날짜 범위 내 날씨 데이터 조회
     */
    List<WeatherData> findByCityCodeAndCollectedAtBetweenOrderByCollectedAtDesc(
            String cityCode, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 모든 도시의 최신 날씨 데이터 조회 (도시별 최신 1개씩)
     */
    @Query("SELECT w FROM WeatherData w WHERE w.collectedAt = " +
           "(SELECT MAX(w2.collectedAt) FROM WeatherData w2 WHERE w2.cityCode = w.cityCode)")
    List<WeatherData> findLatestWeatherDataForAllCities();
    
    /**
     * 이상 기후 데이터 조회
     */
    List<WeatherData> findByIsAbnormalTrueOrderByCollectedAtDesc();
    
    /**
     * 특정 온도 범위의 날씨 데이터 조회
     */
    List<WeatherData> findByTemperatureBetweenOrderByCollectedAtDesc(
            Double minTemp, Double maxTemp);
    
    /**
     * 특정 도시의 일간 평균 온도 계산
     */
    @Query("SELECT AVG(w.temperature) FROM WeatherData w " +
           "WHERE w.cityCode = :cityCode " +
           "AND w.collectedAt BETWEEN :startOfDay AND :endOfDay")
    Double calculateDailyAverageTemperature(
            @Param("cityCode") String cityCode,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
    
    /**
     * 특정 도시의 최고/최저 온도 조회 (특정 날짜)
     */
    @Query("SELECT MAX(w.temperature), MIN(w.temperature) FROM WeatherData w " +
           "WHERE w.cityCode = :cityCode " +
           "AND w.collectedAt BETWEEN :startOfDay AND :endOfDay")
    List<Object[]> findDailyTemperatureRange(
            @Param("cityCode") String cityCode,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
    
    /**
     * 전국 도시별 현재 온도 순위 조회 (높은 순)
     */
    @Query("SELECT w FROM WeatherData w WHERE w.collectedAt = " +
           "(SELECT MAX(w2.collectedAt) FROM WeatherData w2 WHERE w2.cityCode = w.cityCode) " +
           "ORDER BY w.temperature DESC")
    List<WeatherData> findCitiesOrderByTemperatureDesc();
    
    /**
     * 특정 날씨 조건 (비, 눈 등) 데이터 조회
     */
    List<WeatherData> findByWeatherMainAndCollectedAtBetween(
            String weatherMain, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 습도가 높은 지역 조회 (특정 임계값 이상)
     */
    List<WeatherData> findByHumidityGreaterThanEqualOrderByHumidityDesc(Integer humidity);
    
}