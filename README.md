# Subscriber_java
---

## Azena Messaging Subscriber

**개요**

Subscriber 모듈은 내부 Broker 또는 Gateway를 통해 발행된 메시지를 **구독(Subscribe) 및 수신**하는 역할을 담당합니다.
사용자는 특정 토픽을 구독하고, 해당 토픽에 발행되는 메시지를 실시간으로 확인할 수 있습니다.
Gateway 설정(Downlink 규칙)에 따라 **외부 MQTT Broker(Mosquitto)** 의 메시지도 내부로 전달받을 수 있습니다.

---

## 디렉토리 구조

```
subscriber_java/
└─ app/
   └─ java/com/example/
      ├─ broker/
      │  ├─ Filter.java             # 토픽 패턴 필터링 (Downlink 규칙 적용)
      │  ├─ Message.java            # 메시지 데이터 구조 (topic, payload 등)
      │  └─ MessagingManager.java   # MQTT 연결/구독/해제/수신 처리
      │
      └─ subscriber/
         └─ MainActivity.java       # UI 진입점 (토픽 입력, 구독/해제, 메시지 표시)
```

---

## 주요 기능

* **토픽 구독/해제 관리** (subscribe / unsubscribe)
* **실시간 메시지 수신** 및 UI 표시
* **토픽 패턴 필터링**: `Filter.java`로 허용된 토픽만 처리
* **메시지 데이터 구조화**: `Message.java`로 통일된 모델 사용
* **내부/외부 연동**: Gateway Downlink 설정을 통해 외부 MQTT 메시지 수신 가능

---

## 실행 환경

* **Android Studio Koala (2024.1.1 Patch 1)**
* **Android 12 이하 (SDK/API Level ≤ 32)**
* 내부 Broker 및 (외부 수신 시) **Gateway + 외부 MQTT Broker(Mosquitto)** 실행 필요

---

## 빌드 & 실행

1. Android Studio에서 `subscriber_java` 열기
2. `MessagingManager.java` 내 브로커 주소 확인
3. `MainActivity` 실행 → 토픽 입력 후 **구독 버튼** 클릭

---

## 사용 예시

### 내부 메시지 구독

1. **Topic 입력** : `azena/test`
2. **구독하기 버튼 클릭**
3. Publisher가 `azena/test` 토픽으로 메시지를 발행 → 메시지 수신 확인

### 외부 메시지 수신 (Gateway Downlink 경유)

1. Gateway Downlink Pattern: `downlink/#` (예시)
2. Subscriber Topic: `downlink/device1/data`
3. 외부 MQTT Publisher → `downlink/device1/data` 발행
4. Gateway → 내부 Broker 전달 → Subscriber 수신

---

## 동작 규칙

* 구독 이전 시점의 발행 메시지는 수신되지 않음
* 구독하지 않은(또는 해제한) 토픽 메시지는 수신되지 않음
* 동일 토픽 중복 구독 시 메시지는 1회만 수신됨
* 발행 시점 이전부터 구독한 토픽의 메시지는 자동 수신됨
* Downlink 규칙과 다른 토픽은 Gateway에서 내부로 전달되지 않음

---

## 빠른 점검 (Troubleshooting)

* **메시지 수신 안 됨**:

  * 토픽이 정확히 일치하는지 확인
  * Gateway Downlink 규칙과 일치하는지 확인 (외부 메시지의 경우)
* **중복 수신**: 중복 구독 방지 확인
* **연결 실패**: 내부 Broker URL 및 네트워크 상태 확인
