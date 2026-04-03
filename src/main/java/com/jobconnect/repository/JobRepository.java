package com.jobconnect.repository;

import com.jobconnect.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByEmployerId(Long employerId);

    List<Job> findByStatus(Job.JobStatus status);

    /**
     * Full-text style search across title, description, location, category and jobType.
     * All params are optional — pass null to skip a filter.
     */
    @Query("""
            SELECT j FROM Job j
            WHERE j.status = 'ACTIVE'
              AND (:keyword  IS NULL OR LOWER(j.title)       LIKE LOWER(CONCAT('%', :keyword,  '%'))
                                     OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword,  '%')))
              AND (:location IS NULL OR LOWER(j.location)    LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:category IS NULL OR LOWER(j.category)    = LOWER(:category))
              AND (:jobType  IS NULL OR LOWER(j.jobType)     = LOWER(:jobType))
            ORDER BY j.createdAt DESC
            """)
    List<Job> search(
            @Param("keyword")  String keyword,
            @Param("location") String location,
            @Param("category") String category,
            @Param("jobType")  String jobType
    );
}
