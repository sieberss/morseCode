package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MorseCodeDecoder {
    private static Map<Integer, Long> lengthCounts;
    private static Map<Integer, Weight> lengthWeights;

    private static class Weight {
        long count;
        long countTimesValue;
        Weight(long count, long countTimesValue) {
            this.count = count;
            this.countTimesValue = countTimesValue;
        }
    }

    /**
     * Given a string in Morse Code, returns the English translation.
     * <p>
     * Accept dots, dashes and spaces, returns human-readable message.
     */
    public static String decodeMorse(String morseCode) {
        if (morseCode.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (String word : morseCode.trim().split(" {3}")) {
            for (String letter : word.split(" ")) {
                System.out.println(":" + letter + MorseCode.get(letter));
                sb.append(MorseCode.get(letter));
            }
            sb.append(" ");
        }
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
        List<String> segments = getSegments(bits);
        if (segments.isEmpty())
            return "";
        if (segments.size() == 1)
            return ".";
        int[] dotAndDashLengths = getDotAndDashLengths(segments);
        // System.out.printf("longest dot: %d, longest dash: %d", dotAndDashLengths[0], dotAndDashLengths[1]);
        return segmentsToMorse(dotAndDashLengths, segments);
    }

    static int[] getDotAndDashLengths(List<String> segments) {
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
        // Weights (length of segment * frequency) for calculating the average time the operator's speed
        lengthWeights = new HashMap<>();
        lengthCounts.forEach((key, value) ->
                lengthWeights.put(key, new Weight(value, value * key)));
        int shortestSegment = Math.min(shortestOneSegment, shortestZeroSegment);
        int longestSegment = Math.max(longestOneSegment, longestZeroSegment);

        if (longestSegment < 2 * shortestSegment)
            // only dots and short pauses
            return new int[]{longestSegment, longestSegment + 1};
        if (longestZeroSegment < 6 * shortestSegment) {
            // no word breaks, so treat longest 0-segment like 1-segment
            return new int[]{maximumLengthBelow(1 + (longestSegment) / 3), longestSegment};
        }
        if (longestOneSegment < 2 * shortestSegment) {
            // no dashes, but long pauses
            return new int[]{maximumLengthBelow(2 + (longestZeroSegment) / 7), maximumLengthBelow(longestZeroSegment)};
        }

        // maximum time needed per operator bit due to longest dash and longest word separator
        double maxDuration = Math.max(getMinDurationForDashLength(longestOneSegment),
                getMinDurationForWordSeparatorLength(longestZeroSegment));
        // maximum length of dots and dashes at that speed
        int dotMaxEstimated = (int) Math.ceil(maxDuration);      //maybe round instead of ceil?
        int dashMaxEstimated = (int) Math.ceil(3 * maxDuration);
        return calculateLength(dotMaxEstimated, dashMaxEstimated, longestZeroSegment);
    }

    private static int[] calculateLength(int dotStart, int dashStart, int wordSepMax) {
        int dotCursor = dotStart,
                dashCursor = dashStart,
                dotMax = dotStart,
                dashMax = dashStart;
        int firstDash = minimumLengthAbove(dotCursor);
        int firstWordSep = minimumLengthAbove(dashCursor);
        double minVariation = wordSepMax;
        while (dotCursor < dashStart) {
            Weight dotAggregate = aggregateWeights(1, dotCursor);
            double dotAverage = (double) dotAggregate.countTimesValue / dotAggregate.count;
            Weight dashAggregate = aggregateWeights(firstDash, dashCursor);
            double dashAverage = (double) dashAggregate.countTimesValue / dashAggregate.count / 3;
            Weight wsAggregate = aggregateWeights(firstWordSep, wordSepMax);
            double totalAverage = (double) (dotAggregate.countTimesValue + dashAggregate.countTimesValue + wsAggregate.countTimesValue)
                    / (dotAggregate.count + 3 * dashAggregate.count + 7 * wsAggregate.count);
            // minimum difference to total Average
            double difference =
                    Math.max(Math.abs(dotAverage - totalAverage), Math.abs(dashAverage - totalAverage));
            if (difference < minVariation ) {
                minVariation = difference;
                // better fit, update result
                dashMax = dashCursor;
                dotMax = dotCursor;
            }
            // move cursor forward
            dotCursor = firstDash;
            firstDash = minimumLengthAbove(dotCursor);
            if (firstWordSep  < 3 * dotCursor + 2) {
                // dash cursor remains if next words separator would be too long
                dashCursor = firstWordSep;
                firstWordSep = minimumLengthAbove(dashCursor);
            }
        }
        return new int[]{dotMax, dashMax};
    }

    private static Weight aggregateWeights(int from, int to) {
        long countSum = lengthWeights.entrySet().stream()
                .filter(e -> e.getKey() >= from && e.getKey() <= to)
                .mapToLong(e -> e.getValue().count)
                .sum();
        long countTimesValueSum = lengthWeights.entrySet().stream()
                .filter(e -> e.getKey() >= from && e.getKey() <= to)
                .mapToLong(e -> e.getValue().countTimesValue)
                .sum();
        return new Weight(countSum, countTimesValueSum);
    }

    static String segmentsToMorse(int[] dotAndDashLengths, List<String> segments) {
        StringBuilder sb = new StringBuilder();
        segments.forEach(segment -> sb.append(getMorseDigit(segment, dotAndDashLengths)));
        return sb.toString();
    }

    private static double getMinDurationForWordSeparatorLength(long wsLength) {
        return (wsLength - 0.5) / 7;
    }

    private static double getMinDurationForDashLength(long dashLength) {
        return (dashLength - 0.5) / 3;
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