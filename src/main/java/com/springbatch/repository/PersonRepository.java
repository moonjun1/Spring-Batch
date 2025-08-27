package com.springbatch.repository;

import com.springbatch.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Person 엔티티에 대한 데이터베이스 접근을 담당하는 JPA 리포지토리
 * 
 * JpaRepository를 상속받아 기본적인 CRUD 기능을 자동으로 제공받습니다:
 * - save(): 엔티티 저장
 * - findAll(): 모든 엔티티 조회
 * - findById(): ID로 엔티티 조회
 * - delete(): 엔티티 삭제
 * 
 * 배치 처리에서는 주로 save() 메서드가 사용됩니다.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    // JpaRepository가 제공하는 기본 메서드들로 충분하므로
    // 추가 메서드 정의가 필요 없습니다.
}