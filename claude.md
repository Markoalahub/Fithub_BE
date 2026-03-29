# 🚀 Project Overview: Fithub

**Fithub**는 기획자와 개발자 사이의 정보 비대칭성을 해결하기 위한  
**AI 기반 지능형 협업 허브**입니다.  

- 기획안(PDF/텍스트)을 분석하여  
  → 실행 가능한 개발 태스크(GitHub Issue / Milestone)로 자동 변환  
- 실시간 GitHub 활동을  
  → 비즈니스 가치 관점에서 시각화  

---

# 🏗️ System Architecture (MSA)

시스템은 **확장성과 AI 처리 효율**을 위해 두 개의 독립된 서버로 구성됩니다.

## 1. Spring Boot Server (The Orchestrator)

- **Role**
  - 인증 처리
  - GitHub API 제어
  - 비즈니스 로직 총괄
  - UI 데이터 서빙

- **Database**
  - MySQL  
  - (GitHub 동기화 상태, 프로젝트 메타데이터, 유저 정보)

- **GitHub Library**
  - `org.kohsuke.github`

- **Auth**
  - GitHub OAuth 2.0 연동 완료

---

## 2. FastAPI Server (The Intelligence)

- **Role**
  - PDF / 텍스트 파싱
  - LLM 기반 이슈 분할
  - 회의록 요약
  - 언어 변환

- **Database**
  - PostgreSQL / Vector DB  
  - (회의 기록, 지식 자산, RAG 임베딩)

- **AI Stack**
  - LangChain
  - Docling (PDF 파싱)

---

## 3. Inter-Service Communication

- **OpenFeign**
  - Spring → FastAPI 선언적 API 호출

- **Callback / Webhook**
  - 장기 실행 작업은 비동기 처리
  - FastAPI → Spring으로 결과 전송

---

# 📊 Data Schema Highlights

## Spring DB

- `User`
- `Project`
- `Milestone_Sync`
- `Issue_Sync`
  - 상태값:
    - `STABLE`
    - `APPROPRIATE`
    - `RISKY`
- `Delay_Report`

---

## Python DB

- `Meeting_Log`
- `PRD_Knowledge` (PDF 추출 JSON)
- `Vector_Store`

---

# 🔄 Core Pipeline Workflow

## 1. Ingestion

- 기획자가 PDF 업로드  
→ FastAPI가 Markdown 변환  
→ 이슈 초안 생성  

---

## 2. Confirmation

- 기획자 ↔ 개발자 상호 컨펌
- 언어 변환 지원
- 승인 시 다음 단계 진행

---

## 3. Automation

- Spring이 GitHub API 호출  
→ Milestone 및 Issue 자동 생성  

---

## 4. Tracking

- UI에서 업무 체크 시 GitHub 상태 업데이트
- PR Merge 시 UI 상태:
  - 회색(Disabled) 처리

---

# 🧪 Management Metrics & Logic

## 1. Risk Classification (현안 리포트 상태)

AI 기반 분류:

- **Stable (안정)**
  - 계획대로 진행
  - 기술적 / 일정적 문제 없음

- **Appropriate (적절)**
  - 사소한 이슈 존재
  - 관리 가능한 수준

- **Risky (위험)**
  - 일정 지연 1주 이상
  - 핵심 기술 구현 난항

---

## 2. Delay Calculation Formula

지연율(%)은 외부 통제 불가능 요인을 제외한 정량적 수치로 산출:

$$
지연율(\%) = \frac{실제 \ 종료일 - 기준 \ 종료일 - 통제 \ 불가능한 \ 지연 \ 시간}{전체 \ 프로젝트 \ 기간} \times 100
$$

---

# 🛠️ Development Guidelines

## API First

- 모든 기능은 **API 문서화(Swagger)** 우선

---

## Error Handling

- GitHub API Rate Limit 고려
  - 5,000 requests / hour
- 큐잉 및 예외 처리 필수

---

## Clean Code

- Spring에서는  
  → **생성자 주입 (Constructor Injection)** 권장