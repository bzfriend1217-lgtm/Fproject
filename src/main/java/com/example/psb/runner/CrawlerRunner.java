package com.example.psb.runner;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import com.example.psb.service.BookCrawlerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// ============================================================================
// [무엇인가] CrawlerRunner = 앱 부팅이 끝난 직후 "자동으로 한 번" 실행되는 시작 코드.
// ----------------------------------------------------------------------------
// [UML Class Diagram & Relationship]
//
//   (Spring Boot 부팅 완료) ──run()──> CrawlerRunner
//                                           │
//                           ┌───────────────┴───────────────┐
//                    (calls)│                                │(reads back)
//                           ▼                                ▼
//                   BookCrawlerService                 BookRepository
//                   (HTML 긁어 DB 저장)                (DB에서 다시 조회)
//
// [데이터 생명주기 물줄기]  (HTTP 인입이 아닌 "부팅 트리거"로 시작)
//
//   (Trigger) Spring 부팅 끝 ──자동 호출──> run(String... args)
//        │
//        ├─① crawlerService.crawlAndSave(2) ─> int savedCount (저장 권수)
//        ├─② bookRepository.findAll()       ─> List<Book>    (DB 전체 row)
//        └─③ 앞쪽 5권 toString()  ──출력──> 콘솔(터미널)
//   (Egress) 결과는 HTTP 응답이 아니라 System.out 콘솔 로그로 "반출"된다.
//
// [CommandLineRunner 란?]
//   Spring Boot 약속(인터페이스). run()을 채우면 앱이 다 켜진 직후 1회 자동 호출.
//
// [@Component 란?]
//   @Service·@Repository 와 한 가족. 범용 "Spring 관리 빈" 등록 표식.
// ============================================================================
@Component
public class CrawlerRunner implements CommandLineRunner {

    private final BookCrawlerService crawlerService;
    private final BookRepository bookRepository;

    // ------------------------------------------------------------------------
    // [상수] 흩어진 매직 넘버를 이름 있는 상수로 모아 한 곳에서만 수정 가능하게.
    // ------------------------------------------------------------------------

    // 처음에 긁어올 페이지 수. (한 페이지 ≈ 20권 → 2페이지 ≈ 40권 / 전체는 50)
    private static final int DEFAULT_PAGES = 2;

    // 저장 후 콘솔에 미리 보여줄 최대 권수.
    private static final int PREVIEW_LIMIT = 5;

    // Spring이 두 부품(서비스, 저장소)을 자동으로 주입해주는 생성자 (DI).
    public CrawlerRunner(BookCrawlerService crawlerService, BookRepository bookRepository) {
        this.crawlerService = crawlerService;
        this.bookRepository = bookRepository;
    }

    // ------------------------------------------------------------------------
    // [⑤ 예외 발생 시 역방향(U-Turn) 물줄기]
    //
    //   crawlAndSave() 내부에서 DB 저장/조회 중 RuntimeException 발생
    //        │
    //        └── run()이 잡지 않음 → Exception 그대로 위로 던짐(throws)
    //            → Spring Boot가 받아 부팅을 실패 처리.
    //   (서비스 내부 IOException은 거기서 이미 잡으므로 여기까진 안 온다.)
    // ------------------------------------------------------------------------
    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 크롤링 시작 ======");

        // ① 크롤링 + 저장을 서비스에 위임하고, 새로 저장한 권수만 돌려받는다.
        int savedCount = crawlerService.crawlAndSave(DEFAULT_PAGES);
        System.out.println("저장된 책 수: " + savedCount);

        // ② 잘 저장됐는지 확인: DB에서 전체를 다시 꺼내 앞쪽 일부만 보여준다.
        List<Book> books = bookRepository.findAll();
        System.out.println("현재 DB에 저장된 총 책 수: " + books.size());

        printPreview(books);

        System.out.println("====== 크롤링 종료 ======");
        System.out.println("H2 콘솔에서 확인: http://localhost:8080/h2-console");
    }

    // PREVIEW_LIMIT 권만 콘솔에 출력. 책이 부족하면 있는 만큼만 출력.
    private void printPreview(List<Book> books) {
        System.out.println("------ 앞쪽 " + PREVIEW_LIMIT + "권 미리보기 ------");

        int previewCount = Math.min(PREVIEW_LIMIT, books.size());
        for (int i = 0; i < previewCount; i++) {
            System.out.println(books.get(i));
        }
    }

    // ========================================================================
    // [Stateless Check]
    //   멤버 변수는 crawlerService, bookRepository (둘 다 불변 final 빈)뿐.
    //   savedCount·books·previewCount·i 등 "처리 중 바뀌는 값"은 전부 지역변수
    //   → 호출 시마다 각 스레드의 Stack에 따로 생긴다(격리).
    //   단, CommandLineRunner 는 부팅 시 1개 스레드가 1회만 호출하므로
    //   실제 동시 호출 자체가 없어 더더욱 안전하다.
    //
    //   [ Stack 영역 (격리) ]                 [ 공용 Heap 영역 (공유) ]
    //   ┌───────────────────────────┐         ┌──────────────────────────┐
    //   │ 부팅 스레드 : run()        │ ──┐     │ CrawlerRunner (빈 1개)    │
    //   │  └ savedCount, books ...   │   ├───> │  └ crawlerService (final) │
    //   └───────────────────────────┘   │     │  └ bookRepository (final) │
    //                                   │     └──────────────────────────┘
    // ========================================================================
}
