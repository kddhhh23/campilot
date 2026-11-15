# UI/UX 개편 실행 기록

계획: docs/superpowers/plans/2026-11-14-campilot-ux-redesign.md

- 적용: frontend-design / ui-ux-pro-max. 기존 설치본 사용, GitHub 원본 확인. 스킬 추가 설치 없음.
- Ruling: 사용자 자율 구현 요청에 따라 계획 승인 대기 없이 실행. Git 저장소가 없어 IDE 도구로 직접 수정.
- Ruling: 웹 랜딩 권고/영문 글꼴/실험 adaptive API는 이번 한글 차량 앱에 채택하지 않음. 기존 상태 흐름과 안정된 Compose 레이아웃 유지.
- FirstUseTest 0001: 2개 실패. 개편 후 0002 전체 테스트 통과.
- 독립 최종 리뷰: 숨겨진 온도 입력 검증, 오래된 충전/체류 예상, 영문 경로 실패 표시의 3개 중요 문제.
- DesignStateTest 0003: 3개 실패 재현. 0004: 수정 후 단위 19/19 + 기기 26/26 + assembleDebug/lintDebug 성공.
- Ruling: 차량 모드에서 사용하지 않는 시작 온도만 설정 범위로 정규화한다. 실제 차량 설정 온도를 변경하지 않는다.
- 경로 실패는 routeError 상태로 명시. 오류 문자열 언어에 따른 누락 제거.
- 실제 최종 APK를 재설치하여 낮/밤 화면을 확인. 풍경이 텍스트 영역을 침범하던 Canvas clipping 수정.
- 정적 리뷰에서 판단하지 않은 시각 영역은 루트의 에뮬레이터 확인으로 검증. 실차 기능/사람 대상 사용성 실험은 이번 범위 밖으로 보고서에 명시.
- 미해결 중요 리뷰 결함 없음. 기존 라이브러리 deprecation 경고/버전 정리는 후속 범위.
- Task 1/2/3/4 완료. 산출물과 SHA-256은 UIUX 개선 보고서에 기록.
