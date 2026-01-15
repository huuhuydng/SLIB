Role: Senior Spring Boot Backend Engineer
Task: Generate Unit Tests for the currently open Controller file following the specific "Strict Testing Guide" below.

Project Context: SLIB - Smart Library Ecosystem.
Framework: Spring Boot 3.x, JUnit 5, Mockito.

---
STRICT TESTING GUIDE (Must Follow):

1. **Test Type**: Unit Tests only using `@WebMvcTest`. DO NOT load the full context (`@SpringBootTest`).
2. **Tools**: 
   - Use `MockMvc` for HTTP requests.
   - Use `@MockitoBean` (Spring Boot 3.4+) or `@MockBean` to mock ALL dependencies (Services).
   - Use `ObjectMapper` for JSON serialization.
3. **Naming Convention**: 
   - Class: `{ControllerName}UnitTest`
   - Method: `methodName_condition_expectedResult` (e.g., `createBooking_validData_returns201`)
4. **Structure**:
   - FLAT structure (no `@Nested`).
   - Group tests by endpoint using comments (e.g., `// === GET ENDPOINT ===`).
5. **Coverage**:
   - Success cases (200/201).
   - Validation errors (400) - Test `@Valid` constraints.
   - Auth errors (401/403) - if applicable.
   - Not Found (404) - Mock service throwing ResourceNotFoundException.
   - Business Logic exceptions (400) - Mock service throwing BadRequestException.
6. **Code Style**:
   - Use `MockMvcRequestBuilders` and `MockMvcResultMatchers` static imports.
   - Separate code into: `// Arrange`, `// Act & Assert`.
   - Do NOT test business logic inside the controller test (that belongs to Service tests).

---

INPUT:
Please analyze the Controller code I currently have open/selected.

OUTPUT:
Generate the full `*ControllerUnitTest.java` code.
Ensure you mock the return objects using `Builder` or constructors matching the SLIB project's DTOs.