package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import  slib.com.example.entity.UserEntity;
import org.springframework.stereotype.Repository;
import java.util.UUID; 
 
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    public UserEntity findByEmail(String email);
    public UserEntity findByStudentCode(String studentCode);
    public UserEntity findByNotiDevice(String notiDevice);
    public Boolean existsByEmail(String email);
    public Boolean existsByStudentCode(String studentCode);

    
}
