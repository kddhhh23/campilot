# Campilot 자동 에너지 관리 — 구현 가능성 조사

- 조사일: 2026-11-12
- 결론: 배터리 상태를 관측하고 공조 소비를 조절하는 자동 관리는 SDK상 구현 후보가 된다. 실제 차량에서 밤새 지속 실행된다는 보장은 아직 없다.
- 현재 APK는 가상 차량·가상 소비 모델이다. 이번 조사는 구현 변경이나 실차 연동 성공 결과가 아니다.

## 1. 추천 기능 정의

“원하는 퇴실 시각과 다음 이동에 필요한 전력을 지키도록, 남은 에너지를 예측하고 사용자가 허용한 범위에서 공조를 자동 조절한다.”

배터리 셀·BMS·고전압 회로를 직접 관리하는 기능이 아니라, 허용된 차량 기능의 소비를 관리하는 애플리케이션이다.

기본 캠핑 모드는 사용자가 보낸 사진에서 실내 온도·조명·전원을 유지하고, 잔량20% 이하에서 종료한다고 안내한다. 사진은 해당 화면의 증거이며 모든 차종에 동일한20% 기준이 적용된다는 근거는 아니다. 사진만으로 예측 관리 기능이 없다고 단정하지 않는다.

차별화 후보는 자동 종료 전에 부족을 예상해 목표 체류 시간과 다음 이동을 기준으로 소비 조절·대안을 제시하는 흐름이다.

## 2. 공식 API와 구현 판단

Vehicle SDK 소개는 v2.3.2 / Vehicle Service v2.3.21 기준이다. 배포 문서 버전과 현재 설치된 에뮬레이터 v2.1.0의 호환성은 따로 확인해야 한다.

| 기능 | 확인한 API/도메인 | 판단 |
|---|---|---|
| SOC 조회 및 변경 구독 | getEvBatteryStateOfCharge, registerEvBatteryStateOfCharge | SDK 목록에서 확인. 차량 지원·데이터 모델·단위 확인 후 연결 |
| 목표 온도 조회·변경·변경 구독 | getHvacTemperature, setHvacTemperature, registerHvacTemperature | 허용 범위·영역·권한 확인 후 자동 조절 후보 |
| 풍량 조회·설정 | getHvacFanSpeed, setHvacFanSpeed | 지원 시 가능. 초기에는 온도 조절부터 권장 |
| 기능 지원·허용값 확인 | checkEvBatteryStateOfChargeCapabilities, checkHvacTemperatureCapabilities 등 | 차량별로 확인 필요 |
| 충전 목표량 설정 | setEvChargePercentLimit | 충전량 목표에 관한 API. 캠핑 방전 하한을 바꾸는 API가 아님 |
| 충전 전류·충전 활성화 | setEvChargeCurrentDrawLimit, setEvChargeSwitch | 지원 시 충전 제어 후보. 외부 전원 연결 등 물리 조건 없이 충전을 만들어낼 수 없음 |
| 캠핑 모드 종료20% 하한 변경·자동 재진입 | 이번 조사에서 해당 API 확인 못함 | 구현 가능하다고 약속하지 않음 |
| 셀 밸런싱·BMS 정책·고전압 직접 제어 | 이번 조사에서 일반 앱용 기능 확인 못함 | 앱 기획 범위에서 제외 |

[Vehicle SDK 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
[EvBattery API 목록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/ev-battery/entry)
[HVAC API 목록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/h-v-a-c/entry)

개별 API 페이지는 이번 웹 텍스트 조회에서 함수 본문·전체 시그니처가 표시되지 않았다. 따라서 정확한 콜백 타입, 단위, 요구 퍼미션은 SDK 아티팩트/원문 렌더링에서 구현 전에 대조해야 한다. API 이름이 있다는 사실과 실제 차량에서 앱이 호출할 수 있다는 사실은 다르다.

Virtual Console에는 SOC·현재 배터리 에너지·용량·목표 온도·풍량을 바꾸는 시험 기능이 있다. 이는 에뮬레이터 입력 제어이며 실제 차량 API 가용성의 증거로 대신할 수 없다.
[Virtual Console](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide)

## 3. 자동 관리 흐름 제안

1. 사용자에게 퇴실 시각, 다음 목적지, 도착 후 예비 잔량, 허용 온도 범위를 입력받는다.
2. 현재SOC와 시각을 구독하고 용량 정보를 확인한다. 지원하지 않는 값은 검증된 차량 프로필 또는 사용자 입력으로 받는다.
3. SOC 변화와 시간으로 소비 추세를 보정한다. SOC 분해능이 거칠면 짧은 구간의 차이를 정확한 실시간 전력으로 단정하지 않는다.
4. 남은 체류 에너지와 다음 이동 에너지를 계산한다. 외기온·기상·실측 소비를 더하면 예측을 개선할 수 있으나 정확도는 별도 검증한다.
5. 목표 미달이 예상되면 허용 온도 범위에서 난방은 낮추고 냉방은 높인다. 적용 상태를 다시 확인한다.
6. 범위 끝에서도 부족하면 체류 단축·충전·더 가까운 다음 목적지를 제안한다. 유지할 수 없는 목표를 보장하지 않는다.
7. 사용자가 수동으로 조작하거나 주행·연결·권한·신선도 이상이 발생하면 새 자동 명령을 중지한다.
8. 앱 재시작 후 차량 상태와 사용자 동의를 확인하고 재개한다.

정해진 절감률을 실차에 하드코딩하지 않는다. 공조의 설정 온도와 소비 전력은 외기온·히트펌프·제상 등 여러 조건에 따라 달라진다.

## 4. 사진의20% 기준을 반영한 계산

차박 종료 시 확보할 에너지는 아래 두 요구 중 큰 값 이상이어야 한다.

- 캠핑 모드가 꺼지는 잔량에 도달하지 않도록 할 에너지와 불확실성 여유.
- 다음 이동 에너지 + 다음 목적지 도착 후 예비 에너지.

예를 들어 캠핑 종료 하한20%, 다음 이동10%, 도착 예비10%라면 두 요구가 모두20% 수준이다. 이를 기계적으로20+10+10=40%로 합산하지 않는다. 다만 정확히20%에서도 모드가 종료되는 사진이므로 여유를 추가하고20% 초과를 유지하는 방향으로 설계한다. 이 여유는 예측 오차를 완전히 제거하지 않는다.

현재 프로토타입은 예비10% 기본값만 사용하므로 차종별 캠핑 종료 하한·예측 체류 가능 시간 보정이 추가로 필요하다.

## 5. 실차 적용 조건

- 차량 capability와 영역별 설정 범위 확인.
- API별 조회·제어 권한과 사전 승인 확인. Manifest 선언만으로 승인되지 않는다.
- 온도·소비·명령 실패를 실제 차량에서 측정.
- 캠핑 모드 중 화면OFF·앱 백그라운드·프로세스 종료·IGN OFF·Suspend에서 실행 및 API 연결 상태 확인.
- Foreground Service를 추가한다고 차량 Suspend를 우회하거나 밤샘 실행이 보장되는 것은 아니다.
- 배터리 안전 종료 기준은 유지하고, 기본 차량 공조 제어와의 충돌·사용자 수동 조작을 검증.

공식 Suspend 설명은 일반 서비스가 중단될 수 있다고 안내한다. 오디오 가이드의 이 설명만으로 캠핑 모드 실행이 항상 가능/불가능하다고 결론 내리지 않는다.

[API 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)
[퍼미션 선언](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)
[SDK 사용 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)
[Suspend 설명](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus)

## 6. 다음 검증 순서

1. 실제 캠핑 종료 하한을 반영하도록 계산 계약과 테스트를 보강.
2. Vehicle SDK를 연결하는 작은 검증 앱 또는 별도 어댑터로 SOC 구독·온도1회 변경·결과 확인 시험.
3. Virtual Console로 과소비·연결 해제·권한·영역 미지원 시험.
4. 대상 실차에서 권한·지원·화면OFF 지속 실행 확인.
5. 장시간 실측과 기본 캠핑 모드 정책을 검증한 뒤 자동관리 지속 시간을 제품 설명에 반영.

권장 제품명은 “목표 시간 기반 자동 에너지 관리” 또는 “귀가 전력 확보형 차박 관리”다. 현재 단계의 판단은 기술 설계상 가능/조건부 가능이며 실차 구현 완료가 아니다.
