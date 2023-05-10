package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ru.job4j.grabber.model.Post;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private String retrieveDescription(String link) throws IOException {
        Document document = Jsoup.connect(link).get();
        Element description = document.selectFirst(".style-ugc");
        return description.text();
    }

    @Override
    public List<Post> list(String link) throws IOException {
        HabrCareerDateTimeParser habrCareerDateTimeParser = new HabrCareerDateTimeParser();
        List<Post> list = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            Connection connection = Jsoup.connect(String.format("%s%d", link, i));
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Post post = new Post();
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Elements dateElement = row.select(".basic-date");
                String dateName = dateElement.attr("datetime");
                LocalDateTime dateTime = habrCareerDateTimeParser.parse(dateName);
                post.setTitle(titleElement.text());
                post.setLink(String.format("%s%s", SOURCE_LINK, linkElement.attr("href")));
                post.setCreated(dateTimeParser.parse(String.valueOf(dateTime)));
                try {
                    post.setDescription(retrieveDescription(post.getLink()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                list.add(post);
            });
        }
        return list;
    }

    public static void main(String[] args) throws IOException {
        HabrCareerParse parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        System.out.println(parse.list(PAGE_LINK));
    }
}

