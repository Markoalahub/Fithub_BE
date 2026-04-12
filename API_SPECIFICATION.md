# FitHub API Specification

프론트엔드에서 필요한 REST API 명세입니다.

---

## 📌 Base URL

```
http://localhost:8080/api/v1
```

---

## 🔐 Authentication

모든 요청에 JWT 토큰 필요:

```
Authorization: Bearer <JWT_TOKEN>
```

---

## 1. GitHub Repository 관리 API

### 1-1. 사용 가능한 GitHub 레포 조회

**사용 시나리오**: 프로젝트 생성 후, 사용자의 GitHub 레포 목록 표시

```http
GET /projects/{projectId}/repositories/github-available
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**: 없음

**응명 설명**
- JWT 토큰의 사용자가 GitHub에서 소유/접근 가능한 레포 목록 조회
- 저장된 GitHub Access Token을 사용하여 실시간 조회
- 이미 프로젝트에 등록된 레포도 포함됨

**Response (200 OK)**
```json
{
  "repositories": [
    {
      "id": 1207474638,
      "name": "travel-plan",
      "fullName": "KYH-99/travel-plan",
      "description": "Travel planning app",
      "htmlUrl": "https://github.com/KYH-99/travel-plan",
      "isPrivate": false,
      "language": "Python",
      "stargazersCount": 1,
      "openIssuesCount": 1,
      "createdAt": "2023-01-15T10:00:00Z",
      "updatedAt": "2026-04-12T15:30:00Z"
    },
    {
      "id": 1206745673,
      "name": "ai-model",
      "fullName": "KYH-99/ai-model",
      "description": "Machine learning models",
      "htmlUrl": "https://github.com/KYH-99/ai-model",
      "isPrivate": false,
      "language": "Python",
      "stargazersCount": 5,
      "openIssuesCount": 3,
      "createdAt": "2023-06-20T08:00:00Z",
      "updatedAt": "2026-04-10T12:00:00Z"
    }
  ],
  "total": 2
}
```

**Response 필드 상세**
| 필드 | 타입 | 설명 |
|-----|------|------|
| repositories | Array | 저장소 정보 배열 |
| ├─ id | Long | GitHub 저장소 ID |
| ├─ name | String | 저장소 이름 |
| ├─ fullName | String | 저장소 전체명 (owner/repo) |
| ├─ description | String | 저장소 설명 |
| ├─ htmlUrl | String | GitHub 저장소 URL |
| ├─ isPrivate | Boolean | 프라이빗 여부 |
| ├─ language | String | 주 사용 언어 |
| ├─ stargazersCount | Integer | 스타 개수 |
| ├─ openIssuesCount | Integer | 열린 이슈 개수 |
| ├─ createdAt | String | GitHub 저장소 생성 시각 |
| ├─ updatedAt | String | GitHub 저장소 수정 시각 |
| total | Integer | 전체 저장소 개수 |

---

### 1-2. GitHub 레포 동기화 (프로젝트에 등록)

**사용 시나리오**: 사용자가 레포를 선택 → 직군(category) 지정 후 등록

```http
POST /projects/{projectId}/repositories/sync-from-github
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**
```json
{
  "githubRepoIds": [1207474638, 1206745673],
  "categoryMappings": [
    {
      "githubRepoId": 1207474638,
      "repoName": "travel-plan",
      "category": "BE"
    },
    {
      "githubRepoId": 1206745673,
      "repoName": "ai-model",
      "category": "AI"
    }
  ]
}
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| githubRepoIds | Array[Long] | O | GitHub 저장소 ID 목록 |
| categoryMappings | Array | O | 저장소별 직군 분류 정보 |
| ├─ githubRepoId | Long | O | GitHub 저장소 ID |
| ├─ repoName | String | O | 저장소명 |
| ├─ category | String | O | 직군 분류 (FE/BE/AI/DEVOPS/QA) |

**응답 설명**
- 각 저장소별로 GithubRepository 엔티티 생성
- 추후 파이프라인 생성 시 category를 기준으로 직군별 파이프라인 자동 생성

**Response (201 Created)**
```json
[
  {
    "id": 1,
    "projectId": 1,
    "repoUrl": "https://github.com/KYH-99/travel-plan",
    "repoType": "GITHUB",
    "category": "BE",
    "createdAt": "2026-04-12T10:00:00Z",
    "updatedAt": "2026-04-12T10:00:00Z"
  },
  {
    "id": 2,
    "projectId": 1,
    "repoUrl": "https://github.com/KYH-99/ai-model",
    "repoType": "GITHUB",
    "category": "AI",
    "createdAt": "2026-04-12T10:00:00Z",
    "updatedAt": "2026-04-12T10:00:00Z"
  }
]
```

**Response 필드 상세**
| 필드 | 타입 | 설명 |
|-----|------|------|
| id | Long | 저장소 등록 ID |
| projectId | Long | 프로젝트 ID |
| repoUrl | String | GitHub 저장소 URL |
| repoType | String | 저장소 타입 (GITHUB) |
| category | String | 직군 분류 |
| createdAt | String | 등록 시각 |
| updatedAt | String | 수정 시각 |

---

### 1-3. 프로젝트의 등록된 레포 목록 조회

**사용 시나리오**: 파이프라인 생성 시, 어떤 레포들이 등록되어 있는지 확인

```http
GET /projects/{projectId}/repositories
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**: 없음

**응답 설명**
- 프로젝트에 등록된 모든 GitHub 저장소 목록 반환
- 각 저장소의 직군(category) 분류 정보 포함

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "projectId": 1,
    "repoUrl": "https://github.com/KYH-99/travel-plan",
    "repoType": "GITHUB",
    "category": "BE",
    "createdAt": "2026-04-12T10:00:00Z",
    "updatedAt": "2026-04-12T10:00:00Z"
  },
  {
    "id": 2,
    "projectId": 1,
    "repoUrl": "https://github.com/KYH-99/frontend",
    "repoType": "GITHUB",
    "category": "FE",
    "createdAt": "2026-04-12T10:05:00Z",
    "updatedAt": "2026-04-12T10:05:00Z"
  }
]
```

---

## 2. Pipeline 생성 및 관리 API

### 2-1. 모든 카테고리 파이프라인 일괄 생성 (PDF 지원)

**사용 시나리오**: 레포 등록 후, 전체 파이프라인을 한 번에 생성

```http
POST /pipelines/generate-all
Content-Type: multipart/form-data
Authorization: Bearer <JWT_TOKEN>
```

**Request Body** (multipart/form-data)
```
- request: JSON 형식의 요청 데이터
  {
    "projectId": 1
  }
- prdFile: (선택) PDF 파일
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| projectId | Long | O | 프로젝트 ID |
| prdFile | File | X | PRD PDF 파일 (없으면 AI가 기본 파이프라인 생성) |

**Response (201 Created)**
```json
{
  "projectId": 1,
  "count": 2,
  "pipelines": [
    {
      "id": 1,
      "projectId": 1,
      "category": "BE",
      "version": 1,
      "isActive": true,
      "steps": [
        {
          "id": 1,
          "pipelineId": 1,
          "title": "API 설계",
          "description": "REST API 설계",
          "isCompleted": false,
          "origin": "ai_generated"
        }
      ]
    }
  ]
}
```

---

### 2-2. 파이프라인을 GitHub Issues로 동기화

**사용 시나리오**: 생성된 파이프라인의 모든 스텝을 GitHub Issues로 한 번에 동기화

```http
POST /pipelines/{pipelineId}/sync-to-github
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**
```json
{
  "repositoryId": 1,
  "accessToken": "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| repositoryId | Long | O | GitHub 저장소 ID |
| accessToken | String | O | GitHub Personal Access Token (리포지토리 쓰기 권한 필요) |

**Response (200 OK)**
```json
{
  "pipelineId": 1,
  "repositoryId": 1,
  "syncedSteps": 5,
  "totalSteps": 5,
  "status": "SUCCESS",
  "message": "Pipeline synced to GitHub successfully"
}
```

---

### 2-3. Pipeline Step 수정

**사용 시나리오**: 개발자/기획자가 AI 생성 스텝을 수정

```http
PUT /pipelines/steps/{stepId}
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body** (모든 필드 선택 사항)
```json
{
  "title": "REST API 및 GraphQL 설계",
  "description": "API 설계 및 문서화",
  "isCompleted": false
}
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| title | String | X | 스텝 제목 (최대 255자) |
| description | String | X | 스텝 상세 설명 |
| isCompleted | Boolean | X | 완료 여부 |

**응답 설명**
- 제공되지 않은 필드는 기존 값 유지
- 최대 3개 필드까지 부분 수정 가능

**Response (200 OK)**
```json
{
  "id": 1,
  "pipelineId": 1,
  "title": "REST API 및 GraphQL 설계",
  "description": "API 설계 및 문서화",
  "isCompleted": false,
  "version": 1,
  "origin": "ai_generated",
  "createdAt": "2026-04-12T10:00:00Z",
  "updatedAt": "2026-04-12T10:15:00Z"
}
```

---

## 3. Issue 생성 및 GitHub 동기화 API

### 3-1. Pipeline Step에서 Issue 생성

**사용 시나리오**: 파이프라인 스텝을 실제 작업 Issue로 변환 (GitHub 동기화 전 로컬 Issue)

```http
POST /pipelines/steps/{pipelineStepId}/create-issue
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**
```json
{
  "repositoryId": 1,
  "title": "API 설계",
  "description": "REST API 설계 및 문서화"
}
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| repositoryId | Long | O | GitHub 저장소 ID |
| title | String | O | Issue 제목 |
| description | String | O | Issue 상세 설명 |

**응답 설명**
- Issue를 로컬 DB에만 생성 (GitHub에는 아직 동기화 안 함)
- 상태는 PENDING으로 생성
- 나중에 sync 엔드포인트를 통해 GitHub에 동기화

**Response (201 Created)**
```json
{
  "id": 1,
  "repositoryId": 1,
  "githubIssueNumber": null,
  "title": "API 설계",
  "description": "REST API 설계 및 문서화",
  "status": "PENDING",
  "pipelineStepId": 1,
  "createdAt": "2026-04-12T10:00:00Z",
  "updatedAt": "2026-04-12T10:00:00Z"
}
```

**Response 필드 상세**
| 필드 | 타입 | 설명 |
|-----|------|------|
| id | Long | Issue ID |
| repositoryId | Long | 저장소 ID |
| githubIssueNumber | Integer | GitHub Issue 번호 (동기화 후 설정) |
| title | String | Issue 제목 |
| description | String | Issue 상세 설명 |
| status | String | 상태 (PENDING, SYNCED, FAILED 등) |
| pipelineStepId | Long | 연결된 파이프라인 스텝 ID |
| createdAt | String | 생성 시각 |
| updatedAt | String | 수정 시각 |

---

### 3-2. Issue 상태 업데이트 및 GitHub 반영

**사용 시나리오**: Issue의 상태를 변경하고 GitHub에 반영 (OPEN → CLOSED)

```http
PATCH /issues/{issueId}/status
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**
```json
{
  "status": "CLOSED",
  "repoUrl": "https://github.com/KYH-99/travel-plan"
}
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| status | String | O | 변경할 상태 (OPEN, CLOSED) |
| repoUrl | String | O | GitHub 저장소 URL |

**Response (200 OK)**
```json
{
  "issueId": "1",
  "status": "CLOSED",
  "message": "Issue status updated successfully"
}
```

---

### 3-3. 저장소별 Issue 목록 조회

**사용 시나리오**: 특정 레포의 모든 Issue 확인

```http
GET /issues/repository/{repositoryId}
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**: 없음

**응답 설명**
- 해당 저장소에 등록된 모든 Issue 목록 반환 (GitHub 동기화 여부 무관)
- 각 Issue의 상태(status)를 통해 동기화 여부 확인 가능

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "repositoryId": 1,
    "githubIssueNumber": null,
    "title": "API 설계",
    "description": "REST API 설계 및 문서화",
    "status": "PENDING",
    "pipelineStepId": 1,
    "createdAt": "2026-04-12T10:00:00Z",
    "updatedAt": "2026-04-12T10:00:00Z"
  },
  {
    "id": 2,
    "repositoryId": 1,
    "githubIssueNumber": 42,
    "title": "데이터베이스 설계",
    "description": "MySQL 스키마 설계",
    "status": "SYNCED",
    "pipelineStepId": 2,
    "createdAt": "2026-04-12T10:05:00Z",
    "updatedAt": "2026-04-12T10:15:00Z"
  }
]
```

**Response 필드 상세**
| 필드 | 타입 | 설명 |
|-----|------|------|
| id | Long | Issue ID |
| repositoryId | Long | 저장소 ID |
| githubIssueNumber | Integer | GitHub Issue 번호 (동기화 전: null) |
| title | String | Issue 제목 |
| description | String | Issue 상세 설명 |
| status | String | 상태 (PENDING, SYNCED, FAILED 등) |
| pipelineStepId | Long | 연결된 파이프라인 스텝 ID |
| createdAt | String | 생성 시각 |
| updatedAt | String | 수정 시각 |

---

### 3-4. Issue를 GitHub로 동기화

**사용 시나리오**: Spring Issue를 실제 GitHub Issue로 생성

```http
POST /issues/{issueId}/sync
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**
```json
{
  "repoUrl": "https://github.com/KYH-99/travel-plan"
}
```

**Request Body 상세**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| repoUrl | String | O | GitHub 저장소 URL (e.g., https://github.com/owner/repo) |

**응답 설명**
- GitHub에 Issue 생성 후 이슈 번호(number)를 받음
- IssueSync 레코드를 생성하여 동기화 상태 추적
- JWT 토큰에서 사용자 정보 추출 후 저장된 GitHub Access Token 사용

**Response (201 Created)**
```json
{
  "id": 1,
  "issueId": 1,
  "repositoryId": 1,
  "githubIssueNumber": 42,
  "status": "SYNCED",
  "githubUrl": "https://github.com/KYH-99/travel-plan/issues/42",
  "createdAt": "2026-04-12T10:00:00Z",
  "updatedAt": "2026-04-12T10:10:00Z"
}
```

**Response 필드 상세**
| 필드 | 타입 | 설명 |
|-----|------|------|
| id | Long | IssueSync 레코드 ID |
| issueId | Long | Spring Issue ID |
| repositoryId | Long | 저장소 ID |
| githubIssueNumber | Integer | GitHub에서 자동 할당된 Issue 번호 |
| status | String | 동기화 상태 (PENDING, SYNCED, FAILED, CLOSED) |
| githubUrl | String | GitHub Issue 페이지 URL |
| createdAt | String | 동기화 생성 시각 (ISO 8601) |
| updatedAt | String | 동기화 업데이트 시각 (ISO 8601) |

---

## 4. Frontend Workflow

### Step 1: GitHub 레포 등록
```
GET /projects/{projectId}/repositories/github-available
  ↓ (사용자가 레포 선택 + 카테고리 지정)
POST /projects/{projectId}/repositories/sync-from-github
```

### Step 2: 파이프라인 생성
```
POST /pipelines/generate-all
```

### Step 3: Pipeline Step 수정 (선택)
```
PUT /pipelines/steps/{stepId}
```

### Step 4: Issue 생성
```
POST /pipelines/steps/{pipelineStepId}/create-issue
```

### Step 5: GitHub 동기화
```
POST /issues/{issueId}/sync
Content-Type: application/json
Request Body: { "repoUrl": "..." }
```

---

## 5. Error Responses

```json
{
  "timestamp": "2026-04-12T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request data"
}
```

### HTTP Status Codes

| Status | 의미 |
|--------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 404 | 리소스 찾을 수 없음 |
| 503 | 서버 연결 실패 |
