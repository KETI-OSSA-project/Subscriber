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
 * Constants for the message broker service
 */
public class Constants {

    // 현재 앱의 패키지 이름 (MessagingService가 포함된 패키지)
    public static final String MESSAGE_BROKER_PACKAGE = "com.example.subscriber";

    // MessagingService 클래스 이름
    public static final String MESSAGING_SERVICE = ".MessagingService";

    // 권한 정의
    public static final String MESSAGE_PUBLISH_PERMISSION = "com.example.permission.MESSAGE_PUBLISH";
    public static final String MESSAGE_SUBSCRIBE_PERMISSION = "com.example.permission.MESSAGE_SUBSCRIBE";
    // public static final String USE_MESSAGE_PORTS_PERMISSION = "com.example.permission.USE_MESSAGE_PORTS";
    // public static final String USE_MESSAGE_PORT_GROUPS_PERMISSION = "com.example.permission.USE_MESSAGE_PORT_GROUPS";

    private Constants() {
        // Utility class - no instantiation
    }
}
