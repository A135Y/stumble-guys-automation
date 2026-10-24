package com.ayusuf.sg.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pulls the sign-in link and one-time code out of Scopely ID emails. */
public final class ScopelyEmailParser {

    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_URL = Pattern.compile("https://id\\.scopely\\.com/\\S+");
    private static final Pattern SIGN_IN_PATH = Pattern.compile("verify|token|magic|login|activate", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE = Pattern.compile("(?<![\\d#])(\\d{6})(?!\\d)");
    private static final Pattern CODE_HINT = Pattern.compile("code", Pattern.CASE_INSENSITIVE);

    private ScopelyEmailParser() {
    }

    public static Optional<String> signInLink(String body) {
        if (body == null) return Optional.empty();

        List<String> links = new ArrayList<>();
        Matcher href = HREF.matcher(body);
        while (href.find()) links.add(unescape(href.group(1)));
        Matcher bare = BARE_URL.matcher(body);
        while (bare.find()) links.add(unescape(bare.group()));

        List<String> scopely = links.stream().filter(l -> l.startsWith("https://id.scopely.com/")).toList();
        return scopely.stream().filter(l -> SIGN_IN_PATH.matcher(l).find()).findFirst()
                .or(() -> scopely.stream().findFirst());
    }

    public static Optional<String> oneTimeCode(String body) {
        if (body == null) return Optional.empty();
        String text = toText(body);

        // prefer the 6-digit number closest after the word "code"
        Matcher hint = CODE_HINT.matcher(text);
        while (hint.find()) {
            Matcher m = CODE.matcher(text);
            if (m.find(hint.end())) return Optional.of(m.group(1));
        }
        Matcher any = CODE.matcher(text);
        return any.find() ? Optional.of(any.group(1)) : Optional.empty();
    }

    static String toText(String html) {
        return unescape(html
                .replaceAll("(?is)<(style|script|head)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String unescape(String s) {
        return s.replace("&amp;", "&").replace("&nbsp;", " ").replace("&#61;", "=");
    }
}
