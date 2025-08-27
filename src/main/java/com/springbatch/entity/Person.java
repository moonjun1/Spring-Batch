package com.springbatch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보를 저장하는 JPA 엔티티 클래스
 * 데이터베이스의 PERSON 테이블과 매핑됩니다.
 * 
 * Lombok 어노테이션 설명:
 * @Data - getter, setter, toString, equals, hashCode 자동 생성
 * @NoArgsConstructor - 기본 생성자 자동 생성 (JPA 필수)
 * @AllArgsConstructor - 모든 필드를 매개변수로 받는 생성자 자동 생성
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    
    // 기본키 - 자동으로 증가하는 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 사용자의 이름
    private String firstName;
    
    // 사용자의 성
    private String lastName;
    
    // 사용자의 이메일 주소
    private String email;
    
    /**
     * ID를 제외한 필드들로 Person 객체를 생성하는 생성자
     * (배치 처리 시 새로운 엔티티 생성에 사용)
     * 
     * @param firstName 이름
     * @param lastName 성
     * @param email 이메일 주소
     */
    public Person(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}