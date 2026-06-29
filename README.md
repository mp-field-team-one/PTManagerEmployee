# 알바 관리 · 알바 앱 (PTManagerEmployee)

알바(직원)용 근무 관리 안드로이드 앱. 내 근무를 확인하고, 출근하고, 대타·소통을 빠르게 처리하는 데 초점을 둔 단순한 구성입니다.

공통 백엔드([PTManagerBackend](../PTManagerBackend), Spring Boot · JWT)에 연결되어 로그인부터 근무·대타·공지·알림까지 실제 REST API로 동작합니다.

> 사장(관리자)용은 [PTManagerEmployer](../PTManagerEmployer)를 참고하세요. 같은 백엔드·데이터를 공유하되 화면은 역할별로 분리되어 있으며, 알바 앱은 **파랑(#3182F6)** 테마입니다.

## 화면 구성 (하단 4탭)

| 탭 | 설명 |
|---|---|
| **홈** | 오늘 근무 카드·출근하기, 대타 요청 / 전체 스케줄 바로가기, 새 공지 |
| **스케줄** | 주간 캘린더, 내 근무 시간, 근무 가능 시간 등록 |
| **소통** | 공지(백엔드 연동) / 메신저(정적 UI, 백엔드 미연동) |
| **내 정보** | 프로필 수정, 매장 멤버, 알림 설정, 로그아웃 |

### 주요 화면 (탭 외)
- **로그인 / 회원가입** — 이메일·비밀번호 기반(JWT). 매장 미소속 시 초대 코드로 가입 신청 → 사장 승인 후 진입
- **출근 체크인** — 근무에 대한 출근 기록(`POST /shifts/{id}/check-in`). QR 스캐너는 미연동 단계로 더미 토큰을 전송하며, 서버가 시점 기준으로 PRESENT/LATE 판정
- **시프트 상세** — 근무 날짜·시간, 대타 요청 진입
- **대타 요청** — 사유 입력 후 요청(`POST /swap-requests`) → 사장 승인으로 연결

## 기술 스택
- Kotlin, View 기반 XML 레이아웃 (Compose 미사용)
- 하단 탭 네비게이션 (`BottomNavigationView` + Fragment 전환)
- 네트워킹: Retrofit2 + OkHttp(로깅·인증 인터셉터) + Gson, Kotlin Coroutines
- 인증: JWT 액세스/리프레시 토큰을 `SharedPreferences`(`TokenStore`)에 저장, 요청 시 `Authorization: Bearer` 자동 부착
- `applicationId` : `com.example.ptmanageremployee`
- minSdk 35 / targetSdk 36, 라이트 전용 테마

## 백엔드 연동
- 데이터 계층: `com.example.ptmanageremployee.data` (`Network`·`ApiService`·`TokenStore`·`Dtos`)
- Base URL은 `local.properties`(버전관리 제외)의 `base.url` 값을 빌드 시 `BuildConfig.BASE_URL`로 주입합니다. 키가 없으면 기본값으로 폴백합니다.
  ```properties
  # local.properties
  base.url=http://10.0.2.2:8080/   # 에뮬레이터 → 호스트 PC의 localhost
  ```
  실제 기기/운영에서는 이 값을 서버 주소로 바꾸면 됩니다(소스 수정 불필요). 평문 HTTP 허용을 위해 `usesCleartextTraffic=true` 설정

## 빌드 & 실행
```bash
# 1) 백엔드 먼저 기동 (별도 터미널)
cd ../PTManagerBackend && ./gradlew bootRun   # H2 인메모리, 시드 데이터 자동 생성

# 2) 앱 빌드/설치
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # 연결된 기기/에뮬레이터에 설치
```

시드 계정으로 바로 로그인할 수 있습니다 — 직원: `employee@ptmanager.test` / `password` (시드 매장 초대코드 `CAFE01`).
