package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
