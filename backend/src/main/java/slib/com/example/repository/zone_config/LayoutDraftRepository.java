package slib.com.example.repository.zone_config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.zone_config.LayoutDraftEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LayoutDraftRepository extends JpaRepository<LayoutDraftEntity, Long> {
    Optional<LayoutDraftEntity> findFirstByUpdatedByUserId(UUID updatedByUserId);

    void deleteByUpdatedByUserId(UUID updatedByUserId);
}
