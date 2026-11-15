# Pleos Connect 앱 개발 필수 규칙 조사

- 조사일: 2026-11-12 (KST)
- 대상: Pleos Connect 차량용 앱과 Pleos App Market 출시 심사
- 활용 프로젝트: Campilot
- 조사 방법: Pleos 공식 개발·디자인·심사 문서를 직접 열어 요약했다.
- 상태: 문서 조사 결과다. 앱 구현, 규정 준수 검사, 실차·에뮬레이터 검증을 완료했다는 의미는 아니다.

## 0. 현재 단계에서 적용할 범위

현재 목표는 Campilot 프로토타입이며, App Market 등록은 진행하지 않는다. 아래 구분은 사용자와 합의한 프로젝트 진행 기준이다. 공식 출시 요건 자체를 면제한다는 뜻은 아니다.

| 시점 | 지금 정리해 둘 사항 |
|---|---|
| 프로토타입 설계·시연 | 주차 중심 흐름, 가로·세로 창 대응, 가독성과 터치 피드백, 상태 보존, 모의 데이터 표시, 자동 공조 시작·중지·실패 안내 |
| 실제 차량·위치 연동 전 | SDK 권한·승인·capability 확인, 주행 제한 연동, 실위치 동의·데이터 보호, 명령 결과 확인 |
| 밤샘 자동 제어 검증 시 | 차박 전원 모드, Suspend, 앱 중단·복귀 및 제어 지속 가능성 |
| App Market 출시 준비 시 | 제출용 스크린샷·아이콘·APK 규격, 정식 서명, 심사 계정·약관·등급·제출 자료 |

스크린샷 규격의 표기 차이는 프로토타입 시작을 막는 문제가 아니다. 버튼의 세부 수치 검증도 화면 초안 작성의 선행 조건으로 두지 않으며, 실제 차량 화면을 다듬고 검증할 때 확인한다. 공모전 자체의 제출 조건은 App Market 조건과 별도로 확인해야 한다.

## 1. 문서 해석 기준

| 표시 | 의미 |
|---|---|
| 필수 | 공식 문서에서 준수·금지·심사 요건으로 명시한 사항 |
| 조건부 필수 | 해당 기능, 배포 방식 또는 앱 종류를 선택할 때 적용되는 사항 |
| 권장 | 공식 문서가 권장·고려하도록 안내하는 사항 |
| Campilot 제안 | 공식 규칙을 바탕으로 이 프로젝트에 제안하는 설계. Pleos가 직접 지정한 규칙은 아님 |
| 확인 필요 | 문서 간 불일치, 이미지 전용 수치, 권한·플랫폼별 차이 등으로 추가 확인이 필요한 사항 |

공모전의 프로토타입 제출과 App Market 출시 심사는 서로 다른 절차다. 공모전 시연용 모의 데이터 사용이 App Market 출시 심사에서 허용된다는 뜻은 아니다. 준수 여부만으로 출시 승인도 보장되지 않는다.

출처: [앱 리뷰 가이드라인](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide)

## 2. 주행 안전과 앱 카테고리

### 2.1 주행 중 제한 — 필수

- 앱은 플랫폼의 CarUxRestrictions에 따라 기능과 UI를 제한해야 한다.
- CarUxRestrictionsManager로 현재 제한을 얻고, registerListener() / onUxRestrictionsChanged()로 변경을 반영한다.
- DO(Distraction Optimization)를 선언한 앱도 ActiveRestrictions에 따른 기능별 제한을 직접 적용해야 한다. DO 선언은 제한 해제 허가가 아니다.
- 지역별 허용 여부를 앱이 임의로 고정하기보다 플랫폼에서 내려오는 제한 플래그를 반영한다.
- 속도 0만으로 주차·사용 가능 상태를 단정하지 않는다. 플랫폼 정책은 속도·기어·브레이크와 지역별 조건에 영향을 받는다.
- DO 미선언 앱은 공식 기본 표상 Parking에서 사용 가능, Drive에서 사용 불가다. 일시 정지한 주행 상태와 Parking을 동일하게 취급하지 않는다.

출처: [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), [지역별 주행 규제 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)

### 2.2 카테고리와 상호작용

- **필수:** 앱의 실제 목적에 맞는 카테고리를 설계 단계에서 정한다. 카테고리를 선택했다고 모든 주행 중 기능이 허용되는 것은 아니다.
- **필수:** 운전자 주의 분산을 줄이고, 규제 상태에서 제한 대상 기능을 축소·비활성화한다.
- **설계 지침:** 핵심 정보, 단순 메뉴, 큰 터치 영역, 프리셋·최근 항목·음성 활용을 우선한다. 장식적 움직임과 반복 화면 전환을 줄인다.
- **조건부 필수:** 동영상·게임·브라우저 앱은 주행 시 사용을 중지하고 제한 중 다시 실행하지 못하게 한다. 이 규칙을 모든 음악·오디오 앱의 전면 금지로 확대 해석하지 않는다.

출처: [앱 카테고리](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category), [주행 안전 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline), [주의 분산 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements)

**Campilot 제안:** 차박 계획 입력·캠핑장 상세 비교·취침 설정은 주차 중심 기능으로 설계한다. Lifestyle을 초기 분류 후보로 검토하되, 차량 에너지 관리 기능과 함께 최종 카테고리를 확인한다. 목적지를 추천한다는 이유만으로 Maps/Navigation 앱으로 단정하지 않는다.

## 3. 화면, 창 크기, 입력 규칙

### 3.1 디스플레이 대응 — 필수

- 일반 앱은 Portrait와 Landscape에 대응한다. 공식 문서의 가로 전용 게임 등 예외를 Campilot에 임의 적용하지 않는다.
- 앱은 Application window 안에서 동작하며, Driving view를 임의로 가리거나 종료할 수 없다.
- 상태 표시줄·GNB·공조 패널·키보드 등 시스템 UI가 앱보다 위에 표시될 수 있음을 고려한다.
- 화면 크기·방향·해상도가 바뀌어도 작업 내용과 상태, 터치 좌표의 정합성을 보존한다.
- 크기 변경 시 기능 패리티를 유지한다. 배치를 바꾸더라도 핵심 기능이 사라지지 않아야 한다.
- 호환성 모드로 실행되지 않아야 한다. 레터박스를 최소화하되, 미세 비율 보정용 한 방향 레터박스는 허용될 수 있다.

출처: [Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout), [기능·성능 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)

### 3.2 화면 방향·생명주기

- Pleos의 screenOrientation 설정은 앱이 사용하는 창 영역과 연결된다. 공식 앱 구조 문서는 기본 Portrait 한 영역, Landscape 확장 영역을 설명한다.
- Connect-S와 Connect-W는 창 배치·신규 앱 실행 정책이 다르다. 한 기기의 고정 해상도만 기준으로 구현하지 않는다.
- 화면에서 사라진 앱이 반드시 종료되는 것은 아니다. 화면 복귀와 프로세스 재생성 시 상태를 복원한다.
- 백그라운드에서는 화면 갱신·애니메이션 등 불필요한 작업을 중단하고 타이머·폴링·네트워크 요청을 최소화한다.
- 화면 축소 시 부가 정보를 재배치하거나 접을 수는 있지만, 해당 기능에 접근할 수 있어야 한다.

출처: [앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model), [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)

### 3.3 키보드·안전 영역 — 조건부 필수

- 입력창이 있는 window는 IME 표시 시 입력 영역이 위로 조정되는 동작에 대응한다.
- 입력창이 없는 window는 IME에 따라 불필요하게 밀리지 않아야 한다.
- Dialog 등의 IME insets 제외 설정과 Activity의 입력 처리는 구분해서 검토한다.
- 컷아웃·시스템 바·키보드를 포함한 안전 영역을 지켜 필수 조작 요소가 가려지지 않게 한다.

출처: [IME window resize 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ime-window-resize-handling-guide), [기능·성능 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)

**Campilot 제안:** 좁은 창에서는 핵심 잔량과 조작을 세로로, 넓은 창에서는 계획·그래프를 병렬 배치한다. 창 변경 중 선택 캠핑장·체류 시간·온도·자동 제어 상태를 유지한다.

## 4. UI 디자인과 수치 기준

| 항목 | 공식 기준·안내 | 구분 |
|---|---|---|
| 대비 | 아이콘·텍스트·기타 이미지 대비율 4.5:1 이상 | 필수 |
| 핵심 정보 인지 | 1~2초 안에 파악하도록 핵심만 표시 | 설계 지침 |
| 기본·보조 텍스트 | 중요한 기본 텍스트 높이 5 mm 이상, 보조 텍스트 3 mm 이상 | 필수 |
| 영어 메시지 | 단일 메시지 120자 또는 24단어 이하, 한 줄 80자 이하 | 필수 |
| 문장·단락 | 3줄 이하 | 필수 |
| 주행 중 움직임 | 동영상·애니메이션 이미지·텍스트 자동 스크롤로 시선을 유도하지 않음 | 필수 |
| 조작 | 한 손으로 가능한 단순 조작, 일관된 아이콘·용어·패턴 | 필수 |
| 알림 | 운전자 필요와 관련된 정보로 제한하고 광고 제한 준수 | 필수 |
| 주야간 | 밝기·대비를 조명 환경에 맞게 조정 | 디자인 지침 |

출처: [Pleos UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)

Typography 상세 문서는 12sp 이상과 가독성을 위한 더 큰 글자 사용을 권장하며, 실제 픽셀 높이 안내도 별도로 제시한다. **12sp를 차량 UI의 충분한 최소 기준으로 단정하지 않는다.** mm·px·dp·sp 변환은 대상 화면 DPI와 글꼴 스케일을 확인한 뒤 검증한다. 한국어 글자 수를 영어 기준에서 임의 환산하지 않는다.

출처: [Typography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography)

**세부 UI 검증 시 확인:** Basic button의 크기 예시는 SVG 이미지로 제공된다. 원본은 조회했지만 렌더링된 이미지의 치수·단위를 시각적으로 검증하지 못했다. 이는 공식 규칙이 없다는 뜻이 아니라 이번 조사의 확인 한계다. 48dp 등을 Pleos 공식 최소값이라고 단정하지 않는다. 프로토타입 화면 초안은 진행할 수 있으며 실제 화면 검증 전에 공식 자산과 대조한다. 본문에서 확인된 Pressed·Disabled·Loading 상태는 구분하고, Loading 중에는 제어할 수 없게 한다.

출처: [Basic button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button)

## 5. 차량 하드웨어·SDK·권한

### 5.1 접근 방식 — 필수

- 차량의 물리적 제어·센서에 직접 접근하지 않는다. Pleos가 제공하고 허용한 플랫폼 API를 사용한다.
- 모바일의 전화·SMS·일부 센서·캘린더·주소록 등을 차량에서도 사용할 수 있다고 가정하지 않는다.
- 지원하지 않는 기능에 대한 대체 동작을 준비한다.

출처: [하드웨어 접근 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture)

### 5.2 API 사용 승인 — 조건부 필수

- Vehicle SDK 사용 시 API별 권한을 선언해야 한다.
- 해당 권한은 Pleos App Market과 사전 협의·승인된 범위에서만 선언·사용한다.
- 승인되지 않은 권한 선언은 심사 거부 사유다. Manifest 선언만으로 사용 승인이 확보되지 않는다.
- 차량 상태 변경·안전 영향 API에는 별도 심사 기준이 적용된다.

출처: [SDK 정책](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/), [SDK·API 심사 기준](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)

### 5.3 차량별 가용성 — 필수

- Vehicle SDK는 도메인별 checkXXXCapabilities로 지원 기능·범위를 확인한다.
- FusedLocation SDK는 isSupported로 확인한다.
- 모델명만으로 기능 지원 여부를 고정하지 않는다. AAOS property도 실제 가용성과 접근 권한을 확인한다.
- Pleos SDK가 제공하지 않는 특수 property는 직접 구현 전에 기술 지원팀의 검토를 요청한다.
- API 문서에 존재하는 기능, 차량의 지원 여부, 앱의 사용 권한은 서로 다른 조건이다.

출처: [API 호환성 및 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)

**Campilot 제안:** 공조 조회·설정에 필요한 승인 목록을 먼저 정리한다. 미지원·권한 거부·연결 해제·명령 실패 시 제어를 멈추고 안내하며, 요청 결과를 확인한 뒤 적용 완료로 표시한다. 모의 제어는 실제 차량 명령과 구분한다.

## 6. 성능·오프라인·전원 상태

### 6.1 성능 기준

| 항목 | 공식 기준 |
|---|---|
| CPU: 내비게이션 앱, 주행 중 | 15% 이하 |
| CPU: 비내비게이션 앱, 주행 중 | 5% 미만 |
| CPU: 모든 앱, 정차 중 | 25% 이하 |
| RAM | 400 MB 이하 |
| 앱 실행·콘텐츠 로드 | 각각 10초 이내 |
| 버튼 동작·상태 반응 | 2초 이내. 지연되면 동작 중 UI 표시 |
| 터치 효과 피드백 | 입력 후 0.25초 이내 |

위 수치는 문서상 기준이다. 0.25초는 터치 피드백, 2초는 기능·화면·상태 반응으로 구분한다. 측정 환경·평균 구간·CPU 집계 방식은 인용한 페이지에 상세히 정의되지 않아 임의 해석하지 않는다.

출처: [성능·리소스 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt), [기능·성능 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)

### 6.2 오프라인과 리소스 — 설계 지침·권장

- 통신 중단·저대역폭에 대비한다. 최근 데이터 캐시, 타임아웃·재시도, 오프라인 안내를 권장한다.
- 초기화는 최소화하고 부가 리소스는 지연 로딩한다.
- 상시 화면 갱신·고빈도 폴링·불필요한 장시간 작업을 줄인다.

출처: [하드웨어·네트워크 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture), [성능·리소스 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt)

### 6.3 Suspend와 밤샘 동작

공식 Suspend 문서는 IGN OFF 이후 일반 기능이 중단될 수 있는 저전력 상태를 설명한다. 앱은 언제든 중단·재시작될 수 있다고 취급해야 한다. 오디오 사용 시 Suspend 전 재생 중단·포커스 반납이 필수이며 Resume 때 포커스를 자동 복원하면 안 된다.

출처: [Suspend 오디오 포커스 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus)

**Campilot 제안:** 일반 백그라운드 실행과 차량 Suspend를 구분한다. 차박 전원 모드에서의 앱 실행·공조 제어 지속 가능성은 별도 검증 과제로 둔다. 위 문서만으로 차박 중 제어가 항상 가능하거나 항상 불가능하다고 결론 내리지 않는다. 복귀 시 차량 상태를 새로 확인한다.

## 7. CCP·Back·음성 출력의 조건부 규칙

### 7.1 CCP 물리 컨트롤러

- 사용자 설치 앱은 CCP 지원을 선택할 수 있다. 미지원이어도 마켓 등록 가능하지만 필수 조작을 터치만으로 완료할 수 있어야 한다.
- 미지원 시 application에 com.hmg.rotary.ROTARY_SUPPORTED=false 메타데이터를 선언한다.
- 기본 탑재 앱은 원칙적으로 CCP 필수이며, 예외는 계약에 명시해야 한다.
- CCP Back의 최상위 화면 동작은 종료가 아니라 숨김이다. 표준 Back 이벤트 전달 여부를 확인하고, 해당 경우 moveTaskToBack(true)를 사용하며 finish()/finishAffinity()로 종료하지 않는다.
- 하위 화면·팝업에서는 이전 화면·팝업 닫기를 처리한다. 숨김 후에도 프로세스 종료에 대비해 상태를 저장한다.

출처: [CCP 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ccp-rotary-controller-development)

### 7.2 오디오·TTS를 추가할 때

- 재생 전 AudioAttributes와 AudioFocusRequest로 포커스를 요청하고 획득·상실 이벤트를 처리한다.
- 일반 Android 동작만 가정하지 말고 Pleos 오디오 포커스 매트릭스를 따른다. 거부·일시 상실·감쇄·긴급음·통화·Gleo와의 충돌을 검증한다.
- 음성 안내에는 선행 묵음을 적용한다. 권장 길이는 150~200ms이며, 출력 경로 초기화·포커스 확보를 대신하지 않는다.
- 연속된 자연스러운 발화에 불필요한 묵음을 반복 삽입하지 않는다.

출처: [오디오 포커스 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development), [TTS 선행 묵음 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide)

## 8. 개인정보·보안

### 8.1 수집·동의 — 조건부 필수

- 개인정보·위치 접근 전에 항목·목적·보유기간·동의 거부 권리를 명확히 고지하고 동의를 받는다.
- 뒤로가기·빈 곳 터치 등을 동의로 처리하지 않는다. 허용된 데이터만 접근한다.
- 최소 권한·최소 수집을 적용하고 목적 외 사용을 금지한다.
- 개인정보처리방침을 공개하며 제3자 공유도 투명하게 알린다.
- 제공 철회 시 관련 이용 경로를 중단하고 저장 데이터를 삭제한다.
- 위치정보 서비스는 서비스 국가의 등록·신고 등 증빙을 요청받을 수 있다.

출처: [개인정보·보안 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-user-privacy-security)

### 8.2 기술 보안 — 필수 / 조건부 필수

- 저장하는 민감정보는 내부 저장소·DB·캐시·임시 사본까지 암호화한다.
- 토큰·비밀키를 소스·리소스·설정에 하드코딩하지 않는다. 암호화 키는 Keystore 등으로 보호한다.
- 민감정보를 로그·URL 경로·쿼리에 노출하지 않는다.
- 외부 통신을 암호화한다. TLS 사용 시 최소 1.2, 1.3을 권장한다.
- 불필요한 컴포넌트를 외부에 노출하지 않고, 공개 기능에는 호출자와 접근 권한을 확인한다.
- 민감 데이터의 컴포넌트 전송은 수신자를 제한한다.
- 승인된 API만 기능에 필요한 범위에서 사용한다.
- 최종 배포는 정식 서명 인증서를 사용하고 앱·WebView 디버깅을 비활성화한다.
- WebView 사용 시 파일·스크립트·연결 대상 접근을 제한한다. 브라우저 앱에는 추가 요건이 적용된다.

**권장:** 자동 백업 비활성화 또는 민감정보 제외, 민감 캐시 최소화, 검증된 암호 방식·안전한 난수 사용, 라이브러리 보안 패치 유지.

공식 문서는 현재 Pleos Connect를 non-GAS AAOS 환경으로 설명한다. Google 기반 자동 백업·서비스 동작을 전제하지 않는다.

출처: [앱 보안 가이드](https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide)

### 8.3 공동 차량 환경 — 조건부 필수

계정 기능이 있다면 소유자·운전자 변경 시 로그아웃과 계정 전환을 쉽게 제공한다. 개인정보 로그는 마스킹한다. 외부 분석 전송에는 식별자 익명화·가명화 정책을 세우고 운전 행동 데이터를 마케팅 용도로 사용하지 않도록 구분한다.

출처: [보안·개인정보 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy)

**Campilot 제안:** 첫 프로토타입은 로그인·서버 업로드 없이 가상 위치와 가상 차량을 사용한다. 실위치 도입 시 동의와 위치정보 약관을 함께 준비한다.

## 9. App Market 출시 심사

### 9.1 기능·콘텐츠

- 설치·실행·삭제와 주요 기능이 정상 동작해야 한다.
- 실제 출시 서비스와 심사 앱이 일치해야 하며 더미 데이터·임시 콘텐츠를 제거해야 한다.
- 시스템 손상·과도한 자원 점유를 유발하지 않고, 핵심 기능과 무관한 시스템 설정 변경이나 기기 재시작을 요구하지 않는다.

출처: [기능·성능 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)

- 유해·불법·위험 운전 조장 콘텐츠와 지적재산권 침해를 금지한다.
- 사용자 제작·AI 생성 콘텐츠를 제공하면 부적절한 내용 필터링과 신고 기능을 포함한다.
- 연령등급을 정직하게 신고하고 국가별 요건을 따른다.

출처: [콘텐츠 안전성](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-content-safety)

### 9.2 제출 자료

| 항목 | 기준 |
|---|---|
| 패키지 | APK. 업로드 파일과 압축 다운로드 크기 기준은 아래 참고 |
| 앱 제목 | 영문 기준 30자 이내 |
| 아이콘 | PNG/JPEG, 512×512px, 최대 1MB |
| 스크린샷 | 2~8장, PNG/JPEG, 장당 최대 8MB |
| 메타데이터 | 실제 앱과 일치하는 국가별 언어의 이름·설명·카테고리 등 |
| 개인정보 | 개인정보처리방침 URL. 위치정보 사용 시 관련 약관도 필요 |
| 심사 접근 | 로그인 필요 시 일반 사용자 기준으로 모든 기능에 접근 가능한 계정 |
| 기타 | 콘텐츠 사용 권리 증빙 등 해당 기능별 자료 |

출처: [App Market 등록정보](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-app-registration-info)

추가로 콘솔의 버전·플랫폼·권한·SDK 프로젝트 ID 해당 항목, 출시 노트, 고객 지원 정보 중 1개 이상을 준비한다. 심사 중 외부 서비스가 동작해야 하며, IARC 연령등급 증빙과 기능 설명 자료를 준비한다.

출처: [앱 심사 제출](https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide)

**출시 준비 시 확인 — 재조사 결과**

- APK: 등록정보는 사용자가 받을 압축 다운로드 크기를 600MB 미만으로, 제출 가이드는 업로드 파일을 최대 600MB로 설명한다. 측정 대상이 달라 서로 모순된다고 단정할 수 없다. 출시 시 두 조건을 각각 확인한다.
- 스크린샷: [등록정보](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-app-registration-info)는 840×1212px, [심사 제출](https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide)은 840×121px로 실제 표기가 다르다. 공식 정정은 확인하지 못했으므로 임의로 하나를 정답 처리하지 않는다.
- 두 페이지에 공통으로 기재된 규격은 2560×1380px와 1700×1212px다. 출시용 자료를 준비할 때 우선 참고하고, 840px 폭 규격이 필요하면 콘솔·담당자에게 확인한다.
- 이는 스토어 제출 이미지 규격이며 런타임 UI의 고정 해상도 목록이 아니다. 현재 프로토타입 개발을 보류할 이유는 아니다.

## 10. Campilot 적용 우선순위

아래 항목은 공식 규칙을 해석한 프로젝트 제안이며 아직 구현·검증되지 않았다.

| 우선순위 | 설계·개발 반영 사항 |
|---|---|
| 프로토타입 시작 전 | 주차 중심 사용 흐름, 화면별 제한 대상, 앱 카테고리 후보 결정 |
| 프로토타입 화면 | 가로·세로 창과 안전 영역, 상태 보존, CCP Back 정책 검토 |
| 핵심 시연 | 실제 계산과 가상 측정값을 표시상 구분하고 자동 제어의 시작·중지 동작 제공 |
| 차량 연동 전 | API별 권한 승인, capability, 명령 성공·실패·연결 해제 처리 |
| 밤샘 기능 약속 전 | 차박 전원 모드·Suspend·백그라운드 실행 조건과 제어 지속성 검증 |
| 실제 데이터 도입 전 | 위치 동의·보관·암호화·출처·서버 전송 정책 |
| 출시 심사 전 | 더미 제거, 정식 서명, 메타데이터·약관·리소스 성능 검증 |

기존 [Campilot 기획안](Campilot_기획안_v1.0.md)의 모의 시연 범위는 유지할 수 있으나, 이를 출시 준비 완료로 표현하지 않는다. 예상 잔량·공조 절감량은 추정치로 표시하고, 사용자 허용 온도 범위·수동 중지·제어 실패 처리를 프로젝트의 명시적 기준으로 둔다.

## 11. 구현 전후 확인 체크리스트

- [ ] CarUxRestrictions 초기 상태·변경 이벤트와 기능 제한을 설계했다.
- [ ] 주차와 일시 정지, 주행·지역별 제한을 혼동하지 않는다.
- [ ] 대상 차량의 Portrait/Landscape 및 창 크기 변경에서 상태와 기능을 유지한다.
- [ ] 입력창·Dialog별 IME 처리와 안전 영역을 확인했다.
- [ ] 버튼 공식 수치·텍스트 대비·실제 화면 글자 크기를 확인했다.
- [ ] CCP 지원 선택·미지원 선언·최상위 Back 숨김 처리를 확인했다.
- [ ] 사용할 차량 API·권한·승인·capability를 정리했다.
- [ ] 미지원·권한 거부·연결 해제·오래된 값·제어 실패에 대응한다.
- [ ] 리소스·응답 시간과 오프라인·복귀 동작을 검증한다.
- [ ] 음성 출력 추가 시 오디오 포커스·선행 묵음·Suspend 처리를 검증한다.
- [ ] 실위치·민감 데이터 사용 전에 동의·보관·통신·보안 처리를 준비한다.
- [ ] 공모전 시연 앱과 출시 심사 앱의 완성도·데이터 조건을 구분한다.
- [ ] 출시 전 공식 문서와 실제 제출 콘솔의 최신 기준을 재확인한다.

## 12. 검증 방법과 조사 한계

- 공식 FormFactorConsole로 디스플레이와 방향을 바꿔 잘림·겹침·정렬·터치를 확인할 수 있다.
- 해당 공식 도구 절차는 Pleos 에뮬레이터, Platform-Tools, Go 1.22 이상, 8095·8096 포트 환경을 요구한다.
- 가상 디스플레이 테스트는 에뮬레이터를 별도 창으로 실행해야 하며, Running Devices 내장·Headless 실행은 사용하지 않는다.
- 가상 디스플레이 제거 시 해당 앱이 강제 종료되므로 테스트 데이터를 먼저 보존한다.

출처: [폼 팩터 테스트 가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide)

이번 조사는 Campilot과 관련된 공통·조건부 규칙을 요약했다. 모든 컴포넌트의 시각 명세, Maps/Navigation·Music/Audio·브라우저 전용 상세 요건, 법률 해석까지 모두 검증한 문서는 아니다. 카테고리 확장·웹 콘텐츠·음성 기능 도입 시 해당 세부 가이드를 추가 조사한다. UX 규제 Figma는 공식 주행 규제 개발 가이드에서 비공개라고 명시한 자료다. 버튼 SVG는 공개 자료지만 이번 조사에서 시각적 검증을 완료하지 못한 자료다. 두 경우를 구분한다. 2026-11-12 재조사에서는 기존 문서에 연결된 공식 페이지 29개를 다시 열어 대조했다. 컴퓨터 사용 도구의 브라우저 접근 및 URL 확인 제한으로 이미지 확인은 완료하지 못했으며, 완료한 것으로 기록하지 않는다.

일반 Android 스킬은 구현 보조 자료로 활용한다. Pleos 전용 화면 방향·IME·CCP·주행 제한 정책과 충돌하는 일반 지침을 그대로 적용하지 않는다. Compose·Navigation 3 사용 자체를 이번 조사에서 확인된 Pleos 필수 요건으로 간주하지 않는다.
