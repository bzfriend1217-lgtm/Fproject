package com.example.psb.service;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// ============================================================================
// [무엇인가] BookService = 이미 저장된 책을 "꺼내 보는" 읽기 전용 창구 (조회 로직)
// ----------------------------------------------------------------------------
// [Layer Docking Map] 누가 누구에게 도킹하는가
//
//   (미래) Controller ──> BookService ──> BookRepository ──> [books] (H2)
//                          (이 파일)        (DB 출입문)         (테이블)
//                          읽기 중심           │
//                                       Spring Data JPA가 SQL 발행
//
// [쓰기 vs 읽기 분리] 같은 books 테이블을 두 서비스가 역할로 나눠 쓴다
//   ┌ BookCrawlerService : 인터넷 → DB  "저장(save)"    ── 쓰기 중심
//   └ BookService (이 파일): DB → 바깥   "조회(find)"     ── 읽기 중심
//
// [HTTP End-to-End Flow Map] 예) 별점 4점 이상 책 조회 (Controller 연결 시)
//
//   [Ingress]  GET /books?minRating=4
//        │  (Tomcat 스레드가 요청 1건을 잡음)
//        ▼
//   Controller ──minRating(int)──> BookService.findByRating(4)
//        │                                │
//        │                                ▼
//        │                    bookRepository.findByRatingGreaterThanEqual(4)
//        │                                │  SQL ▶ SELECT * FROM books WHERE rating >= 4
//        │                                ▼
//        │                          List<Book> (Heap)
//        ▼
//   [Egress]  200 OK + JSON 배열  [ {id,title,price,rating,...}, ... ]
//
//   [Exception U-Turn] DB 커넥션 고갈/쿼리 실패 시
//     Repository ✖ ──throw──> Service(그냥 통과) ──> Controller ──> 500 + 에러 JSON
//     (이 클래스엔 try-catch 없음 → 예외를 위로 "전파"만 한다)
// ============================================================================
@Service // [퀴즈] "이 클래스는 비즈니스 로직 부품(Bean)"임을 Spring에 알리는 애너테이션
public class BookService {

    // [의존성] DB 출입문. final = 한 번 꽂히면 안 바뀜 → 무상태 보장에 기여.
    private final BookRepository bookRepository;

    // [생성자 주입(DI)] Spring이 BookRepository Bean을 찾아 자동으로 끼워준다.
    //   필드가 1개뿐이라 @Autowired 생략 가능 (생성자가 1개면 자동 인식).
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ------------------------------------------------------------------------
    // 조회(읽기) 기능들 — 전부 "Repository에 위임"하는 얇은 통로 (지금은 가공 없음)
    //
    //   [위임 패턴] Service 메서드 ──그대로 호출──> Repository 메서드
    //   나중에 검증/필터/조합 로직이 생기면 "이 자리"에 끼워 넣으면 됨.
    // ------------------------------------------------------------------------

    // [전체 조회] SELECT * FROM books   (JpaRepository 공짜 기본 메서드)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // [PK 1건 조회] SELECT * FROM books WHERE id = ?
    //   반환이 Optional 인 이유: 그 id가 "없을 수도" 있음 → null 대신 빈 상자.
    // TODO(빈칸): 없을 수도 있는 값을 감싸는 안전 상자 타입은?  ________<Book>
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    // [개수 세기] SELECT count(*) FROM books   (JpaRepository 공짜 기본 메서드)
    public long count() {
        return bookRepository.count();
    }

    // [제목 검색] title 에 keyword 가 포함된 책.  SQL ▶ ... WHERE title LIKE %?%
    // TODO(이름 맞히기): Repository의 어떤 메서드로 위임?  bookRepository.findByTitle__________(keyword)
    public List<Book> findByTitle(String keyword) {
        return bookRepository.findByTitleContaining(keyword);
    }

    // [별점 필터] rating 이 minRating 이상(>=)인 책.  SQL ▶ ... WHERE rating >= ?
    public List<Book> findByRating(int minRating) {
        return bookRepository.findByRatingGreaterThanEqual(minRating);
    }

    // [정렬 조회] 가격 오름차순 전체.  SQL ▶ ... ORDER BY price ASC ("저렴한 순")
    public List<Book> findAllOrderByPriceAsc() {
        return bookRepository.findAllByOrderByPriceAsc();
    }
}

// [Stateless Check]
//  유일한 멤버 변수 bookRepository 는 final 싱글톤(읽기 전용 참조)이라 상태가 아니다.
//  모든 입력(id/keyword/minRating)과 결과(List<Book>)는 메서드 인자·지역 변수로만
//  흐르므로 각 스레드의 Stack에 격리된다.
//  → 200개 스레드가 동시에 조회해도 서로의 값을 덮어쓰지 않는다. (동시성 안전)
//
//   [ 스레드별 Stack (격리) ]            [ 공용 Heap (공유) ]
//   ┌─────────────────────────┐         ┌──────────────────────────┐
//   │ Thread-1 findByRating(4) │ ──┐     │ BookService (싱글톤 1개)  │
//   │  └ local: minRating=4    │   │     │  └ bookRepository(final) │
//   └─────────────────────────┘   ├───> │ BookRepository (싱글톤)   │
//   ┌─────────────────────────┐   │     │                          │
//   │ Thread-2 findByTitle(..) │ ──┘     │ List<Book> (조회 결과)    │
//   │  └ local: keyword="Light"│         │  : 호출마다 새로 생성     │
//   └─────────────────────────┘         └──────────────────────────┘
