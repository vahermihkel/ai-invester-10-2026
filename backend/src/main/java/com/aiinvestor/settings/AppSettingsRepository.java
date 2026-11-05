package com.aiinvestor.settings;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AppSettingsRepository extends Repository<AppSettings, Short> {

    Optional<AppSettings> findById(Short id);

    /** Locks the row until the transaction ends, so two changes can't overwrite each other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppSettings s where s.id = :id")
    Optional<AppSettings> findForUpdate(@Param("id") Short id);
}
