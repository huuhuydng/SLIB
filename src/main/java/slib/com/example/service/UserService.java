package slib.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import javax.swing.text.html.Option;

import slib.com.example.entity.UserEntity;
import slib.com.example.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    //CREATE
    public boolean createUser(UserEntity user) {
        try {
            // Kiểm tra trùng lặp
            if (userRepository.existsByEmail(user.getEmail()) ||
                    userRepository.existsByStudentCode(user.getStudentCode())) {
                return false;
            }
            // QUAN TRỌNG: Phải có lệnh này mới lưu vào Supabase
            user.setReputationScore(100);
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update đơn giản: Nếu có ID thì nó tự update, chưa có thì nó tạo mới
//    public boolean updateUser(UserEntity user) {
//        try {
//            if (user.getUserId() == null) return false; // Không có ID thì không update được
//            userRepository.save(user);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    //UPDATE
    public boolean updateUser(UserEntity user) {
        try {
            if (user.getUserId() == null) {
                return false;
            }

            // Kiểm tra xem user có tồn tại trong DB không
            if (!userRepository.existsById(user.getUserId())) {
                // Nếu không tồn tại thì không update
                return false;
            }

            // Nếu tồn tại thì mới save (update)
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //DELETE
    public boolean deleteUser(UserEntity user) {
        try {
            userRepository.delete(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public UserEntity getUserByStudentCode(String studentCode) {
        return userRepository.findByStudentCode(studentCode);
    }

    public boolean checkUserAuth(String email, String password) {
        Optional<UserEntity> User = Optional.ofNullable(userRepository.findByEmail(email));
        if (User.isPresent() && User.get().getPassword().equals(password)) {
            return true;
        } else {
            return false;
        }
    }



}