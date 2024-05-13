package org.example.utils;

import java.util.regex.Pattern;

public class RegularExpression {
    private RegularExpression(){}
    public static boolean matchRegex(String stringToMatch, String commitKey) {
        Pattern pattern = Pattern.compile(commitKey + "\\b");
        return pattern.matcher(stringToMatch).find();
    }
}