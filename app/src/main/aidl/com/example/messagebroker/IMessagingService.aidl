// IMessagingService.aidl
package com.example.messagebroker;

// import statements for the AIDL interface -> not java
import com.example.messagebroker.Message;
import com.example.messagebroker.IMessageListener;
import com.example.messagebroker.Filter;

// Declare any non-default types here with import statements

interface IMessagingService {
    void publish(in Message message);
    boolean subscribe(in IMessageListener listener, int clientHash, in Filter filter, String applicationPackageName);
    boolean unsubscribe(in int clientHash);
    boolean unsubscribeAll(in int clientHash);
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString);
}