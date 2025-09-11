# Subscriber
---

## Azena Messaging Subscriber

<p align="center">
  <img src="https://github.com/user-attachments/assets/d15b5f1f-2052-4b2e-b914-270107ee7431" alt="<프로젝트명> Banner" width="70%" />
</p>

<p align="center">
  <b>Validate and integrate MQTT messaging services and brokers with a unified tool.</b>
</p>

<div align="center">

  <!-- 윗줄: Android 관련 2개 -->
  <img src="https://img.shields.io/badge/Android_Studio-Koala-3DDC84?logo=androidstudio&logoColor=white" />
  <img src="https://img.shields.io/badge/Android-12_(API_32)-3DDC84?logo=android&logoColor=white" />
  <!-- 아랫줄: 나머지 3개 -->
  <img src="https://img.shields.io/badge/Java-11-007396?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Mosquitto-2.0.22-3C5280?logo=eclipseide&logoColor=white" />

</div>

## 개요

Subscriber 모듈은 내부 Broker 또는 Gateway를 통해 발행된 메시지를 **구독(Subscribe) 및 수신**하는 역할을 담당합니다.
사용자는 특정 토픽을 구독하고, 해당 토픽에 발행되는 메시지를 실시간으로 확인할 수 있습니다.
Gateway 설정(Downlink 규칙)에 따라 **외부 MQTT Broker(Mosquitto)** 의 메시지도 내부로 전달받을 수 있습니다.

---
## Demo
### 내부 ↔ 내부 통신
<p align="center">
  <img src="https://github.com/user-attachments/assets/5fda5904-9d37-4324-8dae-87ddd36bc08a" width="40%" />
</p>

### 내부 ↔ 외부 통신
<table>
<tr>
    <td><img src="https://github.com/user-attachments/assets/3372b3b6-22df-4a4c-b832-d55950039a4c" alt="uplink-broker"></td>
    <td><img src="https://github.com/user-attachments/assets/e029e60a-968f-4e0d-9743-4cc5d612e071" alt="downlink-subscriber"></td>
</tr>
</table>
<p align="center">
  <a href="https://drive.google.com/file/d/1_TukQ0CwD708QUjqD8ZCfM2t8XZdoEBp/view?usp=drive_link">
    <b>📽️ MQTT Demo</b>
  </a>
</p>


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

## 빠른 점검 (Troubleshooting)

* **메시지 수신 안 됨**:

  * 토픽이 정확히 일치하는지 확인
  * Gateway Downlink 규칙과 일치하는지 확인 (외부 메시지의 경우)
* **중복 수신**: 중복 구독 방지 확인
* **연결 실패**: 내부 Broker URL 및 네트워크 상태 확인

---

## Acknowledgement
**개발 및 유지 관리 기관** : 한국전자기술연구원(KETI)

※ 본 연구는 2025년도 정부(과학기술정보통신부)의 재원으로 정보통신기획평가원의 지원을 받아 수행된 연구임(No. RS-2024-00394190, 온-디바이스 자율보호가 내재화된 개방형 영상보안플랫폼 기술 개발).
