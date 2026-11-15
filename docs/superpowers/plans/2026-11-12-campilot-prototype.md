# Campilot 프로토타입 Implementation Plan

> **구현 상태 (2026-11-12):** 가상 차량 프로토타입 구현 및 빌드·단위/계측 테스트를 완료했다. 아래 체크리스트는 원래 계획을 보존한 것으로 완료 여부의 최신 기준은 [구현 보고서](../../Campilot_프로토타입_구현_보고서.md)다. 파일 축약·실제 SDK 미연동·폼팩터 검증 한계 등 변경 사항도 보고서에 기록했다.

**Goal:** 캠핑장 선택부터 다음 이동까지의 전력 예산을 공유하는, 네 기능이 연결된 재현 가능한 차박 시연 앱을 만든다.

**Architecture:** 단일 app 모듈에서 Kotlin 계산·제어 정책과 Android 화면을 분리한다. UI는 ViewModel의 상태를 표시하고, 차량·위치·캠핑장 데이터는 인터페이스 뒤의 가상 구현으로 제공한다. 실제 Pleos 구현은 같은 인터페이스를 교체하는 후속 단계다.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 기반 커스텀 테마, Navigation 3, ViewModel, Coroutines/StateFlow, DataStore, JUnit 및 Compose UI test를 제안한다. 이 선택은 Pleos 필수 요건이 아니다. 정확한 버전은 기반 작업에서 기존 AGP와 Pleos 에뮬레이터 호환성을 확인하여 고정한다.

**Spec:** [기획안 v1.0](../../Campilot_기획안_v1.0.md), [Pleos 규칙](../../Pleos_앱_개발_필수규칙.md)

## Global Constraints

- 첫 시연은 가상 캠핑장 3곳, 가상 차량, 실제 계산 로직, 모의 공조 제어로 구성한다.
- 캠핑장 중심 추천, 전력 예산 연동 공조, 다음 목적지까지의 이동 에너지 포함.
- 예비 배터리 기본 10%는 제품 초기값이며 안전 보장 기준이 아니다.
- 이동·기본 전장·공조만 계산하며 V2L, 예약·결제·로그인, 실차 밤샘 제어는 제외한다.
- 모든 화면에 시연 모드를 표시하고 측정값·입력값·추정값의 출처를 구분한다.
- 가로·세로 창 변경에서 기능과 상태를 보존하며 Pleos 화면·IME·CCP 정책을 우선한다.
- 자동 제어는 사용자 시작 후에만 동작한다. 수동 변경·주행 전환·연결 끊김·오래된 값·권한 부족 시 중단한다.
- 앱 재실행 후 자동 제어는 저절로 재개하지 않는다.
- 기존 기획안의 달력 일정은 이번 계획에서 확정하지 않는다. 아래 작업 순서와 완료 기준으로 진행한다.

## 현재 코드 상태

2026-11-12 IDE 조회 기준:

- app/build.gradle.kts: AGP 9.5.1, compile/target SDK 37, minSdk 28, Java 11 설정.
- Compose·Navigation·ViewModel 의존성은 아직 없고 AppCompat/Material 의존성이 있다.
- app/src/main/AndroidManifest.xml에 Activity가 없다.
- 앱 Kotlin 소스는 기본 단위·계측 테스트만 확인된다.
- 현재 빌드 성공 여부와 Pleos 에뮬레이터 설치 여부는 이번 작업에서 검증하지 않았다.
- 스킬 설치는 앱의 Gradle 의존성이나 Manifest를 변경하지 않았다.

## 구현 파일과 책임

아래 Kotlin 경로 기준은 app/src/main/java/com/example/campilot/ 이다. 실제 코드가 추가되기 전의 예정 구조다.

| 파일 | 책임 |
|---|---|
| MainActivity.kt, CampilotApp.kt | 실행 진입점, 앱 의존성 조립, 내비게이션 |
| model/CampModels.kt | 계획·차량 프로필·캠핑장·시각·출처 모델 |
| energy/EnergyCalculator.kt | Android에 의존하지 않는 전력 계산 |
| energy/CampsiteRanker.kt | 계획 충족 여부와 경로 거리 기반 정렬 |
| vehicle/VehicleGateway.kt, DemoVehicleGateway.kt | 관측값과 공조 명령, 결정적 가상 차량 |
| climate/ClimatePolicy.kt, ClimateController.kt | 변경 제안 정책과 명령 실행·확인 상태 |
| data/CampsiteRepository.kt, DemoCampsiteRepository.kt | 캠핑장 3곳 및 가상 출발 위치 |
| data/PlanStore.kt | 계획 저장·복구, 진행 중 자동 제어는 복원하지 않음 |
| demo/DemoScenario.kt, DemoClock.kt | 가속 시간, 정상·과소비·오류 시나리오와 초기화 |
| ui/CampilotViewModel.kt | 단일 시연 세션의 상태 및 사용자 이벤트 |
| ui/plan/PlanScreen.kt | 체류·다음 이동·온도 범위 입력 |
| ui/campsites/CampsiteScreen.kt | 조건·추천 이유·전력 여유 비교 |
| ui/budget/BudgetScreen.kt | 에너지 내역과 충전·대안 |
| ui/dashboard/DashboardScreen.kt | 현재값·예측·제어 상태·중지 |
| ui/theme/, ui/components/ | 주야간 색·글자·공통 상태 표시 |

## 계산과 시연 계약

- 에너지 단위 kWh, 평균 전력 kW, 거리 km, 전비 km/kWh, 체류 시간 h로 통일한다.
- B = 사용 가능 배터리 용량. E현재 = B × SOC / 100.
- E이동 = 캠핑장 경로 거리 / 전비. E다음 = 다음 목적지 경로 거리 / 전비.
- E체류 = 남은 시간 × (기본 전력 + 공조 전력). 총소비 전력 입력 모드는 합계에 기본 전력을 다시 더하지 않는다.
- E예비 = B × reservePercent / 100.
- E추가 = max(0, E이동 + E체류 + E다음 + E예비 − E현재).
- 도착 잔량과 차박 종료 잔량을 별도로 표시한다. 다음 이동·예비분은 출발 가능성 판단에 반영하며 이미 소비한 에너지처럼 이중 차감하지 않는다.
- 도착 후 계산은 현재 관측 잔량으로 시작하고 남은 체류와 다음 이동만 포함한다.
- 소비 범위의 상한으로 충전 필요 여부를 판단한다. 같은 시나리오에서 임의 난수를 사용하지 않는다.
- 부족분이 배터리 최대 용량으로도 해결되지 않으면 “현재 계획으로 불가”를 표시한다. 목표 SOC를 100%로 잘라 충분하다고 표시하지 않는다.
- NaN·무한대·0 이하 전비/용량, 음수 거리/시간, 범위 밖 SOC, 온도 역전은 유효하지 않은 입력이다. 누락값을 0으로 대체하지 않는다.
- 경로상 충전이 필요하면 출발 전 충전 필요와 구분한다. 첫 프로토타입은 충전소 경유 최적화를 구현하지 않는다.

### 고정 시험 데이터

가상 용량 60kWh, SOC 50%, 전비 5km/kWh, 캠핑장 거리 50km, 다음 이동 25km, 체류 8h, 기본 0.2kW, 공조 1.0kW, 예비 10%:

- 현재 30kWh, 이동 10kWh, 체류 9.6kWh, 다음 이동 5kWh, 예비 6kWh.
- 총 요구 30.6kWh, 추가 충전 0.6kWh, 권장 출발 SOC 51%.
- 차박 종료 잔량 10.4kWh, 다음 이동 이후 잔량 5.4kWh로 예비분 미달.
- 위 숫자는 계산 검증용 가상값이며 특정 현대차 제원이나 실제 소비 예측이 아니다.

## Review Focus

1. 한글 소수·빈 입력·유효 범위 오류: 잘못된 숫자로 정상 계산하지 않음 — 작업 1·3.
2. 도착 이후 재계산 및 재실행: 이동 이중 차감과 자동 제어 재개 방지 — 작업 1·4.
3. 지연 명령 응답과 중지의 경합: 중지 후 새 명령 없음, 늦은 응답으로 Running 복귀 없음 — 작업 4.
4. 오래된 값·연결 해제: 정상 예측/성공으로 표시하지 않음 — 작업 2·4.
5. 작은 창·큰 글꼴·키보드: 중지 버튼과 필수 정보 가림 없음 — 작업 3·5.

## 작업 1: 계산 엔진과 단위 테스트

**Files:** model/CampModels.kt, energy/EnergyCalculator.kt 생성.
**Tests:** app/src/test/java/com/example/campilot/energy/EnergyCalculatorTest.kt 생성.
**Interfaces:** calculate(plan: CampPlan, vehicle: VehicleProfile, snapshot: VehicleSnapshot, phase: TripPhase): BudgetResult. TripPhase는 BEFORE_TRAVEL / STAY. BudgetResult는 Valid / MissingData / InvalidInput, Valid에 도착·종료·다음 이동 이후 잔량 범위, 추가 충전량, 목표 SOC, 실현 가능성을 담는다.

- [ ] 위 시험 데이터, 예비 경계, 체류 증가 시 잔량 감소, 총소비 모드 중복 합산 방지, STAY 이동 미차감, 잘못된 입력 테스트를 먼저 작성한다.
- [ ] ./gradlew.bat :app:testDebugUnitTest --tests "com.example.campilot.energy.EnergyCalculatorTest" 실행으로 미구현 실패를 확인한다.
- [ ] 단위가 드러나는 필드 이름과 순수 계산 함수를 구현한다. 표시 반올림은 UI에서 하고 판단에는 원값을 사용한다.
- [ ] 같은 명령으로 모든 사례 통과를 확인한다. Double 비교 허용 오차 1e-6을 사용한다.
- [ ] 변경 파일만 검토하고 논리 단위 커밋을 만든다.

## 작업 2: 가상 차량·캠핑장 추천

**Files:** vehicle/의 두 파일, data/캠핑장 두 파일, energy/CampsiteRanker.kt, demo/의 두 파일 생성.
**Tests:** vehicle/DemoVehicleGatewayTest.kt, energy/CampsiteRankerTest.kt (app/src/test/java/com/example/campilot/ 아래).
**Interfaces:** VehicleGateway.snapshots: StateFlow<VehicleSnapshot>; suspend requestTemperature(celsius: Double): ClimateCommandResult. ClimateCommandResult는 Confirmed / Rejected / Timeout. CampsiteRepository.getCandidates(): List<Campsite>. rank(candidates: List<Campsite>, budgets: Map<String, BudgetResult>): List<CampsiteRecommendation>.

- [ ] 계획 충족 후보 우선→거리순→ID순, 정보 부족 후보의 별도 표시, 동일 시간 진행 결과의 재현성 테스트를 작성한다.
- [ ] 가상 현재 위치와 서로 다른 거리·시설·숙박 허용 상태를 가진 3곳을 만든다. 거리도 시연 데이터로 표시한다.
- [ ] 측정 시각·데이터 출처·연결·권한·주행 상태를 관측 모델에 포함한다. 가상 시간은 테스트에서 직접 전진시킨다.
- [ ] 정상/과소비/연결해제/명령실패 이벤트를 정의하고 시간 가속·초기화 API를 제공한다.
- [ ] ./gradlew.bat :app:testDebugUnitTest 통과 후 검토·커밋한다.

## 작업 3: 실행 기반·네 화면·계획 저장

**Files:** app/build.gradle.kts, gradle/libs.versions.toml, app/src/main/AndroidManifest.xml 수정. MainActivity.kt, CampilotApp.kt, ui/ 전체 및 data/PlanStore.kt 생성.
**Tests:** app/src/androidTest/java/com/example/campilot/CampilotFlowTest.kt; app/src/test/java/com/example/campilot/ui/CampilotViewModelTest.kt.
**Interfaces:** CampilotViewModel.uiState: StateFlow<CampilotUiState>; onAction(action: CampilotAction). PlanStore는 suspend save(plan: CampPlan), suspend load(): CampPlan? 제공.

- [ ] 기존 빌드를 먼저 확인한다. Compose compiler 플러그인·BOM·Navigation 3·lifecycle·DataStore·테스트 의존성을 공식 문서와 현재 AGP 조합으로 검증하고 버전을 고정한다.
- [ ] Compose Activity와 시작 화면을 등록한다. 실제 Pleos API 권한은 아직 선언하지 않는다.
- [ ] 디자인 초안을 문서화한다. UI UX Pro Max는 정보 위계·접근성 검토, frontend-design은 시각 방향 검토에 사용하고 충돌하면 Pleos 규칙을 우선한다.
- [ ] 계획→캠핑장 비교→전력 확인→대시보드 흐름을 만든다. 충전 필요·정보 부족·불가능한 계획을 다른 상태로 표시한다.
- [ ] ViewModel에 공유 상태를 두고 DataStore에 계획을 저장한다. 원시 입력은 검증 후 모델로 변환하며 소수점 처리 규칙을 명시한다.
- [ ] 좁은 창은 세로 흐름, 넓은 창은 주요 정보와 보조 정보를 나란히 배치한다. 주야간 테마, 시연 배지, 텍스트가 있는 경고를 제공한다.
- [ ] 테스트에서 입력 수정→추천/계산 갱신, 뒤로가기 후 값 유지, recreation 후 계획 유지, 잘못된 입력 시 진행 불가를 확인한다.
- [ ] ./gradlew.bat :app:assembleDebug :app:testDebugUnitTest 실행 후, 연결된 테스트 기기에서 :app:connectedDebugAndroidTest를 실행한다.
- [ ] 빌드·핵심 흐름 통과 후 검토·커밋한다.

## 작업 4: 공조 모의 제어와 실시간 모니터링

**Files:** climate/의 두 파일 생성, ui/CampilotViewModel.kt와 ui/dashboard/DashboardScreen.kt 연결.
**Tests:** climate/ClimatePolicyTest.kt, climate/ClimateControllerTest.kt.
**Interfaces:** evaluate(input: ClimatePolicyInput): ClimateDecision (Keep / Adjust / SuggestAlternative / Stop). ClimateController는 start(), stop(reason: StopReason), onManualOverride(), state: StateFlow<ClimateControlState> 제공.

- [ ] 난방은 하향·냉방은 상향, 사용자 허용 범위 유지, 범위 끝의 부족 시 대안 안내 테스트를 먼저 작성한다.
- [ ] 제어 상태를 Off / Running / Applying / Paused / Error로 정의한다. Applying에서는 추가 요청을 보내지 않는다.
- [ ] 초기 정책은 1회 0.5도 조절, 변경 간격 최소 60초, 관측 신선도 10초로 제안한다. 모두 가상 시간 기준이며 실제 차량 정책은 후속 검증으로 대체한다.
- [ ] 충분 상태 복귀에는 에너지 여유 0.5kWh를 요구해 경계 반복 조절을 줄인다. 공조 끄기는 자동 동작으로 제공하지 않는다.
- [ ] 확인 응답이 온 경우에만 적용 완료로 표시한다. 중지 세대 번호 또는 취소 가능한 작업으로 늦은 응답의 재활성화를 방지한다.
- [ ] 중지·수동 변경·주행·권한 거부·연결 해제·관측 노후화·명령 타임아웃 테스트를 가상 시계로 검증한다.
- [ ] 재실행 시 계획만 복원하고 제어는 Off로 시작한다. 앱이 비활성화되면 시연 타이머와 새 자동 명령을 멈추고 복귀 시 재개를 안내한다.
- [ ] 대시보드에서 현재 잔량과 예상 종료 잔량, 소비 변화, 제어 이유·성공/실패를 표시한다.
- [ ] ./gradlew.bat :app:testDebugUnitTest 통과 후 검토·커밋한다.

## 작업 5: Pleos 화면 검증·90초 시연 완성

**Files:** docs/Campilot_시연_가이드.md 생성, 필요한 UI 수정. app/src/androidTest/java/com/example/campilot/CampilotFlowTest.kt 보강.

- [ ] 정상 계획→과소비→부족 경고→자동 조절→개선 또는 대안의 90초 시나리오를 고정한다.
- [ ] 중지 직후 시간 진행에도 추가 명령이 없는 사례와 실패 알림 사례를 시연 조작으로 제공한다.
- [ ] Pleos 에뮬레이터에서 실행 가능 여부를 확인하고 창 전환·시스템 패널·IME·Back을 검증한다. 일반 Android 에뮬레이터 결과를 Pleos 검증으로 대체하지 않는다.
- [ ] 실제 SDK 주행 제한 연결 전에는 주행 상태 전환도 시뮬레이션임을 표시한다.
- [ ] 큰 글꼴·좁은 창·야간 모드에서도 중지와 핵심 잔량 정보에 접근 가능한지 확인한다.
- [ ] ./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug 및 대상 기기의 connectedDebugAndroidTest를 실행한다.
- [ ] APK 위치·실행 방법·가상 파라미터·제한사항·테스트 기기/결과를 시연 가이드에 기록한다.
- [ ] 화면 녹화와 재현 가능한 debug APK를 준비한다. App Market 제출·업로드는 이 계획에 포함하지 않는다.

## 후속 작업: 실제 Pleos 연동

첫 프로토타입 완료 후 별도 계획으로 진행한다.

1. 대상 차량·에뮬레이터·SDK 버전, 승인 권한, capability를 표로 확정한다.
2. VehicleGateway의 Pleos 구현과 FusedLocation 기반 위치 공급자를 추가한다.
3. 실제 SOC 단위, 사용 가능 용량, 공조 영역·지원 온도 간격, 설정 확인 이벤트를 문서와 실측으로 대조한다.
4. CarUxRestrictions 초기 상태·리스너·연결 해제 처리와 CCP/IME 정책을 실제 플랫폼에 맞춘다.
5. 캠핑장·경로 데이터 출처와 사용 조건, 위치 동의·보관 정책을 확정한다.
6. Suspend·차박 전원 모드·앱 종료 이후 동작을 검증한 뒤에만 밤샘 자동 관리 가능 여부를 설명한다.

## 의존 순서와 범위 조정

작업 1 → 2 → 3 → 4 → 5가 기본 순서다. 화면 초안은 계산 개발과 별도로 준비할 수 있지만 구현 인터페이스는 위 계약을 공유한다. 기간은 개발 가용 시간과 에뮬레이터 준비 상태를 확인하기 전에는 확약하지 않는다.

시간이 부족하면 지도·풍부한 그래프·세밀한 전환 효과부터 줄인다. 계산 검증, 시연 데이터 구분, 수동 중지, 실패 상태는 유지한다.

## 계획 검토 결과

네 핵심 기능은 작업 1~4, 시연 및 화면 검증은 작업 5에 배정했다. 기존 기획의 V2L 제외·다음 이동·10% 예비분·수동 변경 중지·재시작 시 재개 확인을 반영했다. 위 기술 선택과 제어 수치는 구현 제안이며 실차 승인·예측 정확도 검증 결과가 아니다.
