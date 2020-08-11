/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.configuration;

import nxt.Nxt;
import nxt.util.Logger;
import nxt.util.ResourceLookup;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a custom property files (as in {@link java.util.Properties}) parser suited for the format of
 * the nxt-default.properties.
 * <p>
 * It extracts groups from separator comments, descriptions of each property from the preceding comment lines
 * and metadata from the last line of the comments using some custom tags and a simple syntax.
 * <p>
 * A couple of methods are adapted from the JDK code of the {@link java.util.Properties} file.
 */
class PropertiesParser {
    private static final Pattern GROUP_COMMENT = Pattern.compile("^#### (.*) ####$");

    static Map<String, ConfigPropertyBuilder> parseDefault() {
        Map<String, ConfigPropertyBuilder> propertyBuilders = new LinkedHashMap<>();
        try ( BufferedReader in = new BufferedReader(getDefaultPropertiesReader()) ) {
            List<String> comments = new ArrayList<>();
            String currentGroup = "";
            String line = readLine(in);
            while (line != null) {
                if (line.isEmpty()) {
                    comments.clear();
                } else if (isComment(line)) {
                    // group comment? (always #### <group name> ####)
                    Matcher groupComment = GROUP_COMMENT.matcher(line);
                    if (groupComment.matches()) {
                        currentGroup = groupComment.group(1);
                        comments.clear();
                    } else {
                        comments.add(line.substring(line.length() > 1 && line.charAt(1) == ' ' ? 2 : 1));
                    }
                } else {
                    String[] keyValuePair = parseLine(line);
                    ConfigPropertyBuilder propertyBuilder = ConfigPropertyBuilder.parseDefault(
                            keyValuePair[0], keyValuePair[1], new ArrayList<>(comments), currentGroup);
                    propertyBuilders.put(propertyBuilder.getName(), propertyBuilder);
                }

                line = readLine(in);
            }
            return propertyBuilders;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // adapted from Properties#load0()
    private static String[] parseLine(String line) {
        char c;
        int keyLen = 0;
        int valueStart = line.length();
        boolean hasSep = false;
        boolean precedingBackslash = false;
        while (keyLen < line.length()) {
            c = line.charAt(keyLen);
            if ((c == '=' ||  c == ':') && !precedingBackslash) {
                valueStart = keyLen + 1;
                hasSep = true;
                break;
            } else if (isWhiteSpace(c) && !precedingBackslash) {
                valueStart = keyLen + 1;
                break;
            }
            if (c == '\\') {
                precedingBackslash = !precedingBackslash;
            } else {
                precedingBackslash = false;
            }
            keyLen++;
        }
        while (valueStart < line.length()) {
            c = line.charAt(valueStart);
            if (!isWhiteSpace(c)) {
                if (!hasSep && (c == '=' ||  c == ':')) {
                    hasSep = true;
                } else {
                    break;
                }
            }
            valueStart++;
        }
        String key = convert(line, 0, keyLen);
        String value = convert(line, valueStart, line.length() - valueStart);
        return new String[]{key, value};
    }

    // adapted from Properties#loadConvert()
    private static String convert(String s, int off, int len) {
        StringBuilder buff = new StringBuilder();

        char aChar;
        int end = off + len;

        while (off < end) {
            aChar = s.charAt(off++);
            if (aChar == '\\') {
                aChar = s.charAt(off++);
                if(aChar == 'u') {
                    int value=0;
                    for (int i=0; i<4; i++) {
                        aChar = s.charAt(off++);
                        switch (aChar) {
                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7': case '8': case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a': case 'b': case 'c':
                            case 'd': case 'e': case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A': case 'B': case 'C':
                            case 'D': case 'E': case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed \\uxxxx encoding.");
                        }
                    }
                    buff.append((char)value);
                } else {
                    if (aChar == 't') aChar = '\t';
                    else if (aChar == 'r') aChar = '\r';
                    else if (aChar == 'n') aChar = '\n';
                    else if (aChar == 'f') aChar = '\f';
                    buff.append(aChar);
                }
            } else {
                buff.append(aChar);
            }
        }

        return buff.toString();
    }

    private static Reader getDefaultPropertiesReader() throws FileNotFoundException {
        String configFile = System.getProperty(Nxt.NXT_DEFAULT_PROPERTIES);
        if (configFile != null) {
            Logger.logDebugMessage("Parsing default properties from %s", configFile);
            return new FileReader(configFile);
        }
        return new InputStreamReader(ResourceLookup.getSystemResourceAsStream(Nxt.NXT_DEFAULT_PROPERTIES));
    }

    private static String readLine(BufferedReader bufferedReader) throws IOException {
        String line = bufferedReader.readLine();
        if (line == null) {
            return null;
        }

        line = removeLeadingWhitespaces(line);

        if (line.isEmpty() || isComment(line)) {
            return line;
        }

        if (isTerminatorEscaped(line)) {
            String rest = readLine(bufferedReader);
            if (rest != null) {
                return line.substring(0, line.length() - 1) + rest;
            }
        }

        return line;
    }

    private static String removeLeadingWhitespaces(String line) {
        // remove leading whitespace
        int firstNonWhiteSpace = 0;
        while (firstNonWhiteSpace < line.length() && isWhiteSpace(line.charAt(firstNonWhiteSpace))) {
            firstNonWhiteSpace++;
        }
        if (!line.isEmpty()) {
            line = line.substring(firstNonWhiteSpace);
        }
        return line;
    }

    private static boolean isComment(String line) {
        return line.charAt(0) == '#' || line.charAt(0) == '!';
    }

    private static boolean isTerminatorEscaped(String s) {
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) == '\\') {
            i--;
        }
        int numberOfBackslashes = s.length() - 1 - i;
        return numberOfBackslashes % 2 == 1;
    }

    private static boolean isWhiteSpace(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }
}
