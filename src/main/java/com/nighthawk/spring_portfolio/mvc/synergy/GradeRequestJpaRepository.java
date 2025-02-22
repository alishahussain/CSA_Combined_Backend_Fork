package com.nighthawk.spring_portfolio.mvc.synergy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRequestJpaRepository extends JpaRepository<GradeRequest, Long> {

}
