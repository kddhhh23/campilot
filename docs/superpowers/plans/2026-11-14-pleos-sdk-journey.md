# Pleos SDK와 전체 차박 여정 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 실제 Pleos Vehicle/NaviHelper SDK를 연결하고 목업 장소 한 세트로 추천→충전 판단→출발/충전 경유→도착→차박 관리→귀가→완료 흐름을 만든다.

**Architecture:** 기존 Compose/ViewModel 구조를 유지한다. SDK 어댑터는 연결·조회·명령 확인·해제를 담당한다. 여정 상태와 목업 장소는 별도 모델로 분리한다. SDK 모드에서는 가상 배터리 차감/가상 제어 성공을 사용하지 않는다. 명시적으로 선택한 목업 모드는 SDK 없는 환경에서도 전체 흐름을 확인할 수 있다.

**Tech Stack:** 기존 Android/Compose/StateFlow/DataStore + ai.pleos.playground:Vehicle:2.3.2 / NaviHelper:3.0.5 (공식 Maven).

**Spec:** 사용자 첨부 contest-application-consent.docx 2부 1~6항 및 2026-11-14 요청. 장소/거리/경로 상세는 목업 허용. 출시·실차 인증·예약·개인화 고도화는 이번 범위 아님.

## Global Constraints
- 자동 제어 시작은 명시적 사용자 동작. 주차·권한·지원 범위·최신 관측을 확인하며 실패를 성공으로 표시하지 않는다.
- 실제 SDK 통신과 목업 데이터를 화면에서 구분한다. SDK가 실패해도 자동으로 목업으로 전환하지 않는다.
- 공조 소비 1kW/0.12kW 절감은 실측값으로 취급하지 않는다. SDK 모드에서는 온도 변경만으로 잔량/소비를 감소시키지 않는다.
- 장소·집·충전소 좌표는 시연용이며 실장소의 숙박/충전 가능성을 보장하지 않는다.
- 차량 기본 캠핑 모드 자체는 제어하지 않는다. 시연 종료 기준 20% + 여유 2%는 수정 가능한 계획 가정이다.
- 앱 백그라운드에서는 자동 제어 중단. 밤샘 무인 제어 보장 범위 제외.

## Review Focus
1. SDK 실패/미설치/권한 거부에서 가짜 연결·성공 금지.
2. 취소/화면 이탈/모드 전환 후 늦은 응답이 여정·제어를 되살리지 않음.
3. 충전소 도달 불가·목표 SOC 100% 초과·귀가 잔량 부족을 정상으로 표현하지 않음.
4. SDK 관측 배터리와 목업 이동·충전·절감 수치가 섞이지 않음.
5. 귀가 버튼 중복·뒤로가기·재생성 시 여정 중복 또는 자동 제어 재시작 금지.

## Task 1: 실제 SDK 어댑터
Files: settings.gradle.kts, app/build.gradle.kts, AndroidManifest.xml, vehicle/PleosVehicleGateway.kt, navigation/PleosNavigator.kt.
Interfaces: SDK 상태는 StateFlow, suspend connect/refresh/requestTemperature/requestRoute, close()로 해제. null은 관측 없음.
- [x] 공식 artifact와 API signature/권한 확인, 의존성 해석 및 컴파일.
- [x] 배터리 SOC·온도·주차 읽기 및 capability 확인, SDK set 후 재조회 확인. 지원 불가/타임아웃 표시.
- [x] NaviHelper 목적지·경유 요청 및 결과 콜백 처리.
- [x] 에뮬레이터 연결 smoke test와 미지원/권한 오류 테스트.

## Task 2: 전체 여정과 에너지 예산
Files: domain/Journey.kt, ui/CampilotViewModel.kt, domain/EnergyCalculator.kt, domain/CampModels.kt, test/JourneyTest.kt.
Interfaces: JourneyStage 추천/예산/충전소이동/충전/목적지이동/차박/귀가/완료. MockTrip 한 세트가 캠핑장 후보·집·충전소 좌표와 거리를 제공. live/mock 명시적 모드.
- [x] 여정 전이·충전 목표·도달 불가·캠핑 종료 하한·귀가 에너지 테스트를 먼저 실패로 확인.
- [x] 상태 머신과 계산 구현. SDK 모드에서는 실제 관측만 차량 값으로 반영.
- [x] 단위 테스트 통과 확인.

## Task 3: 화면 연결과 검증
Files: MainActivity.kt, ui/JourneyScreen.kt, androidTest/JourneyFlowTest.kt, docs/Campilot_Pleos_SDK_구현_보고서.md.
Interfaces: 화면은 immutable UiState와 이벤트 콜백을 받고 VM이 SDK를 소유.
- [x] 추천 우선 진입, 간단 설정, SDK 상태, 경유 경로, 충전 완료 확인, 도착/주차, 차박, 귀가/완료 UI.
- [x] 목업 전체 여정 UI 테스트와 SDK 실패 표시 테스트.
- [x] assembleDebug/testDebugUnitTest/connectedDebugAndroidTest/lintDebug, 실제 에뮬레이터 SDK 상태 확인.
- [x] 독립 최종 코드 리뷰 후 중요 문제 수정, APK/보고서 갱신.

## 실행 기록
- Ruling: 사용자가 전체 구현을 맡겼으므로 별도 계획 승인 대기 없이 진행한다.
- Ruling: 기존 기록상 Git 저장소가 없으므로 workspace 직접 수정 및 IDE 충돌 검사를 사용하며 커밋/worktree 스크립트는 적용하지 않는다.
