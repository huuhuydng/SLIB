package slib.com.example.repository.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.library.LibrarySetting;

@Repository
public interface LibrarySettingRepository extends JpaRepository<LibrarySetting, Integer> {
    // Singleton pattern - always use id = 1
}
