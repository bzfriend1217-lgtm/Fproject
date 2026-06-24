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
 * "Books to Scrape"(http://books.toscrape.com) 사이트를 실제로 긁어와서(크롤링)
 * 책 정보를 Book 객체로 만들고, BookRepository를 통해 H2 DB에 저장하는 핵심 로직.
 *
 * [전체 동작 흐름]
 *   1) Jsoup으로 목록 페이지 HTML을 통째로 받아온다.
 *   2) HTML에서 책 1권에 해당하는 덩어리(article.product_pod)를 모두 찾는다.
 *   3) 각 덩어리에서 제목/가격/별점/재고/링크/이미지 값을 뽑아낸다.
 *   4) 뽑은 값으로 Book 객체를 만든다.
 *   5) 이미 저장된 책이 아니면 DB에 save() 한다.
 *   6) 다음 페이지로 넘어가 1~5를 반복한다.
 *
 * [@Service 란?]
 *   - "이 클래스는 비즈니스 로직(핵심 기능)을 담당하는 부품이다"라고 Spring에게 알리는 표식.
 *   - 이렇게 표시해 두면 Spring이 이 클래스의 객체를 자동으로 만들어 관리해 준다.
 *     (이렇게 Spring이 만들어 관리하는 객체를 "빈(Bean)"이라고 부른다.)
 * ----------------------------------------------------------------------------
 */
@Service
public class BookCrawlerService {
    // 테스트
    /**
     * 책을 DB에 저장할 때 쓰는 저장소.
     *  - final : 한 번 정해지면 바뀌지 않는다는 표시.
     *  - 이 값은 아래 "생성자"를 통해 Spring이 자동으로 넣어준다. (의존성 주입)
     */
    private final BookRepository bookRepository;

    /**
     * 크롤링할 목록 페이지 주소 틀(template).
     *  - %d 자리에 페이지 번호(1, 2, 3 ...)가 들어간다.
     *  - 예: page-1.html, page-2.html ...
     *  - static final : 모든 객체가 공유하는 "절대 안 변하는 상수"라는 뜻.
     */
    private static final String PAGE_URL = "http://books.toscrape.com/catalogue/page-%d.html";

    /**
     * 생성자.
     *  - Spring이 이 서비스 객체를 만들 때, BookRepository 빈을 찾아 자동으로 넣어준다.
     *  - 이렇게 "필요한 부품을 밖에서 받아 끼우는 것"을 의존성 주입(DI)이라고 한다.
     */
    public BookCrawlerService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * 지정한 페이지 수만큼 크롤링해서 DB에 저장한다.
     *
     * @param maxPages 최대 몇 페이지까지 긁을지 (사이트 전체는 50페이지)
     * @return 실제로 새로 저장한 책 개수
     */
    public int crawlAndSave(int maxPages) {
        int savedCount = 0; // 새로 저장한 책 수를 세는 변수

        // 1페이지부터 maxPages까지 반복 (파이썬의 for page in range(1, maxPages+1) 과 같음)
        for (int page = 1; page <= maxPages; page++) {

            // %d 자리에 현재 페이지 번호를 끼워 완성된 주소를 만든다.
            String url = String.format(PAGE_URL, page);
            System.out.println("[크롤링] 페이지 접속: " + url);

            try {
                // (핵심) Jsoup으로 해당 주소의 HTML을 통째로 받아온다.
                //  - userAgent: 우리가 어떤 브라우저인 척할지 (서버가 차단하지 않도록)
                //  - timeout: 10초 안에 응답 없으면 포기
                //  - get(): 실제로 접속해서 HTML 문서(Document)를 가져옴
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (PSB-Crawler)")
                        .timeout(10_000)
                        .get();

                // HTML 안에서 책 한 권에 해당하는 덩어리들을 전부 찾는다.
                //  - select("article.product_pod"): <article class="product_pod"> 태그들을 모두 선택
                //  - Elements : 그 덩어리들의 목록 (여러 개)
                Elements products = doc.select("article.product_pod");

                // 더 이상 책이 없으면(빈 페이지면) 반복을 멈춘다.
                if (products.isEmpty()) {
                    System.out.println("[크롤링] 더 이상 책이 없어 종료합니다.");
                    break;
                }

                // 찾은 책 덩어리를 하나씩 꺼내 처리 (파이썬의 for product in products 와 같음)
                for (Element product : products) {
                    Book book = parseBook(product); // 덩어리 -> Book 객체로 변환

                    // 이미 저장된 책(같은 상세 URL)이 아니라면 새로 저장한다. (중복 방지)
                    if (!bookRepository.existsByDetailUrl(book.getDetailUrl())) {
                        bookRepository.save(book);
                        savedCount++;
                    }
                }

            } catch (IOException e) {
                // 네트워크 오류 등으로 접속이 실패하면, 프로그램을 멈추지 않고
                // 메시지만 출력한 뒤 다음 페이지로 넘어간다.
                System.out.println("[크롤링] 페이지 처리 실패: " + url + " (" + e.getMessage() + ")");
            }
        }

        System.out.println("[크롤링] 완료! 새로 저장한 책 수 = " + savedCount);
        return savedCount;
    }

    /**
     * 책 덩어리(HTML 조각) 하나에서 필요한 값들을 뽑아 Book 객체로 만든다.
     *
     * @param product <article class="product_pod"> 한 개에 해당하는 HTML 조각
     * @return 값이 채워진 Book 객체
     */
    private Book parseBook(Element product) {
        // 제목: <h3><a ... title="A Light in the Attic"> 의 title 속성 값.
        //  - selectFirst(): 조건에 맞는 첫 번째 요소 1개를 찾음
        //  - attr("title"): 그 요소의 title 속성 값을 읽음
        //  - 목록의 보이는 글자는 "..."로 잘려있어, 잘리지 않은 title 속성을 쓴다.
        String title = product.selectFirst("h3 a").attr("title");

        // 가격: <p class="price_color">£51.77</p> 의 글자.
        //  - text(): 태그 안의 글자만 가져옴 -> "£51.77"
        String priceText = product.selectFirst("p.price_color").text();
        double price = parsePrice(priceText); // "£51.77" -> 51.77 숫자로 변환

        // 별점: <p class="star-rating Three"> 의 class 에 들어있는 단어(Three 등).
        int rating = parseRating(product);

        // 재고: <p class="instock availability"> ... </p> 의 글자.
        String availability = product.selectFirst("p.instock.availability").text();

        // 상세 페이지 링크: <h3><a href="..."> 의 href.
        //  - absUrl("href"): 상대주소(a-light.../index.html)를 절대주소(http://...)로 변환
        String detailUrl = product.selectFirst("h3 a").absUrl("href");

        // 표지 이미지: <div class="image_container"> 안의 <img src="..."> 의 src.
        String imageUrl = product.selectFirst(".image_container img").absUrl("src");

        // 뽑아낸 값들로 Book 객체를 만들어 돌려준다. (편의 생성자 사용)
        return new Book(title, price, rating, availability, detailUrl, imageUrl);
    }

    /**
     * "£51.77" 같은 가격 문자열에서 숫자만 뽑아 double로 변환한다.
     *
     * @param raw 원본 가격 문자열 (예: "£51.77")
     * @return 숫자 가격 (예: 51.77)
     */
    private double parsePrice(String raw) {
        // 정규식으로 숫자(0-9)와 점(.)을 제외한 모든 문자를 빈 문자열로 지운다.
        //  - "£51.77" -> "51.77"
        String numberOnly = raw.replaceAll("[^0-9.]", "");
        if (numberOnly.isEmpty()) {
            return 0.0; // 혹시 숫자가 하나도 없으면 0으로 처리
        }
        // 문자열 "51.77" 을 진짜 숫자 51.77 로 바꾼다.
        return Double.parseDouble(numberOnly);
    }

    /**
     * 별점 단어를 숫자로 변환한다. (예: class="star-rating Three" -> 3)
     *
     * @param product 책 덩어리 HTML 조각
     * @return 1~5 사이의 별점. 알 수 없으면 0.
     */
    private int parseRating(Element product) {
        // star-rating 이라는 class를 가진 <p> 태그를 찾는다.
        Element starTag = product.selectFirst("p.star-rating");
        if (starTag == null) {
            return 0;
        }

        // 그 태그의 class 속성 전체를 읽는다. (예: "star-rating Three")
        String classValue = starTag.className();

        // class 안에 어떤 단어가 들어있는지에 따라 숫자를 정한다.
        //  - contains(): 특정 글자가 포함되어 있는지 검사 (true/false)
        if (classValue.contains("One")) {
            return 1;
        } else if (classValue.contains("Two")) {
            return 2;
        } else if (classValue.contains("Three")) {
            return 3;
        } else if (classValue.contains("Four")) {
            return 4;
        } else if (classValue.contains("Five")) {
            return 5;
        }
        return 0; // 위에 해당 없으면 0 (별점 정보 없음)
    }
}
