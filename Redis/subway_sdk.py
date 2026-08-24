import json
import threading
import paho.mqtt.client as mqtt
import os
from dotenv import load_dotenv

# 124번에서 만든 Redis 모듈 (import 되는지 확인!)
from redis_client import RedisManager

load_dotenv()

class SubwayHelper:
    def __init__(self):
        # 1. Redis 연결 (124번 활용)
        self.redis = RedisManager().get_client()
        
        # 2. MQTT 설정
        self.mqtt_host = os.getenv("MQTT_HOST", "localhost")
        self.mqtt_port = int(os.getenv("MQTT_PORT", 8000))
        self.mqtt_topic = os.getenv("MQTT_TOPIC", "/platform")
        
        self.mqtt_data = {} 
        self.mqtt_client = mqtt.Client()
        self.mqtt_client.on_connect = self._on_connect
        self.mqtt_client.on_message = self._on_message

    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            print(f"✅ [SDK] MQTT 연결 성공 (Topic: {self.mqtt_topic})")
            client.subscribe(self.mqtt_topic)
        else:
            print(f"❌ [SDK] MQTT 연결 실패 (Code: {rc})")

    # [핵심] 젯슨 JSON 데이터 파싱 로직
    def _on_message(self, client, userdata, msg):
        try:
            # 1. 젯슨이 보낸 문자열 디코딩
            payload_str = msg.payload.decode()
            payload_json = json.loads(payload_str)
            
            # 2. "data" 키 안에 있는 {'1-1': 2, ...} 꺼내기
            if "data" in payload_json:
                new_counts = payload_json["data"]
                self.mqtt_data.update(new_counts)
                # print(f"📡 데이터 수신: {new_counts}") # 디버깅용
                
        except json.JSONDecodeError:
            print(f"⚠️ JSON 형식이 아님: {msg.payload}")
        except Exception as e:
            print(f"⚠️ SDK 에러: {e}")

    def start_listening(self):
        """MQTT 수신 시작"""
        t = threading.Thread(target=self.mqtt_client.connect, args=(self.mqtt_host, self.mqtt_port, 60))
        t.start()
        self.mqtt_client.loop_start()

    def get_current_platform_data(self):
        return self.mqtt_data
    
    def get_platform_area(self, station_id, direction):
        """
        Redis Key: platformArea:{station_id}:{direction}
        내용:
            [
             [50.0, 40.0, 50.0, 40.0],  # 1호차
             [50.0, 40.0, 50.0, 40.0],  # 2호차
             ...
            ]
        """
        key = f"platformArea:{station_id}:{direction}"
        data = self.redis.get(key)
        if data:
            data = json.loads(data)
            return data
        else:
            print(f"⚠️ [SDK] 승강장 면적 데이터 없음: {key}")
            return []
    
    def get_statistical_data(self, station_id, time_slot, day_of_week, direction):
        """
        Redis Key: stats:{day_of_week}:{station_id}:{direction}:{time_slot} (예: stats:MON:233:1:1430)
        """
        key = f"stats:{day_of_week}:{station_id}:{direction}:{time_slot}"
        data = self.redis.get(key)
        if data:
            data = json.loads(data)
            return data
        else:
            print(f"⚠️ [SDK] 통계 데이터 없음: {key}")
            return {}


    # =================================================================
    # [Step 1] 통계 데이터 적재용 (알고리즘 팀이 미리 돌리는 함수)
    # =================================================================
    def save_platform_area(self, station_id, direction, platform_area):
        """
        Redis Key: platformArea:{station_id}:{direction}
        내용:
            [
             [50.0, 40.0, 50.0, 40.0],  # 1호차
             [50.0, 40.0, 50.0, 40.0],  # 2호차
             ...
            ]
        """
        key = f"platformArea:{station_id}:{direction}"
        self.redis.set(key, json.dumps(platform_area, ensure_ascii=False))
        print(f"💾 [Pre-Load] 승강장 면적 데이터 적재 완료: {key}")

    def save_statistical_data(self, station_id, time_slot, day_of_week, direction, car, station, total, off_rate, is_boardable, comfort_boarding):
        """
        Redis Key: stats:{day_of_week}:{station_id}:{direction}:{time_slot} (예: stats:MON:233:1:1430)
        내용: 환승역 미래 예측 데이터 알맹이
        """
        payload = {
            # "stationCode": station_id,
            # "day_of_week" : day_of_week,
            # "direction" : direction,
            "carCongestions": car,
            "stationCongestions": station,
            "totalCongestions": total,
            "offRates": off_rate,
            "isBoardable": is_boardable,
            "comfortBoarding": comfort_boarding,
            "bestBoarding": [],
            "arrivalInfos": [] # 통계니까 도착정보는 없음
        }
        key = f"stats:{day_of_week}:{station_id}:{direction}:{time_slot}"
        
        # 영구 저장 (나중에 갱신 전까지 유지)
        self.redis.set(key, json.dumps(payload, ensure_ascii=False))
        print(f"💾 [Pre-Load] 통계 데이터 적재 완료: {key}")
    # --- 데이터 저장 (Schema 정의) ---
    
    # =================================================================
    # 🔥 [핵심] 전체 JSON을 통째로 조립해서 저장하는 함수
    # =================================================================
    def save_full_route_json(self, 
                             redis_key,           # 저장할 Key path:full:{출발Id}:{도착ID}:{요일}:{시간}

                             estimated_boarding_time,
                             boarding_probability, # 탑승확률
                             
                             # 2. 출발역 정보 (메타 + 알고리즘 결과)
                             start_station_datas,
                             
                             # 3. 환승역 정보 리스트 (메타 + 알고리즘 결과의 리스트)
                             transfer_stations_meta,
                             
                             # 4. 도착역 정보 (메타만 있음)
                             end_station_data):
        """
        params 설명:
        - start_station_data: 딕셔너리. stationId, stationName... 그리고 'result' 안에 들어갈 congestion 데이터들 포함.
        - transfer_stations_list: 리스트. 각 요소는 환승역 하나의 정보 딕셔너리.
        - end_station_data: 딕셔너리. 도착역 정보.
        """

        # 1. Start Station 구조 조립
        start_station_payload = []
        for start_station_data in start_station_datas:
            start_station_info = {
                "stationId": start_station_data["stationId"], # 꼭 채워줘야함!!!!!
                "stationName": start_station_data.get("stationName", ""),
                "lineName": start_station_data.get("lineName", ""),
                "direction": start_station_data["direction"], # 꼭 채워줘야함!!!!!
                "estimatedWatingSec": start_station_data.get("estimatedWatingSec", 0),
                "isBoardable": start_station_data["isBoardable"],
                "result": [
                    {
                        "carCongestions": start_station_data["carCongestions"],
                        "stationCongestions": start_station_data["stationCongestions"],
                        "totalCongestions": start_station_data["totalCongestions"],
                        "bestBoardings": start_station_data["bestBoardings"],
                        "comfortBoarding": start_station_data["comfortBoarding"],
                    }
                ] if len(start_station_data["carCongestions"]) != 0 else []
            }
            start_station_payload.append(start_station_info)


        # 2. Transfer Station
        transfer_payload = []
        
        for tf in transfer_stations_meta:
            stations = []
            for station in tf:
                stations.append({
                    "stationId": station["stationId"],
                    "stationName": station.get("stationName", ""),
                    "lineName": station.get("lineName", ""),
                    "direction": station["direction"],
                    "estimatedWaitingSec": station.get("estimatedWaitingSec", 0),
                    "isBoardable" : station["isBoardable"],
                    "results": [ 
                        {
                            "carCongestions": station["carCongestions"],
                            "stationCongestions": station["stationCongestions"],
                            "totalCongestions": station["totalCongestions"],
                            "bestBoardings": station["bestBoardings"],
                            "comfortBoarding": station["comfortBoarding"],
                        }
                    ] if len(stations) == 0 else []
                })
            transfer_payload.append({"stations": stations})
        

        # 3. End Station 구조 조립
        end_station_payload = {
            "stationId": end_station_data["stationId"],
            "stationName": end_station_data.get("stationName", ""),
            "lineName": end_station_data.get("lineName", ""),
            "NextStationName": end_station_data.get("NextStationName", None),
            "estimatedWaitingSec": end_station_data.get("estimatedWaitingSec", 0),
            "results": []
        }

        # 4. 최종 JSON 완성 (형이 준 구조 그대로)
        final_payload = {
            "estimatedBoardingTime" :estimated_boarding_time,
            "boardingProbability": boarding_probability,
            "startStation": start_station_payload,
            "transferStation": transfer_payload,
            "endStation": end_station_payload
        }

        # 5. Redis 저장
        try:
            self.redis.setex(redis_key, 60, json.dumps(final_payload, ensure_ascii=False))
            print(f"🚀 [SDK] 전체 JSON 저장 완료: {redis_key}")
        except Exception as e:
            print(f"💥 [SDK] 저장 실패: {e}")
    
    def save_route_list_json(self, 
                             redis_key,           # 저장할 Key path:full:{출발Id}:{도착ID}:{요일}:{시간}

                             routes,
                             departure_times,
                             boarding_probability
                             ):
        """
        저장 형식
        {    
            "context": {
                "redisKey": "path:pred:221:748:WED:1800"
            },
                    
            "results": [
                {
                "route": [
                    {
                    "stationId": "221"
                    },
                    {
                    "stationId": "233"
                    },
                    {
                    "stationId": "748"
                    }
                ],
                "schedule": [
                    {
                    "departureTime": "18:03:00"
                    "boardingProbability": 0
                    },
                    {
                    "departureTime": "18:33:00"
                    "boardingProbability": 92
                    }
                ]
                }
            ]
        }
        """
        

        # 4. 최종 JSON 완성 (형이 준 구조 그대로)
        final_payload = {
            "context":{"redisKey":redis_key},
            "results": [
                {
                    "route":[
                        {"stationId" : id} for id in routes
                    ],
                    "schedule":[
                        {"departureTime":time,
                         "boardingProbability":probability 
                        }
                        for time, probability in zip(departure_times, boarding_probability)
                    ]
                }
            ],
        }

        # 5. Redis 저장
        try:
            self.redis.setex(redis_key, 60, json.dumps(final_payload, ensure_ascii=False))
            print(f"🚀 [SDK] 전체 JSON 저장 완료: {redis_key}")
        except Exception as e:
            print(f"💥 [SDK] 저장 실패: {e}")

    # =================================================================
    # 🔥 [DataSet 2] 실시간 전광판용 (인접 열차 3개 + 탑승가능여부)
    # =================================================================
    def save_for_station_board(self, redis_key, station_id, up_bound, down_bound):
        """
        :param station_info: { "stationId": 221, "stationName": "역삼역", "lineName": "2호선" }
        :param up_bound_list: 상행선 열차 정보 리스트 (최대 3개)
        :param down_bound_list: 하행선 열차 정보 리스트 (최대 3개)
        """
        context = {"redisKey": redis_key}
        station = {"stationId":station_id}
        upbound = []
        downbound = []

        for left in down_bound:
            result = {}
            result["direction"] = 0
            result["isBoardable"] = left["isBoardable"]
            result["carCongestions"] = left["carCongestions"]
            downbound.append(result)
        
        for right in up_bound:
            result = {}
            result["direction"] = 1
            result["isBoardable"] = right["isBoardable"]
            result["carCongestions"] = right["carCongestions"]
            upbound.append(result)
        

        
        # 1. 형님이 준 JSON 구조 그대로 조립
        payload = {
            "context": context,
            "station": station,     # 역 정보 객체
            "upBound": upbound,    # 상행선 리스트 (isBoardable 포함)
            "downBound": downbound # 하행선 리스트 (isBoardable 포함)
        }
        
        # 2. Redis Key: realtime:역ID (예: realtime:221)
        # 역 ID는 station_info 딕셔너리에서 꺼내서 씁니다.
        try:
            # 3. 저장 (TTL 120초 - 실시간이니까 짧게)
            self.redis.setex(redis_key, 120, json.dumps(payload, ensure_ascii=False))
            print(f"🚀 [SDK] 실시간 전광판 데이터 저장 완료: {redis_key}")
            
        except KeyError:
            print("💥 [SDK] 에러: station_info에 'stationId'가 없습니다.")
        except Exception as e:
            print(f"💥 [SDK] 저장 실패: {e}")

    def _save_to_redis(self, key, data, ttl):
        try:
            json_str = json.dumps(data, ensure_ascii=False)
            self.redis.setex(key, ttl, json_str)
            print(f"🚀 [SDK] 저장 완료: {key}")
        except Exception as e:
            print(f"💥 [SDK] 저장 실패: {e}")