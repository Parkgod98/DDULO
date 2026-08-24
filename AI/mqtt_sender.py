import paho.mqtt.client as mqtt
import json
import time
import ssl

class MqttSender:
    def __init__(self, broker_ip, port=1883, topic="/platform"):
        self.broker_ip = broker_ip
        self.port = port
        self.topic = topic
        self.client = mqtt.Client()
        self.connected = False
        
        # [Ticket 51] 네트워크 끊김 시 재접속 간격 설정 (1초 ~ 120초까지 늘어남)
        self.client.reconnect_delay_set(min_delay=1, max_delay=120)
        # ★ 2. [보안 추가] SSL/TLS 설정 (여기 추가됨)
        # ==========================================
        try:
            # ca.crt 파일이 파이썬 실행 파일과 같은 폴더에 있어야 합니다.
            self.client.tls_set(ca_certs="./ca.crt", 
                                cert_reqs=ssl.CERT_NONE, 
                                tls_version=ssl.PROTOCOL_TLSv1_2)
            
            # 도메인/IP 불일치 허용 (우리는 IP로 접속하니까 이거 필수)
            self.client.tls_insecure_set(True)
            print("🔒 SSL 보안 설정이 적용되었습니다.")
        except Exception as e:
            print(f"⚠️ SSL 설정 실패 (ca.crt 파일 확인 필요): {e}")
        # 콜백 설정
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect

    def on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            print(f"✅ MQTT 브로커 연결 성공 ({self.broker_ip})")
            self.connected = True
        else:
            print(f"❌ 연결 실패 (코드: {rc})")
            self.connected = False

    def on_disconnect(self, client, userdata, rc):
        print("⚠️ MQTT 연결 끊김 - 재접속 시도 중...")
        self.connected = False

    def connect(self):
        try:
            self.client.connect(self.broker_ip, self.port, 60)
            self.client.loop_start() # 백그라운드 스레드 시작
            time.sleep(1) # 연결 대기
        except Exception as e:
            print(f"❌ 연결 에러: {e}")

    def send_data(self, zone_counts):
        """
        zone_counts: {'1-1': 2, '1-2': 0 ...} 딕셔너리 받아서 전송
        """
        # 연결이 끊겨있어도 paho-mqtt가 내부적으로 재접속 후 전송을 시도하므로
        # publish는 계속 시도하는 것이 좋습니다.
        
        current_time = time.strftime('%Y-%m-%d %H:%M:%S')
        payload = {
            "timestamp": current_time,
            "data": zone_counts
        }
        
        try:
            json_str = json.dumps(payload, ensure_ascii=False)
            self.client.publish(self.topic, json_str)
            # 디버깅용: 전송 로그 보고 싶으면 주석 해제
            # print(f"📤 [MQTT] {json_str}") 
            return True
        except Exception as e:
            print(f"❌ 전송 실패: {e}")
            return False

    def close(self):
        self.client.loop_stop()
        self.client.disconnect()