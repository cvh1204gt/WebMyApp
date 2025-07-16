package com.bmt.MyApp.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_pack_member")
public class ServicePackMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_pack_id")
    private ServicePack servicePack;

    // Constructors
    public ServicePackMember() {}
    public ServicePackMember(AppUser user, ServicePack servicePack) {
        this.user = user;
        this.servicePack = servicePack;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public ServicePack getServicePack() { return servicePack; }
    public void setServicePack(ServicePack servicePack) { this.servicePack = servicePack; }
} 