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

/**
 * Status code list.
 *
 * <p>Used as result of some method calls on {@link MessagingManager}.
 *
 * @hide
 */
public enum MessageBrokerResult implements Parcelable {
    /**
     * Request processed successfully.
     */
    OK,
    /**
     * Failed due to unknown error.
     */
    NOT_OK,
    /**
     * Filter successfully added to the hash set. Return true to the user.
     */
    FILTER_ADDED,
    /**
     * Unable to add filter to the hash set. Return false to the user.
     */
    FILTER_NOT_ADDED,
    /**
     * Filter successfully removed from the hash set. Return true to the user.
     */
    FILTER_REMOVED,
    /**
     * Unable to remove filter from the hash set. Return false to the user.
     */
    FILTER_NOT_REMOVED,
    /**
     * All filters are removed.
     */
    ALL_FILTERS_REMOVED,
    /**
     * Null listener found.
     */
    NULL_LISTENER,
    /**
     * Null filter found.
     */
    NULL_FILTER,
    /**
     * Null package name found.
     */
    NULL_PACKAGE_NAME,
    /**
     * Raise an exception. The listener is not subscribed before.
     */
    LISTENER_NOT_SUBSCRIBED,

    /**
     * The port configuration is invalid.
     */
    INVALID_PORT_CONFIGURATION;


    public static final Creator<MessageBrokerResult> CREATOR = new Creator<MessageBrokerResult>() {
        @Override
        public MessageBrokerResult createFromParcel(Parcel in) {
            return parseMessageBrokerResultFrom(in);
        }

        @Override
        public MessageBrokerResult[] newArray(int size) {
            return new MessageBrokerResult[size];
        }
    };

    private static MessageBrokerResult parseMessageBrokerResultFrom(Parcel in) {
        return MessageBrokerResult.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
