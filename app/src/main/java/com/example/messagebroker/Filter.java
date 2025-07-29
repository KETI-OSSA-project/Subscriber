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

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.Objects;

import static com.example.messagebroker.MqttTopicUtils.checkFilterTopicSyntax;
import static java.util.Objects.requireNonNull;

/**
 * Filter class for MQTT topic subscription.
 *
 * Simplified version that only handles MQTT topic filtering for standard subscriber functionality.
 */
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

    // MQTT 토픽만 저장 (Publisher ID 제거)
    private final String topic;

    private Filter(Builder builder) {
        if (builder.topic == null || builder.topic.isEmpty()) {
            throw new IllegalArgumentException("Topic is required for Filter");
        }
        this.topic = builder.topic;
    }

    /**
     * Constructor for creating a filter object with only topic.
     *
     * @param topic MQTT topic for subscription
     * @throws IllegalArgumentException if topic is null or empty
     */
    public Filter(@NonNull String topic) {
        requireNonNull(topic, "Topic cannot be null");
        if (topic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        checkFilterTopicSyntax(topic);
        this.topic = topic;
    }

    protected Filter(Parcel in) {
        topic = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(topic);
    }

    /**
     * Get the topic filter string.
     *
     * @return The topic filter
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Simple topic matching for exact match.
     * 브로커가 이미 필터링을 수행하므로 간단한 매칭만 제공합니다.
     *
     * @param messageTopic The topic of the received message
     * @return true if the topic matches exactly
     */
    public boolean matches(String messageTopic) {
        if (messageTopic == null) {
            return false;
        }
        return topic.equals(messageTopic);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Filter)) {
            return false;
        }
        Filter that = (Filter) obj;
        return Objects.equals(this.topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    @Override
    public String toString() {
        return "Filter{topic='" + topic + "'}";
    }

    /**
     * Builder class for creating Filter objects.
     */
    public static class Builder {
        private String topic;

        /**
         * Set the topic filter expression for MQTT subscription.
         *
         * @param topic MQTT topic for subscription.
         *              The filtering expression should follow MQTT topic syntax rules.
         * @return Builder object for method chaining
         * @throws IllegalArgumentException if the topic syntax is invalid
         */
        public Filter.Builder setTopic(@NonNull String topic) {
            requireNonNull(topic, "Topic cannot be null");
            checkFilterTopicSyntax(topic);
            this.topic = topic;
            return this;
        }

        /**
         * Build the Filter object.
         *
         * @return Filter object with the specified topic
         * @throws IllegalArgumentException if topic is not set
         */
        public Filter build() {
            return new Filter(this);
        }
    }
}
