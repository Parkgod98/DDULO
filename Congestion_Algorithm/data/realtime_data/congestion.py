import pandas as pd
import json
import time
import ast
import paho.mqtt.client as mqtt
from threading import Thread

# === 설정 값 (Configuration) ===
MQTT_BROKER = "localhost"  # 브로커 주소 (필요시 변경)
MQTT_PORT = 1883
MQTT_TOPIC = "/platform"

# 게이트별 면적 (m^2)
GATE_AREAS = {
    '9-1': 5.46,
    '9-2': 5.46,
    '9-3': 9.66,
    '9-4': 9.66
}

# 기준 값
STD_DENSITY = 4.3   # 면적당 기준 인원 (명/m^2)
TRAIN_CAPACITY = 160 # 열차 1량당 기준 인원 (명)
THRESHOLD_CONGESTION = 100.0 # 혼잡도 임계치 (%)

class CongestionSystem:
    def __init__(self):
        # 1. 데이터 로드
        print("[System] CSV 데이터를 로딩 중입니다...")
        try:
            self.df_car = pd.read_csv('Congestion_Algorithm/data/tmap_puzzle_filtering_car.csv')
            self.df_getoff = pd.read_csv('Congestion_Algorithm/data/tmap_puzzle_filtering_get-off.csv')
            print("[System] 데이터 로드 완료")
        except FileNotFoundError as e:
            print(f"[Error] 파일을 찾을 수 없습니다: {e}")
            self.df_car = None
            self.df_getoff = None

        # 2. 실시간 승강장 인원 저장소 (Key: GateID, Value: Count)
        self.platform_counts = {'1-1': 0, '1-2': 0, '1-3': 0, '1-4': 0}

        # 3. MQTT 클라이언트 설정
        self.client = mqtt.Client()
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message

    # --- MQTT 관련 메서드 ---
    def start_mqtt(self):
        """MQTT 리스닝을 별도 스레드로 시작"""
        try:
            self.client.connect(MQTT_BROKER, MQTT_PORT, 60)
            self.client.loop_start() # 비동기 루프 시작
            print(f"[MQTT] {MQTT_BROKER}:{MQTT_PORT} 연결 및 {MQTT_TOPIC} 구독 시작")
        except Exception as e:
            print(f"[MQTT] 연결 실패: {e}")

    def on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            client.subscribe(MQTT_TOPIC)
        else:
            print(f"[MQTT] 연결 실패, 코드: {rc}")

    def on_message(self, client, userdata, msg):
        """
        수신 메시지 예시 (JSON):
        {"1-1": 25, "1-2": 30 ...}
        """
        try:
            payload = msg.payload.decode('utf-8')
            data = json.loads(payload)

            # 데이터 포맷에 따라 분기 (여기서는 Dict 형태 {gate: count} 가정)
            for gate, count in data.items():
                if gate in self.platform_counts:
                    self.platform_counts[gate] = int(count)
            
            # 디버깅용 출력 (너무 잦으면 주석 처리)
            # print(f"[MQTT] 데이터 갱신: {self.platform_counts}")
            
        except Exception as e:
            print(f"[MQTT] 메시지 파싱 에러: {e}")

    # --- 혼잡도 계산 로직 ---
    def calculate_platform_congestion(self):
        """
        승강장 혼잡도 계산 (게이트별)

        Returns:
        {
            "carNo":량
            "doorNo":게이트(량당 4게이트)
            "count":대기인원
            "congestion_rate":혼잡도(%)
        }
        """
        results = {}
        for gate, count in self.platform_counts.items():
            area = GATE_AREAS.get(gate, 1.0)
            
            # 공식: (인원 / 면적) / 4.3 * 100
            density = count / area
            congestion = (density / STD_DENSITY) * 100
            
            results[gate] = {
                "carNo": int(gate.split('-')[0]),
                "doorNo": int(gate.split('-')[1]),
                "count": count,
                "area": area,
                "congestion_rate": round(congestion, 2)
            }
        return results

    def get_total_congestion(self, day, time_slot, station, direction):
        """
        종합 혼잡도 산출
        입력: 요일, 시각, 역, 방향
        """
        if self.df_car is None or self.df_getoff is None:
            return None
        hh, mm = map(int, time_slot.split(':'))
        station = int(station)
        direction = int(direction)

        # 1. 열차 혼잡도 조회 (df_car)
        # 가정: CSV 컬럼명이 ['dow', 'hh', 'mm', 'stationCode', 'updnLine', 'congestion'] 이라고 가정
        # 실제 CSV 헤더에 맞춰 수정 필요
        row_car = self.df_car[
            (self.df_car['dow'] == day) & 
            (self.df_car['hh'] == hh) & 
            (self.df_car['mm'] == mm) & 
            (self.df_car['stationCode'] == station) & 
            (self.df_car['updnLine'] == direction)
        ]

        # 2. 하차 비율 조회 (df_getoff)
        row_getoff = self.df_getoff[
            (self.df_getoff['dow'] == day) & 
            (self.df_getoff['hh'] == hh) & 
            (self.df_getoff['mm'] == mm) &
            (self.df_getoff['stationCode'] == station) & 
            (self.df_getoff['updnLine'] == direction)
        ]

        if row_car.empty or row_getoff.empty:
            print("[System] 해당 조건의 데이터를 찾을 수 없습니다.")
            return None

        # 데이터 파싱 (리스트 형태의 문자열 '[10, 20...]'을 실제 리스트로 변환 가정)
        # 만약 CSV가 컬럼별로 c1, c2.. 로 되어있다면 로직 수정 필요
        try:
            # 예: csv값이 "[50, 40, 30...]" 문자열인 경우
            train_congestions = ast.literal_eval(str(row_car.iloc[0]['congestionCar'])) 
            get_off_rates = ast.literal_eval(str(row_getoff.iloc[0]['getOffCarRate']))
        except Exception:
            # 단순 예시값 (데이터가 없을 경우 방어 코드)
            train_congestions = [0] * 10
            get_off_rates = [0] * 10

        # 3. 종합 혼잡도 계산
        # Gate '1-1'은 1호차 1게이트, '1-2'는 1호차 2게이트... 로 매핑된다고 가정하고 계산
        # 게이트 정보가 없는 칸(5호차~)은 승강장 인원 0으로 간주
        
        final_results = []
        isBoardable = False  # 탑승 가능 여부 플래그
        
        for i in range(10): # 10량 기준
            for gate in range(1, 5): # 각 칸당 4게이트
                car_idx = i + 1
                gate_key = f"{car_idx}-{gate}"
                
                # A. 기존 열차 혼잡도
                current_train_cong = train_congestions[i] if i < len(train_congestions) else 0
                
                # B. 하차 비율
                off_rate = get_off_rates[i] if i < len(get_off_rates) else 0
                
                # C. 승강장 대기 인원 (해당 칸 게이트가 없으면 0)

                platform_cnt = self.platform_counts.get(gate_key, 0)
                
                # D. 종합 혼잡도 공식 적용
                # 공식: 열차혼잡도 * (1 - (과거하차비율/100)) + 승강장 인원 * (100 / 160)
                
                term1 = current_train_cong * (1 - (off_rate / 100.0))
                term2 = platform_cnt * (100.0 / TRAIN_CAPACITY)
                total_congestion = term1 + term2
                
                if total_congestion < THRESHOLD_CONGESTION:
                    isBoardable = True

                final_results.append({
                    "carNo": car_idx,
                    "doorNo": gate,
                    "trainCongestion": current_train_cong,
                    "getOffRate": off_rate,
                    "platformCount": platform_cnt,
                    "congestionLevel": round(total_congestion, 2),
                    "isBoardable" : isBoardable
                })
                

        return final_results

# === 실행 예시 (Main) ===
if __name__ == "__main__":
    system = CongestionSystem()
    system.start_mqtt()

    # 테스트를 위한 가상 데이터 전송 (실제로는 MQTT로 외부에서 들어옴)
    # 2초 대기 후 가상 데이터 주입
    time.sleep(2)
    mock_payload = json.dumps({'1-1': 25, '1-2': 100, '1-3': 10, '1-4': 40})
    class MockMsg:
        payload = mock_payload.encode()
    system.on_message(None, None, MockMsg())

    print("\n=== [요청] 월요일 08:00 강남역 상행선 혼잡도 조회 ===")
    
    # 1. 승강장 상태 확인
    platform_status = system.calculate_platform_congestion()
    print("\n>> 승강장 실시간 상태:")
    for gate, info in platform_status.items():
        print(f"  Gate {gate}: 인원 {info['count']}명 / 면적 {info['area']}m2 -> 혼잡률 {info['congestion_rate']}%")

    # 2. 종합 혼잡도 계산 요청 (CSV 파일이 실제 존재해야 동작)
    # 예시 입력값: day='MON', time='08:00', station='Gangnam', direction='Up'
    total_result = system.get_total_congestion('MON', '18:00', 222, 1)

    if total_result:
        print("\n>> 종합 혼잡도 (예측):")
        print(f"{'호차':<5} {'열차혼잡':<10} {'하차율':<10} {'승강장대기':<10} {'종합혼잡도':<10}")
        print("-" * 55)
        for res in total_result[:4]: # 게이트가 있는 4개 칸만 출력 예시
            print(f"{res['carNo']:<5} {res['trainCongestion']:<10} {res['getOffRate']:<10} {res['platformCount']:<10} {res['congestionLevel']:<10}")
    else:
        print("\n[Info] 매칭되는 데이터가 없어 종합 결과를 출력할 수 없습니다.")
    
    # MQTT 수신 대기를 위해 유지 (실제 운영 시)
    while True:
        time.sleep(1)