package com.pharmacy.pharmacy_system.Repository;

import com.pharmacy.pharmacy_system.Entity.DrugCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DrugCategoryRepository extends JpaRepository<DrugCategory, Long> {

    List<DrugCategory> findAllByOrderByPriorityAsc();
    List<DrugCategory> findByPriority(Integer priority);


}