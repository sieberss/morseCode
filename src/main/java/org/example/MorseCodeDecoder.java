package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MorseCodeDecoder {
    private static double minDuration, maxDuration;
    private static Map<Integer, Long> lengthCounts;

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
        System.out.println("Bits: " + bits);
        int begin = bits.indexOf('1'),
                end = bits.lastIndexOf('1');
        if (begin == -1)
            return "";
        List<String> segments = getSegments(bits.substring(begin, end + 1));
        if (segments.size() == 1)
            return ".";
        int[] dotAndDashLengths = getDotAndDashLengths(segments);
        return segmentsToMorse(segments, dotAndDashLengths);
    }

    private static int[] getDotAndDashLengths(List<String> segments) {
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
        int shortestSegment = Math.min(shortestOneSegment, shortestZeroSegment);
        int longestSegment = Math.max(longestOneSegment, longestZeroSegment);
        firstDurationCalculation(shortestSegment, longestOneSegment, longestZeroSegment);
        System.out.printf("longest 1-segment: %d, duration %f to %f\n",  longestOneSegment, minDuration, maxDuration);
        /*updateDurationsToMaxDotLength( (int) Math.round(maxDuration));
        System.out.printf("revised duration: %f to %f\n", minDuration, maxDuration);*/

        int maxDotLength = getMaxDotLength(shortestOneSegment, shortestZeroSegment, longestOneSegment, longestZeroSegment);
        updateDurationsToMaxDotLength(maxDotLength, longestSegment);
        System.out.printf("max. dot length: % d, new duration: %f to %f\n", maxDotLength, minDuration, maxDuration);
        int maxDashLength = getMaxDashLength(shortestSegment, maxDotLength, longestOneSegment, longestZeroSegment);
        //long maxDotLength = Math.round(maxDuration);
        //long maxDashLength = Math.round(3 * maxDuration);
       return new int[]{maxDotLength, maxDashLength};
    }

    private static String segmentsToMorse(List<String> segments, int[] dotAndDashLengths) {
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

    static int getMaxDashLength(int minDotLength, int maxDotLength, int longestOneSegment, int longestZeroSegment) {
        int sevenMinimum = (int) Math.round(7 * minDuration);
        int threeMaximum = Math.min( (int) Math.round(3 * maxDuration), 3 * maxDotLength);
        int left = minimumLengthAbove(sevenMinimum - 1);
        int right = maximumLengthBelow(threeMaximum + 1);
        int possibleNext = Math.min(left, right);
        return Math.min(possibleNext, 3 * maxDotLength);
        /*int oneBitMore = longestOneSegment + 1;
        int minLongPause = 7 * minDotLength + 6;
        // no dashes
        if (longestOneSegment <= maxDotLength) {
            return maximumLengthBelow(3 * maxDotLength + 3, lengthCounts);

        }
        if (longestZeroSegment <= minLongPause) {
            return maximumLengthBelow(longestZeroSegment, lengthCounts);
        }
        if ((oneBitMore <= maxDotLength * 3) && (longestZeroSegment >= oneBitMore) && (lengthCounts.get(oneBitMore) != null))
                return oneBitMore;
        return longestOneSegment;*/
    }

     static int getMaxDotLength(int shortestOneSegment, int shortestZeroSegment, int longestOneSegment, int longestZeroSegment) {
        //int min = Math.min(shortestOneSegment, shortestZeroSegment);
        int threeMinimum = (int) Math.round(3 * minDuration);
        int oneMaximum = (int) Math.round(maxDuration);
        int limit = Math.max(threeMinimum, oneMaximum + 1);
        return maximumLengthBelow(limit);/*
        int sevenMinimum = 7 * min;
        // no dots or no dashes
        if (shortestOneSegment >= threeMinimum || longestOneSegment < threeMinimum)
            // return longest segment shorter than tripleMin
            return maximumLengthBelow(threeMinimum, lengthCounts);
        int length = maxCountDot;
        long count = lengthCounts.get(length);
        long nextCount = lengthCounts.getOrDefault(maxCountDot + 1, 0L);
        while ((nextCount <= count) && (nextCount > 0) && (length < min+3) && (length <= longestOneSegment)) {
            length++;
            count = nextCount;
            nextCount = lengthCounts.getOrDefault(length + 1, 0L);
        }
        return length;*/
    }

    private static Integer maxCountLengthBelow(int limit) {
        return lengthCounts.entrySet().stream()
                .filter(e -> e.getKey() < limit)
                .max((a, b) -> Long.compare(a.getValue(), b.getValue()))
                .orElseThrow()
                .getKey();
    }

    private static List<String> getSegments(String bits) {
        List<String> segments = new ArrayList<>();
        char bit = '1';
        int oneSegmentStart = 0;
        int zeroSegmentStart = bits.indexOf('0');
        while (zeroSegmentStart != -1) {
            segments.add(bits.substring(oneSegmentStart, zeroSegmentStart));
            // following segment of 0s
            oneSegmentStart = bits.indexOf('1', zeroSegmentStart);
            segments.add(bits.substring(zeroSegmentStart, oneSegmentStart));
            zeroSegmentStart = bits.indexOf('0', oneSegmentStart);
        }
        // add last segment of 1s
        segments.add(bits.substring(oneSegmentStart));
        return segments;
    }
}