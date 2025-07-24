// IMessagingServiceConnectionListener.aidl
package com.example.messagebroker;

// Declare any non-default types here with import statements

/**
 * Callback interface for MessagingService connection state changes.
 * Used to notify clients when the service is connected or disconnected.
 -> 프로세스 간 통신(IPC)에서 서비스 연결 상태를 모니터링하기 위한 인터페이스입니다.
 */
interface IMessagingServiceConnectionListener {
    /**
     * Called when the MessagingService is successfully connected and ready to use.
     */
    void onConnected();

    /**
     * Called when the MessagingService is disconnected.
     * This can happen due to service crashes, unbinding, or other connection issues.
     */
    void onDisconnected();
}