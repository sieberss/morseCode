package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MorseCodeDecoder {
    private static double minDuration, maxDuration;
    private static Map<Integer, Long> lengthCounts;
    private static List<String> segments;

    static void setStaticValues(Map<Integer, Long> map, int shortestOneSegment, int longestOneSegment, int longestZeroSegment){
        lengthCounts = map;
        firstDurationCalculation(shortestOneSegment, longestOneSegment, longestZeroSegment);
    }

    /**
     * Given a string in Morse Code, returns the English translation.
     * <p>
     * Accept dots, dashes and spaces, returns human-readable message.
     */
    public static String decodeMorse(String morseCode) {
        if (morseCode.isEmpty())
            return "";
        System.out.println("Morse: " + morseCode);
        StringBuilder sb = new StringBuilder();
        for (String word : morseCode.trim().split(" {3}")) {
            for (String letter : word.split(" ")) {
                System.out.println(":" + letter + MorseCode.get(letter));
                sb.append(MorseCode.get(letter));
            }
            sb.append(" ");
        }
        System.out.println("Decoded: " + sb);
        return sb.toString().trim();
    }

    /**
     * Given a string of bits, which may or may not begin or end with '0's,
     * and which may have some variation in the length of the time unit used,
     * returns the Morse Code translation of this message.
     * <p>
     * Accepts 0s and 1s, return dots, dashes and spaces
     *
     */
    public static String decodeBitsAdvanced(String bits) {
        segments = getSegments(bits);
        segments.forEach(System.out::println);
        if (segments.size() == 1)
            return ".";
        int[] dotAndDashLengths = getDotAndDashLengths();
        System.out.printf("longest dot: %d, longest dash: %d", dotAndDashLengths[0], dotAndDashLengths[1]);
        return segmentsToMorse(dotAndDashLengths, segments);
    }

    private static int[] getDotAndDashLengths() {
        int shortestOneSegment = segments.stream()
                .filter(s -> s.startsWith("1")).mapToInt(String::length).min().orElseThrow();
        int longestOneSegment = segments.stream()
                .filter(s -> s.startsWith("1")).mapToInt(String::length).max().orElseThrow();
        int shortestZeroSegment = segments.stream()
                .filter(s -> s.startsWith("0")).mapToInt(String::length).min().orElseThrow();
        int longestZeroSegment = segments.stream()
                .filter(s -> s.startsWith("0")).mapToInt(String::length).max().orElseThrow();
        lengthCounts = segments.stream()
                .map(String::length)
                .collect(Collectors.groupingBy(Integer::intValue, Collectors.counting()));
        System.out.println(lengthCounts);
        System.out.println("longest 1-segment: " + longestOneSegment);
        int shortestSegment = Math.min(shortestOneSegment, shortestZeroSegment);
        int longestSegment = Math.max(longestOneSegment, longestZeroSegment);

        if (longestSegment < 2 * shortestSegment)
            // only dots and short pauses
            return new int[]{longestSegment, longestSegment + 1};
        if (maximumLengthBelow(longestZeroSegment) < 6 * shortestSegment) {
            // no word breaks, so treat longest 0-segment like 1-segment
            return new int[]{maximumLengthBelow(1 + (longestSegment) / 3), longestSegment};
        }
        if (longestOneSegment < 2 * shortestSegment) {
            // no dashes, but long pauses
            return new int[]{maximumLengthBelow(2 + (longestZeroSegment) / 7), longestZeroSegment};
        }

        maxDuration = Math.max(getMinDurationForDashLength(longestOneSegment),
                getMinDurationForWordSeparatorLength(longestZeroSegment));
        int dotMax = (int) Math.ceil(maxDuration);
        int dashMax = (int) Math.ceil(3 * maxDuration);
        return checkLengths(dotMax, dashMax, shortestSegment);
    }

    private static int[] checkLengths(int dotMax, int dashMax, int shortestSegment) {
        int firstDash = minimumLengthAbove(dotMax);
        if ((getMinDurationForDashLength(firstDash) <= shortestSegment + 0.5)
                && (dotMax >= getMaxDurationForDashLength(dashMax))) {
            // try with firstDash as dotMax, according dashMax is calculated as follows:
            // maxDuration > dotMax - 0.5 => 3 * maxDuration > 3 * dotMax - 1.5
            return checkLengths(firstDash, firstDash * 3 - 1, shortestSegment);
        }
        // else maximal lengths found
        return new int[]{dotMax, dashMax};
    }

    static String segmentsToMorse(int[] dotAndDashLengths, List<String> segments) {
        StringBuilder sb = new StringBuilder();
        segments.forEach(segment -> sb.append(getMorseDigit(segment, dotAndDashLengths)));
        return sb.toString();
    }

    private static void firstDurationCalculation(int shortestOneSegment, int longestOneSegment, int longestZeroSegment) {
        /* calculate operator's range of bits needed for a dot:
          minimum: shortest segment can be a result of rounding up
          maximum: 3*duration (dash) can round down to longest 1-segment, 7*duration (word separator) to longest 0-segment
         */
        int shortestSegment = maximumLengthBelow(shortestOneSegment);
        minDuration = shortestSegment - 0.5;
        int longestDashCandidate = Math.max(longestOneSegment + 1, (3 * shortestSegment));
        maxDuration = Math.max(minDuration,
                Math.max(getMaxDurationForDashLength(longestDashCandidate), getMaxDurationForWordSeparatorLength(longestZeroSegment)));
    }

    private static double getMaxDurationForWordSeparatorLength(long wsLength) {
        return (wsLength + 0.5) / 7;
    }
    private static double getMinDurationForWordSeparatorLength(long wsLength) {
        return (wsLength - 0.5) / 7;
    }

    private static double getMaxDurationForDashLength(long dashLength) {
        return (dashLength + 0.5) / 3;
    }
    private static double getMinDurationForDashLength(long dashLength) {
        return (dashLength - 0.5) / 3;
    }

    private static void updateDurationsToMaxDotLength(int maxDotLength, int longestSegment) {
        maxDuration = Math.min(maxDuration, maxDotLength + 0.5);
        /*int maxDashLength = (int) Math.min(Math.round(3 * maxDuration), longestSegment);
        int nextLength = minimumLengthAbove(maxDashLength);*/
        int nextLength = minimumLengthAbove(maxDotLength);
        minDuration = Math.max(minDuration, getMinDurationForDashLength(nextLength));
    }

    private static void updateDurationsToMaxDashLength(int maxDashLength) {
        int nextLength = minimumLengthAbove(maxDashLength);
        maxDuration = Math.min(maxDuration, getMaxDurationForWordSeparatorLength(nextLength));
        minDuration = Math.max(minDuration, getMinDurationForDashLength(Math.round(maxDuration + 1)));
    }

    private static Integer minimumLengthAbove(int limit) {
        return lengthCounts.keySet().stream()
                .filter(l -> l > limit)
                .min(Integer::compare)
                .orElse(limit);
    }

    private static Integer maximumLengthBelow(int limit) {
        return lengthCounts.keySet().stream()
                .filter(l -> l < limit)
                .max(Integer::compare)
                .orElse(limit);
    }

    private static String getMorseDigit(String segment, int[] dotAndDashLengths) {
        if (segment.length() > dotAndDashLengths[1])
            return "   ";
        if (segment.length() <= dotAndDashLengths[0])
            return segment.startsWith("1")
                    ? "."
                    : "";
        return segment.startsWith("1")
                ? "-"
                : " ";
    }


    private static Integer maxCountLengthBelow(int limit) {
        return lengthCounts.entrySet().stream()
                .filter(e -> e.getKey() < limit)
                .max((a, b) -> Long.compare(a.getValue(), b.getValue()))
                .orElseThrow()
                .getKey();
    }

    static List<String> getSegments(String bits) {
        List<String> segments = new ArrayList<>();
        System.out.println("Bits: " + bits);
        // begin of code
        int oneSegmentStart = bits.indexOf('1');
        if (oneSegmentStart == -1) {
            return segments;
        }
        // begin of first pause in code
        int zeroSegmentStart = bits.indexOf('0', oneSegmentStart);
        while (true) {
            if (zeroSegmentStart == - 1){
                // no following 0s
                segments.add(bits.substring(oneSegmentStart));
                break;
            }
            segments.add(bits.substring(oneSegmentStart, zeroSegmentStart));
            // begin of next 1-segment
            oneSegmentStart = bits.indexOf('1', zeroSegmentStart);
            if (oneSegmentStart == -1) {
                // no more 1-segments
                break;
            }
            // add segment of 0s
            segments.add(bits.substring(zeroSegmentStart, oneSegmentStart));
            // begin of next 0-segment
            zeroSegmentStart = bits.indexOf('0', oneSegmentStart);
        }
        return segments;
    }
}