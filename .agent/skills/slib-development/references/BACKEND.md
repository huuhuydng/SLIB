# Backend Development Reference

Chi tiết về phát triển backend Spring Boot cho SLIB.

## Controller Pattern

```java
@RestController
@RequestMapping("/slib/resource")
@RequiredArgsConstructor
public class ResourceController {
    
    private final ResourceService resourceService;
    
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAll() {
        return ResponseEntity.ok(resourceService.findAll());
    }
    
    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(resourceService.create(request));
    }
}
```

## Service Pattern

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {
    
    private final ResourceRepository repository;
    
    public ResourceResponse findById(Long id) {
        return repository.findById(id)
            .map(ResourceResponse::fromEntity)
            .orElseThrow(() -> 
                new ResourceNotFoundException("Not found: " + id));
    }
    
    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource entity = request.toEntity();
        return ResourceResponse.fromEntity(repository.save(entity));
    }
}
```

## Entity Pattern

```java
@Entity
@Table(name = "resources")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Resource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ParentEntity parent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

## Repository Pattern

```java
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
    List<Resource> findByStatus(Status status);
    
    @Query("SELECT r FROM Resource r WHERE r.name LIKE %:keyword%")
    Page<Resource> search(@Param("keyword") String keyword, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Resource r SET r.status = :status WHERE r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") Status status);
}
```

## DTO Pattern

```java
// Request
@Data
public class ResourceRequest {
    @NotBlank private String name;
    @NotNull private Status status;
    
    public Resource toEntity() {
        return Resource.builder().name(name).status(status).build();
    }
}

// Response
@Data @Builder
public class ResourceResponse {
    private Long id;
    private String name;
    
    public static ResourceResponse fromEntity(Resource e) {
        return ResourceResponse.builder()
            .id(e.getId())
            .name(e.getName())
            .build();
    }
}
```

## Flyway Migration

File: `src/main/resources/db/migration/V{n}__description.sql`

```sql
CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    parent_id BIGINT REFERENCES parents(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resources_status ON resources(status);
```

## Unit Test

```java
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {
    
    @Mock ResourceRepository repository;
    @InjectMocks ResourceService service;
    
    @Test
    void findById_exists_returnsResponse() {
        Resource entity = Resource.builder().id(1L).name("Test").build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        
        ResourceResponse result = service.findById(1L);
        
        assertEquals(1L, result.getId());
    }
    
    @Test
    void findById_notExists_throwsException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> service.findById(1L));
    }
}
```
