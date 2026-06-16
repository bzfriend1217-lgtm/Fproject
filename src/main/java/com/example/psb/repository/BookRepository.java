package com.example.psb.repository;

import com.example.psb.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BookRepository
 * ----------------------------------------------------------------------------
 * Book 엔티티를 데이터베이스(H2)와 주고받는 "저장소(Repository) 계층" 인터페이스다.
 *
 * [Repository 란?]
 *  - DB에 직접 SQL을 쓰지 않고도 데이터를 저장/조회/수정/삭제(CRUD) 할 수 있게
 *    해주는 객체를 말한다.
 *  - 여기서는 "인터페이스"만 선언하면 끝이다.
 *    실제 구현 클래스는 Spring Data JPA가 애플리케이션 실행 시 자동으로 만들어
 *    스프링 빈(Bean)으로 등록해 준다. (우리가 구현 코드를 짤 필요가 없다!)
 *
 * [JpaRepository<Book, Long> 의 의미]
 *  - 제네릭 첫 번째 Book : 이 저장소가 다루는 엔티티 타입
 *  - 제네릭 두 번째 Long : 그 엔티티의 PK(@Id) 타입 (Book.id 가 Long 이므로)
 *  - 이것을 상속(extends)하면 아래의 기본 메서드들을 "공짜로" 사용할 수 있다:
 *      save(book)        : 저장(INSERT) 또는 수정(UPDATE)
 *      saveAll(list)     : 여러 건 한 번에 저장
 *      findById(id)      : PK로 1건 조회 -> Optional<Book> 반환
 *      findAll()         : 전체 조회 -> List<Book> 반환
 *      count()           : 전체 개수
 *      existsById(id)    : 해당 PK 존재 여부
 *      deleteById(id)    : PK로 삭제
 *      delete(book)      : 객체로 삭제
 *
 * [사용 예시]
 *      Book book = new Book("제목", 51.77, 3, "In stock", url, imgUrl);
 *      bookRepository.save(book);             // DB에 저장
 *      List<Book> all = bookRepository.findAll(); // 전체 조회
 * ----------------------------------------------------------------------------
 */
@Repository // 이 인터페이스가 "저장소" 역할의 스프링 빈임을 표시 (JpaRepository 상속 시 생략 가능하지만 명시적으로 둠)
public interface BookRepository extends JpaRepository<Book, Long> {

    // ------------------------------------------------------------------------
    // 쿼리 메서드(Query Method)
    //  - 메서드 "이름"만 규칙에 맞게 지으면, Spring Data JPA가 그 이름을 해석해
    //    자동으로 SQL을 만들어 실행해 준다. (직접 구현 X)
    //  - 규칙 예: findBy + 필드명 + 조건키워드(Containing, GreaterThan, OrderBy ...)
    // ------------------------------------------------------------------------

    /**
     * 상세 페이지 URL로 책 1건을 찾는다.
     *  - 생성되는 SQL(개념): SELECT * FROM books WHERE detail_url = ?
     *  - 반환 타입이 Optional<Book> 인 이유:
     *      결과가 없을 수도 있기 때문. (있으면 값이 담기고, 없으면 비어 있음)
     *  - 활용: 크롤링 시 "이미 저장된 책인지" 중복 체크할 때 유용하다.
     */
    Optional<Book> findByDetailUrl(String detailUrl);

    /**
     * 상세 페이지 URL이 이미 DB에 존재하는지 확인한다(true/false).
     *  - 생성되는 SQL(개념): SELECT count(*) > 0 FROM books WHERE detail_url = ?
     *  - 활용: 저장 전에 if (!repo.existsByDetailUrl(url)) { save(...) } 형태로
     *          중복 저장을 막을 때 깔끔하다.
     */
    boolean existsByDetailUrl(String detailUrl);

    /**
     * 제목에 특정 단어가 "포함된" 책들을 모두 찾는다.
     *  - Containing 키워드 -> LIKE '%검색어%' 로 변환된다.
     *  - 생성되는 SQL(개념): SELECT * FROM books WHERE title LIKE %?%
     *  - 활용: 간단한 제목 검색 기능.
     */
    List<Book> findByTitleContaining(String keyword);

    /**
     * 별점(rating)이 특정 값 "이상(>=)"인 책들을 찾는다.
     *  - GreaterThanEqual 키워드 -> >= 비교로 변환된다.
     *  - 생성되는 SQL(개념): SELECT * FROM books WHERE rating >= ?
     *  - 활용: "별점 4점 이상 책만 보기" 같은 필터.
     */
    List<Book> findByRatingGreaterThanEqual(int rating);

    /**
     * 가격이 낮은 순서로 정렬된 전체 책 목록을 반환한다.
     *  - OrderByPriceAsc -> price 기준 오름차순(ASC) 정렬.
     *  - 생성되는 SQL(개념): SELECT * FROM books ORDER BY price ASC
     *  - 활용: "저렴한 책 순으로 보기".
     */
    List<Book> findAllByOrderByPriceAsc();
}
