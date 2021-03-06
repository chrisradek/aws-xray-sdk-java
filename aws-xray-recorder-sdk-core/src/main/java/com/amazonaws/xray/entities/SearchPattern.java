package com.amazonaws.xray.entities;


public class SearchPattern {

    /**
     * Performs a case-insensitive wildcard match against two strings. This method works with pseduo-regex chars; specifically ? and * are supported.
     * <ul>
     *   <li>An asterisk (*) represents any combination of characters</li>
     *   <li>A question mark (?) represents any single character</li>
     * </ul>
     *
     * @param pattern
     *            the regex-like pattern to be compared against
     * @param text
     *            the string to compare against the pattern
     * @return whether the text matches the pattern
     */
    public static boolean wildcardMatch(String pattern, String text){
        return wildcardMatch(pattern, text, true);
    }

    public static boolean wildcardMatch(String pattern, String text, boolean caseInsensitive) {
        if (pattern == null || text == null) {
            return false;
        }

        int patternLength = pattern.length();
        int textLength = text.length();
        if(patternLength==0) {
            return textLength==0;
        }

        // Check the special case of a single * pattern, as it's common
        if(isWildcardGlob(pattern)) {
            return true;
        }

        if(caseInsensitive) {
            pattern = pattern.toLowerCase();
            text = text.toLowerCase();
        }

        // Infix globs are relatively rare, and the below search is expensive especially when
        // Balsa is used a lot. Check for infix globs and, in their absence, do the simple thing
        int indexOfGlob = pattern.indexOf('*');
        if(indexOfGlob == -1 || indexOfGlob == patternLength-1) {
            return simpleWildcardMatch(pattern, text);
        }

        /*
         * The res[i] is used to record if there is a match
         * between the first i chars in text and the first j chars in pattern.
         * So will return res[textLength+1] in the end
         * Loop from the beginning of the pattern
         * case not '*': if text[i]==pattern[j] or pattern[j] is '?', and res[i] is true,
         *   set res[i+1] to true, otherwise false
         * case '*': since '*' can match any globing, as long as there is a true in res before i
         *   all the res[i+1], res[i+2],...,res[textLength] could be true
        */
        boolean[] res = new boolean[textLength+1];
        res[0] = true;
        for(int j=0;j<patternLength;j++) {
            char p = pattern.charAt(j);
            if(p!='*') {
                for(int i=textLength-1;i>=0;i--) {
                    char t = text.charAt(i);
                    res[i+1] = res[i] && ( p == '?' || (p == t));
                }
            } else {
                int i = 0;
                while( i <= textLength && !res[i] ) {
                    i++;
                }
                for(;i<=textLength;i++) {
                    res[i] = true;
                }
            }
            res[0] = res[0] && p == '*';
        }
        return res[textLength];
    }

    private static boolean simpleWildcardMatch(String pattern, String text) {
        int j = 0;
        int patternLength = pattern.length();
        int textLength = text.length();
        for(int i = 0; i < patternLength; i++) {
            char p = pattern.charAt(i);
            if(p == '*') {
                // Presumption for this method is that globs only occur at end
                return true;
            } else if (p == '?') {
                if(j == textLength) {
                    return false; // No character to match
                }
                j++;
            } else {
                if(j >= textLength) {
                    return false;
                }
                char t = text.charAt(j);
                if(p != t) {
                    return false;
                }
                j++;
            }
        }
        // Ate up all the pattern and didn't end at a glob, so a match will have consumed all
        // the text
        return j == textLength;

    }

    /**
     * indicates whether the passed pattern is a single wildcard glob
     * (i.e., "*")
     */
    private static boolean isWildcardGlob(String pattern) {
        return pattern.length() == 1 && pattern.charAt(0) == '*';
    }
}
