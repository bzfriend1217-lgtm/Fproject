package com.example.psb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ============================================================================
// [무엇인가] Book = "책 1권" = books 테이블의 "행(row) 1줄"
// ----------------------------------------------------------------------------
// [Object ↔ Table 매핑 구조]
//
//   Book 객체 (Java/Heap)            books 테이블 (H2 DB)
//   ┌──────────────────────┐        ┌────┬───────┬───────┬────────┐
//   │ id        : Long     │ ─────> │ id │ title │ price │  ...   │
//   │ title     : String   │        ├────┼───────┼───────┼────────┤
//   │ price     : double   │        │ 1  │ A...  │ 51.77 │  ...   │  <- 객체 1개 = 1행
//   │ rating    : int      │        │ 2  │ B...  │ 33.34 │  ...   │
//   │ ...                  │        └────┴───────┴───────┴────────┘
//   └──────────────────────┘
//
// [데이터 생명주기 (Egress: 객체 -> DB)]
//   Jsoup HTML 파싱 ──> new Book(...) ──> repository.save(book)
//                                              │
//                                  Hibernate가 INSERT SQL 자동 생성
//                                              ▼
//                                        H2 DB(books)에 저장
// ============================================================================
@Entity
@Table(name = "books")
public class Book {

    // --- [필드 = 테이블의 컬럼] -------------------------------------------------
    // 패턴:  @애너테이션(컬럼 특징)  →  자료형  필드명;   (특징이 위/먼저, 값이 아래)

    // PK: 각 행을 유일하게 구분하는 번호. 크롤링 데이터엔 없고 DB가 자동 부여.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 책 제목 (예: "A Light in the Attic")
    @Column(nullable = false, length = 500)
    private String title;

    // 가격: 사이트는 "£51.77" 문자열 → £ 떼고 숫자만 저장 → 51.77
    @Column(nullable = false)
    private double price;

    // 별점: 사이트는 "One~Five" 영어 단어 → 1~5 정수로 변환 (예: "Three" -> 3)
    @Column(name = "rating")
    private int rating;

    // 재고 텍스트: 원문 그대로 저장 (예: "In stock (22 available)")
    @Column(name = "availability", length = 100)
    private String availability;

    // 상세 페이지 URL: unique = true 로 중복 저장 방지
    @Column(name = "detail_url", length = 1000, unique = true)
    private String detailUrl;

    // 표지 이미지 URL (예: ".../media/cache/.../xxx.jpg")
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // --- [생성자] --------------------------------------------------------------

    // 기본 생성자: JPA가 DB → 객체 복원 시 내부적으로 사용 → 반드시 필요.
    public Book() {
    }

    // 편의 생성자: 크롤링 값들을 한 번에 받아 객체 생성. id는 DB가 채우므로 안 받음.
    public Book(String title, double price, int rating,
                String availability, String detailUrl, String imageUrl) {
        this.title = title;
        this.price = price;
        this.rating = rating;
        this.availability = availability;
        this.detailUrl = detailUrl;
        this.imageUrl = imageUrl;
    }

    // --- [Getter / Setter] -----------------------------------------------------
    // JPA/Spring이 이 메서드로 필드에 접근 → public 으로 열어둠.
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

    // toString: 객체를 사람이 읽기 좋은 문자열로. 디버깅/로그(println) 시 유용.
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

// [Stateless Check]
//  Book은 "공유 싱글톤 빈"이 아니라 요청마다 new 로 찍어내는 "데이터 박스"다.
//  각 스레드가 자기 Stack에서 자기 Book 객체를 따로 만들므로(공유 X),
//  200개 스레드가 동시에 크롤링해도 객체끼리 서로의 값을 덮어쓰지 않는다.
