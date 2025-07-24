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

/**
 * Constants for Message Broker and Messaging Access Control.
 * 메시지 브로커 및 메시징 접근 제어에 사용되는 상수들을 정의한 자바 클래스
 * @hide
 */
public class Constants {

    /**
     * Messaging Service package.
     */
    public static final String MESSAGE_BROKER_PACKAGE
            = "com.example.messagebroker";
    /**
     * Service to bind to in order to use the messaging service.
     */
    public static final String MESSAGING_SERVICE
            = ".MessagingService";
    /**
     * Permission needed to manage the messaging access control.
     */
    public static final String MESSAGING_ACCESS_CONTROL_MANAGE
            = "com.example.permission.MESSAGING_ACCESS_CONTROL_MANAGE";
    /**
     * Permission needed to query the messaging access control.
     */
    public static final String MESSAGING_ACCESS_CONTROL_QUERY
            = "com.example.permission.MESSAGING_ACCESS_CONTROL_QUERY";

    /**
     * Permission needed to publish message.
     */
    public static final String MESSAGE_PUBLISH_PERMISSION
            = "com.example.permission.MESSAGE_PUBLISH";

    /**
     * Permission needed to subscribe.
     */
    public static final String MESSAGE_SUBSCRIBE_PERMISSION
            = "com.example.permission.MESSAGE_SUBSCRIBE";

    /**
     * Permission needed to create & use message ports.
     */
    public static final String USE_MESSAGE_PORTS_PERMISSION
            = "com.example.permission.USE_MESSAGE_PORTS";

    /**
     * Permission needed to get message ports info.
     */
    public static final String GET_MESSAGE_PORTS_INFO_PERMISSION
            = "com.example.permission.GET_MESSAGE_PORTS_INFO";


    /**
     * Permission needed to stitch message ports.
     */
    public static final String STITCH_MESSAGE_PORTS_PERMISSION
            = "com.example.permission.STITCH_MESSAGE_PORTS";

    /**
     * Permission needed to use message port groups.
     */
    public static final String USE_MESSAGE_PORT_GROUPS_PERMISSION
            = "com.example.permission.USE_MESSAGE_PORT_GROUPS";

    /**
     * Permission needed to get message port groups info.
     */
    public static final String GET_MESSAGE_PORT_GROUPS_INFO_PERMISSION
            = "com.example.permission.GET_MESSAGE_PORT_GROUPS_INFO";

    /**
     * Name of universal group principal.
     *
     * @hide
     */
    public static final String UNIVERSAL_GROUP_NAME
            = "com.example.messagebroker.messagingaccesscontrol.universalgroup";

    /**
     * Name of Message Ports configuration file.
     *
     * @hide
     */
    public static final String MESSAGING_MANIFEST_FILE_NAME
            = "MessagingManifest.xml";

    private Constants() {
    }
}

