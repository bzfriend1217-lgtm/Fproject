package com.example.psb.service;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// ============================================================================
// [무엇인가] BookService = 이미 저장된 책을 "꺼내 보는" 읽기 전용 창구 (조회 로직)
// ----------------------------------------------------------------------------
// [① UML Class Diagram & Relationship]
//
//   BookController ──(calls)──> BookService ──(uses)──> BookRepository ──> [books] (H2)
//                                (이 파일)              (DB 출입문)            (테이블)
//                                읽기 중심
//
// [쓰기 vs 읽기 역할 분리] 같은 books 테이블을 두 서비스가 나눠 쓴다
//   ┌ BookCrawlerService : 인터넷 → DB   "저장(save)"  ── 쓰기 중심
//   └ BookService  (이 파일) : DB → 바깥  "조회(find)"  ── 읽기 중심
//
// [Skeleton]
//   public class BookService {
//       private final BookRepository bookRepository;        // 생성자 DI
//       public List<Book>     findAll() { ... }             // 전체 조회
//       public Optional<Book> findById(Long id) { ... }    // 1건 조회
//       public long           count() { ... }               // 개수 세기
//       public List<Book>     findByTitle(String) { ... }  // 제목 검색
//       public List<Book>     findByRating(int) { ... }    // 별점 필터
//       public List<Book>     findAllOrderByPriceAsc() { ... } // 가격순 정렬
//   }
//
// ----------------------------------------------------------------------------
// [③ End-to-End 데이터 물줄기]  예) GET /api/books/rating?min=4
//
//   (Ingress) Browser ─GET─> /api/books/rating?min=4
//        │  Tomcat 스레드 1개가 요청을 잡음
//        ▼
//   BookController ──minRating(int=4)──> BookService.findByRating(4)
//        │                                      │
//        │                                      ▼
//        │                      bookRepository.findByRatingGreaterThanEqual(4)
//        │                                      │  SQL ▶ WHERE rating >= 4
//        │                                      ▼
//        │                               List<Book> (Heap)
//        ▼
//   (Egress)  200 OK + JSON 배열  [ {id, title, price, rating, ...}, ... ]
//
// [⑤ Exception U-Turn]
//   Repository ✖ ──throw──> Service(전파) ──> Controller(전파) ──> Spring ──> 500 + 에러 JSON
//   (이 클래스에 try-catch 없음 → 예외를 위로 "전파"만 한다)
//
// ============================================================================
@Service
public class BookService {

    private final BookRepository bookRepository;

    // Spring이 BookRepository 빈을 찾아 자동으로 끼워준다. (생성자 주입, DI)
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ------------------------------------------------------------------------
    // 조회(읽기) 메서드들 — 전부 Repository 에 "위임"하는 얇은 통로
    //   [위임 패턴] Service 메서드 ──그대로 호출──> Repository 메서드
    //   나중에 검증 · 필터 · 조합 로직이 생기면 이 자리에 끼워 넣으면 된다.
    // ------------------------------------------------------------------------

    // SQL ▶ SELECT * FROM books
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // SQL ▶ SELECT * FROM books WHERE id = ?  (없으면 Optional.empty() 반환)
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    // SQL ▶ SELECT count(*) FROM books
    public long count() {
        return bookRepository.count();
    }

    // SQL ▶ SELECT * FROM books WHERE title LIKE %keyword%
    public List<Book> findByTitle(String keyword) {
        return bookRepository.findByTitleContaining(keyword);
    }

    // SQL ▶ SELECT * FROM books WHERE rating >= minRating
    public List<Book> findByRating(int minRating) {
        return bookRepository.findByRatingGreaterThanEqual(minRating);
    }

    // SQL ▶ SELECT * FROM books ORDER BY price ASC
    public List<Book> findAllOrderByPriceAsc() {
        return bookRepository.findAllByOrderByPriceAsc();
    }

    // ========================================================================
    // [④ Stateless Safety Check]
    //   멤버 변수 = bookRepository(final 불변 빈) 1개뿐.
    //   id / keyword / minRating 등 입력값과 List<Book> 결과는 전부 지역변수
    //   → 호출마다 각 스레드의 Stack 에 따로 생긴다(격리).
    //   ⇒ 200개 스레드가 동시에 조회해도 서로의 값을 덮어쓰지 않는다.
    //
    //   [ Thread별 Stack (격리) ]             [ 공용 Heap (공유) ]
    //   ┌──────────────────────────┐          ┌──────────────────────────┐
    //   │ Thread-1 findByRating(4) │ ──┐      │ BookService (싱글톤 1개)  │
    //   │  └ minRating = 4         │   ├────> │  └ bookRepository(final) │
    //   ├──────────────────────────┤   │      └──────────────────────────┘
    //   │ Thread-2 findByTitle(…)  │ ──┘
    //   │  └ keyword = "Light"     │
    //   └──────────────────────────┘
    // ========================================================================
}
