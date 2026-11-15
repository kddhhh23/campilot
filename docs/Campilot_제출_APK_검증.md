# 공모전 제출 APK 확인

- 팀명: 일단 출발
- 앱: Campilot 0.1.0 (versionCode 1)
- 제출물: apk/일단 출발_Campilot_release.apk
- 크기: 33,824,186 bytes
- SHA-256: E7F1B8A40AC0EA400247B1FFD91913D333C36681A0B04808F23E9C6239E617ED

## 확인 결과 (2026-11-15)

- 질문: Does the submission release APK build with the dedicated signing key? → assembleRelease 성공. gradle-run으로 실행하고 종료 시 도구 소유 로그만 정리했다.
- release 빌드, 디버깅 비활성화. aapt manifest 검사에서 debuggable 속성 없음(기본 false).
- apksigner verify 성공, APK Signature Scheme v2, RSA 3072-bit 제출 전용 키.
- 서명 인증서 SHA-256: c96a697b4e08e8b61922527ec6774f3e0f244127ba557fc61e743f6e9dd1b5eb
- 빌드 출력 APK와 제출 경로 APK의 SHA-256 일치.
- Pleos Connect v2.1.0 에뮬레이터 API 34/user 10에 release APK 설치·실행 성공.
- 현재 위치 → 목업 추천 → 장소 선택 → 내비 HOME 주소 확인 → 시간 선택 → 실제 배터리 100% 및 전력 계획 표시 → 실제 내비 목적지 안내까지 확인.
- 전체 테스트와 실제 주행·공조 실차 검증은 수행하지 않았다.
- 최신 영상은 앞선 debug 빌드의 동일 기능 흐름을 녹화했으며, 자동 공조·기록 저장·실제 귀가 완료를 포함하지 않는다.

## 보관

서명 개인키와 비밀번호는 로컬 .signing/ 폴더에만 있다. 후속 업데이트 서명에 필요하므로 해당 폴더를 별도 안전한 위치에 백업한다. GitHub나 접수 메일에 포함하지 않는다.

## 공식 요구사항

https://appdevchallenge.auto-hmg.io/guide/submission

실행 가능한 release APK, 팀명_앱이름_release.apk 파일명, 소스와 APK를 포함한 저장소, README 실행 환경 안내, 접수 메일에 최종 커밋 번호 기재가 필요하다. 앱 기능 완성도와 APK 빌드 형식은 별개이며 이 저장소는 프로토타입 범위를 README에 명시한다.
