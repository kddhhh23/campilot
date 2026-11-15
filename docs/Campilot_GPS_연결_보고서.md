# Campilot 실제 위치 연결 — 이전 구현 기록

> 이 Android 위치 API 구현은 사용자 지시에 따라 교체되었습니다. 현재 버전은 [Pleos 전용 위치 연결 보고서](Campilot_Pleos_위치_연결_보고서.md)를 참고하세요.

작성: 2026-11-14

## 변경
- 현재 위치의 서울시청 고정값을 Android LocationManager의 실제 위치 수신으로 교체했다.
- 첫 화면에서 ‘현재 위치 사용하기’를 누르면 정밀/대략 위치 권한을 함께 요청한다. 권한 거부 시 설명과 재요청, 영구 거부 시 앱 설정 진입을 제공한다.
- 현재 위도·경도, 위치 제공자, 정확도를 표시한다. GPS 수신 시 GPS로, 대략 위치만 허용하면 네트워크 제공자로 수신한다.
- 권한 없음·서비스 꺼짐·제공자 없음·수신 대기 상태를 안내한다. 수신 실패 시 고정 좌표로 대체하지 않는다.
- 추천 화면이 활성화된 동안 수신하고, 일시 정지/화면 이탈 시 리스너와 타이머를 해제한다. 60초 이상 오래된 값은 현재 위치에서 제거한다.
- 좌표를 서버에 전송하거나 영구 저장하지 않는다. Play services나 새 외부 SDK는 추가하지 않았다.
- 추천 차박지 3곳과 이동 거리는 기존 목업으로 유지했다. 거리는 ‘시연 거리’로 표시하며 실제 GPS에서 계산한 경로 거리로 해석하지 않도록 문구를 변경했다.
- 시연 모드에서도 현재 위치는 동일한 Android 위치 API를 사용한다. 기존 배터리/공조 시연 여부와 별개다.
- 주소 변환/지도 표시는 이번 변경에 포함하지 않았다. 현재 위치는 좌표로 표시한다.

## 파일
- app/src/main/java/com/example/campilot/ui/LiveLocationPanel.kt: 위치 권한, 수신 및 화면 수명 관리, 상태별 위치 카드.
- app/src/main/java/com/example/campilot/ui/JourneyScreen.kt: 실제 위치 카드 및 시연 거리 문구.
- app/src/main/AndroidManifest.xml: 전경 위치 권한, 선택적 GPS 하드웨어.
- app/src/androidTest/java/com/example/campilot/PlaceTimeFlowTest.kt: 고정 위치 제거와 권한 미허용 상태 검증.

## 검증
gradle-run 관리 도구로 :app:assembleDebug, :app:testDebugUnitTest, :app:connectedDebugAndroidTest, :app:lintDebug를 실행했다.
질문 “Do real foreground location integration, existing journey tests, build and lint pass?” — exit 0, 단위 25개·기기 30개 통과, 실패 0.
API 28 호환 모의 위치 판별 API와 기존 테스트/도구의 deprecation 경고가 남아 있다. finish는 wrapper 소유 로그만 정리했다.

Pleos 에뮬레이터에서 위치 권한 요청 → ‘앱 사용 중에만 허용’ → 좌표 표시를 직접 확인했다.
에뮬레이터의 GPS 입력을 37.49790/127.02760에서 35.17960/129.07560으로 변경했을 때 화면 좌표도 갱신됐다. 시연 차박지와 거리는 유지됐다.
이는 Android 위치 경로의 에뮬레이터 검증이며, 실제 차량 하드웨어의 수신 품질은 실차에서 확인해야 한다.

## 결과물
- [최신 APK](../artifacts/Campilot-gps-debug.apk)
- [GPS 화면](../artifacts/Campilot-gps.png)

에뮬레이터에 최신 APK를 설치했다. 첫 실행에는 ‘현재 위치 사용하기’로 권한을 허용하면 된다.

## 공식 참고
- [LocationManager](https://developer.android.com/reference/android/location/LocationManager)
- [위치 권한 요청](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime)
