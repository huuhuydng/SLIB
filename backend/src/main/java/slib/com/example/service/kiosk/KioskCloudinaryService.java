package slib.com.example.service.kiosk;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KioskCloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload ảnh slideshow lên Cloudinary
     * Folder: slib_slideshow (Tự động tạo nếu chưa có)
     */
    public Map<String, Object> uploadSlideShowImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // DEBUG: Kiểm tra xem code đang dùng tài khoản nào
        log.info("🚀 Đang upload lên Cloudinary Account: [{}]", cloudinary.config.cloudName);

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "slib_slideshow",
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            // Log URL để kiểm tra xem ảnh đang được up lên cloud nào
            log.info("✅ Upload slideshow thành công. URL: {}", uploadResult.get("secure_url"));

            // Trả về map chứa thông tin cần thiết để lưu vào DB
            return Map.of(
                    "public_id", uploadResult.get("public_id"),
                    "url", uploadResult.get("secure_url"),
                    "width", uploadResult.get("width"),
                    "height", uploadResult.get("height"),
                    "format", uploadResult.get("format"),
                    "original_filename", uploadResult.get("original_filename")
            );
        } catch (Exception e) {
            log.error("❌ Lỗi upload slideshow: {}", e.getMessage());
            throw new RuntimeException("Không thể upload ảnh: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả ảnh slideshow từ Cloudinary folder "slib_slideshow"
     */
    public List<String> getSlideShowImages() {
        try {
            Map searchResult = cloudinary.search()
                    .expression("folder:\"slib_slideshow\"")
                    .maxResults(100)
                    .execute();

            List<String> imageUrls = new ArrayList<>();
            List<Map> resources = (List<Map>) searchResult.get("resources");
            
            if (resources != null) {
                for (Map resource : resources) {
                    imageUrls.add((String) resource.get("secure_url"));
                }
            }

            log.info("✅ Lấy {} ảnh từ Cloudinary slideshow", imageUrls.size());
            return imageUrls;
        } catch (Exception e) {
            log.error("❌ Lỗi lấy slideshow ảnh: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy ảnh: " + e.getMessage());
        }
    }

    /**
     * Xóa ảnh slideshow từ Cloudinary dựa trên identifier (URL hoặc public_id)
     */
    public Map<String, Object> deleteSlideShowImage(String identifier) {
        try {
            String publicId = extractPublicIdFromUrl(identifier);
            Map deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("✅ Xóa slideshow ảnh thành công: {}", publicId);
            return deleteResult;
        } catch (Exception e) {
            log.error("❌ Lỗi xóa slideshow ảnh: {}", e.getMessage());
            throw new RuntimeException("Không thể xóa ảnh: " + e.getMessage());
        }
    }

    /**
     * Đổi tên ảnh slideshow trên Cloudinary
     */
    public String renameSlideShowImage(String currentUrl, String newName) {
        try {
            String publicId = extractPublicIdFromUrl(currentUrl);
            
            // Giữ nguyên folder path, chỉ thay đổi tên file
            String folder = "";
            if (publicId.contains("/")) {
                folder = publicId.substring(0, publicId.lastIndexOf("/") + 1);
            }
            
            // Làm sạch tên mới (bỏ path hoặc extension nếu người dùng lỡ nhập)
            String cleanName = newName;
            if (cleanName.contains("/")) cleanName = cleanName.substring(cleanName.lastIndexOf("/") + 1);
            if (cleanName.contains(".")) cleanName = cleanName.substring(0, cleanName.lastIndexOf("."));

            String newPublicId = folder + cleanName;
            
            Map result = cloudinary.uploader().rename(publicId, newPublicId, ObjectUtils.emptyMap());
            log.info("✅ Đổi tên ảnh thành công: {} -> {}", publicId, newPublicId);
            return result.get("secure_url").toString();
        } catch (Exception e) {
            log.error("❌ Lỗi đổi tên ảnh: {}", e.getMessage());
            throw new RuntimeException("Không thể đổi tên ảnh: " + e.getMessage());
        }
    }

    /**
     * Helper: Trích xuất public_id từ URL Cloudinary
     * VD: https://res.cloudinary.com/.../upload/v12345/slib/slideshow/image.jpg -> slib/slideshow/image
     */
    private String extractPublicIdFromUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            return url; // Giả sử input đã là public_id nếu không phải URL
        }
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return url;

            String path = url.substring(uploadIndex + 8); // Bỏ qua "/upload/"

            // Bỏ version (v12345/) nếu có
            if (path.matches("^v\\d+/.*")) {
                path = path.substring(path.indexOf("/") + 1);
            }

            // Bỏ đuôi file mở rộng (.jpg, .png)
            int lastDotIndex = path.lastIndexOf(".");
            if (lastDotIndex != -1) {
                path = path.substring(0, lastDotIndex);
            }

            return URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.warn("Không thể trích xuất public_id từ URL: {}", url);
            return url; // Fallback về giá trị gốc
        }
    }
}