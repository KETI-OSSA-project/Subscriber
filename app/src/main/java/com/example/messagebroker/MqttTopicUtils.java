package com.example.messagebroker;

/*
 * Copyright 2020 by Security and Safety Things GmbH
 *
 * All rights reserved,
 * also regarding any disposal, exploitation, reproduction,
 * editing, distribution, as well as in the event of
 * applications for industrial property rights.
 *
 * This software contains proprietary and confidential
 * information of Security and Safety Things GmbH.
 * The disclosure of confidential information without
 * express authorization is prohibited.
 *
 * This software may include Open Source Software.
 * As far as Open Source Software is included,
 * your rights for the respective Open Source Software
 * components are governed by the applicable
 * Open Source Software licenses.
 */

// import android.support.annotation.NonNull;
import androidx.annotation.NonNull;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Helper class to process and analyze topics and topic filters as per the MQTT Specification
 * (MQTT v.5.0).
 *
 * @hide
 */
public class MqttTopicUtils {
    private static final char SINGLE_LEVEL_WILD_CARD = '+';
    private static final char MULTI_LEVEL_WILD_CARD = '#';
    private static final char LEVEL_SEPARATOR = '/';

    private MqttTopicUtils() {
    }

    /**
     * Checks whether the given topic expression contains a wild card.
     *
     * @param topic the topic to check.
     * @return Returns {@code true} if the given topic has wild cards, {@code false} otherwise.
     */
    public static boolean hasWildcards(@NonNull String topic) {
        requireNonNull(topic);
        if (topic.indexOf(SINGLE_LEVEL_WILD_CARD) >= 0
                || topic.indexOf(MULTI_LEVEL_WILD_CARD) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Method to check whether an MQTT topic expression has a match with an MQTT topic filter.
     *
     * @param filterTopic  A filter expression as per the MQTT Specification.
     * @param messageTopic A topic expression as per the the MQTT Specification.
     * @return Returns {@code true} if the {@code messageTopic} matches to the {@code filterTopic}
     * as per the MQTT Specification, {@code false} otherwise.
     */
    public static boolean match(@NonNull final String filterTopic,
                                @NonNull final String messageTopic) {
        //TODO(tha1ein): Improvise the design to have a topic class where below sanity
        // checks can be done once during construction, and there after operate (e.g. match)
        // with the class specific methods.
        checkFilterTopicSyntax(filterTopic);
        checkMessageTopicSyntax(messageTopic);

        if ((filterTopic.charAt(0) == MULTI_LEVEL_WILD_CARD
                || filterTopic.charAt(0) == SINGLE_LEVEL_WILD_CARD)
                && messageTopic.charAt(0) == '$') {
            return false;
        }

        // Check if the filter topic has multilevel wild card, else skip to single level wild
        // card match
        int wildCardIndex = filterTopic.indexOf(MULTI_LEVEL_WILD_CARD);

        // The message length should be at least as much as the length of the string before the
        // wild card
        if (messageTopic.length() <= wildCardIndex - 1) {
            return false;
        }

        if (wildCardIndex != -1) {

            // If the wild card is at starting position return true
            if (wildCardIndex == 0) {
                return true;
            }

            // Extract number of levels in filter topic
            int nFilterLevels = countLevels(filterTopic);

            // Return false if the number of levels in the message topic is less than the number
            // of levels in the filter topic
            if (countLevels(messageTopic) < nFilterLevels) {
                return false;
            }
            // Extract the substring of the message containing the number of levels the filter
            // topic has in total
            int nthIndex = getIndexOfNthLevel(messageTopic, nFilterLevels);

            // Proceed to single level wild card match processing by removing the last level
            // from both message and filter topics.
            return singleLevelWildCardMatch(filterTopic.substring(0, wildCardIndex - 1),
                    messageTopic.substring(0, nthIndex));
        } else {
            return (countLevels(filterTopic) == countLevels(messageTopic))
                    && singleLevelWildCardMatch(filterTopic, messageTopic);
        }
    }

    private static boolean singleLevelWildCardMatch(final String filterTopic,
                                                    final String messageTopic) {

        // Check if the filter topic has single-level wild card, else skip to string match
        int wildCardIndex = filterTopic.indexOf(SINGLE_LEVEL_WILD_CARD);

        // The message length should be at least as much as the length of the string before the
        // wild card
        if (messageTopic.length() <= wildCardIndex - 1) {
            return false;
        }

        if (wildCardIndex != -1) {
            String messageTopicRhs = messageTopic.substring(wildCardIndex);
            boolean hasLevel = messageTopicRhs.contains(String.valueOf(LEVEL_SEPARATOR));
            boolean beforeWildCardMatch = (wildCardIndex == 0)
                    || filterTopic.substring(0, wildCardIndex - 1)
                    .equals(messageTopic.substring(0, wildCardIndex - 1));

            return beforeWildCardMatch
                    && (((wildCardIndex == filterTopic.length() - 1) && !hasLevel)
                    || (hasLevel
                    && (singleLevelWildCardMatch(filterTopic.substring(wildCardIndex + 1),
                    messageTopicRhs.substring(messageTopicRhs.indexOf(LEVEL_SEPARATOR))))));
        } else {
            return filterTopic.equals(messageTopic);
        }
    }

    private static int countLevels(final String expression) {
        int count = 0;
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == LEVEL_SEPARATOR) {
                count++;
            }
        }
        return count;
    }

    private static int getIndexOfNthLevel(final String expression, int n) {
        int pos = expression.indexOf(LEVEL_SEPARATOR);
        while (--n > 0 && pos != -1) {
            pos = expression.indexOf(LEVEL_SEPARATOR, pos + 1);
        }
        return pos;
    }

    /**
     * Check whether a filter expression is a superset of other filter expression.
     *
     * @param thisFilter  A filter expression as per the MQTT Specification.
     * @param otherFilter Another filter expression as per the the MQTT Specification.
     * @return Returns {@code true} if filter {@code thisFilter} is a super set of
     * {@code otherFilter}, ({@code false}) otherwise.
     */
//    public static boolean isSuperset(@NonNull String thisFilter, @NonNull String otherFilter) {
//        Objects.requireNonNull(otherFilter);
//
//        //Maths: set A is a Superset of set B, if intersection(A,B) equals B.
//        return otherFilter.equals(
//                intersection(thisFilter, otherFilter));
//    }

    /**
     * Check whether a filter expression has intersection with other filter expression.
     *
     * @param thisFilter  A filter expression as per the MQTT Specification.
     * @param otherFilter Another filter expression as per the the MQTT Specification.
     * @return Returns {@code true} if {@code thisFilter} has intersection with
     * {@code otherFilter}, ({@code false}) otherwise.
     */
//    public static boolean hasIntersection(@NonNull String thisFilter, @NonNull String otherFilter) {
//        return !(intersection(thisFilter, otherFilter) == null);
//    }

    /**
     * Method to find a filter expression which is the intersection of two MQTT topic expressions.
     *
     * @param thisFilter  A filter expression as per the MQTT Specification.
     * @param otherFilter Another filter expression as per the the MQTT Specification.
     * @return Returns an intersected expression if there is an intersection,
     * ({@code null}) otherwise.
     */
//    public static String intersection(String thisFilter, String otherFilter) {
//        checkFilterTopicSyntax(thisFilter);
//        checkFilterTopicSyntax(otherFilter);
//
//        if (thisFilter.equals(otherFilter)) {
//            return thisFilter;
//        }
//
//        //TODO(tha1ein): consider deviation from MQTT Specification [SODP-322].
//        //Filter starting with dollar are not a subset of other filters starting with wildcards.
//        if ((thisFilter.charAt(0) == '$' && otherFilter.charAt(0) != '$')
//                || (thisFilter.charAt(0) != '$' && otherFilter.charAt(0) == '$')) {
//            return null;
//        }
//
//        //if multi level wildcard in one filter is at index zero, the second
//        // filter is always the subset.
//        if (thisFilter.charAt(0) == MULTI_LEVEL_WILD_CARD) {
//            return otherFilter;
//        }
//        if (otherFilter.charAt(0) == MULTI_LEVEL_WILD_CARD) {
//            return thisFilter;
//        }
//
//        //find index of multi level wild card in each expression
//        int multiLevelWildCardIndexInThis = thisFilter.indexOf(MULTI_LEVEL_WILD_CARD);
//        int multiLevelWildCardIndexInOther = otherFilter.indexOf(MULTI_LEVEL_WILD_CARD);
//
//        //if there are no multi level wild cards, skip to single level wild card intersection.
//        if (multiLevelWildCardIndexInThis != -1 || multiLevelWildCardIndexInOther != -1) {
//
//            // Count number of levels in each filter
//            int numberOfLevelsInThisFilter = countLevels(thisFilter);
//            int numberOfLevelsInOtherFilter = countLevels(otherFilter);
//
//            /* Among the two, find the expression which has multi level wild card earlier
//            in order of appearance*/
//            //Assuming thisFilter has wildcard earlier, mark and proceed.
//            // If otherFilter is earlier, correctly assign later.
//            String firstFilter = thisFilter;
//            String secondFilter = otherFilter;
//            int wildCardIndexInFirstFilter = multiLevelWildCardIndexInThis;
//            int numberOfLevelsInFirstFilter = numberOfLevelsInThisFilter;
//            int numberOfLevelsInSecondFilter = numberOfLevelsInOtherFilter;
//
//            //true if otherFilter has a multi level wild card and thisFilter doesn't have one
//            //or true if both filters have multi level wild card, and appears earlier in
//            //otherFilter.
//            boolean isOtherFilterEarlier = (multiLevelWildCardIndexInOther > -1)
//                    && ((multiLevelWildCardIndexInThis == -1)
//                    || (numberOfLevelsInOtherFilter < numberOfLevelsInThisFilter));
//            //if otherFilter is earlier, correctly assign it here.
//            if (isOtherFilterEarlier) {
//                firstFilter = otherFilter;
//                secondFilter = thisFilter;
//                wildCardIndexInFirstFilter = multiLevelWildCardIndexInOther;
//                numberOfLevelsInFirstFilter = numberOfLevelsInOtherFilter;
//                numberOfLevelsInSecondFilter = numberOfLevelsInThisFilter;
//            }
//
//            // Return null if the number of levels in the second filter is less than the
//            // number of levels in the first filter.
//            // example case: a/b/c/# and a/b/
//            if (numberOfLevelsInSecondFilter < numberOfLevelsInFirstFilter) {
//                return null;
//            }
//
//            /* Check the levels before the multi level wild card filter in both first and
//            second filters have single level intersection
//            */
//            //extract the substring before multi level wild card.
//            firstFilter = firstFilter.substring(0, wildCardIndexInFirstFilter - 1);
//
//            //Extract the part of expression which is intersection because of multi level wild card.
//            int nthIndex = (numberOfLevelsInFirstFilter == 0)
//                    ? 0 : getIndexOfNthLevel(secondFilter, numberOfLevelsInFirstFilter);
//            String multiLevelIntersection = secondFilter.substring(nthIndex);
//
//            // Extract the substring of the second filter having the same number of levels
//            // in updated firstFilter
//            secondFilter = secondFilter.substring(0, nthIndex);
//
//            String singleLevelIntersection =
//                    singleLevelInterSection(firstFilter, secondFilter);
//
//            return (singleLevelIntersection == null)
//                    ? null : (singleLevelIntersection + multiLevelIntersection);
//        }
//
//        return singleLevelInterSection(thisFilter, otherFilter);
//    }

//    private static String singleLevelInterSection(final String thisFilter,
//                                                  final String otherFilter) {
//        if (countLevels(thisFilter) != countLevels(otherFilter)) {
//            return null;
//        } else {
//            return singleLevelInterSectionWhenEqualLevels(thisFilter,
//                    otherFilter);
//        }
//    }

//    private static String singleLevelInterSectionWhenEqualLevels(final String thisFilter,
//                                                                 final String otherFilter) {
//        if (thisFilter.equals(otherFilter)) {
//            return thisFilter;
//        }
//
//        //find index of single level wild card in each filter.
//        int wildCardIndexInThis = thisFilter.indexOf(SINGLE_LEVEL_WILD_CARD);
//        int wildCardIndexInOther = otherFilter.indexOf(SINGLE_LEVEL_WILD_CARD);
//
//        // If there is no wild card in both filters, no possibility of intersection at all.
//        if (wildCardIndexInThis == -1 && wildCardIndexInOther == -1) {
//            return null;
//        }
//
//        //Number of levels before and till the wildcard
//        int numberOfLevelsBeforeWildcardInThis = (wildCardIndexInThis >= 0)
//                ? countLevels(thisFilter.substring(0, wildCardIndexInThis)) : 0;
//        int numberOfLevelsBeforeWildcardInOther = (wildCardIndexInOther >= 0)
//                ? countLevels(otherFilter.substring(0, wildCardIndexInOther)) : 0;
//
//        /* Among the two, find the filter which has single level wild card earlier
//        in order of appearance*/
//        //Assuming thisFilter has earlier, mark and proceed. If otherFilter is earlier,
//        // correctly assign later.
//        String firstFilter = thisFilter;
//        String secondFilter = otherFilter;
//        int wildCardIndexInFirst = wildCardIndexInThis;
//        int numberOfLevelsBeforeWildcardInFirst = numberOfLevelsBeforeWildcardInThis;
//
//        //true if otherFilter has a single level wild card and thisFilter doesn't have one
//        //or true if both Expressions have single level wild card, and appears earlier in
//        //otherFilter.
//        boolean isOtherFilterEarlier = (wildCardIndexInOther > -1)
//                && ((wildCardIndexInThis == -1)
//                || (numberOfLevelsBeforeWildcardInOther < numberOfLevelsBeforeWildcardInThis));
//        if (isOtherFilterEarlier) {
//            firstFilter = otherFilter;
//            secondFilter = thisFilter;
//            wildCardIndexInFirst = wildCardIndexInOther;
//            numberOfLevelsBeforeWildcardInFirst = numberOfLevelsBeforeWildcardInOther;
//        }
//
//        //Index of the level in second filter corresponding to the wild card level in first filter.
//        int nthIndex = (numberOfLevelsBeforeWildcardInFirst == 0)
//                ? 0 : getIndexOfNthLevel(secondFilter, numberOfLevelsBeforeWildcardInFirst);
//
//        //check if portion before spotted wild card is same in both filters.
//        boolean isPortionBeforeWildCardSame =
//                (wildCardIndexInFirst == 0)
//                        || subStringOrEmptyString(firstFilter, 0, wildCardIndexInFirst - 1)
//                        .equals(subStringOrEmptyString(secondFilter, 0, nthIndex));
//
//        if (!isPortionBeforeWildCardSame) {
//            return null;
//        }
//
//        //extract substring with levels after wild card form both filters.
//        String levelsAfterWildCardInFirstFilter
//                = subStringOrEmptyString(firstFilter, wildCardIndexInFirst + 1);
//        int indexOfNextLevelInSecondFilter
//                = secondFilter.indexOf(LEVEL_SEPARATOR, wildCardIndexInFirst);
//        String levelsAfterWildCardInSecondFilter
//                = subStringOrEmptyString(secondFilter, indexOfNextLevelInSecondFilter);
//
//        //do recursive intersection on the substrings
//        String afterWildCardIntersection = singleLevelInterSectionWhenEqualLevels(
//                levelsAfterWildCardInFirstFilter,
//                levelsAfterWildCardInSecondFilter);
//
//        if (afterWildCardIntersection == null) {
//            return null;
//        }
//        //extract the substring in second filter till the wild card (including level of wild card)
//        String tillWildCardIntersection = (indexOfNextLevelInSecondFilter >= 0)
//                ? secondFilter.substring(0, indexOfNextLevelInSecondFilter) : secondFilter;
//        return tillWildCardIntersection + afterWildCardIntersection;
//
//    }

//    private static String subStringOrEmptyString(String string, int index) {
//        return (index < string.length() && index >= 0)
//                ? string.substring(index) : "";
//    }
//
//    private static String subStringOrEmptyString(String string, int beginIndex, int endIndex) {
//        return (endIndex <= string.length() && beginIndex >= 0)
//                ? string.substring(beginIndex, endIndex) : "";
//    }

    /**
     * Check if the topic name is in accordance with the syntax rules described in
     * MQTT Specification.
     *
     * @param topic The topic to check against MQTT Specification.
     * @throws IllegalArgumentException if the topic is not as per the MQTT specification.
     */
    public static void checkMessageTopicSyntax(final @NonNull String topic) {

        Objects.requireNonNull(topic);

        // Check if the topic name is at least one character long
        if (topic.isEmpty()) {
            throw new IllegalArgumentException("The topic name must " +
                    "be at least one character long");
        }
        // Check if the topic name contains any wild card characters
        if (hasWildcards(topic)) {
            throw new IllegalArgumentException("The topic name should not contain any " +
                    "wild card characters");
        }
        // Check if the topic name exceeds 65535 bytes
        if (topic.getBytes().length > 65535) {
            throw new IllegalArgumentException("The topic name should not exceed 65535 bytes");
        }
    }

    /**
     * Checks whether the syntax in a given expression is as per the MQTT Specification.
     *
     * @param filterTopic An expression to be checked against the MQTT Specification.
     * @throws IllegalArgumentException if the topic is not as per the MQTT specification.
     */
    public static void checkFilterTopicSyntax(@NonNull final String filterTopic) {
        Objects.requireNonNull(filterTopic);
        checkFilterTopicIsNotEmpty(filterTopic);
        checkProperUsageOfMultiLevelWildCard(filterTopic);
        checkProperUsageOfSingleLevelWildCard(filterTopic);
    }

    private static void checkFilterTopicIsNotEmpty(final String filterTopic) {

        if (filterTopic.isEmpty()) {
            throw new IllegalArgumentException(" The filtering expression should be at least one " +
                    "character long");
        }
    }

    private static void checkProperUsageOfMultiLevelWildCard(final String filterTopic) {

        // Check for proper usage only if the filter expression has multilevel wild card.
        int wildCardIndex = filterTopic.indexOf(MULTI_LEVEL_WILD_CARD);

        if (wildCardIndex != -1) {

            // If the wild card is at starting position, ensure that the length of the filter
            // is 1 and no other characters are present after it. Otherwise, throw an exception.
            if (wildCardIndex == 0) {
                if (filterTopic.length() == 1) {
                    return;
                } else {
                    throw new IllegalArgumentException("Invalid filter expression");
                }
            }

            // Ensure, that the preceding character of the multilevel wild card is '/' and
            // the multilevel wild card is indeed at the last position, otherwise, throw an
            // exception.
            if ((filterTopic.charAt(wildCardIndex - 1) != LEVEL_SEPARATOR)
                    || (wildCardIndex != filterTopic.length() - 1)) {
                throw new IllegalArgumentException("Invalid filter expression");
            }
        }
    }

    private static void checkProperUsageOfSingleLevelWildCard(final String filterTopic) {

        // Check for proper usage only if the filter expression has single-level wild card
        int wildCardIndex = filterTopic.indexOf(SINGLE_LEVEL_WILD_CARD);

        if (wildCardIndex != -1) {

            if (!(wildCardIndex == 0)
                    && (filterTopic.charAt(wildCardIndex - 1) != LEVEL_SEPARATOR)) {
                throw new IllegalArgumentException("Invalid filter expression");
            }

            if (!(wildCardIndex == filterTopic.length() - 1)) {

                if ((filterTopic.charAt(wildCardIndex + 1) != LEVEL_SEPARATOR)) {
                    throw new IllegalArgumentException("Invalid filter expression");
                }
                checkProperUsageOfSingleLevelWildCard(filterTopic.substring(wildCardIndex + 1));
            }
        }
    }

}

