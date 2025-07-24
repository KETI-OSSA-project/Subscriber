package com.example.messagebroker;

/*
 * Copyright 2019 by Security and Safety Things GmbH
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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static com.example.messagebroker.MqttTopicUtils.checkFilterTopicSyntax;
import static com.example.messagebroker.MqttTopicUtils.match;
import static java.util.Objects.requireNonNull;


public class Filter implements Parcelable {

    public static final Creator<Filter> CREATOR = new Creator<Filter>() {
        @Override
        public Filter createFromParcel(Parcel in) {
            return new Filter(in);
        }

        @Override
        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };

    // 필터 조건들을 담는 Bundle 객체
    private final Bundle bundle;

    private Filter(Builder builder) {
        String topic = builder.topic;
        String publisherId = builder.publisherId;

        if (topic == null && publisherId == null) {
            throw new IllegalArgumentException("At-least one of the " +
                    "attributes topic or publisherId needs to be set.");
        }
        bundle = createFilterBundle(topic, publisherId);
    }

    /**
     * Constructor
     *
     * <p>Constructor for creating a filter object. Filtering of {@link Message} objects can be
     * done on either on a particular topic or on a particular publisher, or a combination of both.
     * In cases where both the arguments are null a {@link InvalidFilterException} exception is
     * thrown to the user.
     *
     * @param topic       For filtering on the topic attribute of a {@link Message}.
     *                    A filter expression following the filter syntax rules described in
     *                    MQTT v.5.0 Sec 4.7 can also be passed for more advanced filtering on
     *                    the topic. Passing a null value to this argument disables
     *                    filtering on the topic attribute of the {@link Message} object.
     * @param publisherId For filtering on the publisherId attribute of a {@link Message}. The
     *                    publisherId of a {@link Message} is the package name of the application
     *                    that published the message. This attribute is automatically set by the
     *                    {@link MessagingManager} before publishing the {@link Message}.
     *                    A filter expression following the filter syntax rules described in
     *                    MQTT v.5.0 Sec 4.7 can also be passed for more advanced filtering on the
     *                    publisherId. Passing a null value to this argument disables
     *                    filtering on the publisherId attribute of the {@link Message} object.
     * @throws InvalidFilterException   if both topic and publisherId arguments are null.
     * @throws IllegalArgumentException if the filter expressions do not follow the topic filter
     *                                  syntax rules described in MQTT v.5.0 section 4.7.
     * @deprecated Use {@link Builder} instead
     **/
    // Workaround for checkstyle bug: checkstyle complains about missing SecurityException
    // in the method signature. See: https://github.com/checkstyle/checkstyle/issues/5088
    @SuppressWarnings("checkstyle:javadocmethod")
    @Deprecated
    public Filter(String topic, String publisherId) throws InvalidFilterException {
        if (topic == null && publisherId == null) {
            throw new InvalidFilterException();
        }
        bundle = createFilterBundle(topic, publisherId);
    }

    //전달된　두　필터　객체가　null이　아니면　검증　후　bundle에 추가
    private static Bundle createFilterBundle(final String topic, final String publisherId) {
        Bundle bundle = new Bundle();

        // Fill the bundle for only non-null values
        if (topic != null) {
            checkFilterTopicSyntax(topic);
            bundle.putString(MessageHeaderKeyNames.TOPIC, topic);
        }

        if (publisherId != null) {
            checkFilterTopicSyntax(publisherId);
            bundle.putString(MessageHeaderKeyNames.PUBLISHER_ID, publisherId);
        }
        return bundle;
    }

    protected Filter(Parcel in) {
        bundle = in.readBundle(getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(bundle);
    }

    /**
     * Check whether a {@link Message} object satisfies the filter criteria encapsulated in the
     * {@link Filter} object.
     *
     * @param message The {@link Message} object on which the filter criteria is applied on.
     * @return Returns true if the {@link Message} satisfies the filter criteria, otherwise false is
     * returned.
     *
     * @hide
     */
    public boolean matches(Message message) {

        // Common failure case checks. As of the current implementation, even though the
        // failure conditions might not get hit at all, we keep them for now as a safety guard.

        // Extract header bundle from the message
        Bundle messageHeaderBundle = message.getBundle();
        if (messageHeaderBundle == null) {
            return false;
        }

        Set<String> messageKeys = messageHeaderBundle.keySet();
        if (messageKeys == null || messageKeys.isEmpty()) {
            return false;
        }

        // Loop over the keys in the filter and check if there is a match
        for (String filterKey : bundle.keySet()) {
            String messageValue = messageHeaderBundle.getString(filterKey);
            if (messageValue == null) {
                return false;
            }

            if (!match(bundle.getString(filterKey), messageValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the {@code topic} filter string in the {@link Filter}.
     *
     * @return The topic filter if present, otherwise null.
     *
     * @hide
     * */
    public String getTopic() {
        return bundle.getString(MessageHeaderKeyNames.TOPIC);
    }

    /**
     * Get the {@code publisherId} filter string in the {@link Filter}.
     *
     * @return The publisherId filter if present, otherwise null.
     *
     * @hide
     * */
    public String getPublisherId() {
        return bundle.getString(MessageHeaderKeyNames.PUBLISHER_ID);
    }

    /**
     * Compares the two given filters based on the content.
     * If any element is not matched, it returns false. If all elements are matched, it returns
     * true.
     */
    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        // Note: If this class is extended, ensure that equals() and hashcode() are properly
        // implemented wrt symmetry and transitivity properties.
        // Or, the subclass should not add members.
        if (!(obj instanceof Filter)) {
            return false;
        }
        Filter that = (Filter) obj;

        //Extracting key set from the first filter
        Set<String> thisBundleKeySet = bundle.keySet();

        // Extract bundle from the second filter
        Bundle thatBundle = that.bundle;

        // Check if the size of the key sets are equal. This is to avoid returning true for a match
        // on subset of keys. If the sizes are equal and still the key sets contain different set of
        // keys, the subsequent check will ensure that the match returns false.
        if (thisBundleKeySet.size() != thatBundle.keySet().size()) {
            return false;
        }

        // Loop over all the filter keys and check if there is a match
        for (String key : thisBundleKeySet) {
            if (!Objects.equals(bundle.getString(key), thatBundle.getString(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes hash code based on the contents of the filter.
     */
    @Override
    public int hashCode() {
        ArrayList<String> keyList = new ArrayList<>(bundle.keySet());

        //Sort the key list so the order of computing hashcode is standard every time
        Collections.sort(keyList);

        // Choose a random prime
        final int prime = 31;
        int result = 1;
        for (String key : keyList) {
            result = result * prime + key.hashCode();
            result = result * prime + bundle.getString(key).hashCode();

        }
        return result;
    }

    public static class Builder {
        private String topic;
        private String publisherId;

        /**
         * Optional method for setting the topic filter expression of the {@link Filter} object
         * which can be used to filter {@link Message} objects by topic.
         *
         * @param topic For filtering on the topic attribute of a {@link Message}.
         *              The filtering expression used should be in accordance with the syntax rules
         *              described in MQTT v.5.0 section 4.7.
         * @return Builder object encapsulating the conditions to filter messages by topic
         * @throws IllegalArgumentException if the provided expression for filtering is
         *                                  not in accordance with the syntax rules described in
         *                                  MQTT v.5.0 section 4.7.
         */
        public Filter.Builder setTopic(@NonNull String topic) {
            requireNonNull(topic);
            checkFilterTopicSyntax(topic);
            this.topic = topic;
            return this;
        }

        /**
         * Optional method for setting the publisherId filter expression of the {@link Filter}
         * object which can be used to filter {@link Message} objects by publisherId.
         *
         * @param publisherId For filtering on the publisherId attribute of a {@link Message}. The
         *                    publisherId of a {@link Message} is the package name of the
         *                    application that published the message. This attribute is
         *                    automatically set by the {@link MessagingManager} before publishing
         *                    the {@link Message}. While setting the attribute the '.' characters in
         *                    the package name are replaced by the '/' characters to facilitate
         *                    hierarchical filtering in accordance with the MQTT specification.
         *                    The filtering expression used should follow the syntax rules
         *                    described in MQTT v.5.0 section 4.7.
         * @return Builder object encapsulating the conditions to filter messages by publisher
         * @throws IllegalArgumentException if the provided expression for filtering is
         *                                  not in accordance with the syntax rules described in
         *                                  MQTT v.5.0 section 4.7.
         */
        public Filter.Builder setPublisherId(@NonNull String publisherId) {
            requireNonNull(publisherId);
            checkFilterTopicSyntax(publisherId);
            this.publisherId = publisherId;
            return this;
        }


        /**
         * Build {@link Filter} object from the builder object.
         *
         * @return {@link Filter} object
         * @throws IllegalArgumentException if neither topic and publisherId attributes are set.
         *                                  For setting these filter attributes, see
         *                                  {@link Builder#setTopic(String)} and
         *                                  {@link Builder#setPublisherId(String)}
         */
        public Filter build() {
            return new Filter(this);
        }
    }
}

