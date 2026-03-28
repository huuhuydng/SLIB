package slib.com.example.service.chat;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
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

    /**
     * TẢI AVATAR: Dành cho ảnh đại diện người dùng
     */
    public String uploadAvatar(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_avatars", "image");
    }

    public String uploadImageChat(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_chat", "image");
    }

    public String uploadNewBookCoverFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new RuntimeException("URL ảnh bìa không được để trống");
        }

        try {
            URLConnection connection = URI.create(imageUrl.trim()).toURL().openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new RuntimeException("URL cung cấp không phải là ảnh hợp lệ");
            }

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                if (bytes.length == 0) {
                    throw new RuntimeException("Không thể tải dữ liệu ảnh từ URL đã cung cấp");
                }
                return uploadBytesToCloudinary(bytes, "slib_new_books", "image", imageUrl);
            }
        } catch (IOException e) {
            log.error("LOI DOC ANH TU URL {}: {}", imageUrl, e.getMessage());
            throw new RuntimeException("Không thể tải ảnh bìa từ nguồn ngoài: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("URL ảnh bìa không hợp lệ");
        } catch (Exception e) {
            log.error("LOI TAI ANH BIA TU URL {}: {}", imageUrl, e.getMessage());
            throw new RuntimeException("Không thể đồng bộ ảnh bìa lên Cloudinary: " + e.getMessage());
        }
    }

    /**
     * TẢI TÀI LIỆU: Ép kiểu resource_type là 'raw' để Cloudinary không báo lỗi "Is
     * it an image?"
     */
    public String uploadDocument(MultipartFile file) {
        // Đối với PDF/Word, ta dùng resource_type là 'raw' hoặc 'auto'
        return uploadFileToCloudinary(file, "slib_documents", "raw");
    }

    /**
     * XOA 1 ANH: Xoa anh tren Cloudinary theo URL
     * Dung chung cho avatar, news, chat images
     *
     * @param url URL cua anh can xoa
     * @return true neu xoa thanh cong
     */
    public boolean deleteImageByUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            String publicId = extractPublicIdFromUrl(url);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("[Cloudinary] Da xoa anh: {}", publicId);
                return true;
            }
        } catch (Exception e) {
            log.warn("[Cloudinary] Loi xoa anh {}: {}", url, e.getMessage());
        }
        return false;
    }

    /**
     * XOA AVATARS: Dung cho rollback khi import fail
     * 
     * @param urls List cac URL can xoa
     * @return So anh da xoa thanh cong
     */
    public int deleteAvatars(java.util.List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (String url : urls) {
            try {
                // Extract public_id from URL
                // Example:
                // https://res.cloudinary.com/xxx/image/upload/v123/slib_avatars/file_abc.jpg
                // -> public_id = slib_avatars/file_abc
                String publicId = extractPublicIdFromUrl(url);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    deletedCount++;
                    log.info("Da xoa avatar: {}", publicId);
                }
            } catch (Exception e) {
                log.error("Loi xoa avatar {}: {}", url, e.getMessage());
            }
        }
        return deletedCount;
    }

    /**
     * Extract public_id from Cloudinary URL
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            // URL format:
            // https://res.cloudinary.com/{cloud}/image/upload/v{version}/{public_id}.{ext}
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String afterUpload = parts[1];
                // Remove version prefix (v123456/)
                if (afterUpload.startsWith("v")) {
                    int slashIndex = afterUpload.indexOf('/');
                    if (slashIndex > 0) {
                        afterUpload = afterUpload.substring(slashIndex + 1);
                    }
                }
                // Remove extension
                int dotIndex = afterUpload.lastIndexOf('.');
                if (dotIndex > 0) {
                    return afterUpload.substring(0, dotIndex);
                }
                return afterUpload;
            }
        } catch (Exception e) {
            log.error("Loi parse URL: {}", url);
        }
        return null;
    }

    /**
     * HAM XU LY CHINH: Su dung InputStream de toi uu bo nho
     */
    private String uploadFileToCloudinary(MultipartFile file, String folderName, String resourceType) {
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống!");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return uploadBytesToCloudinary(
                    inputStream.readAllBytes(),
                    folderName,
                    resourceType,
                    file.getOriginalFilename());

        } catch (IOException e) {
            log.error("LOI DOC LUONG FILE: {}", e.getMessage());
            throw new RuntimeException("Loi he thong khi xu ly du lieu file: " + e.getMessage());
        }
    }

    private String uploadBytesToCloudinary(byte[] content, String folderName, String resourceType, String sourceLabel) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("folder", folderName != null ? folderName : "slib_default");
            params.put("resource_type", resourceType);
            params.put("use_filename", true);
            params.put("unique_filename", true);

            log.info("Dang tai {} len Cloudinary (loai: {})...", sourceLabel, resourceType);

            Map<?, ?> uploadResult = cloudinary.uploader().upload(content, params);

            if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                String url = uploadResult.get("secure_url").toString();
                log.info("==> UPLOAD THANH CONG [{}]: {}", resourceType, url);
                return url;
            }

            throw new RuntimeException("Cloudinary khong tra ve URL!");
        } catch (Exception e) {
            log.error("== CLOUDINARY UPLOAD FAILED ==: {}", e.getMessage());
            throw new RuntimeException("Cloudinary tu choi file: " + e.getMessage());
        }
    }
}
