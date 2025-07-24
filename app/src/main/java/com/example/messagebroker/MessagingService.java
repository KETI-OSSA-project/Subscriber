package com.example.messagebroker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MessagingService provides MQTT message broker functionality for Android applications.
 *
 * This service implements the IMessagingService AIDL interface and manages:
 * - Message publishing to subscribed clients
 * - Client subscription management
 * - Message routing between publishers and subscribers
 */
public class MessagingService extends Service {

    private static final String TAG = "MessagingService";

    // 클라이언트 구독 정보를 관리하는 맵 (clientHash -> ClientInfo)
    private final Map<Integer, ClientInfo> subscribedClients = new ConcurrentHashMap<>();

    // 모든 발행된 메시지를 저장하는 리스트 (최근 메시지 기록용)
    private final CopyOnWriteArrayList<Message> messageHistory = new CopyOnWriteArrayList<>();

    // 최대 저장할 메시지 수
    private static final int MAX_MESSAGE_HISTORY = 1000;

    /**
     * 클라이언트 정보를 담는 내부 클래스
     */
    private static class ClientInfo {
        final String packageName;
        final long subscribeTime;
        final IMessageListener listener; // 추가: 클라이언트 리스너
        final Filter filter; // 추가: 메시지 필터

        ClientInfo(String packageName, IMessageListener listener, Filter filter) {
            this.packageName = packageName;
            this.listener = listener;
            this.filter = filter;
            this.subscribeTime = System.currentTimeMillis();
        }
    }

    /**
     * AIDL 인터페이스 구현
     */
    private final IMessagingService.Stub binder = new IMessagingService.Stub() {

        @Override
        public void publish(Message message) throws RemoteException {
            if (message == null) {
                Log.w(TAG, "Null message received for publishing");
                return;
            }

            Log.d(TAG, "Publishing message: topic=" + message.getTopic() +
                      ", payload size=" + (message.getPayload() != null ? message.getPayload().length : 0));

            // 메시지 히스토리에 추가
            addToMessageHistory(message);

            // 구독된 모든 클라이언트에게 메시지 전달
            deliverMessageToSubscribers(message);
        }

        @Override
        public boolean subscribe(IMessageListener listener, int clientHash, Filter filter, String applicationPackageName) throws RemoteException {
            if (listener == null || filter == null || applicationPackageName == null) {
                Log.w(TAG, "Null parameter received for subscription");
                return false;
            }

            ClientInfo existingClient = subscribedClients.get(clientHash);
            if (existingClient != null) {
                Log.d(TAG, "Client already subscribed: " + applicationPackageName);
                return false; // 이미 구독 중
            }

            ClientInfo clientInfo = new ClientInfo(applicationPackageName, listener, filter);
            subscribedClients.put(clientHash, clientInfo);

            Log.d(TAG, "Client subscribed: " + applicationPackageName + " (hash: " + clientHash + ")");
            Log.d(TAG, "Filter topic: " + filter.getTopic());
            Log.d(TAG, "Total subscribers: " + subscribedClients.size());

            // 기존 메시지 히스토리를 새 구독자에게 전달 (선택적)
            deliverHistoryToNewSubscriber(clientHash);

            return true;
        }

        @Override
        public boolean unsubscribe(int clientHash) throws RemoteException {
            ClientInfo removedClient = subscribedClients.remove(clientHash);

            if (removedClient != null) {
                Log.d(TAG, "Client unsubscribed: " + removedClient.packageName +
                          " (hash: " + clientHash + ")");
                Log.d(TAG, "Remaining subscribers: " + subscribedClients.size());
                return true;
            } else {
                Log.w(TAG, "Attempted to unsubscribe non-existent client: " + clientHash);
                return false;
            }
        }

        @Override
        public boolean unsubscribeAll(int clientHash) throws RemoteException {
            // 현재 구현에서는 클라이언트당 하나의 구독만 지원하므로 unsubscribe와 동일
            return unsubscribe(clientHash);
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean,
                             float aFloat, double aDouble, String aString) throws RemoteException {
            // AIDL 테스트용 메서드 - 실제 기능 없음
            Log.d(TAG, "basicTypes called with: " + anInt + ", " + aLong + ", " +
                      aBoolean + ", " + aFloat + ", " + aDouble + ", " + aString);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "MessagingService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "MessagingService started");
        return START_STICKY; // 서비스가 종료되면 자동으로 재시작
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MessagingService destroyed");

        // 리소스 정리
        subscribedClients.clear();
        messageHistory.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Client binding to MessagingService");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Client unbinding from MessagingService");
        return super.onUnbind(intent);
    }

    /**
     * 메시지를 히스토리에 추가
     */
    private void addToMessageHistory(Message message) {
        messageHistory.add(message);

        // 최대 크기 초과 시 오래된 메시지 제거
        while (messageHistory.size() > MAX_MESSAGE_HISTORY) {
            messageHistory.remove(0);
        }
    }

    /**
     * 구독된 모든 클라이언트에게 메시지 전달
     */
    private void deliverMessageToSubscribers(Message message) {
        if (subscribedClients.isEmpty()) {
            Log.d(TAG, "No subscribers to deliver message to");
            return;
        }

        Log.d(TAG, "Delivering message to " + subscribedClients.size() + " subscribers");

        for (Map.Entry<Integer, ClientInfo> entry : subscribedClients.entrySet()) {
            ClientInfo clientInfo = entry.getValue();
            try {
                // 필터 조건 확인
                if (matchesFilter(message, clientInfo.filter)) {
                    Log.v(TAG, "Delivering message to client: " + clientInfo.packageName);
                    clientInfo.listener.onMessage(message);
                } else {
                    Log.v(TAG, "Message filtered out for client: " + clientInfo.packageName);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error delivering message to client " + entry.getKey(), e);
                // 연결이 끊어진 클라이언트는 구독 목록에서 제거
                subscribedClients.remove(entry.getKey());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error delivering message to client " + entry.getKey(), e);
            }
        }
    }

    /**
     * 메시지가 필터 조건에 맞는지 확인
     */
    private boolean matchesFilter(Message message, Filter filter) {
        // 토픽 필터링
        String filterTopic = filter.getTopic();
        String messageTopic = message.getTopic();

        if (filterTopic == null || messageTopic == null) {
            return false;
        }

        // 간단한 토픽 매칭 (정확히 일치하거나 와일드카드 지원)
        if (filterTopic.equals(messageTopic) || filterTopic.equals("*")) {
            return true;
        }

        // MQTT 스타일 와일드카드 지원 (+ 및 #)
        return matchesMqttPattern(messageTopic, filterTopic);
    }

    /**
     * MQTT 패턴 매칭 (간단한 구현)
     */
    private boolean matchesMqttPattern(String topic, String pattern) {
        // # 와일드카드: 모든 하위 토픽 매칭
        if (pattern.endsWith("#")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return topic.startsWith(prefix);
        }

        // + 와일드카드: 단일 레벨 매칭
        if (pattern.contains("+")) {
            String[] patternParts = pattern.split("/");
            String[] topicParts = topic.split("/");

            if (patternParts.length != topicParts.length) {
                return false;
            }

            for (int i = 0; i < patternParts.length; i++) {
                if (!patternParts[i].equals("+") && !patternParts[i].equals(topicParts[i])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * 새 구독자에게 메시지 히스토리 전달 (선택적 기능) -> 아직 구현은 안되어있음
     */
    private void deliverHistoryToNewSubscriber(int clientHash) {
        if (messageHistory.isEmpty()) {
            return;
        }

        ClientInfo clientInfo = subscribedClients.get(clientHash);
        if (clientInfo == null) {
            return;
        }

        Log.d(TAG, "Delivering " + messageHistory.size() +
                  " historical messages to new subscriber: " + clientInfo.packageName);

        // 실제 구현에서는 여기서 히스토리 메시지들을 전달
        // 현재는 로깅만 수행
    }

    /**
     * 현재 구독자 수 반환 (디버깅용)
     */
//    public int getSubscriberCount() {
//        return subscribedClients.size();
//    }

    /**
     * 메시지 히스토리 크기 반환 (디버깅용)
     */
//    public int getMessageHistorySize() {
//        return messageHistory.size();
//    }
}
