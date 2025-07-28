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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * MessagingService provides MQTT message broker functionality for Android applications.
 * 이 서비스는 MQTT 서버로부터 메시지를 수신하고 구독된 클라이언트들에게 전달합니다.
 *
 * This service implements the IMessagingService AIDL interface and manages:
 * - Message subscription from MQTT broker
 * - Client subscription management with filters
 * - Message routing from MQTT broker to subscribed clients
 */
public class MessagingService extends Service {

    private static final String TAG = "MessagingService";
    private Mqtt3AsyncClient mqttClient;

    // 클라이언트 구독 정보를 관리하는 맵 (clientHash -> ClientInfo)
    private final Map<Integer, ClientInfo> subscribedClients = new ConcurrentHashMap<>();

    // MQTT 토픽 구독 관리
    private final Set<String> subscribedMqttTopics = new CopyOnWriteArraySet<>();

    // 모든 수신된 메시지를 저장하는 리스트 (최근 메시지 기록용)
    private final CopyOnWriteArrayList<Message> messageHistory = new CopyOnWriteArrayList<>();

    // 최대 저장할 메시지 수
    private static final int MAX_MESSAGE_HISTORY = 1000;

    /**
     * 클라이언트 정보를 담는 내부 클래스
     */
    private static class ClientInfo {
        final String packageName;
        final long subscribeTime;
        final IMessageListener listener; // 클라이언트 메시지 리스너
        final Filter filter; // 메시지 필터

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

        // publish는 subscriber에서는 사용하지 않지만 인터페이스 호환성을 위해 유지
        @Override
        public void publish(Message message) throws RemoteException {
            Log.w(TAG, "publish() 호출됨 - subscriber 서비스에서는 지원하지 않습니다.");
        }

        @Override
        public boolean subscribe(IMessageListener listener, int clientHash, Filter filter, String packageName) throws RemoteException {
            if (packageName == null || listener == null) {
                Log.w(TAG, "Null package name or listener for subscription");
                return false;
            }

            ClientInfo existingClient = subscribedClients.get(clientHash);
            if (existingClient != null) {
                Log.d(TAG, "Client already subscribed: " + packageName);
                return false; // 이미 구독 중
            }

            ClientInfo clientInfo = new ClientInfo(packageName, listener, filter);
            subscribedClients.put(clientHash, clientInfo);

            Log.d(TAG, "Client subscribed: " + packageName + " (hash: " + clientHash + ")");
            Log.d(TAG, "Total subscribers: " + subscribedClients.size());

            // 필터에 따른 MQTT 토픽 구독 업데이트
            updateMqttTopicSubscriptions();

            // 기존 메시지 히스토리를 새 구독자에게 전달
            deliverHistoryToNewSubscriber(clientInfo);

            return true;
        }

        @Override
        public boolean unsubscribe(int clientHash) throws RemoteException {
            ClientInfo removedClient = subscribedClients.remove(clientHash);

            if (removedClient != null) {
                Log.d(TAG, "Client unsubscribed: " + removedClient.packageName +
                          " (hash: " + clientHash + ")");
                Log.d(TAG, "Remaining subscribers: " + subscribedClients.size());

                // MQTT 토픽 구독 업데이트
                updateMqttTopicSubscriptions();

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
        Log.i(TAG, "🚀 MessagingService onCreate() 진입"); // 서비스 실행 확인용

        // MQTT 클라이언트 초기화
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("android-subscriber-001")
                .serverHost("192.168.0.8") // MQTT 브로커 주소
                .serverPort(1883)
                .buildAsync();

        Log.i(TAG, "MQTT 클라이언트 생성됨");

        connectToMqttBroker();

        Log.i(TAG, "🔗 MessagingService 바인딩 준비 완료");
    }

    /**
     * MQTT 브로커에 연결
     */
    private void connectToMqttBroker() {
        mqttClient.connectWith()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable == null) {
                        Log.i(TAG, "✅ MQTT 브로커 연결 성공");
                        // 연결 성공 시 기본 토픽들을 구독
                        subscribeToDefaultTopics();
                    } else {
                        Log.e(TAG, "❌ MQTT 연결 실패: " + throwable.getMessage());
                    }
                });
    }

    /**
     * 기본 토픽들을 구독 (예: "sensor/#", "device/#" 등)
     */
    private void subscribeToDefaultTopics() {
        // 기본적으로 모든 토픽을 구독 (와일드카드 사용)
        subscribeToMqttTopic("#");
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

                    Log.d(TAG, "📥 [" + topicName + "] MQTT 메시지 수신: " + new String(payload));

                    // Message 객체 생성
                    Message message = createMessageFromMqttPayload(topicName, payload);

                    // 메시지 히스토리에 추가
                    addToMessageHistory(message);

                    // 구독된 클라이언트들에게 메시지 전달
                    deliverMessageToSubscribers(message);
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable == null) {
                        subscribedMqttTopics.add(topic);
                        Log.i(TAG, "📡 '" + topic + "' MQTT 토픽 구독 시작");
                    } else {
                        Log.e(TAG, "❌ '" + topic + "' MQTT 구독 실패: " + throwable.getMessage());
                    }
                });
    }

    /**
     * MQTT 페이로드로부터 Message 객체 생성
     */
    private Message createMessageFromMqttPayload(String topic, byte[] payload) {
        try {
            Message.Builder builder = new Message.Builder(topic)
                    // .setTopic(topic)
                    .setPayload(payload);

            return builder.build();     // build 작성
        } catch (Exception e) {
            Log.e(TAG, "Message 객체 생성 실패", e);
            // 기본 Message 객체 반환
            return new Message.Builder(topic)
                    //.setTopic(topic) -> setTpoic은 Filter.Builder에서만 사용
                    .setPayload(payload != null ? payload : new byte[0])
                    .build();
        }
    }

    /**
     * 클라이언트 필터에 따라 MQTT 토픽 구독 업데이트
     */
    private void updateMqttTopicSubscriptions() {
        // 현재는 모든 토픽을 구독하고 있으므로 별도 업데이트 불필요
        // 실제 구현에서는 클라이언트 필터에 따라 특정 토픽만 구독할 수 있음
        Log.d(TAG, "MQTT 토픽 구독 상태 업데이트 완료");
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
            Log.d(TAG, "구독자가 없어 메시지 전달을 건너뜁니다.");
            return;
        }

        Log.d(TAG, "📤 " + subscribedClients.size() + "명의 구독자에게 메시지 전달 중");

        for (Map.Entry<Integer, ClientInfo> entry : subscribedClients.entrySet()) {
            ClientInfo clientInfo = entry.getValue();
            try {
                // 필터 조건 확인
                if (shouldDeliverMessage(message, clientInfo.filter)) {
                    // IMessageListener 콜백을 통해 메시지 전달
                    clientInfo.listener.onMessage(message);
                    Log.v(TAG, "✅ 메시지 전달 완료: " + clientInfo.packageName);
                } else {
                    Log.v(TAG, "🔍 필터 조건에 맞지 않아 메시지 전달 건너뜀: " + clientInfo.packageName);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "❌ 클라이언트에게 메시지 전달 실패: " + clientInfo.packageName, e);
                // 연결이 끊어진 클라이언트는 제거
                subscribedClients.remove(entry.getKey());
            } catch (Exception e) {
                Log.e(TAG, "❌ 메시지 전달 중 예외 발생: " + clientInfo.packageName, e);
            }
        }
    }

    /**
     * 필터 조건에 따라 메시지 전달 여부 결정
     */
    private boolean shouldDeliverMessage(Message message, Filter filter) {
        if (filter == null) {
            return true; // 필터가 없으면 모든 메시지 전달
        }

        try {
            // Filter 클래스의 메서드를 사용해 필터링 로직 구현
            // 실제 구현은 Filter 클래스의 API에 따라 달라질 수 있음
            return true; // 임시로 모든 메시지 허용
        } catch (Exception e) {
            Log.e(TAG, "필터 검사 중 오류 발생", e);
            return false; // 오류 발생 시 메시지 전달하지 않음
        }
    }

    /**
     * 새 구독자에게 메시지 히스토리 전달
     */
    private void deliverHistoryToNewSubscriber(ClientInfo clientInfo) {
        if (messageHistory.isEmpty()) {
            return;
        }

        Log.d(TAG, "📜 " + messageHistory.size() + "개의 히스토리 메시지를 새 구독자에게 전달: " + clientInfo.packageName);

        for (Message historicalMessage : messageHistory) {
            try {
                if (shouldDeliverMessage(historicalMessage, clientInfo.filter)) {
                    clientInfo.listener.onMessage(historicalMessage);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "히스토리 메시지 전달 실패: " + clientInfo.packageName, e);
                break; // 연결 문제가 있으면 중단
            } catch (Exception e) {
                Log.e(TAG, "히스토리 메시지 전달 중 예외: " + clientInfo.packageName, e);
            }
        }
    }

    /**
     * 현재 구독자 수 반환 (디버깅용)
     */
    public int getSubscriberCount() {
        return subscribedClients.size();
    }

    /**
     * 메시지 히스토리 크기 반환 (디버깅용)
     */
    public int getMessageHistorySize() {
        return messageHistory.size();
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
        messageHistory.clear();
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
