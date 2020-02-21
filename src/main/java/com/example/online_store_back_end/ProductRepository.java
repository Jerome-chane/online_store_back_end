package com.example.online_store_back_end;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByName(@Param("name") String name);
}
