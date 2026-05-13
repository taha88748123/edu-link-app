package ma.naja7i.notification.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.i18n.SupportedLocale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny Mustache-style template engine. Templates live in
 * {@code classpath:/templates/{templateKey}/{locale}.json} with a {@code subject} +
 * {@code body} field. Placeholders are {@code &#123;&#123;name&#125;&#125;} and resolved against the
 * provided parameter map. Missing variables render as empty strings (logged at WARN).
 *
 * <p>Templates are cached in-memory after first load.</p>
 */
@Component
@Slf4j
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    public TemplateRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Loads {@code templates/{templateKey}/{locale}.json}, falling back to {@code fr}
     * when the requested locale is missing.
     */
    public RenderedTemplate render(String templateKey, String localeCode, Map<String, Object> params) {
        SupportedLocale locale = SupportedLocale.fromCodeOrDefault(localeCode);
        Map<String, String> raw = loadOrFallback(templateKey, locale.code());
        if (raw == null) {
            log.error("Template not found: key={} locale={}", templateKey, localeCode);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Template not found: " + templateKey + "/" + localeCode);
        }
        String subject = substitute(raw.getOrDefault("subject", ""), params);
        String body = substitute(raw.getOrDefault("body", ""), params);
        return new RenderedTemplate(subject, body);
    }

    private Map<String, String> loadOrFallback(String templateKey, String localeCode) {
        Map<String, String> primary = cache.computeIfAbsent(
                templateKey + ":" + localeCode,
                k -> load(templateKey, localeCode));
        if (primary != null) return primary;
        if (!"fr".equals(localeCode)) {
            log.warn("Template missing for locale={}, falling back to fr (key={})", localeCode, templateKey);
            return cache.computeIfAbsent(templateKey + ":fr", k -> load(templateKey, "fr"));
        }
        return null;
    }

    private Map<String, String> load(String templateKey, String localeCode) {
        Resource res = new ClassPathResource("templates/" + templateKey + "/" + localeCode + ".json");
        if (!res.exists()) return null;
        try (InputStream in = res.getInputStream()) {
            return objectMapper.readValue(in, MAP_TYPE);
        } catch (IOException e) {
            log.error("Failed to load template {}/{}: {}", templateKey, localeCode, e.getMessage());
            return null;
        }
    }

    /** Visible for testing : substitute {@code {{var}}} placeholders. */
    public String substitute(String template, Map<String, Object> params) {
        if (template == null || template.isEmpty()) return "";
        Map<String, Object> safe = params == null ? new HashMap<>() : params;
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            Object val = safe.get(key);
            if (val == null) {
                log.warn("Template placeholder {{{}}} not provided", key);
            }
            String replacement = val == null ? "" : val.toString();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
