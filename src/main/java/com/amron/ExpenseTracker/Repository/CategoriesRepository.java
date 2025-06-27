package com.amron.ExpenseTracker.Repository;

import com.amron.ExpenseTracker.Model.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriesRepository extends JpaRepository<Categories, Integer> {
}
