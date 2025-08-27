package com.springbatch.entity;

/**
 * 알림 유형
 */
public enum AlertType {
    HEAT_WAVE("폭염"),
    COLD_WAVE("한파"),
    HEAVY_RAIN("호우"),
    HEAVY_SNOW("대설"),
    STRONG_WIND("강풍"),
    ABNORMAL_WEATHER("이상기후");
    
    private final String koreanName;
    
    AlertType(String koreanName) {
        this.koreanName = koreanName;
    }
    
    public String getKoreanName() {
        return koreanName;
    }
}