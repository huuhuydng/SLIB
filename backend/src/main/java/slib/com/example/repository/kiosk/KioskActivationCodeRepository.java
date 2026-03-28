package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.kiosk.KioskActivationCodeEntity;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface KioskActivationCodeRepository extends JpaRepository<KioskActivationCodeEntity, Integer> {

    Optional<KioskActivationCodeEntity> findByCodeAndUsedFalse(String code);

    boolean existsByCode(String code);

    void deleteByExpiresAtBefore(LocalDateTime time);
}
