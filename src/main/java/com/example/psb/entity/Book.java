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
//   │ price     : double   │        │ 1  │ A...  │ 51.77 │  ...   │ <- 객체 1개 = 1행
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
@Entity                 // [퀴즈] 이 클래스가 "DB 테이블과 매핑됨"을 선언하는 애너테이션
@Table(name = "books")  // 매핑될 테이블 이름 지정 (생략 시 클래스명 그대로 사용)
public class Book {
        // Q: 이게 각 행의 열들을(column)들을 구별하는거지? 여기서 long, string 이런식으로 데이터 속성을 적고 그다음 column을 쓰고 다음 데이터로 넘어가는 식(column에 대한 특징이 먼저오네)으로 그 옆에다 그 값의 특징을 적고 밑에다

    // --- [필드 = 테이블의 컬럼] -------------------------------------------------
    // 패턴:  @애너테이션(컬럼 특징)  →  자료형  필드명;   (특징이 위/먼저, 값이 아래)

    // PK: 각 행을 유일하게 구분하는 번호. 크롤링 데이터엔 없고 DB가 자동 부여.
    @Id // [퀴즈] 이 필드가 PK(기본 키)임을 표시하는 애너테이션
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 auto_increment로 1씩 자동 증가
    private Long id;

    // 책 제목 (예: "A Light in the Attic")
    @Column(nullable = false, length = 500) // NULL 금지 + VARCHAR(500)
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

    // 상세 페이지 URL: 중복 저장 방지용으로 유니크 제약
    @Column(name = "detail_url", length = 1000, unique = true) // [퀴즈] 같은 값 2번 저장 막는 옵션 이름은?
    private String detailUrl;

    // 표지 이미지 URL (예: ".../media/cache/.../xxx.jpg")
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // --- [생성자] --------------------------------------------------------------

    // 기본 생성자(빈 생성자): JPA가 DB → 객체 복원 시 내부적으로 사용 → 반드시 필요.
    public Book() {
    }
        // Q: 이게 약간 그런건가? 파이선에서 함수 만들었을때 self쓰는듯한 그런거? 한마디로 자바의 문법인가?

    // 편의 생성자: 크롤링 값들을 한 번에 받아 객체 생성. id는 DB가 채우므로 안 받음.
        // Q: 여기서 @param 뒤에 오는건 자기 클래스안에서만 읽어오는게 가능한가?
    public Book(String title, double price, int rating,
                String availability, String detailUrl, String imageUrl) {
        // [퀴즈] 아래 빈칸: "이 객체 자신의 필드"를 가리키는 키워드는?
        // TODO(빈칸): ____.title = title;   ← 4글자 키워드를 떠올려 채워보기
        this.title = title;
        this.price = price;
        this.rating = rating;
        this.availability = availability;
        this.detailUrl = detailUrl;
        this.imageUrl = imageUrl;
    }

    // --- [Getter / Setter] -----------------------------------------------------
    // Getter: 값 읽기(getTitle() -> 반환)  |  Setter: 값 변경(setTitle("..."))
    // JPA/Spring이 이 메서드로 필드에 접근 → public 으로 열어둠.
        // Q: 여기서는 public인데 그게 다른 곳에서 접근하기 위해서인가?
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
