# 장소 선택과 출발 시간 분리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 첫 화면은 현재 위치와 차박지 3곳, 다음 화면은 현재 시각과 내일 귀가 출발 시간 선택에 집중한다.

**Architecture:** 기존 ViewModel에 SCHEDULE 단계를 추가한다. 위치는 기존 목업 데이터에 출발지 이름을 명시하고, 시간은 기기 시간대의 실제 현재 시각을 사용한다. 선택한 시각으로 전력 계획의 기간을 연결하되 기존 SDK 제어는 유지한다.

**Tech Stack:** Kotlin, Compose Material3, java.time, 기존 Pleos SDK.

**Spec:** 2026-11-14 사용자 요청: 첫 화면에 현재 위치와 추천 차박지 3곳, 장소 선택 후 현재 시각 및 다음날 집으로 출발할 시간 선택. 이번 범위는 앞의 두 화면 수정.

## Global Constraints
- 새 SDK·위치 권한 추가 없음. 현재 위치·추천은 명확히 시연 데이터로 표시.
- 선택 시각은 집 도착 시각이 아니라 차박지에서 집으로 출발하는 시각.
- 기기 현지 시각 기준 내일 날짜, 기본 09:00, 00:00~23:59 선택 가능.
- 첫 화면에서 체류·배터리·전력 설정 제거. 기존 설정은 연결 정보 안으로 이동.
- 기존 전력 기능은 시간 선택 이후 별도 화면에서 제공.

## Review Focus
- 자정·월말·연말: 내일 날짜 및 양수 기간.
- 재생성·뒤로 가기: 장소와 선택한 시각 유지.
- 첫 화면에서 장소 외 설정이 노출되지 않음.
- 큰 글자·좁은 화면: 시간 선택과 하단 버튼 접근 가능.
- 시각 변경 후 전력 기간이 갱신되며 원격 차량 명령은 발생하지 않음.

### Task 1: 두 단계 선택 흐름
**Files:** domain/Journey.kt, 신규 domain/DepartureSchedule.kt, ui/CampilotViewModel.kt, ui/JourneyScreen.kt, 신규 ui/DepartureScreen.kt, MainActivity.kt, 관련 단위/UI 테스트.
**Interfaces:** DepartureSchedule.from(now: ZonedDateTime, minuteOfDay: Int), VM.select / chooseDepartureTime / confirmSchedule / backToSchedule; JourneyActions에 시간 관련 콜백 추가.
- [x] 첫 화면 단순화와 별도 시간 선택 화면 테스트를 작성하고 기존 구현에서 실패를 확인한다.
- [x] 날짜 계산, SCHEDULE 상태, 현지 시계 및 선택값 보존, 장소/시간 화면을 구현한다.
- [x] 기존 여정 테스트에 새 단계를 반영하고 날짜 경계 단위 테스트 및 UI 회귀 검증을 실행한다.
- [x] Pleos 에뮬레이터에서 두 화면을 확인하고 최신 APK와 작업 보고서를 저장한다.
- [x] 독립 최종 리뷰와 필요한 수정 검증을 마친다.

## 진행 기록
- Ruling: 기존 사용자 자율 구현 승인에 따라 계획 승인 대기 없이 직접 구현한다.
- Ruling: Git 저장소가 없어 커밋·worktree 기반 스크립트는 생략하고 이 문서에 증거를 보존한다.
- Ruling: GPS/실시간 추천은 이번 화면 수정 범위에 포함하지 않는다. 시연 출발지 및 거리임을 표시한다.
- Ruling: 도착 예상 시각이 없는 목업이므로 현재~내일 출발까지를 보수적인 사용 기간으로 계산하며 실제 체류 시간과 다를 수 있음을 전력 상세에 표시한다.
- Pre-flight: 시간 선택과 전력 계획은 CampPlan.hours를 공유하므로 confirmSchedule에서만 기간을 적용한다.
- Task 1: complete — 변경 전 PlaceTimeFlowTest 2개 실패(위치/시간 단계 부재), 변경 후 통과.
- Final review: place_time_review 독립 읽기 전용 리뷰 완료.
- Final: Ruling: 리뷰의 자정 소수 초 절삭을 필수 수정으로 상향했다 — 선택한 미래 시각에 0시간 계획이 생겨 양수 기간 계약을 위반하기 때문. 수정은 날짜 계산 한 곳으로 제한했다.
- Final: fixed fractionalSecondBeforeMidnightStillHasPositiveBudget — RED(0시간) → GREEN, 최종 단위 25/25·기기 30/30 통과.
- 시간 변경 → 전력 사용 기간/예산 반영 및 내비 미호출 회귀 검증 추가.
- Final: Ruling: 프로세스 종료 복원은 기존과 동일하게 이번 범위 밖이다 — Activity 재생성과 화면 이동 보존은 검증했다. 종료 뒤 새 여정부터 시작할 수 있다.
- Final: Ruling: 기존 에너지 정책·실시간 GPS 평가를 별도 후속 범위로 남겼다 — 이번 수정은 목업과 시간 선택 흐름이며 실제 사용 가능성으로 확대 해석하면 안 된다.
- Final: 기기 레이아웃은 작성자가 Pleos 화면 및 375dp/1.5배 테스트로 확인. 직접 선택 08:45 반영을 수동 확인.
- 미해결 리뷰 항목 없음.
- 최종 APK: artifacts/Campilot-place-time-debug.apk. SHA256 061BD043F4A38A5A056FEAD1B5FF953CED34CFF0B1AB0FF9F464C415E75AC731.
- Gradle workflow e5eab01ac1fe07a0d5ca8ab6e0660081 finish 완료. wrapper 소유 로그만 정리됨.

### 검증 질문과 관측 결과
1. "Do the new place and tomorrow-time UI requirements fail on the previous flow?" — 예. 기기 테스트 2개가 기존 위치/시간 화면 부재로 실패.
2. "Do the place-time flow, date boundaries, existing journey tests, APK build and lint pass?" — 예. 24 단위 + 29 기기 테스트, 빌드/린트 exit 0.
3. "Does the fractional-second midnight regression reproduce zero duration?" — 예. 해당 단위 테스트 실패로 재현.
4. "After preserving fractional seconds, do all unit and device tests, APK build and lint pass?" — 예. 25 단위 + 30 기기 테스트, 빌드/린트 exit 0. 기존 도구 deprecation 경고 존재.

