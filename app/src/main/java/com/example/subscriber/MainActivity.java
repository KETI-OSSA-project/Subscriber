package com.example.subscriber;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText inputTopic;
    private Button subscribeButton;
    private Button unsubscribeButton;
    private TextView messageView;

    private static final String TAG = "Subscriber";

    // 시험 신호(adb 셸만 보낼 수 있음). 예: am broadcast -a test.SUBSCRIBE --es topic 'sensor/+/temp'
    private static final String ACTION_SUBSCRIBE = "test.SUBSCRIBE";
    private static final String ACTION_UNSUBSCRIBE = "test.UNSUBSCRIBE";
    private static final String ACTION_CLEAR = "test.CLEAR";

    private MessagingManager messagingManager;
    private final Map<String, Filter> filterMap = new HashMap<>();

    // 모든 필터에 같은 리스너(L1)를 쓴다. 여러 필터에 맞아도 브로커가 한 번만 알린다(절차서 19번)
    private final MessagingManager.MessageListener messageListener = message -> {
        byte[] payloadBytes = message.getPayload() != null ? message.getPayload() : new byte[0];
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        // 받은 원시 바이트를 그대로 저장한다(절차서 32번: adb pull로 가져가 송신 샘플과 비교). 마지막 메시지만 남김
        try (FileOutputStream out = new FileOutputStream(new File(getExternalFilesDir(null), "last_payload.bin"))) {
            out.write(payloadBytes);
        } catch (IOException e) {
            Log.w(TAG, "수신 페이로드 저장 실패", e);
        }
        // 여러 줄(XML)은 내용 대신 크기만 남기고, 송수신 대조용 sha256은 항상 남긴다
        Log.i(TAG, "L1 onMessage topic=" + message.getTopic() + " id=" + message.getMessageId() + " bytes=" + payloadBytes.length
                + " sha256=" + sha256(payloadBytes) + (payload.indexOf('\n') < 0 ? " payload=" + payload : ""));
        runOnUiThread(() -> logMessage("📥 [" + message.getTopic() + "] 수신: " + payload));
    };

    static String sha256(byte[] data) {
        try {
            StringBuilder sb = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private final BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (ACTION_CLEAR.equals(intent.getAction())) {
                messageView.setText("");
            } else if (topic != null) {
                inputTopic.setText(topic);
                if (ACTION_SUBSCRIBE.equals(intent.getAction())) subscribeTo(topic);
                else unsubscribeFrom(topic);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("Android", "🚀 Subscriber 앱 시작됨");

        inputTopic = findViewById(R.id.inputTopic);
        subscribeButton = findViewById(R.id.subscribeButton);
        unsubscribeButton = findViewById(R.id.unsubscribeButton);
        messageView = findViewById(R.id.messageView);

        // 화면에 시작 메시지 표시
        logMessage("🚀 Subscriber 앱이 시작되었습니다");

        // MessagingManager 초기화 및 연결 상태 확인
        initializeMessagingManager();

        subscribeButton.setOnClickListener(v -> {
            String topic = inputTopic.getText().toString().trim();
            Log.d("Android", "🔘 구독 버튼 클릭: 토픽=" + topic);
            if (topic.isEmpty()) {
                Toast.makeText(this, "토픽을 입력해주세요", Toast.LENGTH_SHORT).show();
                Log.w("Android", "⚠️ 빈 토픽으로 구독 시도");
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

        IntentFilter filter = new IntentFilter(ACTION_SUBSCRIBE);
        filter.addAction(ACTION_UNSUBSCRIBE);
        filter.addAction(ACTION_CLEAR);
        // DUMP 권한을 가진 쪽만 보낼 수 있다: adb 셸은 갖고 있고 일반 앱은 받을 수 없는 권한
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(testReceiver, filter, Manifest.permission.DUMP, null, RECEIVER_EXPORTED);
        } else {
            registerReceiver(testReceiver, filter, Manifest.permission.DUMP, null);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(testReceiver);
        super.onDestroy();
    }

    // MessagingManager 초기화 및 객체 생성
    private void initializeMessagingManager() {
        try {
            messagingManager = MessagingManager.getMessagingManager(this);

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
                                Log.w("MQTT", "❌ MessagingService 연결 실패 - 브로커 앱이 설치되지 않았거나 서비스가 exported=false일 수 있습니다");
                            });
                        }
                    };

            // 초기 연결 리스너 설정
            messagingManager.setMessagingServiceConnectionListener(initialConnectionListener, null);
        } catch (Exception e) {
            logMessage("❌ 브로커 서비스 초기화 중 예외 발생: " + e.getMessage());
        }
    }

    // broker로부터 메시지 수신하는 코드
    private void subscribeTo(String topic) {
        try {
            // 이미 필터가 있으면 재사용
            Filter filter = filterMap.get(topic);
            if (filter == null) {
                filter = new Filter.Builder().setTopic(topic).build();
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
                            runOnUiThread(() -> logMessage("⚠️ Android Broker 연결이 끊어졌습니다"));
                        }
                    };
            // 연결 리스너 설정
            messagingManager.setMessagingServiceConnectionListener(connectionListener, null);
            runOnUiThread(() -> logMessage("🔄 [" + topic + "] 구독 요청 중..."));
            if (messagingManager.isConnected()) {
                // 직접 호출
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
            Filter filter = filterMap.get(topic);
            if (filter == null) {
                logMessage("❌ [" + topic + "] 구독 해제 실패: 해당 토픽에 대한 구독 정보 없음");
                return;
            }
            boolean result = messagingManager.unsubscribe(messageListener, filter);
            if (result) {
                logMessage("✅ [" + topic + "] 구독 해제 성공");
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
        Log.d("Android", msg);
        messageView.append(msg + "\n");
    }
}