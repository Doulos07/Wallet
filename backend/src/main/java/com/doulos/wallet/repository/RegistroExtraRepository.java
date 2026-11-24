package com.doulos.wallet.repository;

import com.doulos.wallet.entity.RegistroExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroExtraRepository extends JpaRepository<RegistroExtra, Long> {

    List<RegistroExtra> findByPagoId(Long pagoId);
}
