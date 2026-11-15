# Campilot 스킬 설치 기록

## 현재 상태 — 2026-11-12 정리 후

사용자 요청에 따라 프로젝트 스킬을 **74개에서 10개로 축소**했다. 아래 최초 설치 기록은 이력으로 보존한다.

| 용도 | 유지한 스킬 |
|---|---|
| Android 화면·테스트 | adaptive, edge-to-edge, testing-setup |
| Kotlin·Compose | compose-state-and-effects, kotlin-concurrency-and-flow |
| 디자인 | ui-ux-pro-max, frontend-design |
| 계획·디버깅·검증 | writing-plans, systematic-debugging, verification-before-completion |

- 활성 검색 경로: `.agents/skills/`
- 나머지 64개 보관 경로: `.skill-archive/2026-11-12/` (스킬 검색 경로 밖)
- 스킬별 데이터·스크립트·참조 파일도 디렉터리와 함께 보존했다.
- 복원할 때 해당 스킬 디렉터리를 `.agents/skills/`로 이동하면 된다. 같은 이름이 있으면 덮어쓰지 않는다.
- 기존 구현 계획이나 유지한 스킬이 navigation-3, executing-plans, test-driven-development 등 보관된 스킬을 요구하면 해당 작업 시 필요한 것만 복원한다.
- 전역/IDE 기본 제공 스킬은 변경하지 않았다. 따라서 IDE 전체 목록의 수가 10개라는 뜻은 아니다.
- 현재 대화에 이미 제공된 스킬 목록은 남아 있을 수 있다. 새 대화에서 목록을 갱신한다.
- 앱 코드와 구현 계획의 기능 범위는 변경하지 않았다.

## 최초 설치 이력

- 설치일: 2026-11-12
- 범위: 프로젝트 전용 `.agents/skills/`
- 방법: skill-installer의 install-skill-from-github.py, 원본 GitHub 저장소의 main을 조회해 commit SHA로 다운로드
- 결과: 5개 후보, 총 74개 SKILL.md 설치. 전역 스킬은 수정하지 않음.
- 다음 대화부터 새 스킬을 사용할 수 있다. IDE 목록이 갱신되지 않으면 대화 또는 IDE를 새로 열어 확인한다.

| 원본 | 설치 범위 | 개수 |
|---|---|---|
| https://github.com/android/skills | 공식 저장소의 모든 스킬 디렉터리 | 36 |
| https://github.com/chrisbanes/skills | skills 아래 모든 스킬 | 21 |
| https://github.com/anthropics/skills | skills/frontend-design만 | 1 |
| https://github.com/nextlevelbuilder/ui-ux-pro-max-skill | .claude/skills/ui-ux-pro-max 및 내부 데이터·스크립트·참조 | 1 |
| https://github.com/obra/superpowers | skills 아래 모든 스킬 | 15 |

## 검증

- 설치 명령 종료 코드 0, 각 디렉터리 설치 성공 출력 확인.
- IDE 파일 검색으로 SKILL.md 74개 확인.
- ui-ux-pro-max의 search.py를 chart 도메인으로 실행하여 종료 코드 0 및 검색 결과 확인. 검색 결과를 차량 UI 설계로 자동 채택하지 않음.
- 앱 코드·Gradle·Manifest는 변경하지 않았으며 앱 빌드는 실행하지 않음.
- 각 스킬의 모든 실행 경로를 검증한 것은 아님. 일부 워크플로는 별도 외부 스킬·도구를 요구할 수 있음.
- 스킬 디렉터리 설치이며 저장소별 플러그인 훅·전역 CLI를 설치한 것은 아님.

## 적용 원칙

설치와 사용은 다르다. 작업과 관련된 스킬만 읽고 적용한다. 일반 차량·웹 규칙보다 Pleos 규칙과 사용자 요구를 우선한다. 두 디자인 스킬은 주 디자인 작성과 검토 역할을 나누고, 설치만으로 앱을 React/웹으로 바꾸지 않는다. 병렬 에이전트·외부 전송·자동 커밋 등은 설치만으로 실행되지 않는다.

기존 [후보 조사](Pleos_개발_스킬_후보.md)의 미설치 표기는 조사 당시 상태다. 현재 설치 상태는 이 문서를 기준으로 한다.
[구현 계획](superpowers/plans/2026-11-12-campilot-prototype.md)은 계획만 작성했으며 미착수다.
