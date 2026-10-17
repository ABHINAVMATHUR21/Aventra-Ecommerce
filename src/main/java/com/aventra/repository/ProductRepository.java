package com.aventra.repository;
import com.aventra.entity.Product;
import java.util.List;
import com.aventra.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByCategoryIgnoreCase(String category);
}