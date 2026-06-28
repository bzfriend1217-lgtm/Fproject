package com.example.psb.repository;

import com.example.psb.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================================
// [무엇인가] BookRepository = DB(H2)로 들어가고 나오는 "출입문" (DAO 계층)
// ----------------------------------------------------------------------------
// [Layer Docking Map] 누가 누구에게 도킹하는가
//
//   CrawlerRunner ──> BookCrawlerService ──> BookRepository ──> [books] (H2)
//                       (비즈니스 로직)         (이 파일)          (테이블)
//                                                  │
//                                          Spring Data JPA가
//                                          구현 클래스 자동 생성
//                                                  ▼
//                                          Hibernate가 SQL 발행
//
// [핵심 메커니즘] 우리는 "인터페이스"만 선언 → 구현체는 0줄.
//   실행 시점에 Spring이 프록시 구현 객체를 만들어 싱글톤 Bean으로 꽂아준다.
// ============================================================================
//
// [JpaRepository<Book, Long> 제네릭 해독]
//   JpaRepository< Book , Long >
//                  ▲      ▲
//                  │      └─ PK(@Id) 타입  ← Book.id 가 ____ 이므로 (TODO 맞히기)
//                  └──────── 다루는 엔티티 타입
//
//   상속만으로 공짜로 생기는 기본 메서드(직접 구현 X):
//     save / saveAll / findById / findAll / count / existsById / deleteById ...
//
@Repository // [퀴즈] 이 인터페이스가 "저장소 Bean"임을 표시. (JpaRepository 상속 시 생략 가능하나 명시)
public interface BookRepository extends JpaRepository<Book, Long> {

    // ------------------------------------------------------------------------
    // [Query Method 메커니즘] 메서드 "이름"이 곧 SQL 설계도
    //
    //   findBy + 필드명 + 조건키워드   ──해석──>   SELECT ... WHERE ...
    //   └ Containing → LIKE %?%   GreaterThanEqual → >=   OrderBy...Asc → ORDER BY ? ASC
    // ------------------------------------------------------------------------

    // [중복 체크용] detail_url 로 1건 조회.
    //   SQL ▶ SELECT * FROM books WHERE detail_url = ?
    //   반환이 Optional 인 이유: 결과가 "없을 수도" 있음 → null 대신 빈 상자로 안전 처리.
    // TODO(빈칸): 반환 타입은?  ________<Book> findByDetailUrl(String detailUrl);
    Optional<Book> findByDetailUrl(String detailUrl);

    // [저장 전 가드레일] 이미 있는 url 인가?  if (!repo.existsByDetailUrl(url)) save(...)
    //   SQL ▶ SELECT count(*) > 0 FROM books WHERE detail_url = ?
    // TODO(이름 맞히기): "존재 여부"를 묻는 접두 키워드는?  ______ByDetailUrl(...)
    boolean existsByDetailUrl(String detailUrl);

    // [제목 검색] 키워드가 "포함된" 책 전체.
    //   SQL ▶ SELECT * FROM books WHERE title LIKE %?%
    // TODO(키워드): LIKE %?% 로 바뀌는 꼬리 키워드는?  findByTitle__________(String keyword)
    List<Book> findByTitleContaining(String keyword);

    // [별점 필터] rating 이 기준값 이상(>=)인 책.
    //   SQL ▶ SELECT * FROM books WHERE rating >= ?
    List<Book> findByRatingGreaterThanEqual(int rating);

    // [정렬 조회] 가격 오름차순 전체 목록 ("저렴한 순").
    //   SQL ▶ SELECT * FROM books ORDER BY price ASC
    List<Book> findAllByOrderByPriceAsc();
}

// [Stateless Check]
//  인터페이스라 멤버 변수(상태) 자체가 없고, Spring이 만든 구현 Bean도 무상태다.
//  모든 입력(url/keyword/rating)은 메서드 인자(각 스레드 Stack)로만 흐르므로,
//  200개 스레드가 동시에 호출해도 서로의 값을 덮어쓰지 않는다. (동시성 안전)
