# Navigation handoff Implementation Plan

**Goal:** 출발 시 실제 Pleos 내비를 열고 작은 Campilot 이동 화면을 표시하며, 목적지 도착 이벤트 후 큰 화면에서 차박을 시작한다.
**Architecture:** NaviHelper 이벤트 Flow는 ViewModel이 경로 요청부터 도착/취소까지 수집한다. Activity는 lifecycleScope 상태 수집으로 이동/도착 상태에 따른 본인 창 방향과 전면 복귀를 요청한다. Compose 수집이 중단된 동안에도 도착 핸드오프는 유지한다. 화면은 상태와 콜백만 받는다.
**Tech Stack:** 기존 NaviHelper 3.0.5, Kotlin Flow, Activity, Compose.
**Spec:** 2026-11-15 사용자 요청: 내비 크게, 앱 작게, 도착하면 앱 확대 및 차박 시작.

## 제약
- 실제 Pleos 위치/내비 API만 사용한다. 장소는 기존 목업.
- 창 비율과 다른 앱 창 크기는 시스템 소유다. portrait/landscape와 NavigationScreenRequest.Foreground만 사용하며 숨겨진 API·셸 창 조작을 앱 코드에 넣지 않는다.
- 경로 요청 제출과 안내 확인을 구분한다.
- 도착 콜백을 실제 목적지 조회와 대조한다. P단만으로 도착 처리하지 않는다.
- 주차 상태가 최신이고 목적지 도착이 확인돼야 차박을 시작한다.
- 기존 10% 예비 배터리는 유지한다.
- 검증은 빌드, 변경한 도착 판정 단위 테스트, 필요한 SDK/창 수동 확인에 한정한다.

## 변경 파일
- navigation/NavigationEvent.kt: 준비·도착·취소 이벤트와 목적지 일치 판정.
- navigation/PleosNavigator.kt: 경로와 별도로 유지되는 도착 리스너, 내비 Foreground 요청.
- ui/CampilotViewModel.kt: 모니터 수집/해제, 도착 상태, 실제 도착 후 차박 게이트.
- ui/TravelScreen.kt: 이동 중/도착의 간결한 표시.
- ui/JourneyScreen.kt: 이동 단계는 TravelScreen으로 분기.
- MainActivity.kt, AndroidManifest.xml: 창 방향 요청과 설정 변경 상태 보존.
- test/.../NavigationArrivalTest.kt: 목적지 일치·빈 이름·잘못된 좌표 판정.

## 구현 순서
- [x] SDK 이벤트 필터와 도착 리스너를 추가했다. 취소를 도착으로 처리하지 않는다.
- [x] ViewModel이 경로별 리스너를 소유하고 새 요청/초기화/여정 전환/해제에서 정리한다.
- [x] 작은 창용 이동 화면과 도착 화면을 연결했다. 도착 전 시작 버튼을 차단하는 집중 상태 테스트가 통과했다.
- [x] Activity가 이동 시 portrait+내비 전면, 도착 시 landscape와 전면 복귀를 요청한다.
- [x] 빌드·도착 필터 3개·상태 전환 1개·실제 SDK 경로 확인 1개·읽기 진단 1개를 확인하고 보고서를 작성했다.
- [ ] **자동 분할 배치 미해결:** 현재 에뮬레이터는 방향 변경으로 task 분할 크기를 바꾸지 않는다. 내비가 Campilot을 덮는다.
- [ ] 실제 목적지 도착 시 자동 전면 복귀는 주행으로 검증하지 않았다.

최종 결과/관측: [내비 화면전환 구현 보고서](../../Campilot_내비_화면전환_구현_보고서.md). 전체 요청 완료로 표시하지 않는다.

## 실패 모드
다른 목적지 도착·경로 취소는 차박 도착으로 처리하지 않는다. 늦은 콜백은 요청 세대로 무시한다. 리스너 연결 실패는 출발 실패로 표시한다. 실제 내비가 도착 이벤트를 제공하지 않으면 자동 복귀가 보장되지 않으며 임의 거리/시간으로 도착을 조작하지 않는다. 앱 프로세스 종료 후 전체 여정 복원은 기존 프로토타입의 후속 과제다.

사용자는 구현을 이미 위임했으므로 이 세션에서 직접 구현을 이어간다.
