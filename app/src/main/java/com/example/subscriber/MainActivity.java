package com.example.subscriber;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.messagebroker.Filter;
import com.example.messagebroker.MessagingManager;

import androidx.appcompat.app.AppCompatActivity;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.nio.ByteBuffer;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {

    // 에뮬레이터 연동 시: 공유기 ip 주소 -> 이더넷 말고 공유
    private final String brokerHost = "192.168.0.8";
    private final int brokerPort = 1883;

    private EditText inputTopic;
    private Button subscribeButton;
    private TextView messageView;

    private Mqtt3AsyncClient mqttClient;

    // MQTT 클라이언트 초기화 및 UI 이벤트 설정하는 함수
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputTopic = findViewById(R.id.inputTopic);
        subscribeButton = findViewById(R.id.subscribeButton);
        messageView = findViewById(R.id.messageView);

        setupMqttClient();

        subscribeButton.setOnClickListener(v -> {
            String topic = inputTopic.getText().toString().trim();
            if (topic.isEmpty()) {
                Toast.makeText(this, "토픽을 입력해주세요", Toast.LENGTH_SHORT).show();
            } else {
                subscribeTo(topic);
            }
        });
    }

    // HiveMQ MQTT 클라이언트를 초기화하고 브로커에 연결하는 함수
    private void setupMqttClient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("android-subscriber-" + System.currentTimeMillis())
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .buildAsync();

        mqttClient.connectWith().send().whenComplete((connAck, throwable) -> {
            runOnUiThread(() -> {
                if (throwable == null) {
                    logMessage("✅ MQTT 브로커 연결 성공");
                } else {
                    logMessage("❌ MQTT 연결 실패: " + throwable.getMessage());
                }
            });
        });
    }

    // api 표준화 대상
    // 주어진 토픽을 MQTT 브로커에 구독 요청하는 함수
    private void subscribeTo(String topic) {
        MessagingManager messagingManager = MessagingManager.getMessagingManager(this);
        Filter filter = new Filter.Builder().setTopic(topic).build();
        MessagingManager.MessageListener client = new MessagingManager.MessageListener() {
            @Override
            public void onMessage(com.example.messagebroker.Message message) {
                byte[] payloadBytes = message.getPayload();
                String payload = payloadBytes != null ? new String(payloadBytes) : "";
                runOnUiThread(() -> {
                    logMessage("\uD83D\uDCE5 [" + topic + "] 수신: " + payload);
                });
            }
        };
        boolean result = messagingManager.subscribe(client, filter);
        runOnUiThread(() -> {
            if (result) {
                logMessage("\uD83D\uDCB1 '" + topic + "' 토픽 구독 시작");
            } else {
                logMessage("❌ '" + topic + "' 구독 실패");
            }
        });
    }

    // 로그를 UI에 출력하는 함수
    private void logMessage(String msg) {
        Log.d("MQTT", msg);
        messageView.append(msg + "\n");
    }
}