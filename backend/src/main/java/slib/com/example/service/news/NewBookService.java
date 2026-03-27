package slib.com.example.service.news;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import slib.com.example.dto.news.NewBookRequest;
import slib.com.example.dto.news.NewBookResponse;
import slib.com.example.entity.news.NewBookEntity;
import slib.com.example.entity.users.User;
import slib.com.example.repository.news.NewBookRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.chat.CloudinaryService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewBookService {

    private final NewBookRepository newBookRepository;
    private final UserRepository userRepository;
    private final NewBookScraperService newBookScraperService;
    private final CloudinaryService cloudinaryService;

    public NewBookRequest previewFromUrl(String url) {
        return newBookScraperService.scrapeFromOpac(url);
    }

    public List<NewBookResponse> getPublicBooks() {
        return newBookRepository.findByIsActiveTrue(latestSort()).stream()
                .map(this::toResponse)
                .toList();
    }

    public NewBookResponse getPublicBookDetail(Integer id) {
        NewBookEntity entity = newBookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách mới"));
        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new RuntimeException("Sách mới này hiện đang bị ẩn");
        }
        return toResponse(entity);
    }

    public List<NewBookResponse> getAllForAdmin() {
        return newBookRepository.findAll(latestSort()).stream()
                .map(this::toResponse)
                .toList();
    }

    public NewBookResponse getAdminDetail(Integer id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public NewBookResponse create(NewBookRequest request, UserDetails userDetails) {
        NewBookEntity entity = new NewBookEntity();
        applyRequest(entity, request);
        entity.setCreatedBy(findCurrentUser(userDetails));

        CoverResolution coverResolution = resolveCoverUrl(request.getCoverUrl(), null);
        entity.setCoverUrl(coverResolution.finalUrl());

        try {
            return toResponse(newBookRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            rollbackUploadedCover(coverResolution.uploadedUrl());
            throw new IllegalArgumentException(
                    "Dữ liệu sách mới không hợp lệ hoặc vượt quá giới hạn lưu trữ. Hãy kiểm tra lại tiêu đề, tác giả, ISBN, thể loại và nhà xuất bản.");
        } catch (RuntimeException e) {
            rollbackUploadedCover(coverResolution.uploadedUrl());
            throw e;
        }
    }

    @Transactional
    public NewBookResponse update(Integer id, NewBookRequest request) {
        NewBookEntity entity = getEntity(id);
        String currentCoverUrl = entity.getCoverUrl();
        applyRequest(entity, request);

        CoverResolution coverResolution = resolveCoverUrl(request.getCoverUrl(), currentCoverUrl);
        entity.setCoverUrl(coverResolution.finalUrl());

        try {
            NewBookResponse response = toResponse(newBookRepository.save(entity));
            deleteReplacedCover(currentCoverUrl, coverResolution.finalUrl());
            return response;
        } catch (DataIntegrityViolationException e) {
            rollbackUploadedCover(coverResolution.uploadedUrl());
            throw new IllegalArgumentException(
                    "Dữ liệu sách mới không hợp lệ hoặc vượt quá giới hạn lưu trữ. Hãy kiểm tra lại tiêu đề, tác giả, ISBN, thể loại và nhà xuất bản.");
        } catch (RuntimeException e) {
            rollbackUploadedCover(coverResolution.uploadedUrl());
            throw e;
        }
    }

    @Transactional
    public NewBookResponse toggleActive(Integer id) {
        NewBookEntity entity = getEntity(id);
        entity.setIsActive(!Boolean.TRUE.equals(entity.getIsActive()));
        return toResponse(newBookRepository.save(entity));
    }

    @Transactional
    public NewBookResponse togglePin(Integer id) {
        NewBookEntity entity = getEntity(id);
        entity.setIsPinned(!Boolean.TRUE.equals(entity.getIsPinned()));
        return toResponse(newBookRepository.save(entity));
    }

    @Transactional
    public void delete(Integer id) {
        NewBookEntity entity = getEntity(id);
        String coverUrl = entity.getCoverUrl();
        newBookRepository.delete(entity);
        deleteReplacedCover(coverUrl, null);
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            newBookRepository.findById(id).ifPresent(entity -> {
                deleteReplacedCover(entity.getCoverUrl(), null);
            });
        }
        newBookRepository.deleteAllById(ids);
    }

    public NewBookResponse toResponse(NewBookEntity entity) {
        return NewBookResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .isbn(entity.getIsbn())
                .coverUrl(entity.getCoverUrl())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .publishYear(entity.getPublishDate() != null ? entity.getPublishDate().getYear() : null)
                .arrivalDate(entity.getArrivalDate())
                .isActive(entity.getIsActive())
                .isPinned(entity.getIsPinned())
                .sourceUrl(entity.getSourceUrl())
                .publisher(entity.getPublisher())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Sort latestSort() {
        return Sort.by(
                Sort.Order.desc("isPinned"),
                Sort.Order.desc("arrivalDate"),
                Sort.Order.desc("createdAt"));
    }

    private NewBookEntity getEntity(Integer id) {
        return newBookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách mới với ID: " + id));
    }

    private void applyRequest(NewBookEntity entity, NewBookRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Tiêu đề sách không được để trống");
        }

        String title = request.getTitle().trim();
        String author = trimToNull(request.getAuthor());
        String isbn = trimToNull(request.getIsbn());
        String description = trimToNull(request.getDescription());
        String category = trimToNull(request.getCategory());
        String sourceUrl = trimToNull(request.getSourceUrl());
        String publisher = trimToNull(request.getPublisher());

        validateMaxLength("Tiêu đề sách", title, 300);
        validateMaxLength("Tác giả", author, 200);
        validateMaxLength("ISBN", isbn, 20);
        validateMaxLength("Thể loại / từ khóa", category, 255);
        validateMaxLength("Nhà xuất bản", publisher, 255);

        entity.setTitle(title);
        entity.setAuthor(author);
        entity.setIsbn(isbn);
        entity.setDescription(description);
        entity.setCategory(category);
        entity.setPublishDate(toPublishDate(request.getPublishYear()));
        entity.setArrivalDate(request.getArrivalDate() != null ? request.getArrivalDate() : LocalDate.now());
        entity.setIsActive(request.getIsActive() == null || request.getIsActive());
        entity.setIsPinned(Boolean.TRUE.equals(request.getIsPinned()));
        entity.setSourceUrl(sourceUrl);
        entity.setPublisher(publisher);
    }

    private CoverResolution resolveCoverUrl(String requestedCoverUrl, String currentCoverUrl) {
        String normalizedUrl = trimToNull(requestedCoverUrl);
        if (normalizedUrl == null) {
            return new CoverResolution(null, null);
        }

        if (normalizedUrl.equals(currentCoverUrl) && isCloudinaryUrl(currentCoverUrl)) {
            return new CoverResolution(currentCoverUrl, null);
        }

        if (isCloudinaryUrl(normalizedUrl)) {
            return new CoverResolution(normalizedUrl, null);
        }

        String uploadedUrl = cloudinaryService.uploadNewBookCoverFromUrl(normalizedUrl);
        return new CoverResolution(uploadedUrl, uploadedUrl);
    }

    private void rollbackUploadedCover(String uploadedUrl) {
        if (uploadedUrl != null && isCloudinaryUrl(uploadedUrl)) {
            cloudinaryService.deleteImageByUrl(uploadedUrl);
        }
    }

    private void deleteReplacedCover(String oldCoverUrl, String newCoverUrl) {
        if (oldCoverUrl == null || oldCoverUrl.equals(newCoverUrl) || !isCloudinaryUrl(oldCoverUrl)) {
            return;
        }
        cloudinaryService.deleteImageByUrl(oldCoverUrl);
    }

    private boolean isCloudinaryUrl(String url) {
        return StringUtils.hasText(url) && url.contains("res.cloudinary.com");
    }

    private LocalDate toPublishDate(Integer publishYear) {
        if (publishYear == null || publishYear < 1000 || publishYear > 3000) {
            return null;
        }
        return LocalDate.of(publishYear, 1, 1);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " không được vượt quá " + maxLength + " ký tự");
        }
    }

    private User findCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }

    private record CoverResolution(String finalUrl, String uploadedUrl) {
    }
}
