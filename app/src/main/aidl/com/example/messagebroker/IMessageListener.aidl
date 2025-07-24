// IMessageListener.aidl
package com.example.messagebroker;

// 메시지 수신 콜백 인터페이스
import com.example.messagebroker.Message;

interface IMessageListener {
    /**
     * 메시지가 도착했을 때 호출되는 콜백
     * @param message 수신된 메시지 객체
     */
    void onMessage(in Message message);
}