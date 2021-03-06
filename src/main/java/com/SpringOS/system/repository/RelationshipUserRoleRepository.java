package com.SpringOS.system.repository;

import com.SpringOS.system.entity.RelationshipUserRole;
import com.SpringOS.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by AlbertXmas on 16/8/8.
 */
public interface RelationshipUserRoleRepository extends JpaRepository<RelationshipUserRole, Long>, CommonRepository<RelationshipUserRole, RelationshipUserRoleRepository> {

}
