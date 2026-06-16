package com.example.psb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Book
 * ----------------------------------------------------------------------------
 * "Books to Scrape"(http://books.toscrape.com) 사이트에서 크롤링한 책 1권의
 * 정보를 표현하는 JPA 엔티티(Entity) 클래스다.
 *
 * [엔티티란?]
 *  - 데이터베이스의 "테이블 1개"와 매핑(연결)되는 자바 클래스를 말한다.
 *  - 이 클래스의 "객체(instance) 1개" = 테이블의 "행(row) 1개"에 대응한다.
 *  - 즉, Book 객체 하나 = books 테이블의 한 줄(책 한 권) 이다.
 *
 * [동작 흐름 요약]
 *  Jsoup으로 HTML을 파싱
 *      -> 파싱한 값으로 Book 객체를 생성(set)
 *      -> BookRepository.save(book) 호출
 *      -> JPA/Hibernate가 INSERT SQL을 만들어 H2 DB에 저장
 * ----------------------------------------------------------------------------
 */
@Entity                 // 이 클래스가 JPA 엔티티(= DB 테이블과 매핑됨)임을 선언
@Table(name = "books")  // 매핑될 테이블 이름을 "books"로 지정 (생략 시 클래스명 사용)
public class Book {

    /**
     * 기본 키(Primary Key, PK).
     *  - 각 행(책)을 유일하게 구분하는 식별자다.
     *  - 크롤링한 책 데이터에는 존재하지 않는, DB가 스스로 부여하는 번호다.
     */
    @Id // 이 필드가 PK임을 표시
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY 전략: PK 값을 DB가 auto_increment 처럼 자동으로 1씩 증가시켜 채워준다.
    private Long id;

    /**
     * 책 제목.
     *  - 예: "A Light in the Attic"
     *  - nullable = false : 이 컬럼은 NULL을 허용하지 않음(반드시 값이 있어야 함).
     *  - length = 500     : VARCHAR(500) 으로 생성 (제목이 길 수 있어 넉넉히)
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * 책 가격.
     *  - 사이트에는 "£51.77" 형태의 문자열로 표시된다.
     *  - 통화 기호(£)를 떼고 숫자만 저장하기 위해 double 타입을 사용한다.
     *  - 예: 51.77
     */
    @Column(nullable = false)
    private double price;

    /**
     * 별점(평점).
     *  - 사이트는 "One ~ Five" 같은 영어 단어로 별점을 표시한다.
     *  - 이를 1~5 의 정수로 변환해서 저장한다. (예: "Three" -> 3)
     */
    @Column(name = "rating")
    private int rating;

    /**
     * 재고/구매 가능 여부 텍스트.
     *  - 예: "In stock (22 available)"
     *  - 원문 문자열을 그대로 저장한다.
     */
    @Column(name = "availability", length = 100)
    private String availability;

    /**
     * 책 상세 페이지 URL.
     *  - 각 책의 상세 페이지로 이동하는 링크.
     *  - 중복 저장 방지나 재방문 시 식별용으로 유용하다.
     *  - unique = true : 같은 URL이 두 번 저장되지 않도록 유니크 제약을 건다.
     */
    @Column(name = "detail_url", length = 1000, unique = true)
    private String detailUrl;

    /**
     * 책 표지 이미지 URL.
     *  - 예: ".../media/cache/.../xxx.jpg"
     */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // ------------------------------------------------------------------------
    // 생성자(Constructor)
    //  - 객체를 만들 때 호출되는 특수한 메서드.
    // ------------------------------------------------------------------------

    /**
     * 기본 생성자(파라미터 없는 생성자).
     *  - JPA(Hibernate)는 DB에서 데이터를 읽어 객체로 만들 때
     *    이 "빈 생성자"를 내부적으로 사용하므로 반드시 있어야 한다.
     *  - protected/public 이어야 한다.
     */
    public Book() {
    }

    /**
     * 편의 생성자.
     *  - 크롤링한 값들을 한 번에 받아 객체를 손쉽게 만들기 위한 생성자.
     *  - id는 DB가 자동 생성하므로 여기서는 받지 않는다.
     *
     * @param title        책 제목
     * @param price        책 가격(숫자)
     * @param rating       별점(1~5)
     * @param availability 재고 텍스트
     * @param detailUrl    상세 페이지 URL
     * @param imageUrl     표지 이미지 URL
     */
    public Book(String title, double price, int rating,
                String availability, String detailUrl, String imageUrl) {
        this.title = title;
        this.price = price;
        this.rating = rating;
        this.availability = availability;
        this.detailUrl = detailUrl;
        this.imageUrl = imageUrl;
    }

    // ------------------------------------------------------------------------
    // Getter / Setter
    //  - Getter: 필드 값을 "읽어오는" 메서드 (예: getTitle() -> 제목 반환)
    //  - Setter: 필드 값을 "설정/변경하는" 메서드 (예: setTitle("...") )
    //  - JPA와 Spring은 이 메서드들을 통해 필드에 접근하므로 만들어 두는 것이 표준이다.
    // ------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * toString
     *  - 객체를 사람이 읽기 좋은 문자열로 표현해 주는 메서드.
     *  - 디버깅/로그 출력 시(예: System.out.println(book)) 유용하다.
     */
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", rating=" + rating +
                ", availability='" + availability + '\'' +
                ", detailUrl='" + detailUrl + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
