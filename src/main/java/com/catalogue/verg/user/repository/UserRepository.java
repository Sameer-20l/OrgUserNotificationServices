package com.catalogue.verg.user.repository;

import com.catalogue.verg.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

}