package com.example.psb.service;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * BookService
 * ----------------------------------------------------------------------------
 * 이미 DB에 저장된 책 데이터를 "조회/검색"하는 일을 담당하는 서비스(핵심 기능) 클래스다.
 *
 * [BookCrawlerService 와 무엇이 다른가?]
 *  - BookCrawlerService : 인터넷에서 책을 긁어와 DB에 "저장"하는 역할 (쓰기 중심).
 *  - BookService(이 클래스): 이미 저장된 책을 "꺼내 보는" 역할 (읽기 중심).
 *  - 이렇게 역할을 나눠 두면, 나중에 화면(Controller)이나 다른 코드가
 *    "책 목록을 보여줘", "별점 높은 책만 줘" 같은 요청을 할 때
 *    이 클래스의 메서드만 호출하면 되어 깔끔하다.
 *
 * [왜 Repository를 직접 안 쓰고 Service를 한 겹 더 두나?]
 *  - Repository는 "DB에서 데이터를 꺼내오는 순수한 통로"다.
 *  - Service는 그 위에서 "어떤 규칙으로 데이터를 다룰지"(비즈니스 로직)를 담는 자리다.
 *  - 지금은 단순히 Repository를 그대로 호출하지만, 나중에 검증/가공/조합 같은
 *    로직이 늘어나면 모두 이 Service 안에 모아 둘 수 있다.
 *
 * [@Service 란?]
 *  - "이 클래스는 비즈니스 로직을 담당하는 부품이다"라고 Spring에게 알리는 표식.
 *  - 이렇게 표시하면 Spring이 객체(빈, Bean)를 자동으로 만들어 관리해 준다.
 * ----------------------------------------------------------------------------
 */
@Service
public class BookService {

    /**
     * 책 데이터에 접근하는 저장소.
     *  - final : 한 번 정해지면 바뀌지 않는다는 표시.
     *  - 아래 생성자를 통해 Spring이 자동으로 넣어준다. (의존성 주입, DI)
     */
    private final BookRepository bookRepository;

    /**
     * 생성자.
     *  - Spring이 이 서비스 객체를 만들 때, BookRepository 빈을 찾아 자동으로 끼워준다.
     *  - "필요한 부품을 밖에서 받아 끼우는 것" = 의존성 주입(DI).
     */
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ------------------------------------------------------------------------
    // 조회(읽기) 기능들
    // ------------------------------------------------------------------------

    /**
     * 저장된 모든 책을 가져온다.
     *
     * @return 책 전체 목록 (List<Book> : 여러 개의 Book을 담는 목록)
     */
    public List<Book> findAll() {
        // findAll()은 JpaRepository가 공짜로 제공하는 기본 메서드다. (SELECT * FROM books)
        return bookRepository.findAll();
    }

    /**
     * PK(id)로 책 1권을 찾는다.
     *
     * @param id 찾고 싶은 책의 기본 키(번호)
     * @return 찾은 Book. 없을 수도 있으므로 Optional로 감싸 반환한다.
     *         - Optional<Book> : "값이 있을 수도, 없을 수도 있는 상자"라는 뜻.
     *           (null 때문에 생기는 오류를 줄이기 위한 자바의 안전장치)
     */
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    /**
     * 저장된 책이 모두 몇 권인지 센다.
     *
     * @return 전체 책 개수
     */
    public long count() {
        // count()도 JpaRepository 기본 메서드. (SELECT count(*) FROM books)
        return bookRepository.count();
    }

    /**
     * 제목에 특정 단어가 포함된 책들을 찾는다. (간단한 검색)
     *
     * @param keyword 검색어 (예: "Light")
     * @return 제목에 keyword가 들어간 책 목록
     */
    public List<Book> findByTitle(String keyword) {
        // BookRepository에 우리가 직접 선언해 둔 쿼리 메서드를 사용. (LIKE '%keyword%')
        return bookRepository.findByTitleContaining(keyword);
    }

    /**
     * 별점이 특정 값 "이상"인 책들을 찾는다. (예: 4점 이상만 보기)
     *
     * @param minRating 최소 별점 (이 값 포함, 이상)
     * @return 조건을 만족하는 책 목록
     */
    public List<Book> findByRating(int minRating) {
        return bookRepository.findByRatingGreaterThanEqual(minRating);
    }

    /**
     * 저장된 책 전부를 가격이 낮은 순서(오름차순, ASC)로 정렬해 반환한다.
     *
     * @return 가격 오름차순으로 정렬된 책 목록
     */
    public List<Book> findAllOrderByPriceAsc() {
        return bookRepository.findAllByOrderByPriceAsc();
    }
}
