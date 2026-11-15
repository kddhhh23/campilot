# Campilot 개발 스킬 후보 조사

- 조사일: 2026-11-12 (KST)
- 범위: 온라인·GitHub 공개 Agent Skills의 출처, 인지도, Android/Pleos 적합성 비교
- 상태: 후보 조사만 완료. 신규 스킬 설치·활성화, 앱 코드 구현은 하지 않았다.
- 전제: Campilot은 Pleos Connect용 차박 비서 프로토타입이다. Compose 채택 여부는 구현 단계에서 결정한다.

## 1. 결론

추천 조합은 **Android 공식 스킬 + 필요한 Kotlin/Compose 전문 스킬 + 디자인 스킬 한 종류**다. 개발 절차 보조는 문제가 생기거나 검증이 필요한 작업에서 선택한다.

Pleos 전용으로 검증된 유명 공개 SKILL.md 패키지는 이번 검색에서 확인하지 못했다. 이것이 존재하지 않는다는 단정은 아니다. 일반 Android 스킬이 Pleos SDK·권한·CCP·주행 제한까지 해결해 준다고 간주하지 않는다.

Pleos 규칙의 프로젝트 기준은 [공식 규칙 조사 문서](Pleos_앱_개발_필수규칙.md)를 사용한다. 이 문서는 아직 자동 실행 스킬이 아니라 참고 문서다.

## 2. 후보 비교

별 수는 조사 당시 GitHub 화면의 반올림 표시이며 **저장소 전체** 기준이다. 개별 스킬의 설치 수, 품질 보증, Pleos 호환성 인증을 의미하지 않는다.

| 후보 / 원본 저장소 | 인지도 근거 | 역할 | Campilot 판단 |
|---|---|---|---|
| [Android 공식 Skills](https://github.com/android/skills) | 약 10.5k stars, Google 공식 Android 문서에서 안내 | 화면 적응, 인셋, 내비게이션, 테스트, 성능 등 | 기술 기반으로 우선 |
| [Chris Banes Skills](https://github.com/chrisbanes/skills) | 약 1.2k stars, Android Developers 블로그에서 소개 | Kotlin·Compose 상태·동시성·컴포넌트 설계 | 상태·비동기 처리에 우선 |
| [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) | 약 148.1k stars | 색상·글자·배치·차트·디자인 시스템, Compose 지원 | 디자인 후보 1순위 |
| [Anthropic frontend-design](https://github.com/anthropics/skills/tree/main/skills/frontend-design) | 상위 저장소 약 192.8k stars, Anthropic 제공 | 제품에 맞는 시각 방향·타이포·레이아웃 | 디자인 대안 또는 발표용 웹 데모 |
| [Superpowers](https://github.com/obra/superpowers) | 약 316.1k stars | 계획·디버깅·테스트·검증·리뷰 절차 | 필요한 작업별 선택 |

공식성 확인: [Android Skills 소개](https://developer.android.com/tools/agents/android-skills).
Chris Banes 컬렉션 소개 및 과도한 스킬 설치에 관한 설명: [Android Developers 블로그](https://developer.android.com/blog/posts/inside-android-skills-built-for-deprecation).

## 3. Android 공식 Skills — 필요한 것만 선택

공식 목록: [Browse Android skills](https://developer.android.com/tools/agents/android-skills/browse).

| 스킬 | 활용할 작업 | 적용 조건·주의점 |
|---|---|---|
| adaptive | 좁은 창·넓은 창 레이아웃, 화면 변경 시 상태 유지 | 현재 원문은 Compose와 Navigation 3를 전제한다. 채택 전 의존성 확인 |
| edge-to-edge | 시스템 바·키보드에 가려지지 않는 UI | Pleos의 입력창 유무별 IME 정책을 우선 |
| navigation-3 | 캠핑장 목록→상세→차박 계획→모니터링 이동 | Navigation 3를 채택할 때 사용. Pleos 필수 라이브러리는 아님 |
| testing-setup | 전력 계산 단위 테스트, 핵심 화면·상태 복원 검증 | 프로토타입에 필요한 범위만 구성 |
| android-profiler | 시연 중 버벅임·CPU·메모리 문제 분석 | 실제 성능 문제가 있거나 측정 단계일 때 |
| android-cli | SDK·에뮬레이터·문서 조회 등 개발 도구 작업 | 기존 IDE 도구와 중복되는 작업은 불필요하게 추가하지 않음 |

[adaptive 원문](https://github.com/android/skills/blob/main/jetpack-compose/adaptive/SKILL.md)은 2026-11-05 갱신 표기를 확인했다. 최신 API 사용 제안이 있으므로 프로젝트 버전과 맞춰야 한다. 화면 적응 원칙은 유용하지만 일반 모바일·데스크톱의 방향·내비게이션 정책을 Pleos에 무조건 이식하지 않는다.

앞선 IDE 제공 목록에 adaptive, edge-to-edge, navigation-3, testing-setup, android-profiler, android-cli 등이 이미 노출되어 있었다. 따라서 신규 설치보다 사용 시점에 실제 파일과 버전을 확인하는 것이 먼저다. 이 목록만으로 누가 언제 다운로드했는지는 알 수 없다.

### 이름만 보고 적용하면 안 되는 스킬

[android-for-cars-parked-apps](https://github.com/android/skills/blob/main/cars/android-for-cars-parked-apps/SKILL.md)는 Google Play 대상 영상·게임·브라우저 앱의 Android Auto/AAOS 대응이 중심이다. Campilot은 이 분류와 다르므로 기본 스킬로 추천하지 않는다. 해당 문서의 호환성 모드나 배포 카테고리 규정을 Pleos App Market에 그대로 적용하지 않는다.

AGP 업그레이드·Firebase·AI 모델·CameraX 스킬도 해당 기능을 도입할 때만 검토한다. 현재 네 가지 핵심 기능 때문에 모두 설치할 필요는 없다.

## 4. Chris Banes Skills — 코드 품질과 재작업 감소

[README](https://github.com/chrisbanes/skills#readme)에서 다음 후보를 확인했다. Google 공식 패키지가 아니라 Android 전문 커뮤니티 컬렉션이다.

| 개별 스킬 | Campilot 활용 예 |
|---|---|
| [compose-state-and-effects](https://github.com/chrisbanes/skills/blob/main/skills/compose-state-and-effects/SKILL.md) | 선택 캠핑장·체류 시간·목표 온도의 상태 소유권, 화면 복귀와 구독 정리 |
| [kotlin-concurrency-and-flow](https://github.com/chrisbanes/skills/blob/main/skills/kotlin-concurrency-and-flow/SKILL.md) | 배터리 스트림, 공조 명령 취소, 중복 구독·이벤트 재실행 방지 |
| [compose-component-design](https://github.com/chrisbanes/skills/blob/main/skills/compose-component-design/SKILL.md) | 잔량 카드·온도 조절·예상 전력 표시를 재사용 가능한 컴포넌트로 구성 |
| [compose-ui-testing-patterns](https://github.com/chrisbanes/skills/blob/main/skills/compose-ui-testing-patterns/SKILL.md) | 주요 조작, 비활성 상태, 화면별 표시 검증 |
| [compose-performance](https://github.com/chrisbanes/skills/blob/main/skills/compose-performance/SKILL.md) | 실시간 수치 갱신 시 불필요한 재구성 분석 |

초기에는 상태와 Flow 관련 두 가지가 특히 적합하다는 판단이다. 컴포넌트·성능 스킬은 해당 작업에서 추가한다. 여기서 효율은 토큰이나 작성 시간을 반드시 줄인다는 뜻이 아니라 오류·재작업 감소를 목표로 한다. 개별 스킬의 세부 실행 지침과 참조 파일은 실제 사용 전에 읽어야 한다.

## 5. UI UX Pro Max — 디자인 후보 1순위

[원문 SKILL.md](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/blob/main/.claude/skills/ui-ux-pro-max/SKILL.md)와 [README](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill#readme)를 확인했다.

- 색상, 글꼴 조합, 배치, 접근성, 차트 선택을 위한 검색형 자료를 제공한다.
- 지원 스택에 Jetpack Compose가 포함된다. 검색 스크립트는 Python 3.x를 요구한다.
- 기본 스택은 HTML+Tailwind이므로 사용 시 Kotlin/Jetpack Compose와 차량 디스플레이를 명시해야 한다.
- 일반 안내에 웹 기준 터치 크기·글자 크기·hover·애니메이션 규칙이 섞여 있다. 이를 Pleos 공식 수치로 사용하지 않는다.

**Campilot 활용 제안:** 야간에 눈부심이 적은 색상, 남은 에너지·충전 필요 여부의 정보 위계, 예상치와 실측치 구분, 단순한 에너지 그래프를 설계한다. 경고를 색만으로 구분하지 않고 텍스트·아이콘을 함께 제공한다. 과한 움직임보다 상태 변화가 분명한 UI를 우선한다.

## 6. Anthropic frontend-design — 시각 방향의 대안

[원문 SKILL.md](https://github.com/anthropics/skills/blob/main/skills/frontend-design/SKILL.md)를 확인했다. 제품 주제에 맞는 색상·글꼴·배치와 디자인 초안 검토를 강조한다.

Campilot만의 시각 방향이나 발표용 웹 프로토타입을 구상하는 데 유용하다는 판단이다. 다만 Pleos·Kotlin 구현 전용 스킬은 아니다. 독특한 표현을 강조하는 지침보다 차량 가독성·일관성과 주행 안전을 우선한다.

UI UX Pro Max와 동시에 기본 활성화하기보다 한 가지를 주 디자인 보조로 선택하는 편을 추천한다. 서로 다른 디자인 지침을 불필요하게 중첩하지 않기 위해서다.

## 7. Superpowers — 개발 절차 보조

[저장소 README](https://github.com/obra/superpowers#readme)와 아래 원문을 확인했다.

| 후보 | 활용 |
|---|---|
| [systematic-debugging](https://github.com/obra/superpowers/blob/main/skills/systematic-debugging/SKILL.md) | 재현→원인 확인→가설 검증→수정 순서로 빌드·상태 오류 조사 |
| [verification-before-completion](https://github.com/obra/superpowers/blob/main/skills/verification-before-completion/SKILL.md) | 완료 주장 전에 빌드·테스트·요구사항 근거 확인 |
| writing-plans / requesting-code-review | 구현 계획 세분화 및 리뷰. README에서 제공 여부 확인 |

전체 프레임워크는 설계 승인, worktree, 테스트 우선 개발, 리뷰 등의 절차를 포함한다. 무조건 코딩을 빠르게 만드는 도구로 보기는 어렵다. 작은 프로토타입에서는 절차·토큰 부담이 증가할 수 있다.

**추천:** 디버깅·검증 목적부터 검토한다. 개별 파일만 복사할 경우 참조하는 다른 스킬·자료도 확인해야 하므로 완전히 독립 실행된다고 보장하지 않는다. 전체 자동 워크플로 설치는 보류한다.

## 8. Campilot 최소 도입 제안

1. 프로젝트 기준: 기존 Pleos 규칙 문서를 작업마다 참조한다.
2. 디자인 단계: UI UX Pro Max 또는 frontend-design 중 하나를 선택한다.
3. Compose 구현을 결정하면: compose-state-and-effects, kotlin-concurrency-and-flow를 우선 검토한다.
4. 가변 창·인셋·화면 이동 구현 때: 공식 adaptive, edge-to-edge, navigation-3 중 해당 항목만 사용한다.
5. 오류·검증 단계: systematic-debugging, verification-before-completion 및 필요한 Android 테스트 도구를 활용한다.

스킬은 앱에 포함되는 SDK가 아니라 개발 에이전트의 작업 지침이다. 설치만으로 앱 기능이나 Pleos 지원이 생기지 않는다. 후보의 호환성은 공개 설명과 원문을 바탕으로 판단했으며, Campilot에서 실행·성능 검증한 결과는 아니다.

## 9. 조사 범위와 다음 단계

- GitHub 원본 저장소의 인지도, README, 핵심 SKILL.md를 조사했다.
- Android 공식 자료에서 공식 패키지와 커뮤니티 후보를 교차 확인했다.
- Pleos 관련 SKILL.md 검색에서는 추천할 만한 전용 패키지를 확인하지 못했다.
- 모든 후보의 하위 스크립트·참조 파일을 전수 검토한 것은 아니다.
- 이번 작업에서 스킬 다운로드·설치·업데이트·프로젝트 의존성 변경은 하지 않았다.
- 설치할 때는 현재 에이전트와 Android Studio 버전에 맞는 경로·인식 방법, 이미 있는 스킬과의 중복, 라이선스와 참조 파일을 확인한다.

Android Studio의 가져오기 경로와 버전 차이는 [공식 스킬 사용 안내](https://developer.android.com/studio/gemini/skills)를 참고한다. 설치 경로가 존재하거나 IDE 목록에 표시된다는 사실만으로 자동 다운로드 경위까지 추정하지 않는다.
