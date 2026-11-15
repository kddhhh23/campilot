# Campilot 집 주소·귀가 연동

작성: 2026-11-14

## 동작
현재 위치 확인 → 추천 받기 → 차박지 선택 → **귀가할 집 확인** → 출발 시간 선택 → 전력 확인.

Pleos NaviHelper 3.0.5의 getBookmarkInfo / onBookmarkInfo에서 category가 BookmarkType.HOME인 항목을 조회한다. 주소와 좌표를 사용자에게 보여주고 ‘이 주소로 귀가할게요’를 눌러야 다음 단계로 진행한다. 집 정보는 현재 여정의 ViewModel 메모리에만 유지한다.

집 미등록 시 requestNavigationScreen(NavigationScreenRequest.AddHomeFavorite)로 내비게이션 집 등록 화면을 연다. 이를 위해 pleos.car.permission.NAVI_CUSTOM_MAP를 추가했다. 돌아오면 집 정보를 다시 조회하며, 수동 재조회도 가능하다. 등록 화면 요청의 성공과 실제 집 등록 완료는 구분한다.

잘못된 좌표·조회 오류·시간 초과는 계산에 사용하지 않는다. HOME 항목이 있어도 빈 주소와 (0,0)이 반환되면 미등록으로 처리한다. 주소 확인 전에는 전력 계획을 생성하지 않는다.

확인한 집은 귀가 경로 요청 및 귀가 충전 후 목적지에 사용한다. 고정 ‘목업 집’과 차박지별 고정 귀가 거리 25/55/95km는 제거했다. 추천 차박지와 충전소 좌표는 기존 목업이다.

## 계산과 제한
현재 SDK TripRequest는 목적지만 지정하고 현재 위치를 출발점으로 사용한다. 선택한 미래 차박지를 출발점으로 임의 지정하는 필드는 없다. 경로 거리 확인을 위해 사용자 동의 전 내비 안내를 시작하는 방식은 적용하지 않았다.

따라서 사전 계획의 차박지→집 거리는 **좌표 간 대권 직선거리 × 1.3**의 임시 추정이다. 실제 도로 거리, 교량·통행 제한·우회 등을 반영하지 않는다. 전력 화면 본문과 상세에 추정값임을 표시했다. 실제 귀가 안내는 등록·확인한 집 좌표를 Pleos에 전달한다.

가는 전력(목업 거리/전비) + 현재부터 다음날 출발까지의 소비 가정(시간×1.2kW) + 귀가 전력 + 예비 배터리로 계산한다. 2026-11-15 사용자 요청으로 예비 배터리는 전체 용량의 10%로 고정하고, 기존 22% 하한 정책과 예비량 설정 입력·저장값 적용은 제거했다. 화면 명칭은 ‘예비 배터리’로 통일했다. 집 좌표가 바뀌면 귀가 거리와 충전 판단도 달라진다.

## 실제 확인 결과
- 에뮬레이터의 Pleos 내비에서 즐겨찾기 응답이 오는 것을 확인했다.
- 초기 조회에서 HOME의 유효 좌표를 얻지 못해 ‘집 등록 필요’ 상태로 처리했다.
- 앱의 ‘내비에서 집 등록’을 누르면 실제 UMOS/Pleos 내비의 **‘집을 검색하여 등록하세요.’** 화면이 열리는 것을 직접 확인했다.
- 사용자의 실제 집 주소는 제공되지 않았으므로 임의 주소를 등록하지 않았다. 실제 등록된 집의 정상 수신·확인은 사용자 등록 후 추가로 확인할 수 있다.
- 정상 집 확인 이후 계산 흐름은 테스트용 HomeGateway 응답과 실제 Pleos 배터리 조회를 이용해 검증했다. 테스트 집은 프로덕션 코드나 내비 즐겨찾기에 저장하지 않았다.

## 시연용 집 등록 후속 확인 (2026-11-15)
사용자의 요청에 따라 에뮬레이터 내비게이션에 시연용 주거지 **판교제2테크노밸리LH1단지아파트**를 집으로 등록했다. 주소는 **경기도 성남시 수정구 창업로40번길 6**이다. 사용자의 실제 집 주소가 아니다.

Campilot으로 돌아온 뒤 실제 Pleos HOME 조회 결과에 위 주소와 좌표(37.41271, 127.09616)가 표시되는 것을 확인했다. 앱의 ‘이 주소로 귀가할게요’ 확인 전 상태로 두었다. 등록 화면에서 처음 보였던 ‘솔숲 캠핑장 (목업)’은 최근 목적지였으며 등록된 집이 아니었다.

앱 코드 변경 없이 에뮬레이터의 내비 집 설정만 변경했으므로 빌드·자동 테스트는 추가 실행하지 않았다.

## 검증 범위
전체 테스트와 전체 린트는 실행하지 않았다.
- 최초 SDK 조회 테스트는 유효한 집이 있을 것이라는 가정 때문에 실패했다. 응답 검사와 미등록 처리로 구분했다.
- SDK 응답 확인용 기기 테스트 1개 통과.
- 집 확인 게이트·시간 변경에 따른 계산 검증용 기기 테스트 1개 통과.
- 귀가 거리 추정/기존 여정 계산 단위 테스트 10개 통과.
- assembleDebug 및 테스트 소스 컴파일 통과.
- 추정 거리 안내를 본문에 추가한 최종 APK 빌드 통과.
- 기존 도구·테스트 API deprecation 경고는 남아 있다.
- gradle-run finish는 wrapper 소유 로그만 정리했다.

검증 질문과 관측:
1. “Does the actual Pleos navigation provider answer the HOME bookmark request?” — 응답은 왔으나 유효 집 필수 가정에서 실패.
2. “Does the provider return an unregistered HOME placeholder with zero coordinates?” — 응답 검사 테스트 통과, 미등록 UI에서 재확인.
3. “Do home selection wiring and return-distance/energy calculations compile and pass the focused tests?” — 통과, 단위 10개.
4. “Does confirmed HOME gate planning and preserve time-based energy calculations?” — 통과, 기기 테스트 1개.
5. “Does the final APK build with visible estimated-return-distance disclosure?” — 통과.

## 변경 파일
- domain/HomeDestination.kt: 집 정보와 임시 귀가 거리 추정.
- navigation/PleosHomeGateway.kt: 집 조회·등록 화면 요청.
- ui/HomeScreen.kt: 집 확인 단계.
- ui/CampilotViewModel.kt: 집 상태·확인 게이트·귀가 계산 및 경로 목적지.
- domain/Journey.kt: HOME_SETUP 추가, 고정 목업 집 제거.
- MainActivity.kt / JourneyScreen.kt / DepartureScreen.kt: 화면 전환과 목적지 표시.
- 관련 집·거리·일정 테스트와 기존 흐름 테스트 조정.

## 결과물
[최신 APK](../artifacts/Campilot-home-debug.apk)를 에뮬레이터에 설치했다.

## 공식 참고
- [NaviHelper 소개와 수명 관리](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [즐겨찾기 조회](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-bookmark-info)
- 설치된 NaviHelper 3.0.5 / navi-data 3.0.4 소스에서 HOME 타입, BookmarkInfo 주소·좌표, AddHomeFavorite, TripRequest 필드, NAVI_CUSTOM_MAP 권한을 함께 확인했다.
