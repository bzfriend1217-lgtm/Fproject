package com.example.psb.service;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * BookCrawlerService
 * ----------------------------------------------------------------------------
 * "Books to Scrape"(http://books.toscrape.com)를 실제로 긁어와(크롤링)
 * Book 객체로 만들고, BookRepository를 통해 H2 DB에 저장하는 핵심 로직.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ ① [UML Class Diagram & Relationship]                                      │
 * │                                                                           │
 * │   CrawlerRunner ──(calls)──> BookCrawlerService ──(uses)──> BookRepository│
 * │                                      │                            │       │
 * │                              (Jsoup로 HTML 수집)            (saves Entity) │
 * │                                      │                            ▼       │
 * │                              [HTML Element] ──parse──> [Book] ──> books(DB)│
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ ③ [End-to-End 데이터 물줄기]  (HTTP는 "내가 외부로 거는" 방향)            │
 * │                                                                           │
 * │  (Egress) ─GET─> books.toscrape.com/catalogue/page-N.html                 │
 * │  (Ingress) <─HTML 200─ 응답 본문(통째로)                                   │
 * │       │                                                                   │
 * │   String(HTML)  ──Jsoup.parse──>  Document                                │
 * │       │                              │ select("article.product_pod")      │
 * │       │                              ▼                                     │
 * │   Elements(책 N개)  ──parseBook──>  Book 객체  ──save──>  books 테이블 row  │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * [@Service 란?]
 *   - "이 클래스는 비즈니스 로직 부품"이라고 Spring에 알리는 표식.
 *   - Spring이 객체를 자동 생성·관리한다. (이 객체를 "빈(Bean)"이라 부른다.)
 * ----------------------------------------------------------------------------
 */
@Service
public class BookCrawlerService {

    /**
     * 책을 DB에 저장할 때 쓰는 저장소.
     *  - final : 한 번 정해지면 바뀌지 않는다는 표시.
     *  - 아래 "생성자"를 통해 Spring이 자동으로 넣어준다. (의존성 주입, DI)
     */
    private final BookRepository bookRepository;

    // ------------------------------------------------------------------------
    // [상수] 안 변하는 값은 위에 모아 이름을 붙인다. (static final = 모든 객체 공유)
    //  - 흩어진 "매직 문자열"을 이름 있는 상수로 모으면, HTML 구조가 바뀌어도
    //    여기 한 곳만 고치면 된다. (유지보수 ↑)
    // ------------------------------------------------------------------------

    /** 목록 페이지 주소 틀. %d 자리에 페이지 번호(1,2,3...)가 들어간다. */
    private static final String PAGE_URL = "http://books.toscrape.com/catalogue/page-%d.html";

    /** 책 1권 덩어리 / 각 값으로 가는 CSS 셀렉터(=HTML 속 위치를 가리키는 주소). */
    private static final String SEL_PRODUCT      = "article.product_pod"; // 책 1권 덩어리
    private static final String SEL_TITLE_LINK   = "h3 a";                // 제목 + 상세링크
    private static final String SEL_PRICE        = "p.price_color";       // 가격
    private static final String SEL_STAR         = "p.star-rating";       // 별점
    private static final String SEL_AVAILABILITY = "p.instock.availability"; // 재고
    private static final String SEL_IMAGE        = ".image_container img"; // 표지 이미지

    /** 별점 단어표: 인덱스 0=One(=1점) ... 4=Five(=5점). parseRating에서 사용. */
    private static final String[] RATING_WORDS = {"One", "Two", "Three", "Four", "Five"};

    /**
     * 생성자.
     *  - Spring이 이 서비스를 만들 때 BookRepository 빈을 찾아 자동으로 끼워준다.
     */
    public BookCrawlerService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * 지정한 페이지 수만큼 크롤링해서 DB에 저장한다.
     *
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │ ⑤ [예외 발생 시 역방향(U-Turn) 물줄기]                                 │
     * │                                                                       │
     * │   Jsoup.get()  ──타임아웃/404/500──>  IOException 발생                 │
     * │        │                                   │                          │
     * │        └── 이 페이지 처리만 포기하고 ──────┘                          │
     * │            catch에서 메시지만 찍은 뒤 → 다음 page로 for문 계속.        │
     * │   (즉, 한 페이지가 실패해도 전체 크롤링은 멈추지 않는다.)              │
     * └─────────────────────────────────────────────────────────────────────┘
     *
     * @param maxPages 최대 몇 페이지까지 긁을지 (사이트 전체는 50페이지)
     * @return 실제로 새로 저장한 책 개수
     */
    public int crawlAndSave(int maxPages) {
        int savedCount = 0; // 새로 저장한 책 수를 세는 변수 (지역변수 = Stack에 격리)

        for (int page = 1; page <= maxPages; page++) {
            String url = String.format(PAGE_URL, page);
            System.out.println("[크롤링] 페이지 접속: " + url);

            try {
                Document doc = fetchPage(url);                 // HTML 통째로 받아오기
                Elements products = doc.select(SEL_PRODUCT);   // 책 덩어리 전부 찾기

                if (products.isEmpty()) {                      // 빈 페이지면 종료
                    System.out.println("[크롤링] 더 이상 책이 없어 종료합니다.");
                    break;
                }

                for (Element product : products) {
                    Book book = parseBook(product);            // 덩어리 -> Book 객체
                    if (saveIfNew(book)) {                     // 중복 아니면 저장
                        savedCount++;
                    }
                }

            } catch (IOException e) {
                // 네트워크 오류 등으로 접속 실패 시 → 멈추지 않고 다음 페이지로.
                System.out.println("[크롤링] 페이지 처리 실패: " + url + " (" + e.getMessage() + ")");
            }
        }

        System.out.println("[크롤링] 완료! 새로 저장한 책 수 = " + savedCount);
        return savedCount;
    }

    /**
     * 주소(url) 하나에 접속해 HTML 문서(Document)를 받아온다.
     *  - userAgent : 어떤 브라우저인 척할지 (서버 차단 회피)
     *  - timeout   : 10초 안에 응답 없으면 포기 -> IOException
     *
     * @throws IOException 접속/응답 실패 시. (호출한 쪽 crawlAndSave가 잡는다)
     */
    private Document fetchPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (PSB-Crawler)")
                .timeout(10_000)
                .get();
    }

    /**
     * 같은 상세 URL의 책이 아직 없으면 저장한다. (중복 방지)
     *
     * @return 새로 저장했으면 true, 이미 있어서 건너뛰었으면 false
     */
    private boolean saveIfNew(Book book) {
        if (bookRepository.existsByDetailUrl(book.getDetailUrl())) {
            return false;
        }
        bookRepository.save(book);
        return true;
    }

    /**
     * 책 덩어리(HTML 조각) 하나에서 값들을 뽑아 Book 객체로 만든다.
     *
     *   ② [데이터 변환]  HTML Element  ───추출───>  자바 원시값  ───조립───>  Book
     *      <article.product_pod>                 title, price...           new Book(...)
     *
     * @param product <article class="product_pod"> 한 개에 해당하는 HTML 조각
     * @return 값이 채워진 Book 객체
     */
    private Book parseBook(Element product) {
        // selectFirst(): 조건에 맞는 첫 요소 1개 / attr(): 속성값 / text(): 안쪽 글자
        String title        = product.selectFirst(SEL_TITLE_LINK).attr("title");   // 안 잘린 전체 제목
        double price        = parsePrice(product.selectFirst(SEL_PRICE).text());   // "£51.77" -> 51.77
        int    rating       = parseRating(product);                                // "Three" -> 3
        String availability = product.selectFirst(SEL_AVAILABILITY).text();        // 재고 텍스트
        String detailUrl    = product.selectFirst(SEL_TITLE_LINK).absUrl("href");  // 상대->절대 URL
        String imageUrl     = product.selectFirst(SEL_IMAGE).absUrl("src");        // 표지 이미지 URL

        return new Book(title, price, rating, availability, detailUrl, imageUrl);
    }

    /**
     * "£51.77" 같은 가격 문자열에서 숫자만 뽑아 double로 변환한다.
     *
     * @param raw 원본 가격 문자열 (예: "£51.77")
     * @return 숫자 가격 (예: 51.77), 숫자가 없으면 0.0
     */
    private double parsePrice(String raw) {
        // 정규식: 숫자(0-9)와 점(.)이 아닌 모든 문자를 지운다. "£51.77" -> "51.77"
        String numberOnly = raw.replaceAll("[^0-9.]", "");
        if (numberOnly.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(numberOnly); // "51.77" -> 51.77
    }

    /**
     * 별점 단어를 숫자로 변환한다. (예: class="star-rating Three" -> 3)
     *  - 기존 if-else 5번 사슬을 → RATING_WORDS 배열 + 반복문으로 축약.
     *  - i번째 단어가 들어있으면 점수는 (i+1).
     *
     * @param product 책 덩어리 HTML 조각
     * @return 1~5 사이의 별점, 알 수 없으면 0
     */
    private int parseRating(Element product) {
        Element starTag = product.selectFirst(SEL_STAR);
        if (starTag == null) {
            return 0;
        }
        String classValue = starTag.className(); // 예: "star-rating Three"

        for (int i = 0; i < RATING_WORDS.length; i++) {
            if (classValue.contains(RATING_WORDS[i])) {
                return i + 1; // 인덱스 0(One) -> 1점 ... 인덱스 4(Five) -> 5점
            }
        }
        return 0; // 매칭 없음 (별점 정보 없음)
    }

    // ========================================================================
    // ④ [Stateless Safety Check]
    //   - 멤버 변수는 bookRepository(불변 final 빈) 하나뿐 → 요청마다 값이 바뀌지 않음.
    //   - savedCount, page, book 등 "처리 중 바뀌는 값"은 전부 메서드 안 지역변수
    //     → 호출할 때마다 각 스레드의 Stack에 따로 생긴다(격리).
    //   ⇒ 싱글톤 빈 1개를 여러 스레드가 동시에 호출해도 동시성 충돌이 없다.
    //
    //   [ Thread별 Stack (격리) ]            [ 공용 Heap (공유) ]
    //   ┌─────────────────────────┐          ┌──────────────────────────┐
    //   │ Thread-1: crawlAndSave()│ ──┐      │ BookCrawlerService (빈 1개)│
    //   │  └ savedCount, book ... │   │      │  └ bookRepository (final) │
    //   └─────────────────────────┘   ├────> │                          │
    //   ┌─────────────────────────┐   │      │ BookRepository (빈 1개)   │
    //   │ Thread-2: crawlAndSave()│ ──┘      │  : 싱글톤, 무상태          │
    //   │  └ savedCount, book ... │          └──────────────────────────┘
    //   └─────────────────────────┘
    // ========================================================================
}
