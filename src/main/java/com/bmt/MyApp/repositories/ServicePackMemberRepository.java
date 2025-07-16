package com.bmt.MyApp.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.ServicePack;
import com.bmt.MyApp.models.ServicePackMember;

public interface ServicePackMemberRepository extends JpaRepository<ServicePackMember, Long> {
    List<ServicePackMember> findByServicePack(ServicePack servicePack);
    boolean existsByUserAndServicePack(AppUser user, ServicePack servicePack);
    List<ServicePackMember> findByUser(AppUser user);
    Page<ServicePackMember> findByServicePack(ServicePack servicePack, Pageable pageable);

    @Query("SELECT m FROM ServicePackMember m WHERE m.servicePack.id = :servicePackId AND (LOWER(m.user.fullName) LIKE LOWER(:keyword) OR LOWER(m.user.email) LIKE LOWER(:keyword) OR LOWER(m.user.phone) LIKE LOWER(:keyword))")
    Page<ServicePackMember> findByServicePackAndKeyword(@Param("servicePackId") Long servicePackId, @Param("keyword") String keyword, Pageable pageable);
} 