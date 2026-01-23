// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.function.BiFunction;

public class StringUtils {
    public static final String s_emptyString = "";
    public static final char UTF8_BOM = '\ufeff';

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrWhitespace(String s) {
        return isNullOrEmpty(s) || isWhitespace(s);
    }

    private static boolean isWhitespace(String s) {
        int length = s.length();
        if (length <= 0) {
            return false;
        } else {
            int start = 0;
            int middle = length / 2;

            for (int end = length - 1; start <= middle; end--) {
                if (!Character.isWhitespace(s.charAt(start)) || !Character.isWhitespace(s.charAt(end))) {
                    return false;
                }

                start++;
            }

            return true;
        }
    }

    public static String discardNullOrWhitespace(String str) {
        return isNullOrWhitespace(str) ? null : str;
    }

    public static String trimPrefix(String str, String prefix) {
        return str.startsWith(prefix) ? str.substring(prefix.length()) : str;
    }

    public static String trimSuffix(String str, String suffix) {
        return str.endsWith(suffix) ? str.substring(0, str.length() - suffix.length()) : str;
    }

    public static boolean equals(String a, String b) {
        return a == b ? true : a != null && a.equals(b);
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        int suffixLength = suffix.length();
        return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
    }

    public static boolean containsIgnoreCase(String haystack, String needle) {
        for (int i = haystack.length() - needle.length(); i >= 0; i--) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }

        return false;
    }

    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == b) {
            return true;
        } else {
            return isNullOrEmpty(a) && isNullOrEmpty(b) ? true : a != null && a.equalsIgnoreCase(b);
        }
    }

    public static boolean tryParseBoolean(String varStr) {
        if (isNullOrWhitespace(varStr)) {
            return false;
        } else {
            String processedVar = varStr.trim();
            return processedVar.equalsIgnoreCase("true") || processedVar.equals("1") || processedVar.equals("1.0");
        }
    }

    public static boolean isBoolean(String varStr) {
        String processedVar = varStr.trim();
        return processedVar.equalsIgnoreCase("true") || processedVar.equals("1") || processedVar.equals("1.0")
            ? true
            : processedVar.equalsIgnoreCase("false") || processedVar.equals("0") || processedVar.equals("0.0");
    }

    public static boolean isFloat(String in_varStr) {
        if (isNullOrWhitespace(in_varStr)) {
            return false;
        } else {
            try {
                Float.parseFloat(in_varStr);
                return true;
            } catch (NumberFormatException var2) {
                return false;
            }
        }
    }

    public static float tryParseFloat(String in_valueStr) {
        if (isNullOrWhitespace(in_valueStr)) {
            return 0.0F;
        } else {
            try {
                return Float.parseFloat(in_valueStr);
            } catch (NumberFormatException var2) {
                return 0.0F;
            }
        }
    }

    public static <E extends Enum<E>> E tryParseEnum(Class<E> in_enumClass, String in_enumStr, E in_defaultVal) {
        try {
            return Enum.valueOf(in_enumClass, in_enumStr);
        } catch (Exception var4) {
            return in_defaultVal;
        }
    }

    public static boolean contains(String[] array, String val, BiFunction<String, String, Boolean> equalizer) {
        return indexOf(array, val, equalizer) > -1;
    }

    public static int indexOf(String[] array, String val, BiFunction<String, String, Boolean> equalizer) {
        int indexOf = -1;

        for (int i = 0; i < array.length; i++) {
            if (equalizer.apply(array[i], val)) {
                indexOf = i;
                break;
            }
        }

        return indexOf;
    }

    public static String indent(String text) {
        String firstLineTab = "";
        String secondLineTab = "\t";
        return indent(text, "", "\t");
    }

    private static String indent(String text, String firstLineTab, String secondLineTab) {
        String endln = System.lineSeparator();
        return indent(text, endln, firstLineTab, secondLineTab);
    }

    private static String indent(String text, String endln, String firstLineTab, String secondLineTab) {
        if (isNullOrEmpty(text)) {
            return text;
        } else {
            int length = text.length();
            StringBuilder out = new StringBuilder(length);
            StringBuilder line = new StringBuilder(length);
            int lineIdx = 0;

            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '\n':
                        out.append((CharSequence)line);
                        out.append(endln);
                        line.setLength(0);
                        lineIdx++;
                    case '\r':
                        break;
                    default:
                        if (line.isEmpty()) {
                            if (lineIdx == 0) {
                                line.append(firstLineTab);
                            } else {
                                line.append(secondLineTab);
                            }
                        }

                        line.append(c);
                }
            }

            out.append((CharSequence)line);
            line.setLength(0);
            return out.toString();
        }
    }

    public static String leftJustify(String text, int length) {
        if (text == null) {
            return leftJustify("", length);
        } else {
            int inTextLength = text.length();
            if (inTextLength >= length) {
                return text;
            } else {
                int difference = length - inTextLength;
                char[] differenceChars = new char[difference];

                for (int i = 0; i < difference; i++) {
                    differenceChars[i] = ' ';
                }

                String suffix = new String(differenceChars);
                return text + suffix;
            }
        }
    }

    public static String moduleDotType(String module, String type) {
        if (type == null) {
            return null;
        } else {
            return type.contains(".") ? type : module + "." + type;
        }
    }

    public static String stripModule(String type) {
        if (type == null) {
            return null;
        } else {
            int index = type.indexOf(46);
            return type.substring(index + 1);
        }
    }

    public static String stripBOM(String line) {
        return line != null && !line.isEmpty() && line.charAt(0) == '\ufeff' ? line.substring(1) : line;
    }

    public static boolean containsDoubleDot(String str) {
        return isNullOrEmpty(str) ? false : str.contains("..") || str.contains("\u0000.\u0000.");
    }

    public static boolean containsWhitespace(String s) {
        return s.matches("(.*?)\\s(.*?)");
    }

    public static String removeWhitespace(String s) {
        return s.replaceAll("\\s", "");
    }

    public static String[] trimArray(String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
                arr[i] = arr[i].trim();
            }
        }

        return arr;
    }

    public static String trimSurroundingQuotes(String s) {
        return trimSurroundingQuotes(s, true);
    }

    public static String trimSurroundingQuotes(String s, boolean trim) {
        if (s != null && !s.isEmpty()) {
            if (trim) {
                s = s.trim();
            }

            return s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
        } else {
            return s;
        }
    }

    public static boolean isValidVariableName(String in_varName) {
        boolean valid = false;
        int i = 0;

        for (int length = in_varName.length(); i < length; i++) {
            char ch = in_varName.charAt(i);
            if (!isValidVariableChar(ch)) {
                return false;
            }

            valid = true;
        }

        return valid;
    }

    public static boolean isValidVariableChar(char ch) {
        return ch == '_' || isAlphaNumeric(ch);
    }

    public static boolean isAlpha(char ch) {
        return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z';
    }

    public static boolean isNumeric(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isAlphaNumeric(char ch) {
        return isAlpha(ch) || isNumeric(ch);
    }

    public static int compareIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return 0;
        } else if (a == null) {
            return -1;
        } else {
            return b == null ? 1 : a.compareToIgnoreCase(b);
        }
    }
}
