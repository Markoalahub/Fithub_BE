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
```

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
      "openIssuesCount": 1
    }
  ],
  "total": 1
}
```

---

### 1-2. GitHub 레포 동기화 (프로젝트에 등록)

**사용 시나리오**: 사용자가 레포를 선택 → 직군(category) 지정 후 등록

```http
POST /projects/{projectId}/repositories/sync-from-github
Content-Type: application/json
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
      "repoName": "IT",
      "category": "AI"
    }
  ]
}
```

**Response (201 Created)**
```json
[
  {
    "id": 1,
    "projectId": 1,
    "repoUrl": "https://github.com/KYH-99/travel-plan",
    "repoType": "GITHUB",
    "category": "BE"
  }
]
```

---

### 1-3. 프로젝트의 등록된 레포 목록 조회

**사용 시나리오**: 파이프라인 생성 시, 어떤 레포들이 등록되어 있는지 확인

```http
GET /projects/{projectId}/repositories
```

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "projectId": 1,
    "repoUrl": "https://github.com/KYH-99/travel-plan",
    "repoType": "GITHUB",
    "category": "BE"
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

Body:
- projectId: 1
- prdFile: (optional) PDF file
```

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

### 2-2. Pipeline Step 수정

**사용 시나리오**: 개발자/기획자가 AI 생성 스텝을 수정

```http
PUT /pipelines/steps/{stepId}
Content-Type: application/json
```

**Request Body** (모든 필드 선택)
```json
{
  "title": "REST API 및 GraphQL 설계",
  "description": "API 설계 및 문서화",
  "is_completed": false
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "pipelineId": 1,
  "title": "REST API 및 GraphQL 설계",
  "description": "API 설계 및 문서화",
  "isCompleted": false,
  "origin": "ai_generated"
}
```

---

## 3. Issue 생성 및 GitHub 동기화 API

### 3-1. Pipeline Step에서 Issue 생성

**사용 시나리오**: 파이프라인 스텝을 실제 작업 Issue로 변환

```http
POST /pipelines/steps/{pipelineStepId}/create-issue
Content-Type: application/json
```

**Request Body**
```json
{
  "repositoryId": 1,
  "title": "API 설계",
  "description": "REST API 설계 및 문서화"
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "repositoryId": 1,
  "pipelineStepId": 1,
  "title": "API 설계",
  "description": "REST API 설계 및 문서화",
  "status": "PENDING",
  "createdAt": "2026-04-12T10:00:00Z"
}
```

---

### 3-2. 저장소별 Issue 목록 조회

**사용 시나리오**: 특정 레포의 모든 Issue 확인

```http
GET /issues/repository/{repositoryId}
```

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "repositoryId": 1,
    "pipelineStepId": 1,
    "title": "API 설계",
    "description": "REST API 설계 및 문서화",
    "status": "PENDING",
    "createdAt": "2026-04-12T10:00:00Z"
  }
]
```

---

### 3-3. Issue를 GitHub로 동기화

**사용 시나리오**: Spring Issue를 실제 GitHub Issue로 생성

```http
POST /issues/{issueId}/sync?repoUrl=https://github.com/KYH-99/travel-plan
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**
- `repoUrl`: GitHub 저장소 URL

**Response (201 Created)**
```json
{
  "id": 1,
  "issueId": 1,
  "repositoryId": 1,
  "githubIssueNumber": 42,
  "status": "SYNCED",
  "githubUrl": "https://github.com/KYH-99/travel-plan/issues/42",
  "syncedAt": "2026-04-12T10:10:00Z"
}
```

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
POST /issues/{issueId}/sync?repoUrl=...&accessToken=...
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
