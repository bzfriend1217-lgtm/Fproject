package com.example.psb.runner;

import com.example.psb.entity.Book;
import com.example.psb.repository.BookRepository;
import com.example.psb.service.BookCrawlerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CrawlerRunner
 * ----------------------------------------------------------------------------
 * 애플리케이션이 켜지면(부팅이 끝나면) "자동으로 한 번" 실행되는 시작 코드.
 *
 * [CommandLineRunner 란?]
 *   - Spring Boot가 제공하는 약속(인터페이스).
 *   - 이 인터페이스를 implements(이행)하고 run() 메서드를 채워 넣으면,
 *     앱이 다 켜진 직후 Spring이 그 run()을 자동으로 딱 한 번 호출해 준다.
 *   - 즉, "프로그램 시작하자마자 할 일"을 여기에 적으면 된다.
 *
 * [@Component 란?]
 *   - "이 클래스도 Spring이 관리하는 부품(빈)으로 등록해줘"라는 표식.
 *   - @Service, @Repository 와 비슷한 가족이며, 가장 일반적인(범용) 표식이다.
 *     이렇게 등록돼야 위의 CommandLineRunner 약속이 작동한다.
 * ----------------------------------------------------------------------------
 */
@Component
public class CrawlerRunner implements CommandLineRunner {

    // 실제 크롤링 일을 시킬 서비스
    private final BookCrawlerService crawlerService;
    // 결과를 확인하려고 DB를 다시 조회할 저장소
    private final BookRepository bookRepository;

    /**
     * 생성자.
     *  - 필요한 두 부품(서비스, 저장소)을 Spring이 자동으로 넣어준다. (의존성 주입)
     */
    public CrawlerRunner(BookCrawlerService crawlerService, BookRepository bookRepository) {
        this.crawlerService = crawlerService;
        this.bookRepository = bookRepository;
    }

    /**
     * 앱이 켜진 직후 자동으로 실행되는 메서드.
     *  - throws Exception : 안에서 오류가 나면 바깥으로 던질 수 있다는 표시.
     */
    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 크롤링 시작 ======");

        // 처음에는 2페이지(책 약 40권)만 긁어 본다.
        //  - 한 페이지에 20권씩 있으므로 2페이지 = 약 40권.
        //  - 전체를 다 받고 싶으면 숫자를 50으로 바꾸면 된다.
        int savedCount = crawlerService.crawlAndSave(2);

        System.out.println("저장된 책 수: " + savedCount);

        // 잘 저장됐는지 확인: DB에서 다시 꺼내 앞쪽 5권만 화면에 출력해 본다.
        //  - findAll(): 저장된 모든 책을 List로 가져옴
        List<Book> books = bookRepository.findAll();
        System.out.println("현재 DB에 저장된 총 책 수: " + books.size());
        System.out.println("------ 앞쪽 5권 미리보기 ------");

        // 앞에서 최대 5권만 보여준다. (책이 5권보다 적으면 있는 만큼만)
        int previewCount = Math.min(5, books.size());
        for (int i = 0; i < previewCount; i++) {
            // System.out.println에 Book 객체를 넣으면 우리가 만든 toString()이 자동 호출된다.
            System.out.println(books.get(i));
        }

        System.out.println("====== 크롤링 종료 ======");
        System.out.println("H2 콘솔에서 확인: http://localhost:8080/h2-console");
    }
}
