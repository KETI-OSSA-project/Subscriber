package com.example.subscriber;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.broker.Filter;
import com.example.broker.Message;
import com.example.broker.MessagingManager;

import com.example.broker.R;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText inputTopic;
    private Button subscribeButton;
    private Button unsubscribeButton;
    private TextView messageView;

    // 표준 API 준수를 위한 멤버 변수들
    private MessagingManager messagingManager;
    private MessagingManager.MessageListener currentMessageListener;
    private Filter currentFilter;

    // 브로커 패키지명 및 서비스명 직접 상수로 선언 (MessagingManager.Constants 사용)
    private static final String MESSAGE_BROKER_PACKAGE = MessagingManager.Constants.MESSAGE_BROKER_PACKAGE;
    private static final String MESSAGING_SERVICE = MessagingManager.Constants.MESSAGING_SERVICE;

    // 토픽별 리스너/필터 저장용 맵
    private final Map<String, MessagingManager.MessageListener> listenerMap = new HashMap<>();
    private final Map<String, Filter> filterMap = new HashMap<>();

    // MQTT 클라이언트 초기화 및 UI 이벤트 설정하는 함수
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("MQTT", "🚀 Subscriber 앱 시작됨");

        inputTopic = findViewById(R.id.inputTopic);
        subscribeButton = findViewById(R.id.subscribeButton);
        unsubscribeButton = findViewById(R.id.unsubscribeButton);
        messageView = findViewById(R.id.messageView);

        Log.d("MQTT", "📱 UI 컴포넌트 초기화 완료");

        // 화면에 시작 메시지 표시
        logMessage("🚀 Subscriber 앱이 시작되었습니다");
        logMessage("📱 Android 브로커와 연결 준비 중...");

        // MessagingManager 초기화 및 연결 상태 확인
        initializeMessagingManager();

        subscribeButton.setOnClickListener(v -> {
            String topic = inputTopic.getText().toString().trim();
            Log.d("MQTT", "🔘 구독 버튼 클릭: 토픽=" + topic);
            if (topic.isEmpty()) {
                Toast.makeText(this, "토픽을 입력해주세요", Toast.LENGTH_SHORT).show();
                Log.w("MQTT", "⚠️ 빈 토픽으로 구독 시도");
            } else {
                subscribeTo(topic);
            }
        });

        unsubscribeButton.setOnClickListener(v -> {
            String topic = inputTopic.getText().toString().trim();
            Log.d("MQTT", "🔘 구독 해제 버튼 클릭: 토픽=" + topic);
            if (topic.isEmpty()) {
                Toast.makeText(this, "토픽을 입력해주세요", Toast.LENGTH_SHORT).show();
                Log.w("MQTT", "⚠️ 빈 토픽으로 구독 해제 시도");
            } else {
                unsubscribeFrom(topic);
            }
        });

        Log.i("MQTT", "✅ onCreate() 완료");
    }

    // MessagingManager 초기화 및 연결 상태 확인
    private void initializeMessagingManager() {
        Log.d("MQTT", "🔧 MessagingManager 초기화 중...");
        logMessage("🔧 브로커 서비스 바인딩 시도: " + MESSAGE_BROKER_PACKAGE + "/" + MESSAGING_SERVICE);
        try {
            messagingManager = MessagingManager.getMessagingManager(this);
            Log.d("MQTT", "✅ MessagingManager 생성 완료");
            logMessage("✅ MessagingManager 인스턴스 생성 완료");

            // 연결 리스너 설정하여 연결 상태 모니터링
            MessagingManager.MessagingServiceConnectionListener initialConnectionListener =
                    new MessagingManager.MessagingServiceConnectionListener() {
                        @Override
                        public void onConnected() {
                            runOnUiThread(() -> {
                                logMessage("✅ Android 브로커 연결 완료!");
                                Log.i("MQTT", "🔗 MessagingService 연결됨");
                            });
                        }

                        @Override
                        public void onDisconnected() {
                            runOnUiThread(() -> {
                                logMessage("❌ 브로커 서비스 바인딩 실패 또는 연결 끊김");
                                logMessage("💡 브로커 앱(" + MESSAGE_BROKER_PACKAGE + ")이 설치되어 있고, 서비스가 exported=true인지 확인하세요");
                                Log.w("MQTT", "❌ MessagingService 연결 실패 - 브로커 앱이 설치되지 않았거나 서비스가 exported=false일 수 있습니다");
                            });
                        }
                    };

            // 초기 연결 리스너 설정
            messagingManager.setMessagingServiceConnectionListener(initialConnectionListener, null);
            Log.d("MQTT", "📡 연결 리스너 설정 완료");
            logMessage("📡 브로커 서비스 연결 리스너 등록 완료");
        } catch (Exception e) {
            Log.e("MQTT", "MessagingManager 초기화 중 오류 발생", e);
            logMessage("❌ 브로커 서비스 초기화 중 예외 발생: " + e.getMessage());
            logMessage("💡 브로커 앱이 설치되어 있고, 서비스가 exported=true인지 확인하세요");
        }
    }

    // api 표준화 대상
    // 주어진 토픽을 MQTT 브로커에 구독 요청하는 함수
    private void subscribeTo(String topic) {
        try {
            // 이미 리스너가 있으면 재사용
            MessagingManager.MessageListener messageListener = listenerMap.get(topic);
            Filter filter = filterMap.get(topic);
            if (messageListener == null || filter == null) {
                filter = new Filter.Builder().setTopic(topic).build();
                messageListener = new MessagingManager.MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        byte[] payloadBytes = message.getPayload();
                        String payload = payloadBytes != null ? new String(payloadBytes) : "";
                        runOnUiThread(() -> logMessage("\uD83D\uDCE5 [" + topic + "] 수신: " + payload));
                    }
                };
                // 맵에 저장
                listenerMap.put(topic, messageListener);
                filterMap.put(topic, filter);
            }
            final MessagingManager.MessageListener finalMessageListener = messageListener;
            final Filter finalFilter = filter;
            MessagingManager.MessagingServiceConnectionListener connectionListener =
                    new MessagingManager.MessagingServiceConnectionListener() {
                        @Override
                        public void onConnected() {
                            boolean result = false;
                            Exception subscribeException = null;
                            try {
                                result = messagingManager.subscribe(finalMessageListener, finalFilter);
                            } catch (Exception e) {
                                subscribeException = e;
                            }
                            final boolean subscribeResult = result;
                            final Exception finalException = subscribeException;
                            runOnUiThread(() -> {
                                if (finalException != null) {
                                    logMessage("❌ 구독 중 예외 발생: " + finalException.getMessage());
                                } else if (subscribeResult) {
                                    logMessage("✅ [" + topic + "] 구독 성공");
                                } else {
                                    logMessage("❌ [" + topic + "] 구독 실패");
                                }
                            });
                        }
                        @Override
                        public void onDisconnected() {
                            runOnUiThread(() -> logMessage("⚠️ MessagingService 연결이 끊어졌습니다"));
                        }
                    };
            // 연결 리스너 설정
            messagingManager.setMessagingServiceConnectionListener(connectionListener, null);
            runOnUiThread(() -> logMessage("🔄 [" + topic + "] 구독 요청 중..."));
            if (messagingManager.isConnected()) {
                // 직접 수동으로 구독하기
                boolean result = messagingManager.subscribe(messageListener, filter);
                if (result) {
                    logMessage("✅ [" + topic + "] 구독 성공");
                } else {
                    logMessage("❌ [" + topic + "] 구독 실패");
                }
            } else {
                logMessage("🔄 [" + topic + "] 연결 대기 중...");
            }
        } catch (Exception e) {
            runOnUiThread(() -> logMessage("❌ subscribeTo 전체 예외: " + e.getMessage()));
        }
    }

    // 주어진 토픽에 대한 MQTT 브로커의 구독을 해제하는 함수
    private void unsubscribeFrom(String topic) {
        try {
            runOnUiThread(() -> logMessage("🔄 [" + topic + "] 구독 해제 요청 중..."));
            MessagingManager.MessageListener messageListener = listenerMap.get(topic);
            Filter filter = filterMap.get(topic);
            if (messageListener == null || filter == null) {
                logMessage("❌ [" + topic + "] 구독 해제 실패: 해당 토픽에 대한 구독 정보 없음");
                return;
            }
            boolean result = messagingManager.unsubscribe(messageListener, filter);
            if (result) {
                logMessage("✅ [" + topic + "] 구독 해제 성공");
                // 맵에서 제거
                listenerMap.remove(topic);
                filterMap.remove(topic);
            } else {
                logMessage("❌ [" + topic + "] 구독 해제 실패");
            }
        } catch (Exception e) {
            runOnUiThread(() -> logMessage("❌ unsubscribeFrom 전체 예외: " + e.getMessage()));
        }
    }

    // 로그를 UI에 출력하는 함수
    private void logMessage(String msg) {
        Log.d("MQTT", msg);
        messageView.append(msg + "\n");
    }
}