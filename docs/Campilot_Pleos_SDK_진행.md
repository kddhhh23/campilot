# Pleos SDK 전체 여정 진행 기록

계획: docs/superpowers/plans/2026-11-14-pleos-sdk-journey.md

- 공식 Vehicle 2.3.2/NaviHelper 3.0.5 의존성 다운로드·컴파일 성공. IDE Gradle sync 성공.
- SDK AAR 실제 선언에서 SOC Int?, 온도 Float?/SeatArea/TemperatureUnit, 3인자 capability 콜백 및 권한 확인.
- SDK 자체 manifest가 서비스 intent visibility와 VEHICLE_BINDING을 제공하므로 앱의 추정 package queries는 제거.
- JourneyTest 7개 실패 확인 후 계산/ControlGuard 구현. 전체 단위 테스트 성공.
- Ruling: 테스트용 항상 허용 stub이 자동 승인 검토에서 거절됨. 미구현 시 예외/제어 false로 닫힌 stub으로 수정하고 RED→GREEN 검증. 우회 없음.
- Ruling: 공개 SDK의 Gear API는 deprecated지만 존재하며 CAR_DRIVE 권한을 요구한다. 이 프로토타입은 P단 확인에 사용하며 실패·null이면 제어 금지한다. 후속 AAOS 이전 후보.
- Ruling: 실제 소비 전력을 확인하지 못한 상태에서 가상 절감량으로 실차 자동제어하지 않는다. SDK 모드는 온도 수동 명령/재조회 확인, 자동 제어는 목업에서 시연한다. SDK 화면에 이유를 표시한다.
- Ruling: 세부 장소/거리/충전 계획은 사용자 허용에 따라 목업 한 세트. 차량 수치를 목업으로 자동 대체하지 않는다.
- PowerShell이 -Pandroid.testInstrumentationRunnerArguments.class를 분리하므로 전체 인자를 따옴표로 전달.
- Pleos Connect v2.1.0 에뮬레이터에서 공식 Vehicle IPC SOC=100%, P단=true 확인. HVAC_TEMPERATURE_SET은 PropertyNotAvailableException으로 응답하여 해당 환경 공조 조회/제어를 비활성화했다.
- 실제 NaviHelper 요청 후 네이티브 내비게이션에 목업 경유 충전소 안내가 표시됨. 그러나 완료 콜백이 없고 재조회도 목적지 0,0/경유지 없음/isRouting=false로 반환됨.
- Ruling: 경로 요청과 경로 확정을 별도 RouteReceipt로 구분한다. 상태 응답 미확인 시 여정을 자동 진행하지 않고 사용자의 내비게이션 목적지·경유지 확인을 요구한다. 비용: 현재 에뮬레이터에서 확인 버튼 1회 추가.
- Ruling: 네이티브 내비게이션 화면 전환으로 Activity가 배경으로 이동해도 사용자가 요청한 경로 작업은 유지한다. 자동 공조는 계속 중단하며, 여정 초기화/모드 전환 시 경로 작업과 늦은 응답을 취소한다.
- Ruling: 앱 여정 초기화는 네이티브 안내 종료 명령을 보내지 않는다. 실제 내비게이션에서 별도로 종료하도록 UI에 안내한다.
- Ruling: 캠핑 하한 22%는 이번 목업의 고정 정책값으로 유지한다. 사용자 설정 UI는 후속으로 미루며, 차종별 실제 캠핑 종료 기준으로 확정하지 않는다.
- 단위 19개 및 초기 화면/안전/여정 계측 15개 통과(0007). 실제 SDK 테스트에서 콜백 부재를 재현(0008/0009)한 후 요청/확인 분리로 수정. 최종 전체 재검증 완료.
- Final: 독립 리뷰에서 목적지 변경/뒤로가기 시 이전 경로 결과가 남는 문제 발견. invalidateRoute를 모든 이탈 경로에 적용.
- Final: 귀가 충전 경유 거리와 직접 귀가 거리의 불일치를 명시적 방향별 거리로 통일.
- Final: 위 두 결함 RouteLifecycleTest에서 0011 RED(4개 중 2개 실패) → 0012 GREEN(전체 단위 19/19, 계측 21/21).
- Final: assembleDebug/testDebugUnitTest/connectedDebugAndroidTest/lintDebug 모두 종료 코드 0. Lint 오류 0, 경고 34.
- Final: 리뷰에서 판단 보류한 실제 HVAC 작동은 미지원 환경으로 검증 미완료임을 보고서에 명시. 네이티브 경로 자동 확정 부재는 사용자 확인으로 처리. 실좌표 정확도·밤샘 실행·프로세스 복원은 목업 프로토타입 범위 밖이며 후속 과제.
- Final: 리뷰 미해결 중요 결함 없음. 의존성/백업/리소스 Lint 경고는 이번 시연 기능과 분리하여 후속 정리 대상으로 기록.
- Task 1/2/3: 완료. 실제 공조 작동과 실측 자동 제어는 구현 보고서의 검증 제한을 따름.
