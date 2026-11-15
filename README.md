# Campilot — 차박 비서

**일단 출발** 팀의 현대자동차그룹 차량용 앱 개발 공모전 프로토타입입니다. 차박 장소 선택부터 전력 계획, Pleos 내비 연결, 차박 상태 확인과 귀가 안내까지 하나의 흐름으로 제공합니다.

## 제출 자료

- [설치용 release APK](apk/일단%20출발_Campilot_release.apk)
- [최신 시연 영상](docs/Campilot-demo-20261115.mp4): 약 2분 18초, 1920×1080, 무음, 1.5배속. GitHub에서 재생되지 않으면 Download raw file로 내려받습니다.
- [기획안](docs/Campilot_기획안_v1.0.md)
- [구현 현황·SDK·추가 과제](docs/Campilot_구현현황_SDK_추가과제.md)
- [공식 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)

과거 보고서는 당시 개발 이력입니다. 현재 동작과 제한은 아래 설명을 우선합니다. 과거 보고서의 artifacts/ APK 링크는 로컬 개발 산출물로, 이 저장소에 포함되지 않습니다.

## 테스트 환경

- Pleos Connect 에뮬레이터 v2.1.0 / Android API 34
- 전체 디스플레이 2560×1440, 가로 화면
- 관측된 앱 영역: [853,90]–[2535,1300] (1682×1210). 플랫폼 창 배치에 따라 달라집니다.
- 사용자 프로필: 에뮬레이터 user 10
- Pleos Vehicle SDK 2.3.2 / NaviHelper SDK 3.0.5
- 설치된 내비 공급자 응답 버전 3.0.4
- compileSdk/targetSdk 37, minSdk 28 / Kotlin·Compose Material 3

위 환경에서 개발·시연했습니다. 일반 휴대폰에는 Pleos 차량·내비 서비스가 없어 동일 기능을 보장하지 않습니다.

## APK 설치·실행

1. Pleos Connect v2.1.0 에뮬레이터를 실행합니다.
2. 내비게이션을 한 번 열어 현재 위치가 표시되는지 확인합니다. 집 주소가 없다면 내비에서 등록합니다. 시연에서는 공개 장소인 판교제2테크노밸리LH1단지아파트를 사용했습니다.
3. APK를 설치합니다.

```powershell
adb install -r "apk/일단 출발_Campilot_release.apk"
adb shell am start --user 10 -n com.example.campilot/.MainActivity
```

기기 여러 대가 연결되어 있으면 adb 명령에 -s <기기ID>를 지정합니다. 사용자 번호가 다르면 실제 프로필 번호로 바꿉니다. 기존 debug APK는 서명이 다르므로 업데이트 설치가 실패할 수 있습니다. 이 경우 기존 앱의 설정·진행 상태가 삭제됨을 확인한 뒤 제거하고 설치합니다. 같은 제출 키로 서명한 release APK끼리는 업데이트할 수 있습니다.

차량 정보 및 공조 기능은 Pleos API 권한과 차량 capability에 따라 제공됩니다. 권한 오류가 있으면 해당 개발 환경의 SDK 사용 승인·권한 설정을 확인합니다.

## 시연 순서

현재 위치 확인 → 차박지 추천 받기 → 3개 목업 장소 중 선택 → 내비 HOME 주소 확인 → 다음날 귀가 출발 시각 선택 → 전력 계획 확인 → 출발 → 실제 내비 안내 → 안내 종료 후 Campilot 복귀 → P단에서 차박 시작 → 배터리·예상 잔량 확인 → 귀가 시작 → 집으로 내비 경로 요청.

전력이 부족하고 현재 단일 충전 계획으로 가능한 경우 출발 버튼에서 충전소 경유 동의를 묻습니다. 동의한 경우에만 경유 경로를 요청하며, 취소 시 계획 화면을 유지합니다.

영상은 현재 코드와 같은 기능 흐름을 debug 빌드에서 녹화했습니다. 실제 도로 이동·목적지 도착·귀가 완료를 재현한 영상은 아닙니다. 안내 종료 후 P단에서 직접 차박을 시작합니다.

## 실제 연동과 프로토타입 범위

- 실제 SDK: 현재 위치, 내비 HOME 주소, 배터리 SOC, P단, 내비 목적지·경유지 요청 및 안내 종료 이벤트.
- 추천 차박지 3곳과 충전소는 고정 목업입니다. 실제 주변 검색이나 충전기 가용성 조회는 없습니다.
- 전력은 기본 용량 60kWh, 전비 5km/kWh, 평균 차박 소비 1.2kW 가정입니다. 귀가 거리는 좌표 직선거리 ×1.3이며, 예비 배터리는 10%입니다.
- 현재 계획 시간에는 출발 준비 시점부터 다음날 귀가 출발까지의 시간이 포함됩니다. 순수 체류 시간·실측 공조 소비 모델은 후속 개발 대상입니다.
- **알려진 제한:** 전체 필요 에너지가 배터리 용량을 넘으면 출발을 막습니다. 구간별 충전·복수 충전 시점을 반영한 계산은 아직 구현되지 않았습니다.
- 수동 온도 조회·변경 코드는 있으나 현재 에뮬레이터는 HVAC 값을 제공하지 않아 버튼이 비활성화됩니다. 실차 공조 제어는 미검증입니다.
- 자동 공조·자동 배터리 관리 및 Room 차박 기록 저장은 아직 구현되지 않았습니다.
- 내비 종료 후 앱 복귀를 사용합니다. 큰 내비와 작은 Campilot의 자동 동시 배치는 지원하지 않습니다.
- 귀가 완료는 SDK 도착 이벤트와 P단을 요구합니다. 안내 취소를 실제 도착으로 처리하지 않습니다.
- 여정 전체의 프로세스 종료 후 복원은 미구현이며 계획 설정만 DataStore에 저장합니다.

## 소스 빌드

Android Studio에서 프로젝트 루트를 열고 Android SDK 37 및 Gradle에 필요한 JDK를 준비합니다. Pleos 의존성은 settings.gradle.kts에 선언된 공식 Maven 저장소에서 받습니다. 개인 PC의 SDK 경로는 local.properties에 설정하며 저장소에는 포함하지 않습니다.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

제출 서명은 로컬 .signing/campilot-release.jks (PKCS12, alias campilot) 및 .signing/password.txt를 사용합니다. 두 파일이 있으면 release 빌드에 적용하며, 없으면 release 산출물은 unsigned입니다. 저장소를 새로 받은 개발자는 자신의 키를 설정하거나 debug 빌드로 개발할 수 있습니다. 심사용으로는 위에 제공한 서명된 APK를 사용합니다. 서명 키·비밀번호를 Git에 올리지 마세요.

## 구성

- app/src/main: Compose UI, ViewModel·StateFlow, 전력 계산, Pleos SDK 어댑터
- app/src/test: 계산·정책 단위 테스트
- app/src/androidTest: UI·SDK 연동 테스트
- docs/: 기획·구현 보고서·영상
- apk/: 제출용 서명 release APK

공모전 메일에는 저장소 주소와 최종 커밋 번호를 함께 기재합니다. 개인정보 동의서와 서명이 포함된 접수양식은 공개 저장소에 넣지 않고 별도로 제출합니다.
