package com.aiinvestor.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

public interface BrokerAccountRepository extends Repository<BrokerAccount, String> {

    List<BrokerAccount> findAllByOrderById();

    Optional<BrokerAccount> findById(String id);

    BrokerAccount save(BrokerAccount account);
}
