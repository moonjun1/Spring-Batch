package com.springbatch.repository;

import com.springbatch.entity.AlertLevel;
import com.springbatch.entity.AlertType;
import com.springbatch.entity.WeatherAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기상 특보 데이터 접근을 담당하는 리포지토리
 */
@Repository
public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, Long> {
    
    /**
     * 활성화된 알림 조회 (해제되지 않은)
     */
    List<WeatherAlert> findByIsResolvedFalseOrderByAlertTimeDesc();
    
    /**
     * 특정 도시의 활성화된 알림 조회
     */
    List<WeatherAlert> findByCityCodeAndIsResolvedFalseOrderByAlertTimeDesc(String cityCode);
    
    /**
     * 특정 알림 유형의 활성화된 알림 조회
     */
    List<WeatherAlert> findByAlertTypeAndIsResolvedFalseOrderByAlertTimeDesc(AlertType alertType);
    
    /**
     * 발송되지 않은 알림 조회
     */
    List<WeatherAlert> findByIsSentFalseOrderByAlertTimeAsc();
    
    /**
     * 최근 알림 조회 (기간별)
     */
    List<WeatherAlert> findByAlertTimeBetweenOrderByAlertTimeDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 특정 수준 이상의 알림 조회
     */
    @Query("SELECT wa FROM WeatherAlert wa WHERE wa.alertLevel IN :levels " +
           "AND wa.alertTime BETWEEN :startTime AND :endTime " +
           "ORDER BY wa.alertTime DESC")
    List<WeatherAlert> findByAlertLevelsAndTimeBetween(@Param("levels") List<AlertLevel> levels,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 도시별 알림 발생 빈도 (기간별)
     */
    @Query("SELECT wa.cityName, wa.alertType, COUNT(wa) FROM WeatherAlert wa " +
           "WHERE wa.alertTime BETWEEN :startTime AND :endTime " +
           "GROUP BY wa.cityName, wa.cityCode, wa.alertType " +
           "ORDER BY COUNT(wa) DESC")
    List<Object[]> findAlertFrequencyByCity(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 알림 유형별 통계
     */
    @Query("SELECT wa.alertType, wa.alertLevel, COUNT(wa) FROM WeatherAlert wa " +
           "WHERE wa.alertTime BETWEEN :startTime AND :endTime " +
           "GROUP BY wa.alertType, wa.alertLevel " +
           "ORDER BY wa.alertType, wa.alertLevel")
    List<Object[]> findAlertStatisticsByType(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 중복 알림 방지를 위한 최근 동일 알림 확인
     */
    @Query("SELECT wa FROM WeatherAlert wa WHERE wa.cityCode = :cityCode " +
           "AND wa.alertType = :alertType AND wa.isResolved = false " +
           "AND wa.alertTime > :recentTime")
    List<WeatherAlert> findRecentSimilarAlerts(@Param("cityCode") String cityCode,
                                              @Param("alertType") AlertType alertType,
                                              @Param("recentTime") LocalDateTime recentTime);
    
    /**
     * 해결 시간이 오래된 알림 조회 (자동 해제 대상)
     */
    @Query("SELECT wa FROM WeatherAlert wa WHERE wa.isResolved = false " +
           "AND wa.alertTime < :expiredTime")
    List<WeatherAlert> findExpiredAlerts(@Param("expiredTime") LocalDateTime expiredTime);
    
    /**
     * 알림 발송 성공률 계산
     */
    @Query("SELECT COUNT(wa), SUM(CASE WHEN wa.isSent = true THEN 1 ELSE 0 END) " +
           "FROM WeatherAlert wa WHERE wa.alertTime BETWEEN :startTime AND :endTime")
    List<Object[]> calculateAlertSendingSuccessRate(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
}