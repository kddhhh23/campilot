# Campilot Pleos 전용 위치 연결

작성: 2026-11-14

## 관련 기능 코드 정리 — 최신 변경

버튼 제거 후 남아 있던 VehicleMode/setMode, DemoVehicleGateway 및 가상 충전·배터리 소모·공조 시나리오·시간 가속 경로를 제거했다. 시연 전용 차량 모델과 시연 테스트 파일의 구현도 비웠다. 야간 색상과 테마 전환 매개변수를 제거하고 기본 테마만 남겼다. 차량 연결 상세 화면과 전환용 이벤트도 제거했다. Pleos 위치·차량 조회와 수동 온도 요청, 추천 장소 목업은 유지한다.

폐기된 시연 기능에만 의존하던 테스트는 제거하고, 나머지는 Pleos 기준으로 정리했다. 최초 컴파일에서 남은 시연 테스트 참조가 발견되어 제거 후 재확인했다. 최종 검증 질문 “After clearing obsolete simulation sources, do build and the energy/route unit tests pass?” — assembleDebug, 테스트 소스 컴파일 및 EnergyTest/JourneyTest **16개 통과**. 기기 테스트·전체 테스트·린트는 실행하지 않았다. APK 갱신과 에뮬레이터 설치 완료. wrapper finish는 자체 로그만 정리했다.

사용자의 최소 검증 원칙은 프로젝트 AGENTS.md에 기록했다.

## 상단 메뉴 정리 — 이전 변경 기록

후속 요청에 따라 차량 연결·시연해 보기·어둡게 버튼과 상단 연결 대화상자를 제거했다. 첫 화면 상단에는 Campilot 이름만 표시한다. 테스트에서는 시연 상태를 직접 준비하도록 변경했다.

검증 질문 “Do the simplified header build and affected UI flows pass without the three controls?” — assembleDebug 및 영향받는 UI 테스트 9개 통과. 최신 APK를 갱신하고 에뮬레이터에 설치했다. gradle-run finish는 wrapper 소유 로그만 정리했다.

## 최종 구현

사용자의 ‘위치는 Pleos API만 사용’ 지시에 따라 Android LocationManager·GPS/네트워크 리스너·Android 위치 권한 및 설정 진입 코드를 제거했다.

현재 위치는 기존 공식 의존성 **ai.pleos.playground:NaviHelper:3.0.5**의 getCurrentLocationInfo()와 NaviHelperEventListener.onCurrentLocationInfo()로만 조회한다. 필요한 pleos.car.permission.NAVI_ROUTE는 이미 선언되어 있다.

화면 활성화 시 SDK 연결 → 리스너 등록 → 위치 요청, 이후 5초마다 조회한다. 화면 이탈 시 조회 취소·리스너 제거·release한다. 취소 이후 도착한 초기화 응답은 무시한다.
주소와 좌표를 그대로 표시하며 좌표 범위/유한수 검사를 거친다. 빈 주소의 (0,0)은 미수신 값으로 처리한다. 응답 오류·위치 미수신·15초 이상 응답 부재는 확인 대기 상태로 표시한다. Nav API에는 측정 시각/정확도가 없으므로 실제 GPS 측정 신선도·정확도를 추정하지 않는다.

Android 위치 API 또는 고정 좌표로 대체하는 경로는 없다. 위치 조회 자체는 길 안내 요청을 보내지 않는다.
추천 장소 3곳과 거리는 목업이며 화면에 ‘시연 거리’로 표시한다.

## SDK 선택 근거

공식 문서에는 차량 위치 전용 FusedLocation 1.1.2가 있다. 해당 의존성을 공식 Maven 저장소에서 해석하려 했으나 Could not find 오류로 받을 수 없었다. 구성 캐시 오류가 실제 의존성 오류를 가렸기에 --no-configuration-cache로 원인을 확인했다. 불필요한 의존성은 제거했다.

대신 이미 설치되고 공식 문서에 명시된 Pleos NaviHelper 현재 위치 API를 사용했다. Android API로 돌아가지 않았다.

## 검증

- 전체 실행: assembleDebug, testDebugUnitTest, connectedDebugAndroidTest, lintDebug — exit 0.
- 위치 데이터 검증 단위 테스트 3개 추가. 단위 합계 **28개 통과**.
- 기존 기기 테스트 전체 실행 통과. 이후 늦은 초기화 방어 추가 후 빌드·단위 테스트와 장소/시간 기기 테스트 2개 재실행 — exit 0.
- SDK 현재 위치 수신을 Pleos 에뮬레이터에서 직접 확인: **수정구 시흥동**, 위도 **37.41272**, 경도 **127.09468**. 주소·좌표는 앱에 하드코딩하지 않았다.
- 앱 소스 검색 결과 Android 위치 API 호출 및 ACCESS_FINE/COARSE_LOCATION 선언 없음.
- 에뮬레이터 값은 Pleos 내비 제공자의 값이다. 이전 Android 에뮬레이터 geo fix 변경 검증과 구분한다. 실차 위치 수신 품질은 실차 확인이 필요하다.
- 기존 테스트/도구 deprecation 경고 존재. gradle-run finish는 wrapper 소유 로그만 정리했다.

검증 질문:
1. “Can the official Pleos FusedLocation 1.1.2 dependency be resolved?” — 실패.
2. “What dependency error is masked by configuration-cache serialization?” — FusedLocation 의존성을 찾지 못함.
3. “Does the Pleos-only location implementation pass build, unit and device tests, and lint?” — 통과.
4. “After guarding late SDK initialization, do build, location unit tests and place-time lifecycle tests pass?” — 통과.

## 결과물
- [최신 APK](../artifacts/Campilot-pleos-location-debug.apk)
- [화면](../artifacts/Campilot-pleos-location.png)

## 공식 근거
- [NaviHelper 현재 위치 조회](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-current-location-info)
- [이벤트 리스너](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/event-listeners)
- [FusedLocation 소개](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)
