package slib.com.example.service.chat;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor 
public class CloudinaryService {

    private final Cloudinary cloudinary; 

    /**
     * TẢI ẢNH: Dành cho News và Chat (Xử lý dưới dạng resource_type: image)
     */
    public String uploadImageNews(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_news", "image");
    }

    public String uploadImageChat(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_chat", "image");
    }

    /**
     * TẢI TÀI LIỆU: Ép kiểu resource_type là 'raw' để Cloudinary không báo lỗi "Is it an image?"
     */
    public String uploadDocument(MultipartFile file) {
        // Đối với PDF/Word, ta dùng resource_type là 'raw' hoặc 'auto'
        return uploadFileToCloudinary(file, "slib_documents", "raw");
    }

    /**
     * HÀM XỬ LÝ CHÍNH: Sử dụng InputStream để tối ưu bộ nhớ
     */
    private String uploadFileToCloudinary(MultipartFile file, String folderName, String resourceType) {
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống!");
        }

        try (InputStream inputStream = file.getInputStream()) {
            
            Map<String, Object> params = new HashMap<>();
            params.put("folder", folderName != null ? folderName : "slib_default");
            params.put("resource_type", resourceType); 
            params.put("use_filename", true);
            params.put("unique_filename", true);

            log.info("Đang tải {} lên Cloudinary (loại: {})...", file.getOriginalFilename(), resourceType);

            Map<?, ?> uploadResult = cloudinary.uploader().upload(inputStream.readAllBytes(), params);

            if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                String url = uploadResult.get("secure_url").toString();
                log.info("==> UPLOAD THÀNH CÔNG [{}]: {}", resourceType, url);
                return url;
            } else {
                throw new RuntimeException("Cloudinary không trả về URL!");
            }

        } catch (IOException e) {
            log.error("LỖI ĐỌC LUỒNG FILE: {}", e.getMessage());
            throw new RuntimeException("Lỗi hệ thống khi xử lý dữ liệu file: " + e.getMessage());
        } catch (Exception e) {
            log.error("== CLOUDINARY UPLOAD FAILED ==: {}", e.getMessage());
            e.printStackTrace(); 
            throw new RuntimeException("Cloudinary từ chối file: " + e.getMessage());
        }
    }
}