package com.springbatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CSV 파일에서 읽어온 사용자 데이터를 임시로 담는 DTO(Data Transfer Object) 클래스
 * 배치 처리 과정에서 CSV → DTO → Entity 순서로 데이터가 변환됩니다.
 * 
 * Lombok 어노테이션 설명:
 * @Data - getter, setter, toString, equals, hashCode 자동 생성
 * @NoArgsConstructor - 기본 생성자 자동 생성 (Spring Batch CSV 매핑 시 필수)
 * @AllArgsConstructor - 모든 필드를 매개변수로 받는 생성자 자동 생성
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonDto {
    
    // CSV의 firstName 컬럼과 매핑되는 이름 필드
    private String firstName;
    
    // CSV의 lastName 컬럼과 매핑되는 성 필드
    private String lastName;
    
    // CSV의 email 컬럼과 매핑되는 이메일 필드
    private String email;
}