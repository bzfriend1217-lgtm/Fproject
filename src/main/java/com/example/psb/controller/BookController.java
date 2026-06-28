package com.example.psb.controller;

import com.example.psb.entity.Book;
import com.example.psb.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BookController
 * ============================================================================
 * 웹 브라우저(또는 외부 프로그램)의 HTTP 요청을 가장 먼저 받아,
 * 알맞은 주방(BookService)의 메서드로 연결해 주는 "최상단 입구(Controller) 계층"이다.
 *
 * [3층 구조 다시 보기 — 손님 주문이 흘러가는 길]
 *   Controller (이 클래스, 손님 응대 = 주문 받기)
 *        ↓ 호출
 *   Service (주방 = 요리 = 비즈니스 로직)
 *        ↓ 호출
 *   Repository (창고 = DB에서 재료 꺼내오기)
 *   - 이 클래스는 "DB가 어떻게 생겼는지" 전혀 모른다.
 *     오직 "어떤 URL이 오면 Service의 어떤 메서드를 부를지"만 안다. (역할 분리)
 *
 * ----------------------------------------------------------------------------
 * [핵심: 브라우저의 요청이 어떻게 이 Controller 빈과 "만나는가"]
 *  (네가 방금 배운 톰캣 스레드 풀 · 싱글톤 빈 · 힙/스택이 여기서 전부 만난다.)
 *
 *  1) 브라우저가 http://localhost:8080/api/books 로 HTTP 요청을 보낸다.
 *
 *  2) 스프링 부트 안에 내장된 톰캣(Tomcat, 웹 서버)이 그 요청을 받는다.
 *     - 톰캣은 미리 만들어 둔 "일꾼 스레드 풀"에서 노는 스레드 1개를 깨워
 *       이 요청 1건을 처리하게 시킨다. (요청 1건 = 일꾼 스레드 1명이 담당)
 *
 *  3) 그 일꾼 스레드는 DispatcherServlet(요청을 분배하는 중앙 교환원)에게 간다.
 *     - DispatcherServlet이 "GET /api/books 는 누가 처리하지?"라고 묻고,
 *       HandlerMapping(주소록)이 "BookController.getAllBooks() 가 담당"이라 답한다.
 *
 *  4) 그래서 일꾼 스레드는 아래 getAllBooks() 메서드를 실행한다.
 *     - 이때 호출되는 BookController 객체는 앱 전체에 "딱 하나뿐"이다. (싱글톤 빈)
 *       즉, 동시에 손님 100명이 와도 100명 모두 "같은 하나의 BookController"를
 *       공유해서 쓴다. (객체를 매번 새로 만들지 않아 빠르고 메모리도 아낀다.)
 *
 *  5) [싱글톤인데 왜 안전한가? — 힙/스택 복습]
 *     - 하나의 객체를 여러 스레드가 동시에 쓰면 보통 위험하다(데이터가 섞임).
 *     - 하지만 이 Controller가 가진 필드는 bookService 하나뿐이고, 그것도 final이라
 *       절대 바뀌지 않는다. → 공유해도 안전하다. (상태가 없는 = stateless 객체)
 *     - 손님마다 다른 값(예: 검색어 keyword, 책 번호 id)은 "메서드의 파라미터/지역변수"다.
 *       지역변수는 각 일꾼 스레드가 자기만의 "스택(stack)"에 따로 쌓는다.
 *       → 스레드끼리 서로 안 섞인다. (그래서 하나의 빈을 공유해도 충돌이 없다.)
 *     - 반면 Book 객체나 List 같은 결과물은 "힙(heap)"에 만들어지고,
 *       스택에는 그 힙 객체를 가리키는 "주소(참조)"만 올라간다.
 *
 *  6) 메서드가 리턴한 값(Book, List<Book> 등)을 스프링이 자동으로 JSON 글자로 바꿔
 *     HTTP 응답에 실어 브라우저로 돌려보낸다. (@RestController 덕분, 아래 설명)
 *
 *  7) 응답을 다 보내면 그 일꾼 스레드는 다시 스레드 풀로 "반납"되어 다음 손님을 기다린다.
 * ----------------------------------------------------------------------------
 *
 * [@RestController 란?]
 *  - "이 클래스는 웹 요청을 처리하는 컨트롤러 빈이다"라고 스프링에게 알리는 표식이다.
 *  - 정확히는 @Controller + @ResponseBody 를 합친 것.
 *  - @ResponseBody 의 효과: 메서드가 리턴한 값을 "화면(HTML 페이지) 이름"이 아니라
 *    "HTTP 응답 본문 그 자체(보통 JSON 데이터)"로 본다.
 *    → 즉 List<Book> 을 리턴하면 스프링이 알아서 JSON 배열로 변환해 응답해 준다.
 *
 * [@RequestMapping("/api/books") 란?]
 *  - 이 컨트롤러 안 모든 메서드가 공통으로 쓰는 "주소의 앞부분(접두어)"을 정한다.
 *  - 아래 각 메서드의 @GetMapping 주소는 이 앞부분에 이어 붙는다.
 *    예) @GetMapping("/count") → 실제 주소는 /api/books/count
 * ============================================================================
 */
@RestController                  // 이 클래스 = 웹 컨트롤러 빈(리턴값을 JSON 응답 본문으로)
@RequestMapping("/api/books")    // 이 안의 모든 주소 앞에 /api/books 가 붙는다
public class BookController {

    /**
     * 주방(BookService)으로 연결하기 위한 통로.
     *  - final : 한 번 정해지면 바뀌지 않는다는 표시.
     *  - 이 필드가 "바뀌지 않는 값" 하나뿐이라, 싱글톤 빈을 여러 스레드가
     *    동시에 공유해도 안전한 이유가 된다. (위 5번 설명 참고)
     */
    private final BookService bookService;

    /**
     * 생성자.
     *  - 스프링이 이 컨트롤러 빈을 "딱 하나" 만들 때, 이미 만들어 둔 BookService 빈을
     *    찾아 자동으로 끼워 넣어준다. (의존성 주입, DI = 필요한 부품을 밖에서 받아 끼우기)
     */
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ========================================================================
    // 아래부터는 "어떤 주소(URL)로 오면 어떤 일을 할지"를 적는 연결표(매핑)들이다.
    //  - @GetMapping : HTTP GET 방식 요청(주로 "조회/읽기")을 처리한다는 표시.
    //    (브라우저 주소창에 주소를 치고 엔터 = GET 요청)
    // ========================================================================

    /**
     * [전체 목록] 저장된 모든 책을 돌려준다.
     *  - 요청 예: GET http://localhost:8080/api/books
     *  - 흐름: 이 메서드 → bookService.findAll() → repository → DB(SELECT *)
     *  - 리턴한 List<Book> 은 스프링이 JSON 배열 [ {...}, {...} ] 로 바꿔 응답한다.
     */
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.findAll();
    }

    /**
     * [개수] 저장된 책이 총 몇 권인지 돌려준다.
     *  - 요청 예: GET http://localhost:8080/api/books/count
     *  - 주의: 이 "/count" 매핑을 아래 "/{id}" 매핑보다 위에 두는 이유 →
     *    스프링은 "글자가 똑 떨어지는 주소(/count)"를 "변하는 자리(/{id})"보다
     *    더 우선해서 매칭한다. 그래도 사람이 읽기 좋게 먼저 적어 둔다.
     */
    @GetMapping("/count")
    public long getBookCount() {
        return bookService.count();
    }

    /**
     * [제목 검색] 제목에 특정 단어가 들어간 책들을 돌려준다.
     *  - 요청 예: GET http://localhost:8080/api/books/search?keyword=Light
     *  - @RequestParam : 주소 뒤 "?keyword=Light" 같은 질의 문자열(쿼리 파라미터)에서
     *    keyword 값을 꺼내 메서드 파라미터로 넣어준다.
     *    (여기서 keyword="Light" 라는 값은 이 요청을 맡은 스레드의 "스택"에만 존재한다)
     */
    @GetMapping("/search")
    public List<Book> searchByTitle(@RequestParam String keyword) {
        return bookService.findByTitle(keyword);
    }

    /**
     * [별점 필터] 별점이 특정 값 "이상"인 책들을 돌려준다.
     *  - 요청 예: GET http://localhost:8080/api/books/rating?min=4
     *  - @RequestParam("min") : 쿼리 파라미터 이름은 "min"인데,
     *    자바 쪽 변수 이름은 minRating 으로 다르게 쓰고 싶을 때 괄호로 짝지어 준다.
     */
    @GetMapping("/rating")
    public List<Book> getByRating(@RequestParam("min") int minRating) {
        return bookService.findByRating(minRating);
    }

    /**
     * [가격 정렬] 모든 책을 가격이 낮은 순서로 정렬해 돌려준다.
     *  - 요청 예: GET http://localhost:8080/api/books/sorted-by-price
     */
    @GetMapping("/sorted-by-price")
    public List<Book> getAllSortedByPrice() {
        return bookService.findAllOrderByPriceAsc();
    }

    /**
     * [1권 조회] 책 번호(id)로 책 1권을 찾아 돌려준다.
     *  - 요청 예: GET http://localhost:8080/api/books/3   ← 끝의 3이 id
     *  - @PathVariable : 주소 경로 안의 "{id}" 자리에 들어온 값(여기선 3)을
     *    그대로 메서드 파라미터 id 로 꺼내 준다. (위 @RequestParam 의 ?키=값 방식과 다름)
     *
     *  - 반환 타입 ResponseEntity<Book> 이란?
     *      "HTTP 응답을 통째로(상태 코드 + 본문) 직접 만들어 돌려주는 상자"다.
     *      책을 찾았는지/못 찾았는지에 따라 응답을 다르게 주기 위해 사용한다.
     *        · 찾았으면  → 200 OK 와 함께 그 책(JSON)을 담아 보낸다.
     *        · 못 찾았으면 → 404 Not Found (그런 책 없음) 를 보낸다.
     *
     *  - findById 는 Optional<Book>("값이 있을 수도/없을 수도 있는 상자")을 돌려준다.
     *      .map(...)            : 값이 "있으면" 그 책으로 200 OK 응답을 만든다.
     *      .orElseGet(...)      : 값이 "없으면" 대신 404 응답을 만든다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(book -> ResponseEntity.ok(book))          // 있으면 200 OK + 책
                .orElseGet(() -> ResponseEntity.notFound().build()); // 없으면 404
    }
}
