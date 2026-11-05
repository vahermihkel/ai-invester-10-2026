package com.aiinvestor.account;

import org.springframework.data.repository.Repository;

public interface ActiveAccountChangeRepository extends Repository<ActiveAccountChange, Long> {

    ActiveAccountChange save(ActiveAccountChange change);
}
