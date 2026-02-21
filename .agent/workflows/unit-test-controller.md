---
description: Bắt buộc viết/cập nhật unit test khi chỉnh sửa controller
---

# Unit Test Rule cho Controllers

## Quy tắc bắt buộc

Mỗi khi **tạo mới hoặc chỉnh sửa** bất kỳ Controller nào trong backend, **BẮT BUỘC** phải:

1. **Controller mới**: Tạo file unit test tương ứng `{ControllerName}UnitTest.java`
2. **Thêm endpoint mới**: Thêm test case cho endpoint đó
3. **Sửa logic endpoint**: Cập nhật test case phản ánh logic mới

## Vị trí file test

```
backend/src/test/java/slib/com/example/controller/{ControllerName}UnitTest.java
```

## Pattern test chuẩn

```java
@WebMvcTest(value = XxxController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("XxxController Unit Tests")
class XxxControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XxxService xxxService;

    // Test cho mỗi endpoint
}
```

## Yêu cầu test cases

Mỗi endpoint cần tối thiểu:
- 1 test **happy path** (thành công)
- 1 test **error case** (lỗi/validation)

Endpoint có authentication (`@AuthenticationPrincipal UserDetails`) dùng `@WithMockUser`.

## Chạy test

```bash
cd backend && mvn test -Dtest="slib.com.example.controller.XxxControllerUnitTest"
```

Tất cả tests **PHẢI PASS** trước khi commit.
