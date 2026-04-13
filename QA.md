# QA.md

## 목적
- Re:Start Point 웹 서비스의 기능 테스트 케이스를 한 곳에서 관리한다.
- 신규 기능 개발 시 이 문서에 테스트 케이스를 먼저 추가하거나 함께 업데이트한다.
- 수동 QA와 향후 Playwright 기반 E2E 자동화의 기준 문서로 사용한다.

## 범위
- 사용자 웹(`frontend`)
- 관리자 웹(`admin`)

## 운영 원칙
1. 기능 추가/변경 시 관련 섹션에 테스트 케이스를 추가한다.
2. 버그가 발생하면 재현 케이스와 회귀 방지 케이스를 함께 기록한다.
3. 우선순위는 아래처럼 본다.
   - P0: 로그인, 회원가입, 시즌 참여, 팀 생성/지원, 관리자 승인
   - P1: 프로필, AI 추천, 필터/탭, 상태 전이
   - P2: 반응형, 문구, 빈 상태, 예외 처리
4. 자동화 후보는 `AUTO` 컬럼으로 표시한다.

## 공통 체크리스트

### 공통 UI
- [ ] 페이지 진입 시 로딩 상태가 자연스럽게 보인다.
- [ ] API 실패 시 에러 문구가 사용자에게 보인다.
- [ ] 빈 데이터일 때 empty state가 어색하지 않다.
- [ ] 버튼 disabled 상태가 중복 클릭을 막는다.
- [ ] 뒤로가기/직접 URL 접근 시 페이지가 비정상 상태가 되지 않는다.
- [ ] 모바일/데스크톱에서 주요 CTA가 가려지지 않는다.

### 권한/인증
- [ ] 비로그인 사용자는 보호 페이지 접근 시 로그인 페이지로 이동한다.
- [ ] 로그인 사용자는 로그인/회원가입 페이지 접근 시 홈으로 리다이렉트된다.
- [ ] 수료 인증 미완료 사용자는 시즌 참여/팀 지원/팀 생성이 제한된다.
- [ ] 관리자 페이지는 비인증 시 `/login`으로 이동한다.

---

# 1. 사용자 웹 QA (`frontend`)

## 1-1. 홈 `/`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-HOME-001 | P1 | 비로그인 상태로 홈 진입 | 시작하기, 시즌 둘러보기 CTA가 보인다 | (로그아웃) | Y |
| FE-HOME-002 | P1 | 로그인 상태로 홈 진입 | 시작하기 대신 시즌 참여하기 CTA가 보인다 | `test@example.com` | Y |
| FE-HOME-003 | P2 | 주요 섹션 스크롤 확인 | 기능 소개, 진행 과정, CTA 섹션이 정상 노출된다 | 아무 계정 | N |
| FE-HOME-004 | P2 | 헤더 네비게이션 클릭 | 시즌, 팀 탐색, 내 팀(로그인 시) 이동이 정상 동작한다 | `test@example.com` | Y |

## 1-2. 로그인 `/login`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-LOGIN-001 | P0 | 정상 계정 로그인 | 홈으로 이동하고 로그인 상태가 유지된다 | `test@example.com` | Y |
| FE-LOGIN-002 | P0 | 존재하지 않는 이메일 로그인 | `등록되지 않은 이메일입니다.` 에러가 보인다 | `nouser@test.com` | Y |
| FE-LOGIN-003 | P0 | 비밀번호 불일치 | `비밀번호가 일치하지 않습니다.` 에러가 보인다 | `test@example.com` + 틀린 비밀번호 | Y |
| FE-LOGIN-004 | P0 | 이메일 미인증 계정 로그인 | 이메일 인증 필요 안내가 보인다 | (테스트 데이터 없음) | Y |
| FE-LOGIN-005 | P1 | 네트워크 오류 발생 | 네트워크 오류 안내 문구가 보인다 | 아무 계정 | Y |
| FE-LOGIN-006 | P2 | 비밀번호 보기/숨기기 클릭 | input type이 password/text로 전환된다 | 아무 계정 | Y |
| FE-LOGIN-007 | P1 | 이미 로그인된 상태로 `/login` 진입 | 홈으로 리다이렉트된다 | `test@example.com` (로그인 상태) | Y |
| FE-LOGIN-008 | P2 | USER_001 에러 상태 | 회원가입 하러 가기 링크가 노출된다 | `nouser@test.com` | Y |

## 1-3. 회원가입 `/signup`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-SIGNUP-001 | P0 | 이메일 입력 후 인증 코드 발송 | 인증 코드 입력 step으로 이동한다 | (신규 이메일) | Y |
| FE-SIGNUP-002 | P0 | 인증 코드 6자리 미만 제출 | `6자리 인증 코드를 입력해주세요.` 에러가 보인다 | (신규 이메일) | Y |
| FE-SIGNUP-003 | P0 | 올바른 인증 코드 입력 | 회원정보 입력 step으로 이동한다 | (신규 이메일) | Y |
| FE-SIGNUP-004 | P0 | 잘못된 인증 코드 입력 | 에러 표시 후 코드 입력값이 초기화된다 | (신규 이메일) | Y |
| FE-SIGNUP-005 | P1 | 인증 코드 붙여넣기 | 6칸에 자동 분배된다 | (신규 이메일) | Y |
| FE-SIGNUP-006 | P1 | 재발송 제한 시간 내 재발송 클릭 | 버튼이 비활성화되거나 재발송되지 않는다 | (신규 이메일) | Y |
| FE-SIGNUP-007 | P0 | 비밀번호/확인 불일치 | 회원가입이 막히고 에러 문구가 보인다 | (신규 이메일) | Y |
| FE-SIGNUP-008 | P0 | 8자 미만 비밀번호 | 회원가입이 막히고 에러 문구가 보인다 | (신규 이메일) | Y |
| FE-SIGNUP-009 | P0 | 정상 회원가입 완료 | 자동 로그인 후 홈으로 이동한다 | (신규 이메일) | Y |
| FE-SIGNUP-010 | P1 | 이미 로그인된 상태로 `/signup` 진입 | 홈으로 리다이렉트된다 | `test@example.com` (로그인 상태) | Y |

## 1-4. 수료 인증 `/certification`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-CERT-001 | P0 | 비로그인 사용자가 접근 | 로그인 페이지로 이동한다 | (로그아웃) | Y |
| FE-CERT-002 | P0 | 모든 필드 정상 입력 후 제출 | 인증 요청이 접수되고 상태가 갱신된다 | `newbie@test.com` | Y |
| FE-CERT-003 | P1 | 잘못된 URL 또는 서버 검증 실패 | 에러 문구가 표시된다 | `newbie@test.com` | Y |
| FE-CERT-004 | P1 | 인증 상태가 `PENDING`인 사용자가 진입 | 대기 상태 화면과 새로고침 버튼이 보인다 | `pending@restart-point.com` | Y |
| FE-CERT-005 | P1 | 인증 상태가 `APPROVED`인 사용자가 진입 | 인증 완료 화면이 보인다 | `test@example.com` | Y |
| FE-CERT-006 | P1 | 인증 상태가 `REJECTED`인 사용자가 진입 | 거절 안내와 재신청 폼이 보인다 | `rejected@test.com` | Y |
| FE-CERT-007 | P2 | 상태 새로고침 클릭 | 최신 사용자 상태를 다시 조회한다 | `pending@restart-point.com` | Y |

## 1-5. 프로필 `/profile`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-PROFILE-001 | P0 | 비로그인 접근 | 로그인 페이지로 이동한다 | (로그아웃) | Y |
| FE-PROFILE-002 | P0 | 역할 미선택 상태로 저장 | 저장이 막히고 에러가 보인다 | `test@example.com` | Y |
| FE-PROFILE-003 | P1 | 역할 선택 후 저장 | 저장 성공 메시지가 보인다 | `test@example.com` | Y |
| FE-PROFILE-004 | P1 | 기존 프로필 보유 사용자가 진입 | 저장된 값이 폼에 채워진다 | `test@example.com` | Y |
| FE-PROFILE-005 | P1 | 기술 스택 토글 선택/해제 | chip 선택 상태가 정상 반영된다 | `test@example.com` | Y |
| FE-PROFILE-006 | P1 | 관심 도메인 토글 선택/해제 | chip 선택 상태가 정상 반영된다 | `test@example.com` | Y |
| FE-PROFILE-007 | P2 | 숫자 입력 필드 공란 처리 | 빈 값일 때 undefined로 저장되어 오류가 나지 않는다 | `test@example.com` | N |
| FE-PROFILE-008 | P2 | 취소 버튼 클릭 | 이전 페이지로 이동한다 | `test@example.com` | Y |

## 1-6. 시즌 목록 `/seasons`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-SEASONS-001 | P0 | 시즌 목록 정상 조회 | 시즌 카드 목록이 렌더링된다 | 아무 계정 | Y |
| FE-SEASONS-002 | P1 | 시즌이 0건인 경우 | empty state가 노출된다 | 아무 계정 | Y |
| FE-SEASONS-003 | P1 | API 실패 | 에러 문구가 보인다 | 아무 계정 | Y |
| FE-SEASONS-004 | P1 | 참여 가능한 시즌 (RECRUITING/TEAM_BUILDING) | `참여 가능` 배지가 보인다 | 아무 계정 | Y |
| FE-SEASONS-005 | P1 | 시즌 카드 클릭 | 시즌 상세로 이동한다 | 아무 계정 | Y |
| FE-SEASONS-006 | P1 | 비로그인 사용자 시즌 목록 조회 | 참여 정보 없이 시즌 목록만 보인다 | (로그아웃) | Y |
| FE-SEASONS-007 | P1 | 로그인 + 참여 중인 시즌 | `참여 중: {팀명}` 배지가 보인다 | `test@example.com` | Y |
| FE-SEASONS-008 | P1 | 로그인 + 팀 없는 사용자 | `참여 가능` 배지만 보인다 (참여 중 배지 없음) | `java7ang@gmail.com` | Y |

## 1-7. 시즌 상세 `/seasons/:seasonId`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-SEASON-001 | P0 | 존재하는 시즌 상세 진입 | 제목, 설명, 일정, 심사 비중이 보인다 | 아무 계정 | Y |
| FE-SEASON-002 | P0 | 존재하지 않는 시즌 ID 접근 | 오류 안내 후 목록 이동 CTA가 보인다 | 아무 계정 | Y |
| FE-SEASON-003 | P0 | 비로그인 + 참여 가능한 시즌 | 로그인 필요 안내가 보인다 | (로그아웃) | Y |
| FE-SEASON-004 | P0 | 로그인 + 미인증 사용자 | 수료 인증 필요 안내가 보인다 | `newbie@test.com` | Y |
| FE-SEASON-005 | P0 | 로그인 + 인증 완료 + 참여 가능 시즌 + 팀 없음 | `팀 찾기 / 팀 만들기` 버튼이 보인다 | `java7ang@gmail.com` → 2026 봄 시즌 | Y |
| FE-SEASON-005-1 | P0 | 로그인 + 인증 완료 + 참여 불가 시즌(`IN_PROGRESS`) | `팀 찾기 / 팀 만들기` 버튼이 숨겨진다 | `java7ang@gmail.com` → 2024 겨울 시즌 | Y |
| FE-SEASON-006 | P1 | 현재 상태와 타임라인 active 구간 확인 | 현재 진행 중인 단계가 강조된다 | 아무 계정 | Y |
| FE-SEASON-007 | P1 | 로그인 + 참여 중인 시즌 | `참여 중` 배지 + 팀 안내 배너가 보인다 | `test@example.com` | Y |
| FE-SEASON-008 | P1 | 로그인 + 참여 중인 시즌 | `내 팀 보기` 버튼이 보인다 | `test@example.com` | Y |
| FE-SEASON-009 | P1 | 로그인 + 참여 중인 시즌 | `팀 찾기 / 팀 만들기` 버튼이 숨겨진다 | `test@example.com` | Y |
| FE-SEASON-010 | P1 | 내 팀 보기 버튼 클릭 | 해당 팀 상세 페이지로 이동한다 | `test@example.com` | Y |

## 1-8. 팀 목록 `/teams`, `/seasons/:seasonId/teams`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-TEAMS-001 | P0 | 활성 시즌 존재, 팀 목록 조회 | 팀 카드 목록이 보인다 | 아무 계정 | Y |
| FE-TEAMS-002 | P1 | 활성 시즌 없음 | 시즌 없음 empty state가 보인다 | 아무 계정 | Y |
| FE-TEAMS-003 | P1 | 전체/모집중 필터 전환 | 조건에 맞는 팀만 노출된다 | 아무 계정 | Y |
| FE-TEAMS-004 | P1 | 역할 필터 전환 | 해당 역할 모집 팀만 남는다 | 아무 계정 | Y |
| FE-TEAMS-005 | P1 | 비로그인 사용자 진입 | 목록은 조회 가능하지만 팀 생성/AI 추천 버튼은 숨김 | (로그아웃) | Y |
| FE-TEAMS-006 | P0 | 인증 완료 사용자 + 참여 가능 시즌 | 팀 만들기, AI 추천 버튼이 보인다 | `java7ang@gmail.com` → 2026 봄 시즌 | Y |
| FE-TEAMS-006-1 | P0 | 인증 완료 사용자 + 참여 불가 시즌(`IN_PROGRESS`) | 팀 만들기, AI 추천 버튼이 숨겨진다 | `java7ang@gmail.com` → 2024 겨울 시즌 | Y |
| FE-TEAMS-007 | P0 | 팀 생성 모달에서 이름 없이 제출 | 생성이 막히고 에러가 보인다 | `java7ang@gmail.com` | Y |
| FE-TEAMS-008 | P0 | 팀 생성 모달에서 본인 역할 미선택 제출 | 생성이 막히고 에러가 보인다 | `java7ang@gmail.com` | Y |
| FE-TEAMS-009 | P0 | 정상 팀 생성 | 생성 후 생성된 팀 상세로 이동한다 | `java7ang@gmail.com` | Y |
| FE-TEAMS-010 | P1 | AI 추천 성공 | 추천 팀 목록, 점수, 추천 이유가 보인다 | `java7ang@gmail.com` | Y |
| FE-TEAMS-011 | P1 | AI 추천 실패 - 프로필 미작성 | 프로필 먼저 등록 안내가 보인다 | `newbie@test.com` | Y |
| FE-TEAMS-012 | P1 | AI 추천 실패 - 이미 팀 소속 | 이미 팀 소속 안내가 보인다 | `test@example.com` | Y |
| FE-TEAMS-013 | P1 | AI 추천 실패 - 추천 팀 없음 | 추천 가능한 팀 없음 안내가 보인다 | `java7ang@gmail.com` | Y |
| FE-TEAMS-014 | P2 | URL 없는 `/teams` 에서 활성 시즌 2개 이상 | 시즌 선택 드롭다운이 동작한다 | 아무 계정 | Y |

## 1-9. 팀 상세 `/teams/:teamId`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-TEAM-001 | P0 | 팀 상세 정상 조회 | 팀 정보, 상태, 인원, 모집 역할이 보인다 | 아무 계정 | Y |
| FE-TEAM-002 | P0 | 존재하지 않는 팀 접근 | 오류 안내가 보인다 | 아무 계정 | Y |
| FE-TEAM-003 | P0 | 비로그인 + 모집중 팀 | 로그인 필요 안내가 보인다 | (로그아웃) | Y |
| FE-TEAM-004 | P0 | 인증 완료 일반 사용자 + 지원 가능 조건 충족 | 팀 지원하기 버튼이 보인다 | `java7ang@gmail.com` → AI 챗봇 프로젝트 | Y |
| FE-TEAM-005 | P0 | 리더 본인이 자기 팀 접근 | 지원 버튼이 보이지 않는다 | `test@example.com` → AI 챗봇 프로젝트 | Y |
| FE-TEAM-006 | P0 | 이미 팀 멤버가 접근 | 지원 버튼이 보이지 않는다 | `member2@test.com` → AI 이미지 생성 서비스 | Y |
| FE-TEAM-007 | P1 | 지원 모달에서 역할 미선택 제출 | 에러가 보인다 | `java7ang@gmail.com` | Y |
| FE-TEAM-008 | P0 | 정상 팀 지원 | 완료 알림 후 팀 목록으로 이동한다 | `java7ang@gmail.com` | Y |
| FE-TEAM-009 | P1 | 탭 전환 | 팀 정보/팀원/지원자 탭이 정상 전환된다 | `test@example.com` | Y |
| FE-TEAM-010 | P0 | 리더가 접근 | 지원자 탭이 보이고 지원 목록을 조회한다 | `member4@test.com` → AI 일정 관리 앱 | Y |
| FE-TEAM-011 | P0 | 리더가 지원자 수락 | 목록이 새로고침되고 팀 정보가 갱신된다 | `member4@test.com` → AI 일정 관리 앱 | Y |
| FE-TEAM-012 | P0 | 리더가 지원자 거절 | 해당 지원자가 목록에서 제거된다 | `member4@test.com` → AI 일정 관리 앱 | Y |
| FE-TEAM-013 | P1 | 리더의 AI 멤버 추천 성공 | 추천 후보 목록이 보인다 | `test@example.com` → AI 챗봇 프로젝트 | Y |
| FE-TEAM-014 | P1 | AI 멤버 추천 실패 - 팀 정원 초과 | 정원 가득 안내가 보인다 | `member1@test.com` → AI 이미지 생성 서비스 | Y |
| FE-TEAM-015 | P1 | AI 멤버 추천 실패 - 추천 후보 없음 | 추천 가능한 멤버 없음 안내가 보인다 | `test@example.com` → AI 챗봇 프로젝트 | Y |
| FE-TEAM-016 | P2 | `팀 설정` 버튼 클릭 | 현재 미구현 여부 확인 필요, 동작 정의 필요 | `test@example.com` | N |
| FE-TEAM-017 | P0 | 리더가 AI 추천 모달에서 영입 요청 발송 | 영입 요청 발송 성공 메시지가 보인다 | `test@example.com` → AI 챗봇 프로젝트 | Y |
| FE-TEAM-018 | P1 | 영입 요청 발송 실패 - 이미 영입 요청 보냄 | 이미 영입 요청을 보낸 사용자 에러가 보인다 | `test@example.com` | Y |
| FE-TEAM-019 | P1 | 영입 요청 발송 실패 - 이미 지원한 사용자 | 이미 지원한 사용자 에러가 보인다 | `test@example.com` | Y |
| FE-TEAM-020 | P1 | 영입 요청 발송 실패 - 이미 다른 팀 소속 | 이미 팀에 소속된 사용자 에러가 보인다 | `test@example.com` | Y |
| FE-TEAM-021 | P1 | 영입 요청 발송 후 목록 갱신 | 영입 요청 목록에서 발송된 요청 확인 가능 | `test@example.com` | Y |

## 1-10. 내 팀 `/my-team`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-MYTEAM-001 | P0 | 비로그인 접근 | 로그인 페이지로 이동한다 | (로그아웃) | Y |
| FE-MYTEAM-002 | P1 | 내가 만든 팀 존재 | `내가 만든 팀` 섹션이 보인다 | `test@example.com` | Y |
| FE-MYTEAM-003 | P1 | 내가 멤버인 팀 존재 | `소속 팀` 섹션이 보인다 | `member1@test.com` | Y |
| FE-MYTEAM-004 | P1 | 지원 내역 존재 | `지원 현황` 섹션이 보인다 | `test2@restart-point.com` | Y |
| FE-MYTEAM-005 | P1 | 데이터가 아무것도 없음 + 인증 완료 | 팀 둘러보기 CTA가 보인다 | `java7ang@gmail.com` | Y |
| FE-MYTEAM-006 | P1 | 데이터가 아무것도 없음 + 미인증 | 수료 인증하기 CTA가 보인다 | `newbie@test.com` | Y |
| FE-MYTEAM-007 | P1 | 팀 카드 클릭 | 팀 상세로 이동한다 | `test@example.com` | Y |
| FE-MYTEAM-008 | P2 | 지원 현황 카드에 teamId 존재 | 팀 보기 버튼이 동작한다 | `test2@restart-point.com` | Y |
| FE-MYTEAM-009 | P0 | 받은 영입 요청 존재 | `받은 영입 요청` 섹션이 보인다 | (영입 요청 받은 계정) | Y |
| FE-MYTEAM-010 | P0 | 영입 요청 수락 | 팀에 합류되고 소속 팀 섹션에 추가된다 | (영입 요청 받은 계정) | Y |
| FE-MYTEAM-011 | P0 | 영입 요청 거절 | 거절됨 상태로 변경되고 버튼이 사라진다 | (영입 요청 받은 계정) | Y |
| FE-MYTEAM-012 | P1 | 만료된 영입 요청 | 만료됨 상태 배지가 보이고 수락/거절 버튼이 숨겨진다 | (만료된 요청 받은 계정) | Y |
| FE-MYTEAM-013 | P1 | 영입 요청 마감일 표시 | D-day 남은 일수가 표시된다 | (영입 요청 받은 계정) | Y |
| FE-MYTEAM-014 | P1 | 영입 요청 수락 실패 - 팀 모집 완료 | 더 이상 모집 중이 아닙니다 에러가 보인다 | (영입 요청 받은 계정) | Y |
| FE-MYTEAM-015 | P1 | 영입 요청 수락 실패 - 역할 마감 | 해당 역할은 더 이상 모집하지 않습니다 에러가 보인다 | (영입 요청 받은 계정) | Y |
| FE-MYTEAM-016 | P1 | 영입 요청 수락 실패 - 이미 팀 소속 | 이미 팀에 소속되어 있습니다 에러가 보인다 | (영입 요청 받은 계정) | Y |

## 1-11. 심사 `/seasons/:seasonId/review`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-REVIEW-001 | P0 | 심사 중 시즌 상세 진입 | `프로젝트 심사하기` 버튼이 보인다 | `test1@restart-point.com` | Y |
| FE-REVIEW-002 | P0 | 전문가 계정으로 심사 페이지 진입 | 심사 대상 프로젝트 목록이 보인다 | `test1@restart-point.com` | Y |
| FE-REVIEW-003 | P0 | 일반 사용자로 심사 중 시즌 상세 진입 | `프로젝트 심사하기` 버튼이 보이지 않는다 | `test2@restart-point.com` | Y |
| FE-REVIEW-004 | P0 | 심사 점수 없이 제출 | `모든 항목에 점수를 입력해주세요.` 검증 문구가 보인다 | `test1@restart-point.com` | Y |
| FE-REVIEW-005 | P0 | 전문가 계정으로 정상 심사 제출 | 목록이 새로고침되고 완료된 심사에 추가된다 | `test1@restart-point.com` | Y |
| FE-REVIEW-006 | P0 | 보조 전문가 계정으로 정상 심사 제출 | 목록이 새로고침되고 완료된 심사에 추가된다 | `review-admin@restart-point.com` | Y |
| FE-REVIEW-007 | P1 | 동일 계정으로 같은 프로젝트 재심사 시도 | 목록에서 제거되거나 중복 제출이 차단된다 | `test1@restart-point.com` | Y |
| FE-REVIEW-008 | P1 | 자기 팀 프로젝트 심사 시도 | 심사 대상 목록에 자기 팀 프로젝트가 보이지 않는다 | `test2@restart-point.com` | Y |
| FE-REVIEW-009 | P1 | 일반 사용자로 심사 URL 직접 접근 | 심사 대상 프로젝트가 비어 있고 제출이 불가능하다 | `test2@restart-point.com` | Y |
| FE-REVIEW-010 | P1 | 전문가 심사 완료 후 운영자 분석 진입 | 프로젝트 분석 화면이 전문가 집계 기준으로 보인다 | `admin@restart-point.com` | N |
| FE-REVIEW-012 | P1 | 심사 기간이 아닌 시즌 상세 진입 | `프로젝트 심사하기` 버튼이 숨겨진다 | 아무 계정 | Y |

## 1-12. 헤더/전역 동작
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-GLOBAL-001 | P1 | 비로그인 헤더 | 로그인/회원가입 버튼이 보인다 | (로그아웃) | Y |
| FE-GLOBAL-002 | P1 | 로그인 헤더 | 사용자명, 인증 배지, 프로필 메뉴가 보인다 | `test@example.com` | Y |
| FE-GLOBAL-003 | P1 | 인증 미완료 사용자 메뉴 | 수료 인증 메뉴가 보인다 | `newbie@test.com` | Y |
| FE-GLOBAL-004 | P1 | 로그아웃 클릭 | 인증 정보가 제거되고 홈으로 이동한다 | 아무 계정 | Y |
| FE-GLOBAL-005 | P2 | 모바일 메뉴 열기/닫기 | 메뉴가 자연스럽게 열리고 닫힌다 | 아무 계정 | Y |

---

# 2. 관리자 웹 QA (`admin`)

## 2-1. 관리자 로그인 `/login`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| AD-LOGIN-001 | P0 | 올바른 관리자 계정 로그인 | 대시보드로 이동한다 | `admin@restart-point.com` | Y |
| AD-LOGIN-002 | P0 | 잘못된 계정 로그인 | 에러 문구가 표시된다 | 틀린 비밀번호 | Y |
| AD-LOGIN-003 | P1 | 로딩 중 중복 제출 | 버튼 비활성화로 중복 제출이 막힌다 | `admin@restart-point.com` | Y |

## 2-2. 보호 라우트
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| AD-AUTH-001 | P0 | 비로그인 사용자가 `/` 접근 | `/login`으로 이동한다 | (로그아웃) | Y |
| AD-AUTH-002 | P1 | 인증 확인 중 | 로딩 화면이 보인다 | `admin@restart-point.com` | Y |
| AD-AUTH-003 | P1 | 알 수 없는 경로 접근 | `/`로 리다이렉트된다 | `admin@restart-point.com` | Y |

## 2-3. 대시보드 `/`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| AD-DASH-001 | P1 | 대시보드 진입 | 활성 시즌, 인증 대기, 전체 시즌 통계가 보인다 | `admin@restart-point.com` | Y |
| AD-DASH-002 | P1 | 활성 시즌 0건 | 활성 시즌 empty state가 보인다 | `admin@restart-point.com` | Y |
| AD-DASH-003 | P1 | 인증 대기 0건 | 대기 중인 인증 요청 없음 문구가 보인다 | `admin@restart-point.com` | Y |
| AD-DASH-004 | P2 | 카드 클릭 | 해당 관리 화면으로 이동한다 | `admin@restart-point.com` | Y |

## 2-4. 시즌 관리 `/seasons`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| AD-SEASON-001 | P0 | 시즌 목록 조회 | 시즌 리스트가 렌더링된다 | `admin@restart-point.com` | Y |
| AD-SEASON-002 | P0 | 새 시즌 생성 | 목록에 추가된다 | `admin@restart-point.com` | Y |
| AD-SEASON-003 | P0 | 기존 시즌 수정 | 수정 내용이 반영된다 | `admin@restart-point.com` | Y |
| AD-SEASON-004 | P0 | DRAFT 시즌 삭제 | 삭제 확인 후 목록에서 제거된다 | `admin@restart-point.com` | Y |
| AD-SEASON-005 | P0 | 상태 변경 버튼 클릭 | 다음 상태로 전이되고 목록이 갱신된다 | `admin@restart-point.com` | Y |
| AD-SEASON-006 | P1 | 필터 전환 | 상태별 시즌만 보인다 | `admin@restart-point.com` | Y |
| AD-SEASON-007 | P1 | 시즌 생성/수정 모달 확인 | 심사 비중이 전문가 평가 100%로 고정되어 보인다 | `admin@restart-point.com` | Y |
| AD-SEASON-008 | P1 | 날짜 입력 누락 | 생성/수정이 막힌다 | `admin@restart-point.com` | Y |
| AD-SEASON-009 | P2 | 잘못된 기간 순서 입력 | 현재 검증 여부 확인 필요, 백엔드 검증 포함 점검 필요 | `admin@restart-point.com` | N |

## 2-5. 수료 인증 관리 `/certifications`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| AD-CERT-001 | P0 | 대기 인증 목록 조회 | 사용자 리스트가 보인다 | `admin@restart-point.com` | Y |
| AD-CERT-002 | P0 | 인증 승인 | 목록에서 제거되거나 상태가 갱신된다 | `admin@restart-point.com` | Y |
| AD-CERT-003 | P0 | 인증 거절 | 목록에서 제거되거나 상태가 갱신된다 | `admin@restart-point.com` | Y |
| AD-CERT-004 | P1 | confirm 취소 | 승인/거절이 실행되지 않는다 | `admin@restart-point.com` | Y |
| AD-CERT-005 | P1 | 대기 건수 0건 | empty state가 보인다 | `admin@restart-point.com` | Y |

## 2-6. 회원 관리 `/users`
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| AD-USER-001 | P0 | 회원 목록 조회 | 테이블과 총 인원 수가 정상 노출된다 | `admin@restart-point.com` | Y |
| AD-USER-002 | P1 | 이름/이메일 검색 | 조건에 맞는 사용자만 조회된다 | `admin@restart-point.com` | Y |
| AD-USER-003 | P1 | 역할 필터 | USER/ADMIN 필터가 동작한다 | `admin@restart-point.com` | Y |
| AD-USER-004 | P1 | 인증 상태 필터 | NONE/PENDING/APPROVED/REJECTED 필터가 동작한다 | `admin@restart-point.com` | Y |
| AD-USER-005 | P0 | 일반 사용자를 관리자로 변경 | 역할 변경 후 목록이 갱신된다 | `admin@restart-point.com` | Y |
| AD-USER-006 | P0 | 관리자를 일반 사용자로 변경 | 역할 변경 후 목록이 갱신된다 | `admin@restart-point.com` | Y |
| AD-USER-007 | P0 | 다른 사용자 삭제 | 삭제 후 목록에서 제거된다 | `admin@restart-point.com` | Y |
| AD-USER-008 | P0 | 본인 계정 행 확인 | 역할 변경/삭제 버튼 대신 `(본인)` 표시가 보인다 | `admin@restart-point.com` | Y |
| AD-USER-009 | P1 | 페이지네이션 | 이전/다음 버튼이 정상 동작한다 | `admin@restart-point.com` | Y |
| AD-USER-010 | P1 | API 에러 발생 | 에러 문구가 보인다 | `admin@restart-point.com` | Y |

---

# 3. 회귀 테스트 묶음

## 배포 전 최소 회귀 세트 (P0)
- [x] 회원가입 전체 플로우
- [x] 로그인/로그아웃
- [x] 수료 인증 요청
- [x] 프로필 저장
- [x] 시즌 목록/상세 조회
- [ ] 인증 완료 사용자 기준 팀 생성
- [ ] 팀 지원
- [ ] 리더의 지원자 수락/거절
- [x] 관리자 로그인
- [x] 관리자 수료 인증 승인/거절
- [x] 관리자 시즌 생성/상태 변경
- [x] 관리자 회원 역할 변경

## 스모크 테스트
- [x] 사용자 웹 진입
- [x] 관리자 웹 진입
- [x] 핵심 API 실패 없이 기본 목록 화면 렌더링

---

# 4. 자동화 커버리지 현황

## 현재 자동화 완료
1. 회원가입
2. 로그인
3. 수료 인증 요청 / 상태 화면
4. 프로필 저장
5. 시즌 상세 진입
6. 내 팀 화면
7. 팀 필터링 / 팀 상세 기본 흐름
8. 리뷰 플로우
9. 프로덕션 사용자/관리자 스모크
10. 관리자 로그인 / 보호 라우트 / 대시보드 기본 진입
11. 관리자 인증 승인/거절/confirm 취소/empty state
12. 관리자 회원 검색/필터/페이지네이션/역할 변경/삭제/API 에러
13. 관리자 시즌 생성/수정/삭제/상태 전이/필터

## 다음 자동화 우선순위
1. 사용자 팀 생성 성공 케이스
2. 사용자 팀 지원 성공 케이스
3. 리더의 지원자 수락/거절
4. AI 팀 추천 모달
5. AI 멤버 추천 모달
6. 리더의 AI 추천 모달에서 영입 요청 발송
7. 사용자의 영입 요청 수락/거절
8. 관리자 회원 역할 USER ↔ ADMIN 양방향 전환 결과 검증 강화

---

# 5. 테스트 데이터 및 계정

## 5-1. 테스트 계정 목록

| 이메일 | 비밀번호 | 이름 | 인증 상태 | 주요 테스트 용도 |
|--------|----------|------|-----------|------------------|
| `newbie@test.com` | test1234 | 신규회원 | NONE | 미인증 → 인증 신청 플로우 |
| `pending@restart-point.com` | test1234 | 인증대기회원 | PENDING | 인증 대기 상태 화면 |
| `rejected@test.com` | test1234 | 거절회원 | REJECTED | 인증 거절 → 재신청 플로우 |
| `test1@restart-point.com` | test1234 | E2E 리뷰어 | APPROVED | 전문가 심사(E2E 리뷰 시드) |
| `review-admin@restart-point.com` | test1234 | E2E 보조 리뷰어 | APPROVED | 추가 전문가 심사(E2E 리뷰 시드) |
| `test2@restart-point.com` | test1234 | E2E 팀장 | APPROVED | 심사 대상 팀 리더(E2E 리뷰 시드) |
| `test@example.com` | test1234 | 테스트 | APPROVED | 팀 리더 (지원자 관리), 커뮤니티 작성자 |
| `member1@test.com` | test1234 | 김프론트 | APPROVED | 진행 중 프로젝트 관리, 체크포인트 |
| `member2@test.com` | test1234 | 이백엔드 | APPROVED | 완료된 프로젝트, SUBMITTED 팀 리더 |
| `member3@test.com` | test1234 | 박디자인 | APPROVED | 팀 초대 알림, 거절된 지원 이력 |
| `member4@test.com` | test1234 | 최기획 | APPROVED | 지원자 승인/거절 테스트 |
| `java7ang@gmail.com` | test1234 | 강성지 | APPROVED | 팀 없는 인증 사용자 |
| `admin@restart-point.com` | (개인) | Admin | APPROVED | 관리자 계정 |

## 5-2. 시즌 데이터

| 시즌명 | ID | 상태 | 테스트 용도 |
|--------|-----|------|-------------|
| 2026 봄 시즌 | 1 | RECRUITING | 모집 중 시즌 - 팀 생성/지원 가능 |
| 2024 겨울 시즌 | 2 | IN_PROGRESS | 진행 중 시즌 |
| 2023 가을 시즌 | 3 | COMPLETED | 완료된 시즌 - 결과 확인 |
| 2025 여름 시즌 (준비중) | 4 | DRAFT | 관리자 전용 초안 시즌 |
| 2024 여름 시즌 | 5 | TEAM_BUILDING | 팀빌딩 단계 - 팀 생성/지원 가능 |
| 2024 봄 시즌 (심사중) | 6 | REVIEWING | 일반 심사 QA용 시즌 |
| 테스트 시즌 | 7 | REVIEWING | 전문가 심사 E2E QA용 시즌 |

## 5-3. 팀 데이터

| 팀명 | ID | 상태 | 시즌 | 리더 | 테스트 용도 |
|------|-----|------|------|------|-------------|
| AI 챗봇 프로젝트 | 1 | RECRUITING | 2026 봄 시즌 | test@example.com | 모집 중 팀, 지원하기 |
| AI 일정 관리 앱 | 5 | RECRUITING | 2026 봄 시즌 | member4@test.com | 지원자 승인/거절 (test2가 PENDING) |
| AI 이미지 생성 서비스 | 2 | IN_PROGRESS | 2024 겨울 시즌 | member1@test.com | 진행 중 프로젝트 관리 |
| AI 학습 도우미 | 3 | COMPLETE | 2023 가을 시즌 | member2@test.com | 완료된 프로젝트 |
| AI 코드 리뷰어 | 4 | REVIEWED | 2023 가을 시즌 | test@example.com | 심사 완료 프로젝트 |
| AI 번역 서비스 | 6 | SUBMITTED | 2024 봄 시즌 (심사중) | member2@test.com | 제출됨 상태 |
| E2E 심사 대상 팀 | 8 | SUBMITTED | 테스트 시즌 | test2@restart-point.com | 전문가 심사 대상 팀 |
| E2E 이전 심사 팀 | 9 | REVIEWED | 테스트 시즌 | review-target@restart-point.com | 이전 심사 이력 확인 |

## 5-4. 프로젝트 데이터

| 프로젝트명 | 상태 | 팀 | 체크포인트 | 테스트 용도 |
|------------|------|-----|------------|-------------|
| TranslateAI - 실시간 번역 플랫폼 | DRAFT | AI 번역 서비스 | 0개 | 초기 설정 폼 |
| ImageAI - AI 이미지 생성 플랫폼 | IN_PROGRESS | AI 이미지 생성 서비스 | 2개 | 체크포인트 작성/수정 |
| CodeReview AI - 자동 코드 리뷰 서비스 | SUBMITTED | AI 코드 리뷰어 | 0개 | 제출됨 상태 |
| StudyMate - AI 학습 추천 시스템 | COMPLETED | AI 학습 도우미 | 2개 | 완료된 프로젝트 |
| AI 회고 도우미 | SUBMITTED | E2E 심사 대상 팀 | 0개 | 전문가 심사 시드 |
| 이전 심사 프로젝트 | COMPLETED | E2E 이전 심사 팀 | 0개 | 내가 심사한 목록 시드 |

## 5-5. 커뮤니티 데이터

| 제목 | 타입 | 작성자 | 테스트 용도 |
|------|------|--------|-------------|
| AI API 비용 최적화 방법이 궁금합니다 | QNA | test@example.com | QNA 게시글, 댓글 |
| 부트캠프 수료 후 첫 프로젝트 후기 | QNA | member1@test.com | QNA 게시글 |
| React에서 상태 관리 라이브러리 추천해주세요 | QNA | member3@test.com | QNA 게시글 |
| [2024 봄 시즌] AI 챗봇 프로젝트 백엔드 개발자 모집합니다! | RECRUITMENT | member2@test.com | 팀원 모집 게시글 |
| [공지] 2024 봄 시즌 모집 시작! | ANNOUNCEMENT | admin@restart-point.com | 공지사항 |
| [프로젝트 완료] StudyMate - AI 학습 추천 시스템 | SHOWCASE | member1@test.com | 프로젝트 쇼케이스 |

## 5-6. 알림 데이터

| 알림 타입 | 수신자 | 테스트 용도 |
|-----------|--------|-------------|
| TEAM_APPLICATION | test@example.com | 팀 지원 알림 (리더에게) |
| TEAM_APPLICATION | member4@test.com | 팀 지원 알림 (리더에게) |
| TEAM_APPLICATION_REJECTED | member2@test.com | 지원 거절 알림 |
| TEAM_INVITATION | member3@test.com | 팀 초대 알림 |
| TEAM_RECRUIT_REQUEST | (영입 요청 받은 계정) | 영입 요청 알림 |
| TEAM_RECRUIT_ACCEPTED | test@example.com | 영입 요청 수락 알림 (리더에게) |
| TEAM_RECRUIT_REJECTED | test@example.com | 영입 요청 거절 알림 (리더에게) |
| CERTIFICATION_APPROVED | newbie@test.com | 인증 승인 알림 |
| CERTIFICATION_REJECTED | rejected@test.com | 인증 거절 알림 |
| CHECKPOINT_REMINDER | member1@test.com | 체크포인트 마감 알림 |
| SUBMISSION_REMINDER | test2@restart-point.com | 제출 마감 알림 |
| REVIEW_START | test2@restart-point.com | 심사 시작 알림 |
| REVIEW_END | member1@test.com | 심사 완료 알림 |
| REPORT_PUBLISHED | member1@test.com | 성장 리포트 발행 알림 |
| COMMENT_ON_POST | test@example.com | 댓글 알림 |
| REPLY_ON_COMMENT | member3@test.com | 대댓글 알림 |

## 5-7. 계정별 상세 테스트 시나리오

### `newbie@test.com / test1234` (신규회원) - 인증: NONE
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 헤더 뱃지 확인 | "미인증" 뱃지 표시 |
| 2 | 수료 인증 페이지 접근 | 인증 신청 폼 표시 |
| 3 | 수료증 파일 업로드 | 드래그앤드롭/클릭 업로드 동작 |
| 4 | 팀 생성 시도 | 인증 필요 안내 |
| 5 | 팀 지원 시도 | 인증 필요 안내 |

### `pending@restart-point.com / test1234` (인증대기회원) - 인증: PENDING
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 수료 인증 페이지 진입 | 대기 상태 화면 표시 |
| 2 | 상태 새로고침 클릭 | 최신 인증 상태 재조회 |
| 3 | 헤더 뱃지 확인 | "인증대기" 뱃지 표시 |

### `rejected@test.com / test1234` (거절회원) - 인증: REJECTED
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 수료 인증 페이지 | "인증 거절됨" 메시지 + 재신청 폼 표시 |
| 2 | 알림 확인 | "수료 인증 거절" 알림 표시 |
| 3 | 재신청 후 상태 | PENDING으로 변경 |

### `test1@restart-point.com / test1234` (E2E 리뷰어) - 인증: APPROVED, 역할: REVIEWER
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 시즌 상세 진입 | "프로젝트 심사하기" 버튼 표시 |
| 2 | 심사 페이지 진입 | "AI 회고 도우미" 프로젝트 노출 |
| 3 | 심사 제출 | 완료된 심사 목록에 반영 |
| 4 | 운영자 분석 확인 | EXPERT 심사 집계에 반영 |

### `review-admin@restart-point.com / test1234` (E2E 보조 리뷰어) - 인증: APPROVED, 역할: REVIEWER
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 시즌 상세 진입 | "프로젝트 심사하기" 버튼 표시 |
| 2 | 심사 페이지 진입 | "AI 회고 도우미" 프로젝트 노출 |
| 3 | 심사 제출 | 완료된 심사 목록에 반영 |
| 4 | 운영자 분석 확인 | 추가 EXPERT 심사 집계에 반영 |

### `test2@restart-point.com / test1234` (E2E 팀장) - 인증: APPROVED
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 내 팀 페이지 | "E2E 심사 대상 팀" 리더 팀 표시 |
| 2 | 시즌 상세 진입 | 자기 팀 프로젝트라 심사 대상 버튼/목록 제외 확인 |
| 3 | 프로젝트 상태 확인 | 제출된 프로젝트 "AI 회고 도우미" 확인 |
| 4 | 운영자 분석 확인 | 팀 프로젝트에 EXPERT 심사 반영 |

### `test@example.com / test1234` (테스트) - 인증: APPROVED, 팀 리더
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 내 팀 페이지 - 리더 팀 | "AI 챗봇 프로젝트", "AI 코드 리뷰어" 2개 팀 |
| 2 | 내 팀 페이지 - 소속 팀 | 3개 팀 (RECRUITING, COMPLETE, REVIEWED) |
| 3 | 프로젝트 버튼 | COMPLETE, REVIEWED 팀에만 표시 |
| 4 | 팀 상세 - 지원자 탭 | 리더로서 지원자 목록 확인 가능 |
| 5 | 지원자 승인/거절 | 승인/거절 후 목록 갱신 |
| 6 | 커뮤니티 | 본인 게시글 2개 수정/삭제 가능 |
| 7 | 알림 확인 | 팀 지원 알림, 댓글 알림 (2개 미읽음) |

### `member1@test.com / test1234` (김프론트) - 인증: APPROVED, 팀 리더
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 내 팀 페이지 - 리더 팀 | "AI 이미지 생성 서비스" (IN_PROGRESS) |
| 2 | 프로젝트 관리 | ImageAI 프로젝트 상세 + 체크포인트 2개 |
| 3 | 체크포인트 작성 | 새 체크포인트 작성 폼 |
| 4 | 체크포인트 수정 | 기존 체크포인트 편집 |
| 5 | 알림 확인 | 성장 리포트 발행 알림 (1개 미읽음) |

### `member2@test.com / test1234` (이백엔드) - 인증: APPROVED, 팀 리더
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 내 팀 페이지 - 리더 팀 | "AI 학습 도우미" (COMPLETE), "AI 번역 서비스" (SUBMITTED) |
| 2 | 완료된 프로젝트 | StudyMate 프로젝트 읽기 전용 |
| 3 | DRAFT 프로젝트 | TranslateAI 초기 설정 폼 |
| 4 | 소속 팀 | "AI 이미지 생성 서비스" 팀원 |
| 5 | 알림 확인 | 팀 지원 거절 알림 (1개 미읽음) |

### `member3@test.com / test1234` (박디자인) - 인증: APPROVED
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 소속 팀 | "AI 이미지 생성 서비스" UX/UI 디자이너 |
| 2 | 프로젝트 접근 | ImageAI 프로젝트 체크포인트 확인 |
| 3 | 거절된 지원 확인 | "AI 챗봇 프로젝트" 지원 거절 이력 |
| 4 | 알림 확인 | 팀 초대 알림 (1개 미읽음) |

### `member4@test.com / test1234` (최기획) - 인증: APPROVED, 팀 리더
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 내 팀 페이지 - 리더 팀 | "AI 일정 관리 앱" (RECRUITING) |
| 2 | 팀 상세 - 지원자 탭 | test2 PENDING 지원 확인 |
| 3 | 지원자 승인 | test2 상태 ACCEPTED로 변경 |
| 4 | 지원자 거절 | test2 상태 REJECTED로 변경 |
| 5 | 알림 확인 | 새 팀 지원 알림 (1개 미읽음) |

### `java7ang@gmail.com / test1234` (강성지) - 인증: APPROVED, 팀 없음
| # | 테스트 케이스 | 예상 결과 |
|---|--------------|-----------|
| 1 | 내 팀 페이지 | "아직 팀이 없습니다" + 팀 둘러보기 CTA |
| 2 | 팀 생성 | 팀 생성 폼 접근 가능 |
| 3 | 팀 지원 | 지원하기 버튼 정상 동작 |

## 5-8. 계정별 빠른 테스트 가이드

| 계정 | 비밀번호 | 주요 테스트 시나리오 |
|------|----------|---------------------|
| `newbie@test.com` | `test1234` | 미인증 → 인증 신청 플로우 |
| `pending@restart-point.com` | `test1234` | 인증 대기 상태 화면 |
| `rejected@test.com` | `test1234` | 인증 거절 → 재신청 플로우 |
| `test1@restart-point.com` | `test1234` | 전문가 심사(E2E 리뷰 시드) |
| `review-admin@restart-point.com` | `test1234` | 추가 전문가 심사(E2E 리뷰 시드) |
| `test2@restart-point.com` | `test1234` | 심사 대상 팀 리더(E2E 리뷰 시드) |
| `test@example.com` | `test1234` | 팀 리더 (지원자 관리), 커뮤니티 작성자 |
| `member1@test.com` | `test1234` | 진행 중 프로젝트 관리, 체크포인트 |
| `member2@test.com` | `test1234` | 완료된 프로젝트, SUBMITTED 팀 리더 |
| `member3@test.com` | `test1234` | 팀 초대 알림, 거절된 지원 이력 |
| `member4@test.com` | `test1234` | 지원자 승인/거절 테스트 |
| `java7ang@gmail.com` | `test1234` | 팀 없는 인증 사용자 |

> **모든 계정 비밀번호**: `test1234`

---

# 6. 확인 필요한 미정 사항
- [ ] 사용자 웹 `팀 설정` 버튼은 아직 실제 동작이 없는 것으로 보임, 요구사항 확정 필요
- [ ] 시즌 생성/수정 시 날짜 선후관계 검증을 프론트에서 할지 백엔드에서만 할지 정책 확인 필요
- [ ] AI 추천 결과의 정렬/점수 기준을 QA에서 어디까지 고정 검증할지 정의 필요
- [x] 테스트 계정/시드 데이터/관리자 계정 표준 세트 정리 필요 → 섹션 5 참고

---

# 7. 업데이트 규칙
새 기능 개발 시 아래 형식으로 같은 문서에 추가한다.

```md
## 1-x. 기능명 또는 화면명
| ID | 우선순위 | 케이스 | 기대 결과 | 테스트 계정 | AUTO |
|---|---|---|---|---|---|
| FE-XXX-001 | P0 | 정상 흐름 | 기대 결과 | `test@example.com` | Y |
| FE-XXX-002 | P1 | 예외 흐름 | 기대 결과 | `newbie@test.com` | N |
```

권장 ID 규칙:
- 사용자 웹: `FE-기능-번호`
- 관리자 웹: `AD-기능-번호`
- 공통/회귀: `REG-번호`
