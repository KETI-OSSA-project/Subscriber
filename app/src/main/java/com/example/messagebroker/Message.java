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

import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.example.messagebroker.MqttTopicUtils.checkMessageTopicSyntax;
import static java.util.Objects.requireNonNull;

/**
 * Message.
 *
 * <p>Container class encapsulating messages for enabling communication between different clients
 * using the message broker service. An app can publish {@link Message} objects to the the
 * message broker service using the method {@link MessagingManager#publish(Message)}. Any client
 * registered with the message broker service can receive these messages, provided the
 * {@link Message} satisfies the subscription filter rules set by the client.
 */
public class Message implements Parcelable {

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    // Unique identifier of the message.
    private final UUID messageId;
    private final byte[] payload;
    private final String responseTopic;
    private final byte[] correlationData;
    private final Bundle bundle;
    private final Instant timeStamp;

    private Message(Builder builder) {
        this.messageId = UUID.randomUUID();
        this.payload = builder.payload;
        this.responseTopic = builder.responseTopic;
        this.correlationData = builder.correlationData;
        this.timeStamp = builder.timeStamp;
        bundle = new Bundle();
        bundle.putString(MessageHeaderKeyNames.TOPIC, builder.topic);
        if (builder.destinationIds != null) {
            bundle.putStringArrayList(
                    MessageHeaderKeyNames.DESTINATION_IDS,
                    new ArrayList<>(builder.destinationIds)
            );
        }
    }

    //  Message 클래스의 생성자 중 하나로, 안드로이드의 Parcel 객체에서 데이터를 읽어와 Message 객체를 복원하는 역할
    private Message(Parcel in) {
        messageId = (UUID) in.readSerializable();
        bundle = in.readBundle(getClass().getClassLoader());
        payload = in.createByteArray();
        responseTopic = in.readString();
        correlationData = in.createByteArray();
        // timeStamp = Instant.ofEpochMilli(in.readLong());
        timeStamp = null;
    }

    //   Message 객체를 Parcel 형태로 변환해 IPC(프로세스 간 통신)나 인텐트로 전달할 수 있게 하는 메서드
   @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(messageId);
        dest.writeBundle(bundle);
        dest.writeByteArray(payload);
        dest.writeString(responseTopic);
        dest.writeByteArray(correlationData);
        // dest.writeLong(timeStamp.toEpochMilli());
        // dest.writeLong(timeStamp != null ? timeStamp.toEpochMilli() : 0L);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Get the identifier of the message.
     *
     * @return unique identifier of the message
     */
//    public UUID getMessageId() {
//        return messageId;
//    }

    /**
     * Get the topic the message belongs to.
     *
     * @return topic of the message
     */

    // getTopic: 읽기 역할
    public String getTopic() {
        return bundle.getString(MessageHeaderKeyNames.TOPIC);
    }

    /**
     * Get the response topic of the message.
     *
     * @return The response topic that represents the topic on which the responses from the
     * receivers of the message are expected.
     */
//    public String getResponseTopic() {
//        return responseTopic;
//    }

    /**
     * Get the correlation data encapsulated in the message.
     * Correlation data makes it possible for the publisher of the message to handle
     * asynchronous responses that can possibly be sent from multiple receivers.
     *
     * @return The correlation data that can be used for identifying to which specific message a
     * response that is received later relates.
     */
//    public byte[] getCorrelationData() {
//        return correlationData;
//    }

    /**
     * Get the publisher id of the message. The publisherId is the package name of the sender
     * application. This attribute is automatically set by the {@link MessagingManager} before
     * publishing the {@link Message} object. While setting the attribute the '.' characters in the
     * package name are replaced by the '/' characters to facilitate hierarchical filtering in
     * accordance with the MQTT specification. For more information on filtering of
     * {@link Message} objects refer {@link Filter} API documentation.
     *
     * @return publisher id of the message
     */
//    public String getPublisherId() {
//        return bundle.getString(MessageHeaderKeyNames.PUBLISHER_ID);
//    }

    /**
     * Sets the publisherId of the message. Used only internally in the
     * {@link MessagingManager#publish(Message)} before transferring the {@link Message} over the
     * binder.
     *
     * @param publisherId package name derived from the application context passed during the
     *                    initialization of the {@link MessagingManager}.
     * @hide
     */
//    void setPublisherId(String publisherId) {
//        bundle.putString(MessageHeaderKeyNames.PUBLISHER_ID, publisherId);
//    }

    /**
     * Get the set of destination identifiers the message is directed to.
     *
     * @return destination identifiers of the message, or empty set if there are no
     * identifiers
     */
//    public Set<String> getDestinationIds() {
//        ArrayList<String> destinationIds =
//                bundle.getStringArrayList(MessageHeaderKeyNames.DESTINATION_IDS);
//        if (destinationIds == null) {
//            return Collections.emptySet();
//        }
//        return new HashSet<>(destinationIds);
//    }

    /**
     * Get the payload encapsulated in the message.
     *
     * @return payload of the message -> 읽기 역할
     */
    public byte[] getPayload() {
        return payload;
    }
    /**
     * Get the time stamp of the message.
     *
     * <p>Time stamp is useful for capturing the time of a certain event to which the metadata of
     * the message belongs to. Populated with the time when the message object is built
     * ({@link Builder#build()}).
     *
     * @return Instant representing the time stamp of the message
     */
//    public Instant getTimeStamp() {
//        return timeStamp;
//    }

    /**
     * Get the bundle containing the header content of the message.
     *
     * @return bundle containing the header content of the message
     * @hide
     */
//    public Bundle getBundle() {
//        return bundle;
//    }

    public static class Builder {
        private final String topic;
        private String responseTopic;
        private byte[] correlationData = null;
        private byte[] payload = null;
        private HashSet<String> destinationIds = null;
        private Instant timeStamp;

        /**
         * Constructor.
         *
         * <p>Constructor for creating a Builder object which can be used to build a
         * {@link Message} object. To create a {@link Message} object using the {@link Builder}
         * object, {@link Builder#build()} should be called.
         *
         * @param topic The topic is used to identify the content of the message.
         *              It is not interpreted by the message broker service.
         *              It is only used for filtering  purposes for subscribers.
         *              Subscribers need to use this field to recognize the message being
         *              received. Message broker service also publishes messages about various
         *              events on the service and device. These messages from the message broker
         *              service are identified via a reserved namespace and clients can subscribe to
         *              these topics for updates on the aforementioned events. The topic name
         *              should follow the topic name syntax rules described in MQTT v.5.0 section
         *              4.7. Failure of this would result in an {@link IllegalArgumentException}.
         * @throws IllegalArgumentException if the topic name does not follow the topic name
         *                                  syntax rules described in MQTT v.5.0 section 4.7.
         **/
        public Builder(@NonNull String topic) {
            requireNonNull(topic);
            checkMessageTopicSyntax(topic);
            this.topic = topic;
        }

        /**
         * Optional method for setting the response topic of the message.
         *
         * @param responseTopic The response topic represents the topic on which the
         *                      responses from the receivers of the message are expected.
         * @return Builder object encapsulating the response topic of the message
         */
//        public Builder setResponseTopic(@NonNull String responseTopic) {
//            requireNonNull(responseTopic);
//            checkMessageTopicSyntax(responseTopic);
//            this.responseTopic = responseTopic;
//            return this;
//        }

        /**
         * Optional method for setting the correlation data of the message.
         *
         * @param correlationData Byte-array containing the correlation data of the message.
         *                        Correlation data is optional binary data that follows the
         *                        response topic. The publisher can use the data for identifying to
         *                        which specific message a response that is received later relates.
         * @return Builder object encapsulating the correlationData of the message
         */
//        public Builder setCorrelationData(@NonNull byte[] correlationData) {
//            requireNonNull(correlationData);
//            this.correlationData = correlationData;
//            return this;
//        }

        /**
         * Optional method for setting the payload of the message.
         *
         * @param payload Byte-array containing the payload of the message. It is passed
         *                through unmodified and not interpreted by the message broker
         *                service, except if it is a message targeted to the service itself.
         *                Visit {@link android.os.TransactionTooLargeException} documentation for
         *                the supported maximum payload size.
         * @return Builder object encapsulating the payload of the message
         */
        // 쓰기 전용
        public Builder setPayload(@NonNull byte[] payload) {
            requireNonNull(payload);
            this.payload = payload;
            return this;    // 자기 자신을 반환 -> 메서드 체이닝
        }

        /**
         * Optional method to set time stamp of the message. Useful for capturing the time of a
         * certain event to which the metadata of the message belongs to.
         * Populated with the time when the message object is built ({@link #build()}).
         * <h4> Example usage to capture frame timestamp in
         * {@link com.securityandsafetythings.messagebroker.Message}:</h4>
         * <pre><code>
         * // Get {@link android.media.Image} from video-pipeline by extending
         * // {@link com.securityandsafetythings.app.VideoService} and implementing
         * // {onImageAvailable(ImageReader)} method.
         *
         * // Following this the timeStamp of the image can be obtained using the method
         * {@link Image#getTimestamp()}.
         * long timeStampLong = image.getTimestamp();
         * // The frame timeStamp can be converted into {@link Instant} using the method
         * // {@link
         * com.securityandsafetythings.video.VideoSession#convertTimestampToInstant(long)}
         * Instant timeStamp = videoSession.convertTimestampToInstant(timeStampLong);
         *
         * byte[] payload; // Payload encapsulating the analytics metadata of the frame.
         * // Build {@link com.securityandsafetythings.messagebroker.Message}
         * object and set timeStamp value.
         * Message message = new Message.Builder(topic)
         *                     .setTimeStamp(timeStamp)
         *                     .setPayload(payload).build();
         * </code></pre>
         *
         * @param timeStamp Instant representing the timeStamp of the message.
         * @return Builder object encapsulating the payload of the message
         */
//        public Builder setTimeStamp(final @NonNull Instant timeStamp) {
//            requireNonNull(timeStamp);
//            this.timeStamp = timeStamp;
//            return this;
//        }

        /**
         * Build {@link Message} object from the builder object.
         *
         * @return {@link Message} object
         */
        public Message build() {
            if (timeStamp == null) {
                // timeStamp = Instant.now();
            }
            return new Message(this);
        }
    }
}

