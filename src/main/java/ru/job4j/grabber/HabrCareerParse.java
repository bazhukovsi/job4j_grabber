package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.time.LocalDateTime;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

public class HabrCareerParse {

//    Например:
//    https://career.habr.com/vacancies/java_developer?page=1 Первая страница с вакансиями
//    https://career.habr.com/vacancies/java_developer?page=2 Вторая страница с вакансиями
//    и т.д.


    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        HabrCareerDateTimeParser habrCareerDateTimeParser = new HabrCareerDateTimeParser();
        Connection connection = Jsoup.connect(PAGE_LINK);
        Document document = connection.get();
        Elements rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            Elements dateElement = row.select(".basic-date");
            String vacancyName = titleElement.text();
            String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            String dateName = dateElement.attr("datetime");
            LocalDateTime dateTime = habrCareerDateTimeParser.parse(dateName);
            System.out.printf("%s : %s : %s%n", vacancyName, dateTime, link);
        });
    }
}

