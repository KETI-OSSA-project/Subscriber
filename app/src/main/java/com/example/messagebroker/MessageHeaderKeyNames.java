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

package com.example.messagebroker;

/**
 * MessageHeaderKeyNames.
 *
 * <p>Class for holding key names for the header content in the {@link Message} class. The key names
 * are also used by the {@link Filter} class to filter the {@link Message} objects on their header
 * content.
 *
 * @hide
 */
public class MessageHeaderKeyNames {
    public static final String TOPIC = "topic";
    public static final String PUBLISHER_ID = "publisherId";
    public static final String DESTINATION_IDS = "destinationIds";

    private MessageHeaderKeyNames() {
    }
}
