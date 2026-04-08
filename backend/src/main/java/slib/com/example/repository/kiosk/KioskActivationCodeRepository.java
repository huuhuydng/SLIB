package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.kiosk.KioskActivationCodeEntity;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface KioskActivationCodeRepository extends JpaRepository<KioskActivationCodeEntity, Integer> {

    Optional<KioskActivationCodeEntity> findByCodeAndUsedFalse(String code);

    boolean existsByCode(String code);

    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime time);

    @Modifying
    @Transactional
    void deleteByKioskIdAndUsedFalse(Integer kioskId);
}
