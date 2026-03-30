package slib.com.example.service.system;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

@Service
public class HtmlSanitizerService {

    private static final Set<String> BLOCKED_TAGS = Set.of(
            "script", "style", "object", "embed", "link", "meta", "base", "form", "input", "button");
    private static final Set<String> SAFE_IFRAME_HOSTS = Set.of(
            "www.youtube.com", "youtube.com", "www.youtube-nocookie.com", "player.vimeo.com");

    public String sanitizeRichText(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);

        for (String tag : BLOCKED_TAGS) {
            document.select(tag).remove();
        }

        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (!(node instanceof Element element)) {
                    return;
                }

                element.attributes().asList().forEach(attribute -> {
                    String key = attribute.getKey().toLowerCase(Locale.ROOT);
                    String value = attribute.getValue();

                    if (key.startsWith("on")) {
                        element.removeAttr(attribute.getKey());
                        return;
                    }

                    if (("href".equals(key) || "src".equals(key)) && isUnsafeUrl(value)) {
                        element.removeAttr(attribute.getKey());
                    }
                });

                if ("iframe".equals(element.tagName()) && !isSafeIframe(element.attr("src"))) {
                    element.remove();
                    return;
                }

                if ("iframe".equals(element.tagName())) {
                    element.attr("loading", "lazy");
                    element.attr("referrerpolicy", "strict-origin-when-cross-origin");
                }

                if ("a".equals(element.tagName())) {
                    element.attr("rel", "noopener noreferrer");
                    element.attr("target", "_blank");
                }
            }

            @Override
            public void tail(Node node, int depth) {
            }
        }, document.body());

        return document.body().html();
    }

    private boolean isUnsafeUrl(String rawUrl) {
        if (rawUrl == null) {
            return true;
        }

        String normalized = rawUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("javascript:")
                || normalized.startsWith("vbscript:")
                || normalized.startsWith("data:text/html")
                || normalized.startsWith("file:");
    }

    private boolean isSafeIframe(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || isUnsafeUrl(rawUrl)) {
            return false;
        }

        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            return host != null && SAFE_IFRAME_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (Exception ex) {
            return false;
        }
    }
}
