package no.java.ems.client;

import org.codehaus.httpcache4j.*;

import java.util.*;

/**
 * See http://tools.ietf.org/html/draft-nottingham-http-link-header-03#section-5
 */
public class LinkHeader {
    public final String value;
    public final Map<String, String> params;

    public LinkHeader(Header header) {
        String value = header.getValue();

        this.value = parseValue(value);
        params = parseParams(value.substring(this.value.length() + 2));
    }

    private String parseValue(String value) {
        int i = value.indexOf('<');
        int j = value.indexOf('>');
        if (i != 0) {
            throw new IllegalArgumentException("Expected '<' to be the first character.");
        }
        if (j <= 1) {
            throw new IllegalArgumentException("Expected to find a '>'.");
        }
        return value.substring(1, j);
    }

    /**
     * This is copied from Header.parseDirectives() which has a bug where it doesn't skip the value part.
     */
    public static Map<String, String> parseParams(String linkParams) {
        if(linkParams.charAt(0) != ';') {
            throw new IllegalArgumentException("Expected to find a ';'");
        }
        linkParams = linkParams.substring(1);

        Map<String, String> map = new LinkedHashMap<String, String>();
        java.util.List<String> directives = Arrays.asList(linkParams.split(","));
        for (String directive : directives) {
            directive = directive.trim();
            if (directive.length() > 0) {
                String[] directiveParts = directive.split("=", 2);
                map.put(directiveParts[0], directiveParts.length > 1 ? directiveParts[1] : null);
            }
        }
        return map;

    }

    public String toString() {
        return "Link: value=" + value + ", params=" + params;
    }
}
