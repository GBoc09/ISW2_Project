package org.example.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpression {
    public static boolean matchRegex(String stringToMatch, String commitKey) {
        Pattern pattern = Pattern.compile(commitKey + "+[^0-9]");
        Matcher matcher= pattern.matcher(stringToMatch);
        return matcher.find();
    }
}
