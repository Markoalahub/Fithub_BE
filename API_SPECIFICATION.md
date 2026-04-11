# 🚀 Fithub Spring Boot API 명세서

> **Version**: 1.0.0  
> **Base URL**: `http://localhost:8080/api/v1`  
> **Swagger UI**: `http://localhost:8080/swagger-ui.html`  
> **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 📋 목차

1. [인증 (Authentication)](#인증)
2. [파이프라인 API](#파이프라인-api)
3. [Issue API](#issue-api)
4. [프로젝트 API](#프로젝트-api)
5. [에러 응답](#에러-응답)

---

## 🔐 인증

### 인증 방식

**GitHub OAuth 2.0**

```
Authorization: Bearer <GitHub_Access_Token>
또는
?accessToken=<GitHub_Access_Token>
```

---

## 📊 파이프라인 API

### 1. 프로젝트 파이프라인 조회

```http
GET /pipelines/project/{projectId}
```

**Parameters:**
- `projectId` (path, required): Spring Project ID

**Response (200 OK):**
```json
{
  "pipelines": [
    {
      "id": 1,
      "project_id": 1,
      "category": "기능 개발",
      "version": 2,
      "is_active": true,
      "steps": [
        {
          "id": 1,
          "pipeline_id": 1,
          "title": "회원가입 기능 개발",
          "description": "소셜 로그인 연동",
          "is_completed": false,
          "origin": "ai_generated"
        }
      ]
    }
  ],
  "total": 1
}
```

**Error Responses:**
- `404`: 프로젝트를 찾을 수 없음
- `503`: FastAPI 서버 연결 실패

---

### 2. AI 파이프라인 생성

```http
POST /pipelines/generate
```

**Parameters:**
- `projectId` (query, required): 프로젝트 ID
- `requirements` (query, required): 개발 요구사항 텍스트
- `category` (query, optional): 카테고리 (예: "기능개발", "버그수정")

**Request Example:**
```bash
POST /api/v1/pipelines/generate?projectId=1&requirements=회원가입%20기능%20구현&category=기능개발
```

**Response (201 Created):**
```json
{
  "id": 1,
  "project_id": 1,
  "category": "기능개발",
  "version": 1,
  "is_active": true,
  "steps": [
    {
      "id": 1,
      "pipeline_id": 1,
      "title": "데이터베이스 스키마 설계",
      "description": "사용자 테이블 생성",
      "is_completed": false,
      "origin": "ai_generated"
    },
    {
      "id": 2,
      "pipeline_id": 1,
      "title": "API 엔드포인트 구현",
      "description": "회원가입 API 개발",
      "is_completed": false,
      "origin": "ai_generated"
    }
  ]
}
```

**Error Responses:**
- `400`: 잘못된 요청 데이터
- `503`: FastAPI 서버 연결 실패

---

### 3. 파이프라인에 스텝 추가

```http
POST /pipelines/{pipelineId}/steps
```

**Parameters:**
- `pipelineId` (path, required): 파이프라인 ID
- `title` (query, required): 스텝 제목
- `description` (query, required): 스텝 설명

**Response (201 Created):**
```json
{
  "id": 3,
  "pipeline_id": 1,
  "title": "UI 컴포넌트 구현",
  "description": "회원가입 폼 개발",
  "is_completed": false,
  "origin": "user_created"
}
```

---

### 4. 파이프라인 스텝 완료 처리

```http
PATCH /pipelines/steps/{stepId}/complete
```

**Parameters:**
- `stepId` (path, required): 파이프라인 스텝 ID

**Response (200 OK):**
```
(Empty Body)
```

---

### 5. 파이프라인을 GitHub Issues로 동기화

```http
POST /pipelines/{pipelineId}/sync-to-github
```

**Parameters:**
- `pipelineId` (path, required): 파이프라인 ID
- `repositoryId` (query, required): Spring Repository ID
- `accessToken` (query, required): GitHub Access Token

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "issue_id": 101,
    "github_issue_number": 42,
    "repository_id": 1,
    "status": "SYNCED",
    "github_url": "https://github.com/owner/repo/issues/42",
    "error_message": null,
    "created_at": "2026-04-10T00:05:00Z",
    "updated_at": "2026-04-10T00:05:00Z"
  }
]
```

**Error Responses:**
- `400`: GitHub 동기화 실패
- `401`: GitHub 인증 실패
- `404`: 파이프라인 또는 저장소를 찾을 수 없음

---

## 🔴 Issue API

### 1. Issue 상세 조회

```http
GET /issues/{issueId}
```

**Response (200 OK):**
```json
{
  "id": 101,
  "repository_id": 1,
  "github_issue_number": 42,
  "title": "회원가입 기능 구현",
  "description": "소셜 로그인 연동",
  "status": "OPEN",
  "pipeline_step_id": 1,
  "created_at": "2026-04-10T00:00:00Z",
  "updated_at": "2026-04-10T00:05:00Z"
}
```

---

### 2. 저장소별 Issue 목록 조회

```http
GET /issues/repository/{repositoryId}
```

**Response (200 OK):**
```json
[
  {
    "id": 101,
    "repository_id": 1,
    "github_issue_number": 42,
    "title": "회원가입 기능 구현",
    "description": "소셜 로그인 연동",
    "status": "OPEN",
    "pipeline_step_id": 1,
    "created_at": "2026-04-10T00:00:00Z",
    "updated_at": "2026-04-10T00:05:00Z"
  }
]
```

---

### 3. Issue 동기화 상태 조회

```http
GET /issues/sync/{issueId}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "issue_id": 101,
  "github_issue_number": 42,
  "repository_id": 1,
  "status": "SYNCED",
  "github_url": "https://github.com/owner/repo/issues/42",
  "error_message": null,
  "created_at": "2026-04-10T00:05:00Z",
  "updated_at": "2026-04-10T00:05:00Z"
}
```

---

### 4. 상태별 Issue 조회

```http
GET /issues/sync/repository/{repositoryId}/status/{status}
```

**Parameters:**
- `repositoryId` (path, required): 저장소 ID
- `status` (path, required): 동기화 상태 (`PENDING`, `SYNCED`, `FAILED`, `CLOSED`)

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "issue_id": 101,
    "github_issue_number": 42,
    "repository_id": 1,
    "status": "SYNCED",
    "github_url": "https://github.com/owner/repo/issues/42",
    "error_message": null,
    "created_at": "2026-04-10T00:05:00Z",
    "updated_at": "2026-04-10T00:05:00Z"
  }
]
```

---

### 5. Issue 상태 업데이트

```http
PATCH /issues/{issueId}/status
```

**Parameters:**
- `issueId` (path, required): Issue ID
- `status` (query, required): 새로운 상태 (`OPEN`, `CLOSED`)
- `repoUrl` (query, required): GitHub 저장소 URL (예: `https://github.com/owner/repo`)
- `accessToken` (query, required): GitHub Access Token

**Response (200 OK):**
```json
{
  "issueId": "101",
  "status": "CLOSED",
  "message": "Issue status updated successfully"
}
```

---

### 6. Issue를 GitHub로 동기화

```http
POST /issues/{issueId}/sync
```

**Parameters:**
- `issueId` (path, required): Issue ID
- `repoUrl` (query, required): GitHub 저장소 URL
- `accessToken` (query, required): GitHub Access Token

**Response (201 Created):**
```json
{
  "id": 1,
  "issue_id": 101,
  "github_issue_number": 42,
  "repository_id": 1,
  "status": "SYNCED",
  "github_url": "https://github.com/owner/repo/issues/42",
  "error_message": null,
  "created_at": "2026-04-10T00:05:00Z",
  "updated_at": "2026-04-10T00:05:00Z"
}
```

---

## 📁 프로젝트 API

### 1. 프로젝트 조회

```http
GET /projects/{projectId}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Fithub",
  "description": "AI 기반 협업 허브",
  "created_at": "2026-04-01T10:00:00Z",
  "updated_at": "2026-04-10T00:00:00Z"
}
```

---

### 2. 프로젝트 생성

```http
POST /projects
```

**Parameters:**
- `name` (query, required): 프로젝트 이름
- `description` (query, optional): 프로젝트 설명

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Fithub",
  "description": "AI 기반 협업 허브",
  "created_at": "2026-04-10T00:05:00Z",
  "updated_at": "2026-04-10T00:05:00Z"
}
```

---

### 3. 프로젝트 정보 수정

```http
PATCH /projects/{projectId}
```

**Parameters:**
- `projectId` (path, required): 프로젝트 ID
- `name` (query, optional): 새로운 프로젝트 이름
- `description` (query, optional): 새로운 프로젝트 설명

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Fithub v2",
  "description": "AI 기반 협업 허브 - 개선 버전",
  "created_at": "2026-04-01T10:00:00Z",
  "updated_at": "2026-04-10T00:06:00Z"
}
```

---

### 4. 프로젝트 삭제

```http
DELETE /projects/{projectId}
```

**Response (204 No Content):**
```
(Empty Body)
```

---

### 5. 프로젝트 멤버 목록 조회

```http
GET /projects/{projectId}/members
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "project_id": 1,
    "user_id": 10,
    "role": "PM",
    "joined_at": "2026-04-01T10:00:00Z",
    "updated_at": "2026-04-10T00:00:00Z"
  },
  {
    "id": 2,
    "project_id": 1,
    "user_id": 11,
    "role": "BE",
    "joined_at": "2026-04-02T10:00:00Z",
    "updated_at": "2026-04-10T00:00:00Z"
  }
]
```

---

### 6. 프로젝트에 멤버 추가

```http
POST /projects/{projectId}/members
```

**Parameters:**
- `projectId` (path, required): 프로젝트 ID
- `userId` (query, required): 사용자 ID
- `role` (query, required): 멤버 역할 (예: `PM`, `FE`, `BE`, `AI`)

**Response (201 Created):**
```json
{
  "id": 3,
  "project_id": 1,
  "user_id": 12,
  "role": "FE",
  "joined_at": "2026-04-10T00:07:00Z",
  "updated_at": "2026-04-10T00:07:00Z"
}
```

---

### 7. 멤버 역할 수정

```http
PATCH /projects/{projectId}/members/{memberId}/role
```

**Parameters:**
- `projectId` (path, required): 프로젝트 ID
- `memberId` (path, required): 멤버 ID
- `role` (query, required): 새로운 역할

**Response (200 OK):**
```json
{
  "id": 3,
  "project_id": 1,
  "user_id": 12,
  "role": "BE",
  "joined_at": "2026-04-10T00:07:00Z",
  "updated_at": "2026-04-10T00:08:00Z"
}
```

---

### 8. 멤버 삭제

```http
DELETE /projects/{projectId}/members/{memberId}
```

**Response (204 No Content):**
```
(Empty Body)
```

---

### 9. 특정 사용자의 프로젝트 멤버 조회

```http
GET /projects/{projectId}/members/{userId}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "project_id": 1,
  "user_id": 10,
  "role": "PM",
  "joined_at": "2026-04-01T10:00:00Z",
  "updated_at": "2026-04-10T00:00:00Z"
}
```

---

## ⚠️ 에러 응답

### 400 Bad Request
```json
{
  "timestamp": "2026-04-10T00:05:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-04-10T00:05:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found: 999"
}
```

### 409 Conflict
```json
{
  "timestamp": "2026-04-10T00:05:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "User is already a member of this project"
}
```

### 503 Service Unavailable
```json
{
  "timestamp": "2026-04-10T00:05:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "FastAPI server connection failed"
}
```

---

## 📌 참고사항

### 헤더 설정

모든 요청에 다음 헤더를 포함하세요:

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer <GitHub_Access_Token>
```

### Rate Limiting

- **GitHub API**: 5,000 requests/hour per token
- **FastAPI**: 분당 1000 요청 제한

### CORS 설정

프론트엔드에서 호출 시 CORS 설정:

```javascript
fetch('http://localhost:8080/api/v1/projects/1', {
  headers: {
    'Authorization': `Bearer ${githubToken}`
  }
})
```

---

**Last Updated**: 2026-04-10
