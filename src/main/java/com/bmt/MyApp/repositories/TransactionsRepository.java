package com.bmt.MyApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bmt.MyApp.models.Transactions;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
  
}
