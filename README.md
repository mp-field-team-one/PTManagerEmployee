# 알바 관리 · 알바 앱 (PTManagerEmployee)

알바(직원)용 근무 관리 안드로이드 앱. 내 근무를 확인하고, 출근하고, 대타·소통을 처리한다.
공통 백엔드([Backend](https://github.com/PTManager/Backend), Spring Boot · JWT)에 붙어 실제 REST API로 동작한다. 사장용은 [EmployerApp](https://github.com/PTManager/EmployerApp).

요구사항은 [SPEC.md](https://github.com/PTManager/docs/blob/main/SPEC.md), 구현 계획은 [PLAN.md](https://github.com/PTManager/docs/blob/main/PLAN.md), 작업 현황은 [TASKS.md](https://github.com/PTManager/docs/blob/main/TASKS.md).

## 화면

하단 5탭 — **홈**(오늘 근무·출근하기·소식) · **스케줄**(주간 캘린더) · **소통**(공지·인수인계·대타요청) · **통계**(이번 달 내 급여, 실근태 기준) · **내 정보**(프로필·멤버·알림 설정).

탭 외: 로그인/회원가입(미소속이면 초대 코드로 가입 신청 → 사장 승인), **출근 체크인**(매장 QR 스캔 → `check-in`/`check-out`), 시프트 상세, 대타 요청·지원.

## 기술 스택

Kotlin · View 기반 XML 레이아웃(Compose 미사용) · `BottomNavigationView` + Fragment · Retrofit2 + OkHttp(로깅·인증 인터셉터) + Gson · Coroutines.
JWT 토큰은 Keystore 기반 암호화 저장소에 보관하고 요청 시 `Authorization: Bearer`를 자동 부착한다.
`applicationId` `com.example.ptmanageremployee` · minSdk 29 / targetSdk 36 · 라이트 전용 테마 · 메인 컬러 **Action Blue(#0066CC)**.

데이터 계층은 `com.example.ptmanageremployee.data`(`Network`·`ApiService`·`TokenStore`·`Dtos`).

## 빌드 & 실행

```bash
(cd ../Backend && ./gradlew bootRun)   # 1) 백엔드 기동 (H2 인메모리, 시드 자동 생성)
./gradlew installDebug               # 2) 연결된 기기/에뮬레이터에 설치
```

Base URL은 `local.properties`(커밋 제외)의 `base.url`을 빌드 시 `BuildConfig.BASE_URL`로 주입한다. 없으면 기본값으로 폴백.

```properties
base.url=http://10.0.2.2:8080/   # 에뮬레이터 → 호스트 PC의 localhost
```

HTTP는 디버그 빌드에서만 허용되고 릴리스는 HTTPS가 아니면 기동하지 않는다. FCM을 쓰려면 `app/google-services.json`을 로컬에 두되 커밋하지 않는다.

시드 계정으로 바로 로그인 — `employee@ptmanager.test` / `password` (매장 초대코드 `CAFE01`).
