package com.amron.ExpenseTracker.Service;

import com.amron.ExpenseTracker.Model.Categories;
import com.amron.ExpenseTracker.Repository.CategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriesService {
    @Autowired
    private CategoriesRepository categoriesRepository;

    public List<Categories> getAllCategories() {
        return categoriesRepository.findAll();
    }

}
