package slib.com.example.service.chat;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Avatar compression settings
    private static final int AVATAR_MAX_SIZE = 400;
    private static final double AVATAR_QUALITY = 0.8;

    /**
     * TAI ANH: Danh cho News va Chat (Xu ly duoi dang resource_type: image)
     */
    public String uploadImageNews(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_news", "image");
    }

    /**
     * TAI AVATAR: Danh cho anh dai dien nguoi dung
     * Tu dong compress truoc khi upload
     */
    public String uploadAvatar(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_avatars", "image");
    }

    /**
     * TAI AVATAR TU BYTES: Dung cho ZIP import server-side
     * Compress roi upload truc tiep, khong can MultipartFile
     */
    public String uploadAvatarBytes(byte[] imageBytes, String fileName) {
        try {
            // Compress avatar
            byte[] compressed = compressAvatarBytes(imageBytes);

            Map<String, Object> params = new HashMap<>();
            params.put("folder", "slib_avatars");
            params.put("resource_type", "image");
            params.put("use_filename", true);
            params.put("unique_filename", true);
            params.put("overwrite", true);

            log.info("[AvatarBytes] Uploading {} ({}KB -> {}KB)", fileName,
                    imageBytes.length / 1024, compressed.length / 1024);

            Map<?, ?> uploadResult = cloudinary.uploader().upload(compressed, params);

            if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                String url = uploadResult.get("secure_url").toString();
                return url;
            } else {
                throw new RuntimeException("Cloudinary không trả về URL!");
            }
        } catch (Exception e) {
            log.error("[AvatarBytes] Upload failed for {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Upload avatar thất bại: " + e.getMessage());
        }
    }

    public String uploadImageChat(MultipartFile file) {
        return uploadFileToCloudinary(file, "slib_chat", "image");
    }

    /**
     * TAI TAI LIEU: Ep kieu resource_type la 'raw' de Cloudinary khong bao loi "Is
     * it an image?"
     */
    public String uploadDocument(MultipartFile file) {
        // Doi voi PDF/Word, ta dung resource_type la 'raw' hoac 'auto'
        return uploadFileToCloudinary(file, "slib_documents", "raw");
    }

    /**
     * XOA AVATARS: Dung cho rollback khi import fail
     * 
     * @param urls List cac URL can xoa
     * @return So anh da xoa thanh cong
     */
    public int deleteAvatars(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (String url : urls) {
            try {
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
     * XOA TAT CA RESOURCES TRONG FOLDER
     * Dung Cloudinary Admin API de xoa toan bo anh trong 1 folder
     * 
     * @param folderName Ten folder can xoa (vi du: "slib_avatars")
     * @return So anh da xoa thanh cong
     */
    public int deleteAllInFolder(String folderName) {
        try {
            int totalDeleted = 0;
            boolean hasMore = true;
            String nextCursor = null;

            // Cloudinary API xoa toi da 100 resources moi lan, can loop
            while (hasMore) {
                Map<String, Object> params = new HashMap<>();
                params.put("type", "upload");
                params.put("prefix", folderName);
                params.put("max_results", 100);
                if (nextCursor != null) {
                    params.put("next_cursor", nextCursor);
                }

                // Lay danh sach resources trong folder
                ApiResponse response = cloudinary.api().resources(params);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");

                if (resources == null || resources.isEmpty()) {
                    break;
                }

                // Lay public_ids de xoa hang loat
                List<String> publicIds = resources.stream()
                        .map(r -> (String) r.get("public_id"))
                        .toList();

                if (!publicIds.isEmpty()) {
                    cloudinary.api().deleteResources(publicIds, ObjectUtils.emptyMap());
                    totalDeleted += publicIds.size();
                    log.info("[Cloudinary] Da xoa {} resources trong folder '{}'", publicIds.size(), folderName);
                }

                // Kiem tra con page tiep theo khong
                nextCursor = (String) response.get("next_cursor");
                hasMore = nextCursor != null;
            }

            log.info("[Cloudinary] Tong cong da xoa {} resources trong folder '{}'", totalDeleted, folderName);
            return totalDeleted;

        } catch (Exception e) {
            log.error("[Cloudinary] Loi xoa folder '{}': {}", folderName, e.getMessage());
            throw new RuntimeException("Loi xoa folder Cloudinary: " + e.getMessage());
        }
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
     * Compress avatar: resize xuong 400x400, JPEG quality 0.8
     * Giam dung luong tu 2-5MB xuong con ~50-100KB
     */
    private byte[] compressAvatar(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Thumbnails.of(inputStream)
                    .size(AVATAR_MAX_SIZE, AVATAR_MAX_SIZE)
                    .outputFormat("jpg")
                    .outputQuality(AVATAR_QUALITY)
                    .toOutputStream(outputStream);

            byte[] compressed = outputStream.toByteArray();
            log.info("[Compress] {} : {}KB -> {}KB (giam {}%)",
                    file.getOriginalFilename(),
                    file.getSize() / 1024,
                    compressed.length / 1024,
                    Math.round((1.0 - (double) compressed.length / file.getSize()) * 100));

            return compressed;
        }
    }

    /**
     * Compress avatar từ byte[] (dùng cho ZIP server-side import)
     */
    private byte[] compressAvatarBytes(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Thumbnails.of(inputStream)
                    .size(AVATAR_MAX_SIZE, AVATAR_MAX_SIZE)
                    .outputFormat("jpg")
                    .outputQuality(AVATAR_QUALITY)
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();
        }
    }

    /**
     * HAM XU LY CHINH: Su dung InputStream de toi uu bo nho
     * Voi avatar: tu dong compress truoc khi upload
     */
    private String uploadFileToCloudinary(MultipartFile file, String folderName, String resourceType) {
        if (file.isEmpty()) {
            throw new RuntimeException("File khong duoc de trong!");
        }

        try {
            // Compress avatar truoc khi upload de tang toc do
            byte[] fileBytes;
            if ("slib_avatars".equals(folderName)) {
                fileBytes = compressAvatar(file);
            } else {
                try (InputStream inputStream = file.getInputStream()) {
                    fileBytes = inputStream.readAllBytes();
                }
            }

            Map<String, Object> params = new HashMap<>();
            params.put("folder", folderName != null ? folderName : "slib_default");
            params.put("resource_type", resourceType);
            params.put("use_filename", true);
            params.put("unique_filename", true);
            params.put("overwrite", true);

            log.info("Dang tai {} len Cloudinary (loai: {}, size: {}KB)...",
                    file.getOriginalFilename(), resourceType, fileBytes.length / 1024);

            Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, params);

            if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                String url = uploadResult.get("secure_url").toString();
                log.info("==> UPLOAD THANH CONG [{}]: {}", resourceType, url);
                return url;
            } else {
                throw new RuntimeException("Cloudinary khong tra ve URL!");
            }

        } catch (IOException e) {
            log.error("LOI DOC LUONG FILE: {}", e.getMessage());
            throw new RuntimeException("Loi he thong khi xu ly du lieu file: " + e.getMessage());
        } catch (Exception e) {
            log.error("== CLOUDINARY UPLOAD FAILED ==: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cloudinary tu choi file: " + e.getMessage());
        }
    }
}