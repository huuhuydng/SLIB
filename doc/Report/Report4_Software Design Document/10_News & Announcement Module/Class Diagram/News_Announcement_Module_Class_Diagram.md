# News & Announcement Module Class Diagram

```mermaid
classDiagram
    class NewsController {
        +getPublicNews()
        +getPublicNewsByCategory(categoryId)
        +getPublicNewsDetail(id)
        +getAllNewsForAdmin()
        +getNewsDetailForAdmin(id)
        +getNewsImage(id)
        +createNews(request)
        +updateNews(id, request)
        +deleteNews(id)
        +deleteBatch(ids)
        +togglePin(id)
    }

    class NewsService {
        +getPublicNews()
        +getPublicNewsByCategory(categoryId)
        +getNewsDetailAndIncrementView(newsId)
        +getAllNewsForAdmin()
        +getNewsDetailForAdmin(id)
        +getNewsImage(id)
        +createNews(request)
        +updateNews(id, request)
        +deleteNews(id)
        +deleteBatch(ids)
        +togglePin(id)
    }

    class CategoryController {
        +getAllCategories()
        +createCategory(request)
        +deleteCategory(id)
    }

    class CategoryService {
        +getAllCategories()
        +createCategory(name, colorCode)
        +deleteCategory(id)
        +getCategoryById(id)
    }

    class NewBookController {
        +getPublicBooks()
        +getPublicBookDetail(id)
        +getAllForAdmin()
        +getAdminDetail(id)
        +preview(request)
        +create(request)
        +update(id, request)
        +toggleActive(id)
        +togglePin(id)
        +delete(id)
        +deleteBatch(ids)
    }

    class NewBookService {
        +previewFromUrl(url)
        +getPublicBooks()
        +getPublicBookDetail(id)
        +getAllForAdmin()
        +getAdminDetail(id)
        +create(request, userDetails)
        +update(id, request)
        +toggleActive(id)
        +togglePin(id)
        +delete(id)
        +deleteBatch(ids)
    }

    class NewsScheduler {
        +scheduleNewsPublication(newsId, publishedAt)
        +cancelScheduledPublication(newsId)
        +publishNews(newsId)
    }

    class FirebaseMessagingService {
        +sendNewsNotification(title, body, data)
    }

    class NewsRepository {
        +findByDeletedFalseAndIsPublishedTrueOrderByIsPinnedDescPublishedAtDesc()
        +findByIdAndDeletedFalse(id)
        +save(news)
        +delete(news)
    }

    class NewsCategoryRepository {
        +findAllByOrderByNameAsc()
        +findById(id)
        +save(category)
        +delete(category)
    }

    class NewBookRepository {
        +findByDeletedFalseOrderByIsPinnedDescCreatedAtDesc()
        +findByIdAndDeletedFalse(id)
        +save(book)
        +delete(book)
    }

    class News {
        +UUID id
        +String title
        +String summary
        +String content
        +String imageUrl
        +Boolean isPublished
        +Boolean isPinned
        +Long viewCount
        +LocalDateTime publishedAt
    }

    class NewsCategory {
        +UUID id
        +String name
        +String colorCode
    }

    class NewBook {
        +UUID id
        +String title
        +String author
        +String isbn
        +String publisher
        +String category
        +String description
        +String coverUrl
        +String sourceUrl
        +Integer publishYear
        +LocalDate arrivalDate
        +Boolean isActive
        +Boolean isPinned
    }

    class NewsServiceWeb {
        +getPublicNews()
        +getAllNewsForAdmin()
        +getNewsDetailForAdmin(id)
        +createNews(payload)
        +updateNews(id, payload)
        +deleteNews(id)
        +getAllCategories()
        +createCategory(payload)
        +deleteCategory(id)
    }

    class NewBookServiceWeb {
        +getAllNewBooks()
        +getNewBookDetail(id)
        +previewNewBookFromUrl(url)
        +createNewBook(payload)
        +updateNewBook(id, payload)
        +toggleActive(id)
        +togglePin(id)
        +deleteNewBook(id)
    }

    class NewsServiceMobile {
        +fetchPublicNews()
        +fetchNewsDetail(id)
    }

    class NewBookServiceMobile {
        +fetchPublicNewBooks()
        +fetchNewBookDetail(id)
    }

    class NotificationManagePage {
        +fetchAllNews()
        +handleDelete(newsId)
        +handleBatchDelete(ids)
        +handleTogglePin(newsId)
        +openDetail(newsId)
    }

    class NewCreatePage {
        +loadCategories()
        +loadDraft()
        +saveDraft()
        +handleSubmit()
        +openPreview()
    }

    class NewBookManagePage {
        +fetchBooks()
        +handleToggleActive(bookId)
        +handleTogglePin(bookId)
        +handleDelete(bookId)
    }

    class NewBookCreatePage {
        +previewFromSourceUrl()
        +uploadCover()
        +handleSubmit()
    }

    class NewsScreen
    class NewsDetailScreen
    class NewBooksScreen
    class NewBookDetailScreen

    NewsController --> NewsService
    CategoryController --> CategoryService
    NewBookController --> NewBookService

    NewsService --> NewsRepository
    NewsService --> NewsCategoryRepository
    NewsService --> NewsScheduler
    NewsService --> FirebaseMessagingService
    CategoryService --> NewsCategoryRepository
    NewBookService --> NewBookRepository

    NewsRepository --> News
    NewsCategoryRepository --> NewsCategory
    NewBookRepository --> NewBook
    News --> NewsCategory

    NewsServiceWeb --> NewsController
    NewsServiceWeb --> CategoryController
    NewBookServiceWeb --> NewBookController
    NewsServiceMobile --> NewsController
    NewBookServiceMobile --> NewBookController

    NotificationManagePage --> NewsServiceWeb
    NewCreatePage --> NewsServiceWeb
    NewBookManagePage --> NewBookServiceWeb
    NewBookCreatePage --> NewBookServiceWeb

    NewsScreen --> NewsServiceMobile
    NewsDetailScreen --> NewsServiceMobile
    NewBooksScreen --> NewBookServiceMobile
    NewBookDetailScreen --> NewBookServiceMobile
```
