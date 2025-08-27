package com.springbatch.entity;

/**
 * 알림 수준
 */
public enum AlertLevel {
    NOTICE("주의"),
    ADVISORY("주의보"),
    WARNING("경보"),
    EMERGENCY("긴급");
    
    private final String koreanName;
    
    AlertLevel(String koreanName) {
        this.koreanName = koreanName;
    }
    
    public String getKoreanName() {
        return koreanName;
    }
}