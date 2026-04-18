# FE-118b Create Kiosk Image

```mermaid
sequenceDiagram
    actor Users as "Librarian"
    participant Client as Librairan Web Portal
    participant SlidePage as SlideshowManagement.jsx
    participant SlideController as KioskSlideshowController
    participant CloudinaryService as KioskCloudinaryService
    participant ImageRepo as KioskImageRepository
    participant DB as Database

    activate Users
    Users->>Client: 1. Choose to upload new kiosk images
    activate Client
    
    Client->>Client: 2. Open upload modal and select image files
    Users->>Client: 3. Confirm image upload
    
    # Giai đoạn: Gửi yêu cầu Upload
    Client->>SlidePage: 4. Send upload request with image files
    activate SlidePage
    SlidePage->>SlideController: 5. POST /api/slideshow/images
    activate SlideController
    
    # Bước 6 & 7: Xử lý lưu trữ trên Cloud
    SlideController->>CloudinaryService: 6. Upload each image file
    activate CloudinaryService
    CloudinaryService-->>SlideController: 7. Return hosted image URL and publicId
    deactivate CloudinaryService

    # Bước 8: Lưu thông tin ảnh vào Database
    SlideController->>ImageRepo: 8. Save slideshow image metadata
    activate ImageRepo
    ImageRepo->>DB: 8.1 Insert into kiosk_image table
    activate DB
    DB-->>ImageRepo: 8.2 Persist success
    deactivate DB
    ImageRepo-->>SlideController: 8.3 Return created image rows
    deactivate ImageRepo
    
    # Phản hồi kết quả về UI
    SlideController-->>SlidePage: 9. Return 200 OK with created images
    deactivate SlideController
    
    SlidePage-->>Client: 10. Return created image payloads
    deactivate SlidePage
    
    # Giai đoạn: Cập nhật giao diện Client
    activate Client
    Client->>Client: 11. Append uploaded images to the slideshow table
    
    Client-->>Users: 12. Show kiosk images uploaded successfully
    
    deactivate Client
    deactivate Users
```

