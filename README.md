# 알바 관리 · 알바 앱 (PTManagerEmployee)

알바(직원)용 근무 관리 안드로이드 앱. 내 근무를 확인하고, 출근하고, 대타·소통을 빠르게 처리하는 데 초점을 둔 단순한 구성입니다.

> 사장(관리자)용은 [PTManagerEmployer](../PTManagerEmployer)를 참고하세요. 같은 데이터를 공유하되 화면은 역할별로 분리되어 있으며, 알바 앱은 **파랑(#3182F6)** 테마입니다.

## 화면 구성 (하단 4탭)

| 탭 | 설명 |
|---|---|
| **홈** | 오늘 근무 카드·출근하기, 대타 요청 / 전체 스케줄 바로가기, 새 공지 |
| **스케줄** | 주간 캘린더, 내 근무 시간, 근무 가능 시간 등록 |
| **소통** | 공지 / 메신저 (사장님 공지 확인, 대화) |
| **내 정보** | 프로필 수정, 매장 멤버, 알림 설정, 로그아웃 |

### 주요 화면 (탭 외)
- **출근 체크인** — GPS · QR 기반 출근
- **시프트 상세** — 근무지·포지션·함께 근무·예상 급여, 대타 요청 진입
- **대타 요청** — 전체공개/지정, 사유 입력 후 요청 → 사장 승인으로 연결

## 기술 스택
- Kotlin, View 기반 XML 레이아웃 (Compose 미사용)
- 하단 탭 네비게이션 (`BottomNavigationView` + Fragment 전환)
- `applicationId` : `com.example.ptmanageremployee`
- minSdk 35 / targetSdk 36, 라이트 전용 테마

## 빌드 & 실행
```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # 연결된 기기/에뮬레이터에 설치
```
