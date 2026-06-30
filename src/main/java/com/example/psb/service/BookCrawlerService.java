package com.example.psb.service;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

// ============================================================================
// [무엇인가] BookCrawlerService = "Books to Scrape" 사이트를 긁어 Book 객체로
//            만들고 DB에 저장하는 핵심 비즈니스 로직 계층
// ----------------------------------------------------------------------------
// [① UML Class Diagram & Relationship]
//
//   CrawlerRunner ──(calls)──> BookCrawlerService ──(uses)──> BookRepository
//                                     │                             │
//                             (Jsoup로 HTML 수집)             (saves Entity)
//                                     │                             ▼
//                             [HTML Element] ──parse──> [Book] ──> books(DB)
//
// [Skeleton]
//   public class BookCrawlerService {
//       private final BookRepository bookRepository;   // 생성자 DI
//       public int crawlAndSave(int maxPages) { ... }  // 외부 진입점
//       private Document fetchPage(String url) { ... } // 네트워크 연결
//       private Book parseBook(Element product) { ... } // HTML → 객체 변환
//       private double parsePrice(String raw) { ... }  // 가격 문자열 파싱
//       private int parseRating(Element product) { ... } // 별점 숫자 변환
//       private boolean saveIfNew(Book book) { ... }   // 중복 방지 저장
//   }
//
// ----------------------------------------------------------------------------
// [③ End-to-End 데이터 물줄기]  ← "내가 외부 사이트로 거는" 방향
//
//   (Egress) ─GET─> books.toscrape.com/catalogue/page-N.html
//   (Ingress) <─HTML 200─ 응답 본문(통째로)
//        │
//   String(HTML) ──Jsoup.parse──> Document
//        │                           │  .select("article.product_pod")
//        │                           ▼
//   Elements(책 N개) ──parseBook──> Book 객체 ──save──> books 테이블 row
//
//   데이터 변환:  <article.product_pod> HTML 조각
//             ──추출──> title, price, rating ... (자바 원시값)
//             ──조립──> new Book(...)
//             ──save──> INSERT INTO books (H2 DB)
//
// ----------------------------------------------------------------------------
// [② Memory Architecture Map]
//
//   [ Thread별 Stack (격리) ]              [ 공용 Heap (공유) ]
//   ┌───────────────────────────┐          ┌──────────────────────────────┐
//   │ Thread-1: crawlAndSave()  │ ──┐      │ BookCrawlerService (빈 1개)   │
//   │  └ savedCount, url, book  │   ├────> │  └ bookRepository (final)    │
//   └───────────────────────────┘   │      │                              │
//   ┌───────────────────────────┐   │      │ BookRepository (빈 1개)       │
//   │ Thread-2: crawlAndSave()  │ ──┘      │  : 싱글톤, 무상태             │
//   │  └ savedCount, url, book  │          └──────────────────────────────┘
//   └───────────────────────────┘
//
// ============================================================================
@Service
public class BookCrawlerService {

    private final BookRepository bookRepository;

    // [상수] 흩어진 매직 문자열을 이름 있는 상수로 → HTML 구조가 바뀌어도 여기만 수정.
    private static final String PAGE_URL         = "http://books.toscrape.com/catalogue/page-%d.html";
    private static final String SEL_PRODUCT      = "article.product_pod";    // 책 1권 덩어리
    private static final String SEL_TITLE_LINK   = "h3 a";                   // 제목 + 상세링크
    private static final String SEL_PRICE        = "p.price_color";          // 가격
    private static final String SEL_STAR         = "p.star-rating";          // 별점
    private static final String SEL_AVAILABILITY = "p.instock.availability"; // 재고
    private static final String SEL_IMAGE        = ".image_container img";   // 표지 이미지

    // 별점 단어표: 인덱스 0(One)=1점 ... 인덱스 4(Five)=5점. parseRating에서 사용.
    private static final String[] RATING_WORDS = {"One", "Two", "Three", "Four", "Five"};

    // Spring이 이 서비스를 만들 때 BookRepository 빈을 찾아 자동으로 끼워준다. (DI)
    public BookCrawlerService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ------------------------------------------------------------------------
    // crawlAndSave()  ─ 외부(CrawlerRunner 등)에서 호출하는 단 하나의 공개 진입점.
    //
    // [⑤ 예외 발생 시 역방향(U-Turn) 물줄기]
    //
    //   Jsoup.get() ──타임아웃 / 404 / 500──> IOException 발생
    //        │                                      │
    //        └── 이 페이지만 포기 ──catch─────────┘
    //            메시지 출력 후 → 다음 page 로 for 문 계속
    //   (한 페이지 실패 = 전체 크롤링 중단 없음)
    // ------------------------------------------------------------------------
    public int crawlAndSave(int maxPages) {
        int savedCount = 0; // 지역변수 → 각 스레드의 Stack에 격리

        for (int page = 1; page <= maxPages; page++) {
            String url = String.format(PAGE_URL, page);
            System.out.println("[크롤링] 페이지 접속: " + url);

            try {
                Document doc = fetchPage(url);
                Elements products = doc.select(SEL_PRODUCT);

                if (products.isEmpty()) {
                    System.out.println("[크롤링] 더 이상 책이 없어 종료합니다.");
                    break;
                }

                for (Element product : products) {
                    if (saveIfNew(parseBook(product))) {
                        savedCount++;
                    }
                }

            } catch (IOException e) {
                // 네트워크 오류 → 이 페이지만 건너뛰고 for 문 계속.
                System.out.println("[크롤링] 페이지 처리 실패: " + url + " (" + e.getMessage() + ")");
            }
        }

        System.out.println("[크롤링] 완료! 새로 저장한 책 수 = " + savedCount);
        return savedCount;
    }

    // URL 하나에 접속해 HTML Document 를 받아온다.
    //   timeout 10초 초과 → IOException → crawlAndSave 의 catch 로 U-턴.
    private Document fetchPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (PSB-Crawler)")
                .timeout(10_000)
                .get();
    }

    // HTML 조각 하나(책 1권) → Book 객체.
    //   [데이터 변환] <article.product_pod> ──추출──> 자바 원시값 ──조립──> Book
    private Book parseBook(Element product) {
        Element titleLink   = product.selectFirst(SEL_TITLE_LINK);  // DOM 탐색 1회로 재사용
        String title        = titleLink.attr("title");              // 잘리지 않은 전체 제목
        String detailUrl    = titleLink.absUrl("href");             // 상대 → 절대 URL
        double price        = parsePrice(product.selectFirst(SEL_PRICE).text());
        int    rating       = parseRating(product);
        String availability = product.selectFirst(SEL_AVAILABILITY).text();
        String imageUrl     = product.selectFirst(SEL_IMAGE).absUrl("src");

        return new Book(title, price, rating, availability, detailUrl, imageUrl);
    }

    // "£51.77" → 51.77  (숫자와 점 이외의 모든 문자 제거)
    private double parsePrice(String raw) {
        String numberOnly = raw.replaceAll("[^0-9.]", "");
        return numberOnly.isEmpty() ? 0.0 : Double.parseDouble(numberOnly);
    }

    // class="star-rating Three" → 3  (RATING_WORDS 배열 순서 기반 변환)
    //   i번째 단어가 class 에 포함되면 → 점수 = (i + 1)
    private int parseRating(Element product) {
        Element starTag = product.selectFirst(SEL_STAR);
        if (starTag == null) return 0;

        String classValue = starTag.className(); // 예: "star-rating Three"
        for (int i = 0; i < RATING_WORDS.length; i++) {
            if (classValue.contains(RATING_WORDS[i])) return i + 1;
        }
        return 0;
    }

    // 같은 detailUrl 이 DB에 없을 때만 save → true. 이미 있으면 → false(스킵).
    private boolean saveIfNew(Book book) {
        if (bookRepository.existsByDetailUrl(book.getDetailUrl())) return false;
        bookRepository.save(book);
        return true;
    }

    // ========================================================================
    // [④ Stateless Safety Check]
    //   멤버 변수 = bookRepository(final 불변 빈) 1개뿐.
    //   savedCount, url, doc, book 등 "처리 중 바뀌는 값"은 전부 지역변수
    //   → 호출마다 각 스레드의 Stack에 따로 생긴다(격리).
    //   ⇒ 싱글톤 빈 1개를 200개 스레드가 동시에 호출해도 동시성 충돌이 없다.
    // ========================================================================
}
