package com.springbatch.repository;

import com.springbatch.entity.WeatherStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 날씨 통계 데이터 접근을 담당하는 리포지토리
 */
@Repository
public interface WeatherStatisticsRepository extends JpaRepository<WeatherStatistics, Long> {
    
    /**
     * 특정 날짜와 도시의 통계 조회
     */
    Optional<WeatherStatistics> findByStatisticsDateAndCityCode(LocalDate date, String cityCode);
    
    /**
     * 특정 날짜의 모든 도시 통계 조회
     */
    List<WeatherStatistics> findByStatisticsDateOrderByCityNameAsc(LocalDate date);
    
    /**
     * 특정 도시의 기간별 통계 조회 (최근 순)
     */
    List<WeatherStatistics> findByCityCodeAndStatisticsDateBetweenOrderByStatisticsDateDesc(
            String cityCode, LocalDate startDate, LocalDate endDate);
    
    /**
     * 최근 N일간의 통계 조회
     */
    @Query("SELECT ws FROM WeatherStatistics ws WHERE ws.statisticsDate >= :fromDate ORDER BY ws.statisticsDate DESC, ws.cityName ASC")
    List<WeatherStatistics> findRecentStatistics(@Param("fromDate") LocalDate fromDate);
    
    /**
     * 월간 평균 온도 트렌드 (특정 도시)
     */
    @Query("SELECT ws FROM WeatherStatistics ws WHERE ws.cityCode = :cityCode " +
           "AND ws.statisticsDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ws.statisticsDate ASC")
    List<WeatherStatistics> findMonthlyTrend(@Param("cityCode") String cityCode,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * 이상 기후 발생이 많은 도시 순위
     */
    @Query("SELECT ws FROM WeatherStatistics ws WHERE ws.statisticsDate BETWEEN :startDate AND :endDate " +
           "AND ws.abnormalWeatherCount > 0 ORDER BY ws.abnormalWeatherCount DESC")
    List<WeatherStatistics> findCitiesByAbnormalWeatherCount(@Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);
    
    /**
     * 전국 온도 평균 계산 (특정 기간)
     */
    @Query("SELECT AVG(ws.avgTemperature) FROM WeatherStatistics ws " +
           "WHERE ws.statisticsDate BETWEEN :startDate AND :endDate")
    Double calculateNationalAverageTemperature(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
    
    /**
     * 도시별 연간 최고/최저 온도 기록
     */
    @Query("SELECT ws.cityName, MAX(ws.maxTemperature), MIN(ws.minTemperature) " +
           "FROM WeatherStatistics ws WHERE YEAR(ws.statisticsDate) = :year " +
           "GROUP BY ws.cityName, ws.cityCode ORDER BY ws.cityName")
    List<Object[]> findYearlyTemperatureRecords(@Param("year") int year);
    
    /**
     * 특정 기간 동안 데이터 수집률이 낮은 도시
     */
    @Query("SELECT ws FROM WeatherStatistics ws WHERE ws.statisticsDate BETWEEN :startDate AND :endDate " +
           "AND ws.dataCollectionRate < :threshold ORDER BY ws.dataCollectionRate ASC")
    List<WeatherStatistics> findLowDataCollectionRateCities(@Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate,
                                                           @Param("threshold") Double threshold);
}