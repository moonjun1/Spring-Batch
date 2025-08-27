package com.springbatch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기상 특보 및 알림 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "weather_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "city_code", nullable = false, length = 50)
    private String cityCode;
    
    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false)
    private AlertLevel alertLevel;
    
    @Column(name = "alert_title", nullable = false, length = 200)
    private String alertTitle;
    
    @Column(name = "alert_message", length = 1000)
    private String alertMessage;
    
    @Column(name = "trigger_value")
    private Double triggerValue;
    
    @Column(name = "threshold_value")
    private Double thresholdValue;
    
    @Column(name = "weather_data_id")
    private Long weatherDataId;
    
    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;
    
    @Column(name = "is_sent")
    private Boolean isSent = false;
    
    @Column(name = "sent_time")
    private LocalDateTime sentTime;
    
    @Column(name = "is_resolved")
    private Boolean isResolved = false;
    
    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.alertTime == null) {
            this.alertTime = LocalDateTime.now();
        }
    }
    
    /**
     * 알림 발송 처리
     */
    public void markAsSent() {
        this.isSent = true;
        this.sentTime = LocalDateTime.now();
    }
    
    /**
     * 알림 해제 처리
     */
    public void markAsResolved() {
        this.isResolved = true;
        this.resolvedTime = LocalDateTime.now();
    }
    
    /**
     * 알림 생성 팩토리 메서드
     */
    public static WeatherAlert createHeatWaveAlert(String cityCode, String cityName, Double temperature) {
        WeatherAlert alert = new WeatherAlert();
        alert.setCityCode(cityCode);
        alert.setCityName(cityName);
        alert.setAlertType(AlertType.HEAT_WAVE);
        alert.setAlertLevel(AlertLevel.WARNING);
        alert.setTriggerValue(temperature);
        alert.setThresholdValue(35.0);
        alert.setAlertTitle(String.format("%s 폭염 경보", cityName));
        alert.setAlertMessage(String.format("%s 지역에 폭염 경보가 발령되었습니다. 현재 기온: %.1f°C", cityName, temperature));
        return alert;
    }
    
    public static WeatherAlert createColdWaveAlert(String cityCode, String cityName, Double temperature) {
        WeatherAlert alert = new WeatherAlert();
        alert.setCityCode(cityCode);
        alert.setCityName(cityName);
        alert.setAlertType(AlertType.COLD_WAVE);
        alert.setAlertLevel(AlertLevel.ADVISORY);
        alert.setTriggerValue(temperature);
        alert.setThresholdValue(-10.0);
        alert.setAlertTitle(String.format("%s 한파 주의보", cityName));
        alert.setAlertMessage(String.format("%s 지역에 한파 주의보가 발령되었습니다. 현재 기온: %.1f°C", cityName, temperature));
        return alert;
    }
    
    public static WeatherAlert createHeavyRainAlert(String cityCode, String cityName, Double rainfall) {
        WeatherAlert alert = new WeatherAlert();
        alert.setCityCode(cityCode);
        alert.setCityName(cityName);
        alert.setAlertType(AlertType.HEAVY_RAIN);
        alert.setAlertLevel(AlertLevel.WARNING);
        alert.setTriggerValue(rainfall);
        alert.setThresholdValue(50.0);
        alert.setAlertTitle(String.format("%s 호우 경보", cityName));
        alert.setAlertMessage(String.format("%s 지역에 호우 경보가 발령되었습니다. 시간당 강수량: %.1fmm", cityName, rainfall));
        return alert;
    }
    
    public static WeatherAlert createAbnormalWeatherAlert(String cityCode, String cityName, Double temperatureChange) {
        WeatherAlert alert = new WeatherAlert();
        alert.setCityCode(cityCode);
        alert.setCityName(cityName);
        alert.setAlertType(AlertType.ABNORMAL_WEATHER);
        alert.setAlertLevel(AlertLevel.NOTICE);
        alert.setTriggerValue(Math.abs(temperatureChange));
        alert.setThresholdValue(20.0);
        alert.setAlertTitle(String.format("%s 이상 기후 감지", cityName));
        alert.setAlertMessage(String.format("%s 지역에서 급격한 기온 변화가 감지되었습니다. 전날 대비: %+.1f°C", 
                cityName, temperatureChange));
        return alert;
    }
}