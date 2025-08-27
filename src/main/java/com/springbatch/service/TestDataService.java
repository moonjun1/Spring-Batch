package com.springbatch.service;

import com.springbatch.entity.WeatherData;
import com.springbatch.repository.WeatherDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 테스트용 샘플 데이터 생성 서비스
 */
@Slf4j
@Service
public class TestDataService {
    
    @Autowired
    private WeatherDataRepository weatherDataRepository;
    
    private final Random random = new Random();
    
    private final String[] cities = {"Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan", "Suwon"};
    private final String[] cityNames = {"서울", "부산", "인천", "대구", "대전", "광주", "울산", "수원"};
    private final String[] weatherConditions = {"Clear", "Clouds", "Rain", "Snow", "Thunderstorm"};
    
    /**
     * 테스트용 날씨 데이터 생성
     */
    public void generateTestWeatherData() {
        log.info("🔧 Generating test weather data...");
        
        List<WeatherData> testDataList = new ArrayList<>();
        
        // 지난 3일간의 데이터 생성 (시간별)
        for (int day = 3; day >= 0; day--) {
            LocalDateTime baseDate = LocalDateTime.now().minusDays(day);
            
            for (int hour = 0; hour < 24; hour++) {
                LocalDateTime targetTime = baseDate.withHour(hour).withMinute(0).withSecond(0);
                
                for (int i = 0; i < cities.length; i++) {
                    WeatherData weatherData = createRandomWeatherData(cities[i], cityNames[i], targetTime);
                    testDataList.add(weatherData);
                }
            }
        }
        
        // 극한 상황 데이터 추가 (알림 테스트용)
        addExtremeWeatherData(testDataList);
        
        // 데이터베이스에 저장
        weatherDataRepository.saveAll(testDataList);
        log.info("✅ Generated {} test weather data records", testDataList.size());
    }
    
    /**
     * 랜덤 날씨 데이터 생성
     */
    private WeatherData createRandomWeatherData(String cityCode, String cityName, LocalDateTime collectedAt) {
        WeatherData weatherData = new WeatherData();
        
        weatherData.setCityCode(cityCode);
        weatherData.setCityName(cityName);
        weatherData.setCollectedAt(collectedAt);
        
        // 기본 온도 범위 (-5 ~ 30도)
        double baseTemp = -5 + (random.nextDouble() * 35);
        weatherData.setTemperature(Math.round(baseTemp * 10.0) / 10.0);
        
        // 습도 (30 ~ 90%)
        weatherData.setHumidity(30 + random.nextInt(61));
        
        // 기압 (1000 ~ 1030 hPa)
        weatherData.setPressure(1000 + random.nextInt(31));
        
        // 날씨 상태
        String weatherMain = weatherConditions[random.nextInt(weatherConditions.length)];
        weatherData.setWeatherMain(weatherMain);
        weatherData.setWeatherDescription(getWeatherDescription(weatherMain));
        
        // 온도 변화량 계산 (전날 동시간 대비)
        double tempChange = -10 + (random.nextDouble() * 20); // -10 ~ +10도 변화
        weatherData.setTemperatureChange(Math.round(tempChange * 10.0) / 10.0);
        
        // 이상 기후 판정 (온도 변화가 15도 이상이면 이상 기후)
        weatherData.setIsAbnormal(Math.abs(tempChange) >= 15.0);
        
        return weatherData;
    }
    
    /**
     * 극한 날씨 데이터 추가 (알림 테스트용)
     */
    private void addExtremeWeatherData(List<WeatherData> testDataList) {
        LocalDateTime now = LocalDateTime.now();
        
        // 폭염 데이터 (35도 이상)
        WeatherData heatWave = new WeatherData();
        heatWave.setCityCode("Seoul");
        heatWave.setCityName("서울");
        heatWave.setTemperature(37.5);
        heatWave.setHumidity(85);
        heatWave.setPressure(1010);
        heatWave.setWeatherMain("Clear");
        heatWave.setWeatherDescription("맑음");
        heatWave.setCollectedAt(now.minusHours(1));
        heatWave.setTemperatureChange(8.0);
        heatWave.setIsAbnormal(false);
        testDataList.add(heatWave);
        
        // 한파 데이터 (-10도 이하)
        WeatherData coldWave = new WeatherData();
        coldWave.setCityCode("Daegu");
        coldWave.setCityName("대구");
        coldWave.setTemperature(-12.3);
        coldWave.setHumidity(45);
        coldWave.setPressure(1025);
        coldWave.setWeatherMain("Snow");
        coldWave.setWeatherDescription("눈");
        coldWave.setCollectedAt(now.minusHours(2));
        coldWave.setTemperatureChange(-15.0);
        coldWave.setIsAbnormal(false);
        testDataList.add(coldWave);
        
        // 호우 데이터
        WeatherData heavyRain = new WeatherData();
        heavyRain.setCityCode("Busan");
        heavyRain.setCityName("부산");
        heavyRain.setTemperature(22.0);
        heavyRain.setHumidity(95);
        heavyRain.setPressure(995);
        heavyRain.setWeatherMain("Rain");
        heavyRain.setWeatherDescription("폭우");
        heavyRain.setCollectedAt(now.minusMinutes(30));
        heavyRain.setTemperatureChange(3.0);
        heavyRain.setIsAbnormal(false);
        testDataList.add(heavyRain);
        
        // 이상 기후 데이터 (급격한 온도 변화)
        WeatherData abnormalWeather = new WeatherData();
        abnormalWeather.setCityCode("Incheon");
        abnormalWeather.setCityName("인천");
        abnormalWeather.setTemperature(15.0);
        abnormalWeather.setHumidity(70);
        abnormalWeather.setPressure(1008);
        abnormalWeather.setWeatherMain("Clouds");
        abnormalWeather.setWeatherDescription("구름많음");
        abnormalWeather.setCollectedAt(now.minusMinutes(15));
        abnormalWeather.setTemperatureChange(-22.5); // 급격한 하락
        abnormalWeather.setIsAbnormal(true);
        testDataList.add(abnormalWeather);
        
        log.info("🌡️ Added {} extreme weather data samples for alert testing", 4);
    }
    
    /**
     * 날씨 상태별 설명 반환
     */
    private String getWeatherDescription(String weatherMain) {
        return switch (weatherMain) {
            case "Clear" -> "맑음";
            case "Clouds" -> "구름많음";
            case "Rain" -> "비";
            case "Snow" -> "눈";
            case "Thunderstorm" -> "뇌우";
            default -> "기타";
        };
    }
    
    /**
     * 테스트 데이터 삭제
     */
    public void clearTestData() {
        log.info("🗑️ Clearing all weather data...");
        weatherDataRepository.deleteAll();
        log.info("✅ All weather data cleared");
    }
    
    /**
     * 현재 데이터 개수 확인
     */
    public long getWeatherDataCount() {
        return weatherDataRepository.count();
    }
}