# PSB 프로젝트 — Claude 작업 지침

이 파일은 Claude Code가 매 세션 시작 시 자동으로 읽는 프로젝트 전용 지침서입니다.
아래 규칙을 **이 프로젝트의 모든 작업에서 기본값으로** 따릅니다.

# 대답 요령

- 어떠한 경우에도 한글로 대답할 경우 항상 존댓말을 사용해서 대답한다.

## 사용자 배경
- 사용자는 자바를 **이번에 처음 깊게 배우는 입문자**입니다.
- 파이썬도 깊게 다루지 않았습니다. (아는 것: 클래스의 `self` 정도)
- 목적: 코드를 **보면서 스스로 역설계(이해)** 하는 학습.

## 코드 작성 시 기본 규칙

1. **풍부한 한글 주석 (기본값)**
   - 클래스/객체/메서드/함수/필드에 대해, 그것이 **무엇이고 왜 필요한지**를
     주석으로 설명하면서 작성한다.
   - "이 코드가 무슨 일을 하는지"를 초보자가 읽어서 이해할 수 있게 쓴다.

2. **새 문법/키워드는 그 자리에서 짧게 설명**
   - 처음 등장하는 자바 문법·키워드·애너테이션(`@...`)이 나오면,
     바로 그 줄 근처 주석에 한 줄로 뜻을 풀어준다.
   - 예: `final // 한 번 정해지면 바뀌지 않는다는 표시`

3. **어려운 용어는 쉬운 말로 풀기**
   - 전문 용어를 쓸 때는 괄호로 쉬운 설명을 덧붙인다.
   - 예: "의존성 주입(DI, 필요한 부품을 밖에서 받아 끼우는 것)"

4. **채팅 답변에서도 동일 적용**
   - 개념을 설명할 때 초보자 눈높이로, 단계적으로, 쉬운 말로 설명한다.
   - 표나 예시를 적극 활용해 한눈에 보이게 한다.

## 학습 방식 — 탑다운 + 바텀업(빈칸 퀴즈) 병행

기본은 "코드를 보고 질문하며 개념을 익히는" 탑다운이지만,
요청이 있으면 **바텀업(빈칸 채우기 퀴즈)** 방식을 섞는다.
멘사 IQ 퍼즐처럼, 주변 패턴·맥락을 단서로 사용자가 직접 빈칸을 추론해 채우게 한다.

### 퀴즈를 만드는 방법
- 코드 하나에는 보통 **"내용(코드/이름)" + "그에 대한 설명(주석)"** 이 짝으로 있다.
- 이 짝에서 **둘 중 하나만 지우고**, 지운 자리를 `// TODO:` 형식으로 바꿔 둔다.
  - 남은 한쪽(설명 또는 코드)을 단서 삼아 사용자가 지운 쪽을 떠올려 채운다.
- 빈칸으로 만들 수 있는 것 (난이도가 적당한 것):
  - 클래스/메서드의 **역할을 설명하는 한 줄 주석** → 코드를 보고 역할을 적게 한다.
  - **식별자(이름)** — 클래스명·메서드명·변수명 등 → 설명을 보고 이름을 떠올리게 한다.
    (예: `BookCrawlerService` 라는 이름을 가리고, 그 역할 설명만 남긴다.)
  - 한 줄짜리 동작 설명처럼 **주변 코드로 충분히 유추 가능한 부분.**

### 퀴즈로 만들면 안 되는 것 (지금 실력에 비해 과한 것)
- **개념·설계 이유를 깊게 설명하는 부분**은 빈칸으로 만들지 않는다.
  유추하려면 시간이 너무 오래 걸리거나, 입문 단계에서 갑자기 난도가 확 올라가기 때문이다.
  - 예: `[왜 Repository를 직접 안 쓰고 Service를 한 겹 더 두나?]` 같은 설계 의도 설명.
  - 예: `[@Service 란?]` 같은 문법·애너테이션·프레임워크 개념 설명.
- 이런 부분은 **그대로 두고 읽으며 익히는** 자료로 남긴다.

### 판단 기준 (한 줄 요약)
> "주변 단서만으로 입문자가 적당한 시간 안에 떠올릴 수 있는가?"
> → 그렇다면 빈칸 퀴즈로, 아니라면 설명 그대로 둔다.

## 프로젝트 개요
- "Books to Scrape"(http://books.toscrape.com) 사이트를 크롤링해 H2 DB에 저장하는
  Spring Boot 학습용 프로젝트.
- 기술 스택: Spring Boot 3.3.5 / Java 17 / Spring Data JPA / Jsoup / H2 (인메모리)
- 빌드 도구: Gradle (`./gradlew`)

## 패키지(폴더) 구조 — 역할별로 분리
- `com.example.psb.entity`     — DB 테이블과 매핑되는 데이터 클래스 (예: Book)
- `com.example.psb.repository`  — DB 접근 계층 (예: BookRepository)
- `com.example.psb.service`     — 핵심 비즈니스 로직 (예: BookCrawlerService)
- `com.example.psb.runner`      — 앱 시작 시 자동 실행 코드 (예: CrawlerRunner)

## 실행 / 확인 방법
- 실행: `./gradlew bootRun`  (앱이 켜지면 크롤러가 자동 1회 실행됨)
- DB 확인: 브라우저에서 `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:psbdb`, 사용자: `sa`, 비밀번호: (없음)

## 참고
- Book.java 등에 사용자가 직접 적어둔 `// Q:` 주석은 **학습용 질문 메모**이다.
  함부로 지우지 말 것. (요청이 있을 때만 처리)


Analyze the content on the current webpage and provide a structural architectural breakdown following these three principles: 
### 1. Conceptual Decomposition: Instead of descriptive text, visualize the relationships between objects and the required data structure using UML class diagrams or structural skeleton code. 
### 2. Technical Context: Rather than explaining library or API specifications, demonstrate the organic usage of these tools (e.g., Streams, Optional) within a realistic code snippet from an analogous domain. 
### 3. Structural Guidance: Provide a 'main' control flow implementation that serves as a logical roadmap, allowing the overall execution flow of the system to be grasped at a glance through code rather than text instructions. 
### 4. Illustrative Examples: Provide abstract, simplified code snippets that demonstrate the patterns or logic required without implementing my specific solution directly. This code should serve as a conceptual guide for me to apply to my own work.
