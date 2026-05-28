# 🔌 FitHub AI 파이프라인 통합 REST API 명세서

본 명세서는 FitHub 프로젝트의 **AI 기획/설계 엔진(Stage 1~3)** 및 **파이프라인 제어(태스크 관리, 깃허브 동기화)** 전체 API의 포맷, 파라미터, 헤더, 예시를 수록하고 있습니다.

* **Base URL**: `http://localhost:8080/pipelines`
* **공통 에러 응답**:
  * `401 Unauthorized`: 인증 세션 누락 또는 JWT 유효하지 않음
  * `400 Bad Request`: 필수 파라미터 누락 혹은 불일치
  * `500 Internal Server Error`: AI 서버 통신 장애 혹은 DB 연동 실패

---

## 🗺️ 1. AI 기획 및 설계 파이프라인 API (Stage 1 ~ 3)

### 1️⃣ 유저 플로우 세션 시작 (Stage 1-A)
* **Method**: `POST`
* **Path**: `/generate-userflow`
* **Content-Type**: `multipart/form-data`
* **요청 파라미터**:
  | 필드명 | 타입 | 필수 여부 | 설명 |
  | :--- | :--- | :--- | :--- |
  | **projectId** | Long | 필수 | 스프링 프로젝트 ID |
  | **requirements** | String | 필수 | AI 분석에 들어갈 제품 핵심 기능 요구사항 |
  | **techStack** | String | 선택 | 기본값: `"Spring Boot, React"`. 대상 기술 스택 |
  | **prdFile** | MultipartFile | 선택 | PDF 형식의 상세 요구사항 정의서(PRD) |

* **성공 응답 (200 OK)**:
  ```json
  {
    "flow_id": 14,
    "status": "interviewing",
    "question": "로그인 페이지에서 구글 소셜 로그인도 지원해야 하나요?",
    "nodes": []
  }
  ```

---

### 2️⃣ 기획자 답변 전송 및 플로우 확정 (Stage 1-B)
* **Method**: `POST`
* **Path**: `/userflow-session/{flowId}/answer`
* **요청 파라미터 (Query / Form)**:
  | 필드명 | 타입 | 필수 여부 | 위치 | 설명 |
  | :--- | :--- | :--- | :--- | :--- |
  | **flowId** | Long | 필수 | Path | 유저 플로우 세션 ID |
  | **answer** | String | 필수 | Query | AI 질문에 대한 답변 내용 |
  | **confirm** | Boolean | 선택 | Query | 기본값: `false`. 기획 확정 시 `true` 전송 |

* **성공 응답 (200 OK - `confirm=true` 시)**:
  ```json
  {
    "flow_id": 14,
    "status": "completed",
    "nodes": [
      {
        "id": 110,
        "user_flow_id": 14,
        "name": "메인 화면",
        "description": "주간 그래프와 추천 루틴을 보여주는 허브 스크린",
        "node_type": "screen",
        "wireframe_ascii": null,
        "sequence_order": 1
      }
    ]
  }
  ```

---

### 3️⃣ 화면별 ASCII 와이어프레임 설계 (Stage 2)
* **Method**: `POST`
* **Path**: `/generate-wireframe`
* **요청 파라미터 (Query)**:
  | 필드명 | 타입 | 필수 여부 | 설명 |
  | :--- | :--- | :--- | :--- |
  | **userFlowId** | Long | 필수 | 최종 확정 완료된 유저 플로우 세션 ID |

* **성공 응답 (200 OK)**:
  ```json
  {
    "flow_id": 14,
    "status": "wireframe_generated",
    "nodes": [
      {
        "id": 110,
        "user_flow_id": 14,
        "name": "메인 화면",
        "description": "주간 그래프와 추천 루틴을 보여주는 허브 스크린",
        "node_type": "screen",
        "wireframe_ascii": "┌─────────────────────────────┐\n│  [📊 주간 그래프]            │\n│  [🏠 홈]  [🔔 알림]          │\n└─────────────────────────────┘",
        "sequence_order": 1
      }
    ]
  }
  ```

---

### 4️⃣ 개발 파이프라인 태스크 자동 도출 및 적재 (Stage 3)
* **Method**: `POST`
* **Path**: `/generate-pipeline-from-flow`
* **요청 파라미터 (Query)**:
  | 필드명 | 타입 | 필수 여부 | 설명 |
  | :--- | :--- | :--- | :--- |
  | **userFlowId** | Long | 필수 | 와이어프레임 생성이 완료된 유저 플로우 ID |
  | **projectId** | Long | 필수 | 스프링 프로젝트 ID |
  | **category** | String | 필수 | 개발 포지션 카테고리 (`FE` 또는 `BE`) |

* **성공 응답 (200 OK)**:
  ```json
  {
    "id": 16,
    "project_id": 103,
    "category": "BE",
    "version": 1,
    "tech_stack": "Spring Boot, React Native",
    "steps": [
      {
        "id": 106,
        "step_task_description": "운동 일지 등록 API 개발",
        "step_details": [
          "[API] 운동 일지 등록을 위한 엔드포인트 생성",
          "[DB] 운동 일지 테이블 설계 및 생성",
          "[Service] 운동 일지 등록 로직 구현"
        ],
        "category": "BE",
        "step_sequence_number": 1,
        "priority": 1,
        "is_completed": false
      }
    ]
  }
  ```

---

## 🛠️ 2. 파이프라인 관리 및 깃허브 연동 API

### 5️⃣ 특정 프로젝트의 전체 파이프라인 조회
* **Method**: `GET`
* **Path**: `/project/{projectId}`
* **요청 파라미터 (Path)**:
  * `projectId`: 프로젝트 ID

* **성공 응답 (200 OK)**:
  ```json
  {
    "projectId": 103,
    "pipelines": [
      {
        "id": 16,
        "category": "BE",
        "steps": [
          {
            "id": 106,
            "title": "운동 일지 등록 API 개발",
            "description": "운동 일지 등록을 위한 엔드포인트 생성",
            "is_completed": false
          }
        ]
      }
    ]
  }
  ```

---

### 6️⃣ 파이프라인 커스텀 단계(Step) 수동 추가
* **Method**: `POST`
* **Path**: /{pipelineId}/steps`
* **요청 바디 (JSON)**:
  ```json
  {
    "title": "배포 환경 스케줄러 구성",
    "description": "AWS EC2 환경에 크론탭 세팅 및 알림 배치 가동 테스트"
  }
  ```

* **성공 응답 (201 Created)**:
  ```json
  {
    "id": 115,
    "pipeline_id": 16,
    "title": "배포 환경 스케줄러 구성",
    "description": "AWS EC2 환경에 크론탭 세팅 및 알림 배치 가동 테스트",
    "is_completed": false,
    "origin": "user_created"
  }
  ```

---

### 7️⃣ 특정 파이프라인 단계(Step) 상태 업데이트
* **Method**: `PATCH`
* **Path**: `/steps/{stepId}`
* **요청 바디 (JSON - null인 필드는 미수정)**:
  ```json
  {
    "title": "수정된 타이틀",
    "is_completed": true
  }
  ```

* **성공 응답 (200 OK)**:
  ```json
  {
    "id": 106,
    "title": "수정된 타이틀",
    "description": "운동 일지 등록을 위한 엔드포인트 생성",
    "is_completed": true,
    "origin": "ai_generated"
  }
  ```

---

### 8️⃣ 파이프라인 단계를 깃허브 이슈(GitHub Issue)로 동기화
* **Method**: `POST`
* **Path**: `/{pipelineStepId}/sync-issue`
* **Headers**:
  * `Authorization`: `Bearer {JWT_TOKEN}` (스프링 부트 로그인 후 발급된 액세스 토큰)
* **요청 바디 (JSON)**:
  ```json
  {
    "repositoryId": 42,
    "repoUrl": "https://github.com/myeongsung/Fithub_BE",
    "title": "[BE] 운동 일지 등록 API 개발",
    "description": "## 태스크 상세\n- [ ] 운동 일지 등록을 위한 엔드포인트 생성"
  }
  ```

* **성공 응답 (201 Created)**:
  ```json
  {
    "id": 801,
    "repositoryId": 42,
    "issueNumber": 12,
    "title": "[BE] 운동 일지 등록 API 개발",
    "description": "## 태스크 상세\n- [ ] 운동 일지 등록을 위한 엔드포인트 생성",
    "status": "OPEN",
    "issueUrl": "https://github.com/myeongsung/Fithub_BE/issues/12"
  }
  ```
