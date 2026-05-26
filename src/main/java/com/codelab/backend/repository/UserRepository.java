package com.codelab.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codelab.backend.entity.User;

public interface UserRepository extends JpaRepository<User , Long>{

    
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);


    
}



/*
    *** why do we need this repository? Can't we just use the built-in methods from JpaRepository?

    repository: q important hai kyunki iske through hi hum database se interact karte hain. JpaRepository already provides basic CRUD operations, but agar humein specific queries chahiye, jaise ki findByEmail, toh humein apne custom methods define karne padte hain. Agar hum JpaRepository ke built-in methods hi use karte, toh humein findById, save, deleteById, etc. toh mil jaate, lekin findByEmail jaise specific queries ke liye humein apne custom methods define karne padte hain.

    



*/