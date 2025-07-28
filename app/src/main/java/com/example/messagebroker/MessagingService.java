package com.example.messagebroker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * MessagingService provides standard MQTT subscriber functionality for Android applications.
 * 이 서비스는 표준 MQTT 클라이언트로서 특정 토픽을 구독하고 메시지를 수신합니다.
 *
 * This service implements the IMessagingService AIDL interface and manages:
 * - Subscription to specific MQTT topics
 * - Message reception from MQTT broker
 * - Message delivery to subscribed clients
 */
public class MessagingService extends Service {

    private static final String TAG = "MessagingService";
    private Mqtt3AsyncClient mqttClient;

    // 클라이언트 구독 정보를 관리하는 맵 (clientHash -> ClientInfo)
    private final Map<Integer, ClientInfo> subscribedClients = new ConcurrentHashMap<>();

    // 현재 구독 중인 MQTT 토픽들
    private final Set<String> subscribedMqttTopics = new CopyOnWriteArraySet<>();

    /**
     * 클라이언트 정보를 담는 내부 클래스
     */
    private static class ClientInfo {
        final String packageName;
        final IMessageListener listener;
        final Filter filter;

        ClientInfo(String packageName, IMessageListener listener, Filter filter) {
            this.packageName = packageName;
            this.listener = listener;
            this.filter = filter;
        }
    }

    /**
     * AIDL 인터페이스 구현
     */
    private final IMessagingService.Stub binder = new IMessagingService.Stub() {

        @Override
        public boolean subscribe(IMessageListener listener, int clientHash, Filter filter, String applicationPackageName) throws RemoteException {
            Log.d(TAG, "구독 요청 수신: " + applicationPackageName);

            if (applicationPackageName == null || listener == null) {
                Log.e(TAG, "잘못된 구독 파라미터");
                return false;
            }

            // 중복 구독 체크
            if (subscribedClients.containsKey(clientHash)) {
                Log.d(TAG, "이미 구독 중인 클라이언트: " + applicationPackageName);
                return false;
            }

            // 클라이언트 정보 저장
            ClientInfo clientInfo = new ClientInfo(applicationPackageName, listener, filter);
            subscribedClients.put(clientHash, clientInfo);

            Log.d(TAG, "클라이언트 구독 완료: " + applicationPackageName + " (hash: " + clientHash + ")");
            Log.d(TAG, "총 구독자 수: " + subscribedClients.size());

            // 필터에서 토픽 추출하여 MQTT 구독
            subscribeToTopicFromFilter(filter);

            return true;
        }

        @Override
        public boolean unsubscribe(int clientHash) throws RemoteException {
            ClientInfo removedClient = subscribedClients.remove(clientHash);

            if (removedClient != null) {
                Log.d(TAG, "클라이언트 구독 해제: " + removedClient.packageName +
                          " (hash: " + clientHash + ")");
                Log.d(TAG, "남은 구독자 수: " + subscribedClients.size());

                // 필요 시 MQTT 토픽 구독 해제 (다른 클라이언트가 사용하지 않는 경우)
                updateMqttTopicSubscriptions();

                return true;
            } else {
                Log.w(TAG, "존재하지 않는 클라이언트 구독 해제 시도: " + clientHash);
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
            // AIDL 테스트용 메서드
            Log.d(TAG, "basicTypes 호출됨");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "🚀 MessagingService 시작");

        // MQTT 클라이언트 초기화
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("android-subscriber-001")
                .serverHost("192.168.0.8") // MQTT 브로커 주소
                .serverPort(1883)
                .buildAsync();

        // MQTT 브로커에 연결
        connectToMqttBroker();
    }

    /**
     * MQTT 브로커에 연결
     */
    private void connectToMqttBroker() {
        if (mqttClient == null) {
            Log.e(TAG, "MQTT 클라이언트가 초기화되지 않았습니다.");
            return;
        }

        Log.d(TAG, "🔗 MQTT 브로커 연결 시도 중...");

        mqttClient.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable == null) {
                        Log.i(TAG, "✅ MQTT 브로커 연결 성공!");
                    } else {
                        Log.e(TAG, "❌ MQTT 연결 실패: " + throwable.getMessage());
                    }
                });
    }

    /**
     * 필터에서 토픽을 추출하여 MQTT 토픽 구독
     */
    private void subscribeToTopicFromFilter(Filter filter) {
        if (filter == null) {
            Log.w(TAG, "필터가 null입니다.");
            return;
        }

        String topic = filter.getTopic();
        if (topic != null && !topic.isEmpty()) {
            subscribeToMqttTopic(topic);
        } else {
            Log.w(TAG, "필터에서 유효한 토픽을 찾을 수 없습니다.");
        }
    }

    /**
     * MQTT 토픽 구독
     */
    private void subscribeToMqttTopic(String topic) {
        if (mqttClient == null || mqttClient.getState() != MqttClientState.CONNECTED) {
            Log.w(TAG, "MQTT 클라이언트가 연결되지 않았습니다.");
            return;
        }

        if (subscribedMqttTopics.contains(topic)) {
            Log.d(TAG, "이미 구독 중인 토픽: " + topic);
            return;
        }

        mqttClient.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    Optional<ByteBuffer> payloadOptional = publish.getPayload();
                    byte[] payload = payloadOptional.map(byteBuffer -> {
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        return bytes;
                    }).orElse(new byte[0]);

                    String topicName = publish.getTopic().toString();

                    Log.d(TAG, "📥 [" + topicName + "] 메시지 수신: " + new String(payload));

                    // Message 객체 생성
                    Message message = createMessageFromMqttPayload(topicName, payload);

                    // 구독된 클라이언트들에게 메시지 전달
                    deliverMessageToSubscribers(message);
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable == null) {
                        subscribedMqttTopics.add(topic);
                        Log.i(TAG, "📡 '" + topic + "' 토픽 구독 시작");
                    } else {
                        Log.e(TAG, "❌ '" + topic + "' 구독 실패: " + throwable.getMessage());
                    }
                });
    }

    /**
     * MQTT 페이로드로부터 Message 객체 생성
     */
    private Message createMessageFromMqttPayload(String topic, byte[] payload) {
        try {
            return new Message.Builder(topic)
                    .setPayload(payload)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Message 객체 생성 실패", e);
            return new Message.Builder(topic)
                    .setPayload(payload != null ? payload : new byte[0])
                    .build();
        }
    }

    /**
     * 현재 구독자들이 필요로 하는 토픽들만 유지하도록 MQTT 구독 업데이트
     */
    private void updateMqttTopicSubscriptions() {
        // 현재 필요한 토픽들 수집
        Set<String> requiredTopics = new CopyOnWriteArraySet<>();
        for (ClientInfo clientInfo : subscribedClients.values()) {
            String topic = clientInfo.filter.getTopic();
            if (topic != null && !topic.isEmpty()) {
                requiredTopics.add(topic);
            }
        }

        // 불필요한 토픽 구독 해제
        for (String subscribedTopic : subscribedMqttTopics) {
            if (!requiredTopics.contains(subscribedTopic)) {
                unsubscribeFromMqttTopic(subscribedTopic);
            }
        }

        Log.d(TAG, "MQTT 토픽 구독 업데이트 완료");
    }

    /**
     * MQTT 토픽 구독 해제
     */
    private void unsubscribeFromMqttTopic(String topic) {
        if (mqttClient == null || mqttClient.getState() != MqttClientState.CONNECTED) {
            return;
        }

        mqttClient.unsubscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete((unsuback, throwable) -> {
                    if (throwable == null) {
                        subscribedMqttTopics.remove(topic);
                        Log.i(TAG, "📡 '" + topic + "' 토픽 구독 해제");
                    } else {
                        Log.e(TAG, "❌ '" + topic + "' 구독 해제 실패: " + throwable.getMessage());
                    }
                });
    }

    /**
     * 구독된 모든 클라이언트에게 메시지 전달
     */
    private void deliverMessageToSubscribers(Message message) {
        if (subscribedClients.isEmpty()) {
            Log.d(TAG, "구독자가 없어 메시지 전달을 건너뜁니다.");
            return;
        }

        Log.d(TAG, "📤 " + subscribedClients.size() + "명의 구독자에게 메시지 전달 중");

        for (Map.Entry<Integer, ClientInfo> entry : subscribedClients.entrySet()) {
            ClientInfo clientInfo = entry.getValue();
            try {
                // 간단한 토픽 매칭만 수행 (브로커가 이미 필터링했음)
                if (isTopicMatch(message.getTopic(), clientInfo.filter.getTopic())) {
                    clientInfo.listener.onMessage(message);
                    Log.v(TAG, "✅ 메시지 전달 완료: " + clientInfo.packageName);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "❌ 클라이언트에게 메시지 전달 실패: " + clientInfo.packageName, e);
                // 연결이 끊어진 클라이언트는 제거
                subscribedClients.remove(entry.getKey());
            } catch (Exception e) {
                Log.e(TAG, "메시지 전달 중 예외 발생", e);
            }
        }
    }

    /**
     * 간단한 토픽 매칭 (정확한 매칭만)
     */
    private boolean isTopicMatch(String messageTopic, String filterTopic) {
        if (messageTopic == null || filterTopic == null) {
            return false;
        }
        return messageTopic.equals(filterTopic);
    }

    /**
     * 현재 구독자 수 반환 (디버깅용)
     */
    public int getSubscriberCount() {
        return subscribedClients.size();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "MessagingService started");

        try {
            // Foreground 서비스로 실행하여 우선순위 높이기
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                String channelId = "messaging_service_channel";

                // NotificationManager 가져오기
                android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
                if (manager != null) {
                    // 채널이 이미 존재하는지 확인
                    if (manager.getNotificationChannel(channelId) == null) {
                        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                            channelId, "Messaging Service", android.app.NotificationManager.IMPORTANCE_LOW);
                        channel.setDescription("MQTT 메시징 서비스");
                        manager.createNotificationChannel(channel);
                        Log.d(TAG, "알림 채널 생성됨");
                    }
                }

                android.app.Notification notification = new android.app.Notification.Builder(this, channelId)
                    .setContentTitle("MQTT 메시징 서비스")
                    .setContentText("MQTT 브로커로부터 메시지 수신 중...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();

                startForeground(1, notification);
                Log.i(TAG, "🔥 Foreground 서비스로 실행됨 - 메시지 수신 안정성 향상");
            }
        } catch (Exception e) {
            Log.e(TAG, "Foreground 서비스 시작 중 에러", e);
            // Foreground 서비스 실패해도 일반 서비스로는 계속 실행
        }

        return START_STICKY; // 서비스가 종료되면 자동으로 재시작
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MessagingService destroyed");

        // MQTT 연결 해제
        if (mqttClient != null && mqttClient.getState() == MqttClientState.CONNECTED) {
            mqttClient.disconnect()
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            Log.i(TAG, "MQTT 연결 정상 해제됨");
                        } else {
                            Log.e(TAG, "MQTT 연결 해제 중 오류", throwable);
                        }
                    });
        }

        // 리소스 정리
        subscribedClients.clear();
        subscribedMqttTopics.clear();
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
}
