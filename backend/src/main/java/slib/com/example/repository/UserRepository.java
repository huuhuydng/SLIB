package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.users.User;

import java.util.Optional;
import java.util.UUID; 
 
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    public Optional<User> findByEmail(String email);
    public Optional<User> findByStudentCode(String studentCode);
    public Optional<User> findByNotiDevice(String notiDevice);
    public Boolean existsByEmail(String email);
    public Boolean existsByStudentCode(String studentCode);

    
}
