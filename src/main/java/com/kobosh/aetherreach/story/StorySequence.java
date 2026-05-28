package com.kobosh.aetherreach.story;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser for story.json. No external dependencies.
 *
 * Expected top-level structure:
 * {
 *   "translation": { "key": "value", ... },
 *   "die_dialog":  [ { dialog objects } ],
 *   "sequence":    [ { event objects } ]
 * }
 *
 * Validation rules:
 *   - "die_dialog" must exist and be non-empty
 *   - sequence[0]    must be type "render_empty"
 *   - sequence[last] must be type "win_screen"
 */
public class StorySequence {
    public final List<StoryEvent> events;
    public final List<StoryEvent> dieDialogs;
    public final Map<String, String> translations;

    private StorySequence(List<StoryEvent> events, List<StoryEvent> dieDialogs,
                          Map<String, String> translations) {
        this.events       = events;
        this.dieDialogs   = dieDialogs;
        this.translations = translations;
    }

    /** Returns the translation for key, or fallback if not present. */
    public String t(String key, String fallback) {
        String v = translations.get(key);
        return (v != null) ? v : fallback;
    }

    public static StorySequence load(String resourcePath) {
        try {
            InputStream is = StorySequence.class.getResourceAsStream(resourcePath);
            if (is == null) throw new RuntimeException("Story resource not found: " + resourcePath);
            String json;
            try {
                json = readAll(is);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read story file: " + e.getMessage(), e);
            }

            // Parse the top-level object manually
            Map<String, String> translations = new HashMap<>();
            List<StoryEvent> dieDialogs = new ArrayList<>();
            List<StoryEvent> sequence   = new ArrayList<>();

            int i = skipWs(json, 0);
            if (i >= json.length() || json.charAt(i) != '{')
                throw new RuntimeException("Expected top-level JSON object");
            i++; // skip '{'

            while (i < json.length()) {
                i = skipWs(json, i);
                if (i >= json.length() || json.charAt(i) == '}') break;
                if (json.charAt(i) != '"') { i++; continue; }

                int[] kEnd = {0};
                String key = parseString(json, i, kEnd);
                i = kEnd[0];
                i = skipWs(json, i);
                if (i < json.length() && json.charAt(i) == ':') i++;
                i = skipWs(json, i);

                if ("translation".equals(key)) {
                    int[] vEnd = {0};
                    translations = parseStringMap(json, i, vEnd);
                    i = vEnd[0];
                } else if ("die_dialog".equals(key)) {
                    int[] vEnd = {0};
                    dieDialogs = parseEventArray(json, i, vEnd);
                    i = vEnd[0];
                } else if ("sequence".equals(key)) {
                    int[] vEnd = {0};
                    sequence = parseEventArray(json, i, vEnd);
                    i = vEnd[0];
                } else {
                    i = skipValue(json, i);
                }

                i = skipWs(json, i);
                if (i < json.length() && json.charAt(i) == ',') i++;
            }

            validate(dieDialogs, sequence);
            return new StorySequence(sequence, dieDialogs, translations);

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return errorSequence(msg);
        }
    }

    private static void validate(List<StoryEvent> dieDialogs, List<StoryEvent> sequence) {
        if (dieDialogs == null || dieDialogs.isEmpty())
            throw new RuntimeException("Top-level 'die_dialog' must exist and be non-empty");
        if (sequence == null || sequence.isEmpty())
            throw new RuntimeException("'sequence' must not be empty");
        if (!"render_empty".equals(sequence.get(0).type))
            throw new RuntimeException("First sequence event must be 'render_empty', got: '"
                    + sequence.get(0).type + "'");
        if (!"win_screen".equals(sequence.get(sequence.size() - 1).type))
            throw new RuntimeException("Last sequence event must be 'win_screen'");
    }

    private static StorySequence errorSequence(String msg) {
        StoryEvent err = new StoryEvent();
        err.type    = "dialog";
        err.title   = "ERROR";
        err.text    = msg;
        err.bgColor = "#8b0000";
        List<StoryEvent> events = new ArrayList<>();
        events.add(err);
        return new StorySequence(events, new ArrayList<>(), new HashMap<>());
    }

    // -----------------------------------------------------------------------
    // Parser helpers
    // -----------------------------------------------------------------------

    private static String readAll(InputStream is) throws IOException {
        InputStreamReader r = new InputStreamReader(is, "UTF-8");
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
        return sb.toString();
    }

    private static List<StoryEvent> parseEventArray(String s, int from, int[] end) {
        List<StoryEvent> result = new ArrayList<>();
        int i = from + 1; // skip '['
        while (i < s.length()) {
            i = skipWs(s, i);
            if (i >= s.length()) break;
            char c = s.charAt(i);
            if (c == ']') { i++; break; }
            if (c == '{') {
                int[] objEnd = {0};
                result.add(parseObject(s, i, objEnd));
                i = objEnd[0];
            } else {
                i++;
            }
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ',') i++;
        }
        end[0] = i;
        return result;
    }

    private static Map<String, String> parseStringMap(String s, int from, int[] end) {
        Map<String, String> map = new HashMap<>();
        int i = from + 1; // skip '{'
        while (i < s.length()) {
            i = skipWs(s, i);
            if (i >= s.length() || s.charAt(i) == '}') { i++; break; }
            if (s.charAt(i) != '"') { i++; continue; }

            int[] kEnd = {0};
            String key = parseString(s, i, kEnd);
            i = kEnd[0];
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ':') i++;
            i = skipWs(s, i);

            if (i < s.length() && s.charAt(i) == '"') {
                int[] vEnd = {0};
                map.put(key, parseString(s, i, vEnd));
                i = vEnd[0];
            } else {
                i = skipValue(s, i);
            }

            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ',') i++;
        }
        end[0] = i;
        return map;
    }

    private static StoryEvent parseObject(String s, int from, int[] end) {
        StoryEvent ev = new StoryEvent();
        int i = from + 1; // skip '{'
        while (i < s.length()) {
            i = skipWs(s, i);
            if (i >= s.length()) break;
            char c = s.charAt(i);
            if (c == '}') { i++; break; }
            if (c != '"') { i++; continue; }

            int[] kEnd = {0};
            String key = parseString(s, i, kEnd);
            i = kEnd[0];
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ':') i++;
            i = skipWs(s, i);
            if (i >= s.length()) break;

            char v = s.charAt(i);
            if (v == '"') {
                int[] vEnd = {0};
                String val = parseString(s, i, vEnd);
                i = vEnd[0];
                setString(ev, key, val);
            } else if (v == '[') {
                int peek = skipWs(s, i + 1);
                if (peek < s.length() && s.charAt(peek) == '{') {
                    int[] vEnd = {0};
                    List<StoryEvent> objs = parseEventArray(s, i, vEnd);
                    i = vEnd[0];
                    if ("dialogs".equals(key)) ev.dialogs = objs;
                } else {
                    int[] vEnd = {0};
                    List<String> arr = parseStringArray(s, i, vEnd);
                    i = vEnd[0];
                    if ("jumps".equals(key)) ev.jumps = arr;
                }
            } else if (Character.isDigit(v) || v == '-') {
                int j = i;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '-')) j++;
                if ("length".equals(key)) ev.length = Integer.parseInt(s.substring(i, j));
                i = j;
            } else {
                i = skipValue(s, i);
            }
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ',') i++;
        }
        end[0] = i;
        return ev;
    }

    private static List<String> parseStringArray(String s, int from, int[] end) {
        List<String> list = new ArrayList<>();
        int i = from + 1; // skip '['
        while (i < s.length()) {
            i = skipWs(s, i);
            if (i >= s.length() || s.charAt(i) == ']') { i++; break; }
            if (s.charAt(i) == '"') {
                int[] sEnd = {0};
                list.add(parseString(s, i, sEnd));
                i = sEnd[0];
            } else {
                i++;
            }
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ',') i++;
        }
        end[0] = i;
        return list;
    }

    private static String parseString(String s, int from, int[] end) {
        int i = from + 1; // skip opening '"'
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char esc = s.charAt(i + 1);
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(esc);
                }
                i += 2;
            } else if (c == '"') {
                i++; break;
            } else {
                sb.append(c); i++;
            }
        }
        end[0] = i;
        return sb.toString();
    }

    /** Skip any JSON value (string, number, object, array, literal) without parsing it. */
    private static int skipValue(String s, int i) {
        if (i >= s.length()) return i;
        char c = s.charAt(i);
        if (c == '"') {
            int[] end = {0};
            parseString(s, i, end);
            return end[0];
        } else if (c == '{') {
            return skipBracketed(s, i, '{', '}');
        } else if (c == '[') {
            return skipBracketed(s, i, '[', ']');
        } else {
            // number, boolean, null — skip until delimiter
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (ch == ',' || ch == '}' || ch == ']' || ch <= ' ') break;
                i++;
            }
            return i;
        }
    }

    private static int skipBracketed(String s, int i, char open, char close) {
        int depth = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                int[] end = {0};
                parseString(s, i, end);
                i = end[0];
                continue;
            }
            if (c == open)  depth++;
            if (c == close) { depth--; i++; if (depth == 0) break; continue; }
            i++;
        }
        return i;
    }

    private static void setString(StoryEvent ev, String key, String val) {
        switch (key) {
            case "type":    ev.type    = val; break;
            case "title":   ev.title   = val; break;
            case "text":    ev.text    = val; break;
            case "texture": ev.texture = val; break;
            case "bgColor": ev.bgColor = val; break;
        }
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && s.charAt(i) <= ' ') i++;
        return i;
    }
}
