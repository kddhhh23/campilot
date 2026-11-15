# Campilot 내비 연결·이동·도착 화면 구현

작성: 2026-11-15

최신 변경: 안내를 도중에 종료한 경우에도 최신 P단이면 ‘차박 시작’을 허용한다. 실제 도착 플래그는 유지한다. 아래의 이전 검증 기록과 차이가 있는 현재 조건·SDK·과제는 [최신 구현 현황](Campilot_구현현황_SDK_추가과제.md)을 참고한다.

## 후속 변경: 안내 종료 시 자동 복귀 (2026-11-15)
사용자 요청에 따라 자동 동시 배치 대신 **내비 안내 종료 → Campilot 큰 화면 복귀**를 구현하고 에뮬레이터에서 직접 확인했다. 이동 시 portrait로 축소하는 요청은 제거하고 landscape를 유지한다.

실제 앱에서 ‘바로 출발’ → 실제 내비 안내 시작 → 내비의 ‘안내 종료’를 누른 결과, 추가 앱 실행 조작 없이 Campilot이 앱 영역에 다시 표시됐다. ‘안내를 종료했어요 / 다시 안내받기’ 화면을 확인했다. 도착 전 수동 종료이므로 navigationArrived=false를 유지하고 차박 시작은 허용하지 않는다. 목적지 도착 이벤트와 안내 종료 이벤트를 구분한다.

처음에는 자동 복귀가 되지 않았다. SDK 연결을 한 세션으로 정리한 뒤에도 시스템 로그에서 앱이 cached 상태로 동결되는 것을 확인했다. 사용자 출발 동작에서만 시작하는 NavigationSessionService를 추가해 내비 SDK 알림 수신을 유지했다. FOREGROUND_SERVICE 및 FOREGROUND_SERVICE_SPECIAL_USE 권한과 specialUse 용도를 선언하고, 이동 알림을 제공한다. Android 위치 권한이나 위치 API는 추가하지 않았다. 앱 복귀/초기화/요청 실패/ViewModel 해제/작업 제거에서 서비스를 정리한다.

최종 실제 복귀 후 `dumpsys activity services com.example.campilot`이 `(nothing)`을 반환해 서비스 종료도 확인했다. 해당 검증은 계측 테스트가 실행 중이지 않은 실제 앱 UI에서 수행했다.

검증: 안내 종료 상태·재연결 시 종료 플래그 초기화·도착 전 차박 방지에 대한 집중 기기 테스트 1개 통과. 최종 foreground session 추가 빌드 통과. 실제 내비 종료 후 UI 자동 복귀 및 서비스 정리 확인. 도로 주행에 따른 목적지 도착은 여전히 미검증이다. 다른 차량의 백그라운드 실행 정책까지 보장하는 결과는 아니다.

Gradle 질문 ‘Do the app and focused navigation-end state test compile after adding return-on-guidance-end?’, ‘Does the app build after sharing the active NaviHelper connection through guidance end?’, ‘Does the app build with a navigation-scoped foreground session to prevent cached freezing?’는 모두 성공이다. wrapper finish는 wrapper 소유 로그만 정리했다.

참고: [Android foreground service 시작](https://developer.android.com/develop/background-work/services/fgs/launch), [specialUse 타입](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use).

## 최초 구현 결과와 미완료 범위
출발 버튼 → 실제 Pleos 경로 요청 → 안내 확인 → 이동 상태 화면을 연결했다. 목적지 도착 이벤트 수신 시 Campilot 전면 복귀와 landscape를 요청하고, 최신 P단 확인 후 차박 시작을 허용한다.

**요청한 전체 UX가 검증 완료된 것은 아니다.** 현재 Pleos Connect v2.1.0 에뮬레이터에서는 내비가 앱 영역 전체를 차지해 Campilot을 덮는다. 런타임 portrait 요청은 Activity의 콘텐츠 방향만 바꾸고 시스템 task 분할 영역을 축소하지 않았다. 따라서 **큰 내비 + 작은 Campilot 자동 동시 배치**는 미해결이다. 실제 목적지 도착에 따른 전면 복귀도 도로 주행으로 재현하지 않았다.

## 사용자 흐름
1. 기존 위치·차박지·집·시간·전력 확인을 마친다.
2. ‘바로 출발’ 또는 충전 경유 출발을 누르면 실제 NaviHelper.requestRoute/requestRouteWithWaypoints를 호출한다.
3. 요청한 목적지·경유지와 ROUTE_GUIDING 상태가 확인되면 이동 단계로 전환한다. 20초 안에 확인되지 않은 경우 기존 수동 경로 확인 절차를 유지한다.
4. 이동 화면에는 ‘차박지로 이동 중’ 및 선택한 장소를 표시한다. 충전 경유·귀가는 각각 구분한다.
5. 실제 onDestinationArrived 또는 해당 경유지 onWayPointArrived 콜백을 받으면 도착 상태로 전환한다.
6. 차박지 도착 이후 최신 차량 정보에서 P단이면 ‘차박 시작’이 활성화된다. 버튼을 누르기 전에는 차박 상태로 자동 진입하지 않는다.

## 구성
- NavigationEvent.kt: 이벤트 종류와 목적지 대조 게이트. 목적지 조회 좌표가 일치해야 이름을 대조해 도착을 허용한다. 다른 목적지·취소·미조회 상태는 거절한다.
- PleosNavigator.kt: 여정의 활성 NaviHelper를 경로 확인·도착/종료 이벤트·Foreground 요청이 공유한다. 요청 리스너는 요청 후 제거하고, 공유 helper는 callbackFlow 수집 종료 시 해제한다. 단독 경로 요청에는 임시 연결을 사용한다. 별도 showNavigation은 NavigationScreenRequest.Foreground를 사용한다.
- CampilotViewModel.kt: 도착 Flow와 세대 번호를 소유한다. 새 요청·초기화·차박 시작·귀가 완료·ViewModel 해제에서 정리한다. 내비가 앱을 덮더라도 필요한 SDK 도착 리스너만 유지한다. 창 상태는 travelWindow/navigationArrived로 노출한다.
- MainActivity.kt: UI 렌더링은 기존 Compose 화면이 담당한다. Activity lifecycleScope의 별도 상태 수집은 화면 비가시 상태에서도 명시적인 내비 핸드오프·도착 복귀 요청을 유지하며 Activity 소멸 시 취소된다.
- TravelScreen.kt: SDK/Activity 객체 없이 상태·콜백만 받는 화면이다. 안내 종료와 연결 실패를 구분하고 재연결을 제공한다.
- NavigationSessionService.kt: 활성 이동 중 SDK 이벤트 수신을 위한 사용자 인지 가능한 foreground session. 자체 위치 조회·주기 폴링은 하지 않는다.
- Manifest: 초기 landscape와 orientation/screenSize/smallestScreenSize 설정 변경 처리를 선언했다.

이벤트 Flow는 경로별 단일 ViewModel 소비자가 수집하며, 도착 여부는 StateFlow 상태로 보존해 UI collector 공백 중에도 잃지 않는다. 프로세스 종료 후 전체 여정 복원은 기존 프로토타입의 후속 과제다.

## SDK 확인 중 수정한 문제
에뮬레이터의 실제 안내 시작이 요청 후 약 8초 뒤에 발생해 기존 6초 확인 창을 벗어났다. 확인 시간을 20초로 늘렸다.

추가 읽기 진단 결과:
- 목적지 좌표 일치: true
- 목업 장소 이름: true
- 경유지 수: 0
- isRouting: false
- naviMode: ROUTE_GUIDING

따라서 isRouting만으로 안내 여부를 판단하지 않고 명시적인 ROUTE_GUIDING 모드를 사용한다. NavigationStatus.ROUTE_DRIVE 이벤트도 처리한다. 공급자 3.0.4가 실제로 보내는 구형 onRouteEnded 콜백도 지원하며 최신 onRoutingCompleted와 동일한 조회를 수행한다.

## 창 제어 제한
공식 문서의 portrait/landscape 모델을 따라 방향을 요청하지만, 이것이 다른 앱의 task 배치·크기를 제어하는 API는 아니다. 에뮬레이터 dumpsys에서 Campilot task는 [853,90][2535,1300] 영역을 유지하면서 내부 콘텐츠만 portrait로 바뀌었다. 내비는 같은 앱 영역을 덮어 표시했다.

정확한 자동 분할을 위해서는 대상 차량의 창 관리자 지원 및 공식 앱 간 창 배치 인터페이스 확인이 추가로 필요하다. 숨겨진 API·임의 시스템 권한·ADB 창 조작·가짜 지도는 앱 구현에 넣지 않았다. 실제 차량의 백그라운드 Activity 실행 정책에 따라 도착 시 전면 복귀 요청도 제한될 수 있다.

## 최소 검증 결과
- NavigationArrivalTest: 단위 테스트 3개 통과. 목적지 일치 전 도착 거절, 잘못된 목적지/좌표 거절, 취소 후 도착 거절.
- NavigationHandoffTest: 기기 테스트 1개 통과. 실제 Pleos 배터리/P단 조회 + 제어된 내비 이벤트로 도착 전 차박 방지, 취소 구분, 재연결, 도착 후 차박 전환 확인. 실제 내비 도착을 재현한 테스트는 아니다.
- PleosRouteSnapshotTest: 읽기 진단 1개 통과. 위 공급자 응답 확인.
- PleosNavigationRouteTest: 실제 SDK 경로 요청/안내 확인 1개 통과, 약 8.664초. 수동 확인을 거치지 않고 RouteReceipt.confirmed=true 수신.
- 최종 assembleDebug / assembleDebugAndroidTest 통과.
- 전체 테스트·전체 린트·실제 주행/실제 목적지 도착 시험은 수행하지 않았다.
- 창 분할 수동 확인은 실패 결과를 그대로 기록했다. 자동 동시 표시 성공으로 보고하지 않는다.

Gradle은 gradle-run wrapper로 수행했다. 질문 ‘Do arrival filtering tests pass and do the navigation handoff app and test APK compile?’ 및 최종 ‘Does the final handoff app and actual-provider guidance regression test compile?’의 결과는 성공이다. 중간의 읽기 진단 컴파일 및 콜백 호환성 빌드 질문도 성공이다. finish는 wrapper 소유 로그만 정리한다.

## 결과물
[APK](../artifacts/Campilot-navigation-debug.apk). 최신 앱 APK는 에뮬레이터에 설치했다.

## 공식 자료
- [앱 구조·화면 모델](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)
- [창 정책: Connect-S/Connect-W 차이](https://document.pleos.ai/en/docs/connect/guide/docs-design/getting-started/window)
- [NaviHelper 이벤트](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/event-listeners)
- [목적지·경로 요청 및 결과 확인](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)
- 설치된 NaviHelper 3.0.5 / navi-data 3.0.4 소스의 도착 콜백, Foreground, NaviMode, NavigationStatus를 대조했다.
