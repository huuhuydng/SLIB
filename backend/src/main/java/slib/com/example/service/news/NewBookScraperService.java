package slib.com.example.service.news;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import slib.com.example.dto.news.NewBookRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NewBookScraperService {

    private static final String OPAC_HOST = "library.fpt.edu.vn";
    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}");

    public NewBookRequest scrapeFromOpac(String sourceUrl) {
        String normalizedUrl = normalizeAndValidateUrl(sourceUrl);
        try {
            Document document = Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .referrer("https://library.fpt.edu.vn/")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            validateDetailDocument(document);

            Map<String, String> details = extractDetailMap(document);
            String rawTitle = firstNonBlank(
                    textOf(document.selectFirst(".book-title")),
                    attrOf(document.selectFirst("meta[property=og:title]"), "content")
            );
            ParsedTitle parsedTitle = splitTitleAndAuthor(rawTitle);
            String coverUrl = resolveAbsoluteUrl(
                    normalizedUrl,
                    firstNonBlank(
                            attrOf(document.selectFirst(".book-image img"), "src"),
                            attrOf(document.selectFirst("meta[property=og:image]"), "content")
                    )
            );

            List<String> keywords = extractKeywords(document);
            Integer publishYear = parseYear(details.get("Năm phát hành"));
            String description = "Khám phá đầu sách mới từ thư viện SLIB. Nhấn để xem chi tiết và kiểm tra khả năng mượn trên OPAC.";

            return NewBookRequest.builder()
                    .title(parsedTitle.title())
                    .author(firstNonBlank(parsedTitle.author(), details.get("Tên tác giả"), details.get("Tác giả")))
                    .isbn(cleanText(details.get("ISBN")))
                    .coverUrl(coverUrl)
                    .description(description)
                    .category(toCategoryTags(keywords))
                    .publishYear(publishYear)
                    .arrivalDate(LocalDate.now())
                    .isActive(true)
                    .sourceUrl(normalizedUrl)
                    .publisher(cleanText(details.get("Nhà xuất bản")))
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc dữ liệu từ link OPAC: " + e.getMessage(), e);
        }
    }

    private Map<String, String> extractDetailMap(Document document) {
        Map<String, String> details = new LinkedHashMap<>();
        for (Element row : document.select(".book-content-detail .book-item-detail")) {
            String label = cleanText(textOf(row.selectFirst(".col-md-5")));
            String value = cleanText(textOf(row.selectFirst(".col-md-7")));
            if (StringUtils.hasText(label) && StringUtils.hasText(value)) {
                details.put(label, value);
            }
        }
        return details;
    }

    private List<String> extractKeywords(Document document) {
        List<String> keywords = new ArrayList<>();
        for (Element row : document.select(".book-content-detail .book-item-detail")) {
            String label = cleanText(textOf(row.selectFirst(".col-md-5")));
            if (!"Từ khóa".equalsIgnoreCase(label)) {
                continue;
            }
            for (Element keywordLink : row.select(".col-md-7 a")) {
                String keyword = cleanKeyword(keywordLink.text());
                if (StringUtils.hasText(keyword) && !keywords.contains(keyword)) {
                    keywords.add(keyword);
                }
            }
        }
        return keywords;
    }

    private String normalizeAndValidateUrl(String sourceUrl) {
        try {
            URI uri = new URI(sourceUrl.trim());
            String host = uri.getHost();
            if (host == null || !OPAC_HOST.equalsIgnoreCase(host)) {
                throw new IllegalArgumentException("Chỉ hỗ trợ link từ library.fpt.edu.vn");
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL không hợp lệ");
        }
    }

    private void validateDetailDocument(Document document) {
        String title = firstNonBlank(
                textOf(document.selectFirst(".book-title")),
                attrOf(document.selectFirst("meta[property=og:title]"), "content")
        );
        int detailRows = document.select(".book-content-detail .book-item-detail").size();

        boolean hasBookSignals = StringUtils.hasText(title)
                && detailRows > 0
                && !title.toLowerCase(Locale.ROOT).contains("opac");

        if (!hasBookSignals) {
            throw new IllegalArgumentException("Link không phải trang chi tiết sách hợp lệ của OPAC");
        }
    }

    private String resolveAbsoluteUrl(String baseUrl, String target) {
        if (!StringUtils.hasText(target)) {
            return null;
        }
        try {
            return new URI(baseUrl).resolve(target).toString();
        } catch (URISyntaxException e) {
            return target;
        }
    }

    private Integer parseYear(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        Matcher matcher = YEAR_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group());
    }

    private ParsedTitle splitTitleAndAuthor(String rawTitle) {
        String cleaned = cleanText(rawTitle);
        if (!StringUtils.hasText(cleaned)) {
            return new ParsedTitle(null, null);
        }
        String[] parts = cleaned.split("\\s+/\\s+", 2);
        String title = cleanText(parts[0]);
        String author = parts.length > 1 ? cleanText(parts[1]) : null;
        return new ParsedTitle(title, author);
    }

    private String cleanKeyword(String rawKeyword) {
        String cleaned = cleanText(rawKeyword);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        return cleaned.replaceFirst("^\\d+\\.\\s*", "").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return cleanText(value);
            }
        }
        return null;
    }

    private String toCategoryTags(List<String> keywords) {
        if (keywords.isEmpty()) {
            return null;
        }
        int endIndex = Math.min(3, keywords.size());
        return String.join(", ", keywords.subList(0, endIndex));
    }

    private String textOf(Element element) {
        return element == null ? null : element.text();
    }

    private String attrOf(Element element, String attribute) {
        return element == null ? null : element.attr(attribute);
    }

    private String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() || "Đang cập nhật".equalsIgnoreCase(cleaned) ? null : cleaned;
    }

    private record ParsedTitle(String title, String author) {
    }
}
