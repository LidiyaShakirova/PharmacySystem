package com.pharmacy.pharmacy_system.Repository;

import com.pharmacy.pharmacy_system.Entity.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {
    Optional<Drug> findByNameIgnoreCase(String name);
    @Query("SELECT d FROM Drug d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Drug> searchByName(@Param("name") String name);

}