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

/*
 * ============================================================================
 *  BookController ─ 웹 요청의 "최상단 입구(주문 받는 카운터)" 계층
 * ============================================================================
 *
 * [① UML & 도킹 관계]  ─ Controller는 DB 생김새를 1도 모른다. URL→Service 연결만 한다.
 *
 *   Client(브라우저)
 *      │  HTTP GET /api/books/...
 *      ▼
 *   ┌───────────────────┐  uses   ┌──────────────┐  uses   ┌─────────────────┐
 *   │  BookController    │ ──────▶ │  BookService │ ──────▶ │ BookRepository  │
 *   │  (@RestController) │ (final) │  (@Service)  │ (final) │ (JpaRepository) │
 *   └───────────────────┘         └──────────────┘         └────────┬────────┘
 *        "주문 받기"                  "요리 / 규칙"                    │ SELECT
 *                                                                    ▼
 *                                                               [ books ] (DB)
 *
 * ----------------------------------------------------------------------------
 * [② 메모리 맵]  ─ 싱글톤 빈 1개를 200개 스레드가 공유해도 안전한 이유.
 *
 *   [ 스레드별 Stack (격리) ]                 [ 공용 Heap (공유) ]
 *   ┌──────────────────────────┐             ┌──────────────────────────┐
 *   │ Thread-1 searchByTitle() │             │  BookController (Bean)    │
 *   │   └ keyword="Light"  ◀───┼── 값 격리    │   : 싱글톤 딱 1개          │
 *   ├──────────────────────────┤             │   └ bookService (final)   │
 *   │ Thread-2 getBookById()   │             │                           │
 *   │   └ id=3             ◀───┼── 값 격리    │  List<Book> / Book (결과) │
 *   └──────────────────────────┘             └──────────────────────────┘
 *    지역변수=각자 스택→안 섞임                참조(주소)만 스택에 올라감
 *
 * ----------------------------------------------------------------------------
 * [③ HTTP 엔드투엔드 물줄기]  ─ 예: GET /api/books/search?keyword=Light
 *
 *   (Ingress)        (Routing)                  (Processing)           (Egress)
 *   Browser ─GET─▶ Tomcat Thread ─▶ Dispatcher ─▶ Controller ─▶ Service ─▶ Repo ─▶ DB
 *    ?keyword=Light  (풀에서 1명)     Servlet      searchByTitle  findByTitle  LIKE %..%
 *                                    +HandlerMap      │                          │
 *                                                     ▼   List<Book>(Heap) ◀──────┘
 *                            JSON [{..},{..}] ◀─ @ResponseBody 자동 변환
 *   Browser ◀─ 200 OK + JSON Body ───────────────────┘
 *
 *   데이터 변환:  쿼리스트링 "?keyword=Light"  ─▶  String keyword
 *                ─▶  List<Book> (Entity, DTO 없이 직접 반환)  ─▶  JSON
 *
 * ----------------------------------------------------------------------------
 * [핵심 표식 두 가지]
 *   @RestController        = @Controller + @ResponseBody. 리턴값을 화면이름이 아닌
 *                            "HTTP 응답 본문(JSON)"으로 본다.
 *   @RequestMapping("/api/books") = 이 안 모든 메서드 주소의 공통 접두어.
 *                            예) @GetMapping("/count") → 실제 주소 /api/books/count
 * ============================================================================
 */
@RestController                  // 이 클래스 = 웹 컨트롤러 빈(리턴값을 JSON 본문으로)
@RequestMapping("/api/books")    // 이 안 모든 주소 앞에 /api/books 가 붙는다
public class BookController {

    private final BookService bookService;   // 유일한 멤버(final) → [④] Stateless 근거

    // 생성자 주입(DI): 스프링이 미리 만든 BookService 빈을 찾아 자동으로 끼워준다.
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ========================================================================
    //  연결표(매핑) — "어떤 URL이 오면 Service의 어떤 메서드를 부를지"
    //  @GetMapping = HTTP GET(조회/읽기) 요청 처리 표식.
    // ========================================================================

    // [메커니즘] 전체 목록: GET /api/books → findAll() → SELECT * → JSON 배열
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.findAll();
    }

    // [메커니즘] 개수: GET /api/books/count → SELECT count(*) → long 1개
    @GetMapping("/count")
    public long getBookCount() {
        return bookService.count();
    }

    // [메커니즘] 제목 검색: GET /api/books/search?keyword=Light → LIKE '%Light%'
    //   @RequestParam = ?키=값 형태의 "쿼리스트링" 값을 메서드 인자로 꺼내는 표식.
    //                   (인자명 keyword 와 쿼리이름 keyword 가 같아 이름 생략 가능)
    @GetMapping("/search")
    public List<Book> searchByTitle(@RequestParam String keyword) {
        return bookService.findByTitle(keyword);
    }

    // [메커니즘] 별점 필터: GET /api/books/rating?min=4 → 별점 4 이상
    //   ("min" 쿼리이름 ↔ minRating 자바변수명을 괄호로 짝지어 준다)
    @GetMapping("/rating")
    public List<Book> getByRating(@RequestParam("min") int minRating) {
        return bookService.findByRating(minRating);
    }

    // [메커니즘] 가격 오름차순 정렬: GET /api/books/sorted-by-price → ORDER BY price ASC
    @GetMapping("/sorted-by-price")
    public List<Book> getAllSortedByPrice() {
        return bookService.findAllOrderByPriceAsc();
    }

    // [메커니즘] 1권 조회: GET /api/books/3 ← 끝의 3이 경로상의 {id}
    //   ?키=값(@RequestParam)과 달리, 경로 칸의 값은 @PathVariable 로 꺼낸다.
    //   ResponseEntity = 상태코드+본문을 통째로 직접 조립하는 상자(찾음/못찾음 분기용).
    //
    // [⑤ 예외 U-턴 맵] (이 메서드가 무대)
    //   findById → Optional<Book>(있을수도/없을수도 상자)
    //     · 값 있으면 → .map(...)      → 200 OK + 책(JSON)
    //     · 값 없으면 → .orElseGet(...) → 404 Not Found
    //     · DB 커넥션 고갈 등 진짜 예외 → Repo가 throw → Service 전파 → Controller 전파
    //                                  → 스프링이 가로채 500 + 에러 JSON 으로 U-턴
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(book -> ResponseEntity.ok(book))            // 있으면 200 OK + 책
                .orElseGet(() -> ResponseEntity.notFound().build()); // 없으면 404
    }

    // ========================================================================
    // [④ Stateless Check]: 멤버는 final bookService 1개뿐(상태 변경 없음).
    //   손님별 값(keyword, id, minRating)은 전부 메서드 지역변수(Stack)에 격리되므로,
    //   200개 스레드가 같은 Controller 빈을 동시에 써도 동시성 충돌이 없다.
    // ========================================================================
}
