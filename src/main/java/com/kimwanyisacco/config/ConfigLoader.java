package com.kimwanyisacco.config;



import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigLoader {

    private static final Properties PROPERTIES = new Properties();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)\\}");
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing() // won't blow up in prod where there's no .env file
            .load();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("application.properties not found on classpath");
            }
            Properties raw = new Properties();
            raw.load(input);

            for (String key : raw.stringPropertyNames()) {
                PROPERTIES.setProperty(key, resolve(raw.getProperty(key)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    private static String resolve(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String resolved = lookup(varName);
            if (resolved == null) {
                throw new RuntimeException("Missing required env var: " + varName);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // Priority: real system env (GitHub Actions/server) > .env file (local dev)
    private static String lookup(String key) {
        String fromSystem = System.getenv(key);
        if (fromSystem != null) return fromSystem;
        return DOTENV.get(key);
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }
}
