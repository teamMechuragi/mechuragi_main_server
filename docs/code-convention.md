# Mechuragi Server 코드 컨벤션

## 1. 패키지 구조

### 엔티티별 패키지 구성
각 도메인(엔티티)별로 독립적인 패키지를 구성하며, 다음과 같은 하위 패키지를 포함합니다.

### 패키지별 역할
- **config**: 해당 도메인의 설정 클래스
- **entity**: JPA 엔티티 클래스
- **dto**: 데이터 전송 객체 (Request/Response)
- **repository**: JPA Repository 인터페이스
- **service**: 비즈니스 로직 인터페이스 및 구현체
  - **impl**: Service 구현체 (`~ServiceImpl`)
  - **mapper**: DTO ↔ Entity 변환 클래스 (`엔티티명+Request/ResponseMapper`)
- **controller**: REST API 컨트롤러

## 2. Service 계층 규칙

### Interface 기반 설계
모든 Service는 반드시 Interface를 먼저 정의하고 구현체가 위임받아 구현합니다.

### Service 구현체 위치
- Service 구현체는 `service/impl` 패키지에 위치
- 클래스명: `~ServiceImpl`

## 3. DTO ↔ Entity 변환 (Mapper)

### Mapper 클래스 규칙
- 위치: `service/mapper` 패키지
- 클래스명: `엔티티명 + Request/ResponseMapper` (예: `MemberRequestMapper`, `MemberResponseMapper`)
- 역할: DTO와 Entity 간의 변환 로직 담당
- `@Component`로 등록하여 DI 가능
- 객체 생성 시 `@Builder` 사용


## 4. 메소드 명명 규칙

### CRUD 메소드 접두사 통일
| 작업 | 접두사 | 예시 |
|------|--------|------|
| 조회 | `get` | `getMember()`, `getMembers()`, `getMemberByEmail()` |
| 저장 | `save` | `saveMember()`, `savePost()` |
| 수정 | `update` | `updateMember()`, `updatePassword()` |
| 삭제 | `delete` | `deleteMember()`, `deletePost()` |


## 5. REST API URL 규칙

### 복수형/단수형 규칙
- **목록 조회**: 복수형 사용 (예: `/api/members`)
- **단건 조회/수정/삭제**: 복수형 + `/{id}` (예: `/api/members/{id}`)

## 6. 코드 포맷팅

### 공백 라인 규칙
- **공백 라인은 최대 1줄로 통일**
- 메소드 간: 공백 1줄
- 필드 선언과 메소드 간: 공백 1줄
- import 구문과 클래스 선언 간: 공백 1줄

## 7. DTO 네이밍 규칙

### DTO 클래스명 규칙
- 클래스명: `엔티티명 + Request/Response` (예: `MemberRequest`, `MemberResponse`)
- **Dto 접미사는 사용하지 않음**
- DTO 어노테이션: `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- **Setter는 사용하지 않음** (불변 객체 지향)

## 8. 예외 처리

### 커스텀 예외 사용
- `EntityNotFoundException`: 엔티티를 찾을 수 없는 경우
- `DuplicateEntityException`: 중복된 데이터가 있는 경우
- `UnauthorizedException`: 권한이 없는 경우

## 9. 어노테이션 순서

### 클래스 레벨
1. `@RestController` / `@Service` / `@Repository` / `@Component`
2. `@RequestMapping` (Controller의 경우)
3. `@RequiredArgsConstructor`
4. `@Slf4j`

### 메소드 레벨
1. HTTP 매핑 어노테이션 (`@GetMapping`, `@PostMapping` 등)
2. `@ResponseStatus`

## 10. 추가 규칙

### Lombok 활용
- **Entity**: `@Entity`, `@Getter`, `@NoArgsConstructor`, `@Table(name = "...")`, `@EntityListeners(AuditingEntityListener.class)`, `@Builder`
- **DTO**: `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` (Setter 사용 안 함)
- **Service**: `@RequiredArgsConstructor` (final 필드 DI)
- **Mapper**: `@Component`
- **로깅**: `@Slf4j`

### Entity 작성 규칙
- 기본키: `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Column(name = "엔티티명_id")`
- 생성일시: `@CreatedDate`, `@Column(updatable = false)`
- 수정일시: `@LastModifiedDate`
- 생성자에 `@Builder` 사용
- **엔티티 수정 메소드는 엔티티 내부에 정의** (예: `updateName()`, `updatePassword()`)

### Validation
- Request DTO에는 적절한 검증 어노테이션 사용
- `@NotBlank`, `@Email`, `@Size`, `@Pattern` 등 활용

### Repository 명명
- 인터페이스명: `엔티티명 + Repository`
- `JpaRepository<엔티티, ID타입>` 상속
