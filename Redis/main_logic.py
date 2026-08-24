import sys
import os
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
sys.path.append(parent_dir)

import pandas as pd
import ast

from Congestion_Algorithm import calculate
from Congestion_Algorithm.calculate import CongestionCalculation
from subway_sdk import SubwayHelper

# =================================================================
# 과거 평균 통계 데이터 저장
# ============================================================
def process_statistical_data():
    helper = SubwayHelper()

    base_path = 'Congestion_Algorithm/data/'
    past_average_congestion_path = base_path + 'past_average_congestion.csv'
    df = pd.read_csv(past_average_congestion_path)

    for idx, row in df.iterrows():
        helper.save_statistical_data(
            station_id=row['stationCode'],
            time_slot=str(row['hh']).zfill(2) + str(row['mm']).zfill(2),
            day_of_week=row['dow'],
            direction=row['updnLine'],
            car=ast.literal_eval(row['congestionCar']),
            station=ast.literal_eval(row['platformCongestion']),
            total=ast.literal_eval(row['totalCongestion']),
            off_rate=ast.literal_eval(row['getOffCarRate']),
            is_boardable=ast.literal_eval(row['isBoardable']),
            comfort_boarding=ast.literal_eval(row['comfortBoarding']),
        )

def process_platform_area_data():
    helper = SubwayHelper()

    base_path = 'Congestion_Algorithm/data/'
    past_average_congestion_path = base_path + 'past_average_congestion.csv'
    df = pd.read_csv(past_average_congestion_path)
    df_unique = df[['stationCode', 'updnLine']].drop_duplicates()

    dummy_platform_area = [
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
        [5.46, 5.46, 9.66, 9.66],
    ]

    for idx, row in df_unique.iterrows():
        helper.save_platform_area(
            station_id=row['stationCode'],
            direction=row['updnLine'],
            platform_area=dummy_platform_area
        )

# =================================================================
# 리스트 저장형태를 dict로 변환
# =================================================================
def convert_list_to_dict(infos):
    if not infos:
        return []
    
    infos_dict = []

    # if len(infos) == 1:
    #     infos_dict.append({
    #         "carNo": infos[0],
    #         "doorNo": 2
    #     })

    if len(infos) == 2:
        infos_dict.append({
            "carNo": infos[0],
            "doorNo": infos[1]
        })

    elif type(infos[0]) == list:
        for carNo, info in enumerate(infos):
            for doorNo, value in enumerate(info):
                infos_dict.append({
                    "carNo": carNo + 1,
                    "doorNo": doorNo + 1,
                    "congestionLevel": value
                })

    elif len(infos) <= 10:
        for carNo, value in enumerate(infos):
            infos_dict.append({
                "carNo": carNo + 1,
                "congestionLevel": value
            })

    return infos_dict


# =================================================================
# 전체 경로 최적화 계산 및 저장 (출발역 + 환승역들)
# =================================================================
def optimized_path_calculate_and_save(day_of_week, start_station_id, end_station_id, transfer_station_ids, start_direction, transfer_directions, start_time, transfer_times, start_fast_boarding, transfer_fast_boardings):
    '''
    Args:
        day_of_week (str): 
            요일
            예시: "MON"

        start_station_id (int): 
            출발역 ID
            예시: 101
        start_direction (int): 
            출발역 방향
            예시: 1  # 상행선
        start_time (list[str]): 
            출발역에서 탈 수 있는 열차들의 시각 (hh:mm)
            예시: ["02:00", "03:20", "05:20"]
        start_fast_boarding (list[int]): 
            출발역의 빠른 환승 칸 위치 (칸 번호)
            예시: [1, 4]  # 1-4 칸이 빠른 환승 가능
            
        transfer_station_ids (list[int]):
            환승역 ID 리스트 (순서대로)
            예시: [222, 333]  # 첫 번째 환승역, 두 번째 환승역
        transfer_directions (list[int]): 
            환승역 방향 리스트 (순서대로)
            예시: [1, 2]  # 첫 번째 환승역, 두 번째 환승역
        transfer_times (list[list[str]]): 
            각 환승역에서 탈 수 있는 열차들의 시각 (hh:mm)
            예시: [
                ["02:00", "03:20", "05:00"],
                ["03:00", "06:00", "09:00"]
            ]
        transfer_fast_boardings (list[list[int]]): 
            각 환승역의 빠른 환승 칸 위치 (칸 번호)
            예시: [
                [2, 1],      # 첫 번째 환승역: 2-1 칸
                [1, 3]    # 두 번째 환승역: 1-3 칸
            ]
        
        end_station_id (int):
            도착역 ID
            예시: 222
    
    Saves:
        start_car_congestions (list[int]): 
            출발역의 차량별 혼잡도
            예시: [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
        start_platform_congestions (list[list[dict]]): 
            출발역의 승강장 혼잡도
            예시: [
                [10, 20, 30, 40],  # 1호차
                [50, 60, 70, 80],  # 2호차
                ...
            ]
        start_best_boardings (list[int]): 
            출발역의 추천 탑승 칸 위치
            예시: [1, 5]
        start_comfort_boarding (list[int]):
            출발역의 쾌적 탑승 칸 위치
            예시: [1, 1]
        start_isboardable (list[bool]):
            출발역의 시간별 탑승 가능 여부
            예시: [True, False, True]
        
        transfer_car_congestions (list[list[int]]): 
            각 환승역의 차량별 혼잡도
            예시: [
                [20, 30, 40, 50, 60, 70, 80, 90, 100, 110],  # 첫 번째 환승역
                [15, 25, 35, 45, 55, 65, 75, 85, 95, 105]   # 두 번째 환승역
            ]
        transfer_platform_congestions (list[list[list[int]]]): 
            각 환승역의 승강장 혼잡도
            예시: [
                [[10, 20, 30, 40],  # 1호차
                [50, 60, 70, 80],  # 2호차
                ...
                ],  # 첫 번째 환승역
                [[5, 15, 25, 35],  # 1호차
                [45, 55, 65, 75],  # 2호차
                ...
                ]   # 두 번째 환승역
            ]
        transfer_best_boardings (list[list[int]]): 
            각 환승역의 추천 탑승 칸 위치
            예시: [
                [2, 6],  # 첫 번째 환승역
                [3, 7]   # 두 번째 환승역
            ]
        transfer_comfort_boardings (list[list[int]]):
            각 환승역의 쾌적 탑승 칸 위치
            예시: [
                [2, 2],  # 첫 번째 환승역
                [3, 3]   # 두 번째 환승역
            ]
        transfer_isboardables (list[list[bool]]):
            각 환승역의 시간별 탑승 가능 여부
            예시: [
                [True, False, True],   # 첫 번째 환승역
                [False, True, False]   # 두 번째 환승역
            ]
    '''
    helper = SubwayHelper()

    if not start_time or len(start_time) == 0:
        print(f"❌ Error: start_time이 비어있습니다.")
        return
    
    ## id, direction, fast_boarding 타입 int로 변환
    start_station_id = int(start_station_id)
    start_direction = int(start_direction)
    start_fast_boarding = [int(x) for x in start_fast_boarding]
    transfer_station_ids = [int(x) for x in transfer_station_ids]
    transfer_directions = [int(x) for x in transfer_directions]
    transfer_fast_boardings = [[int(y) for y in x] for x in transfer_fast_boardings]
    end_station_id = int(end_station_id)

    hh, mm = map(int, start_time[0].split(':'))
    time_slot = str(hh).zfill(2) + str(mm).zfill(2)

    start_station_datas = []
    estimated_boarding_time = ""

    start_data = {}
    start_data['stationId'] = start_station_id
    start_data['direction'] = start_direction

    # 출발역 열차 혼잡도 조회(CSV)
    df_start = pd.read_csv(f'../Congestion_Algorithm/data/tmap_puzzle_filtering_car.csv')
    df_start = df_start[
        (df_start['dow']==day_of_week)
         & (df_start['stationCode']==start_station_id)
         & (df_start['updnLine']==start_direction)
         & (df_start['hh']==hh)
         & (df_start['mm']==mm)
    ]
    if not df_start.empty:
        start_data["carCongestions"] = ast.literal_eval(df_start['congestionCar'].iloc[0])
    else:
        start_data["carCongestions"] = [0]*10
        print(f"⚠️ CSV에 데이터 없음: station={start_station_id}, direction={start_direction}, time={hh}:{mm}")

    # 출발역 승강장 인원 조회(MQTT)
    platform_counts = helper.get_current_platform_data()
    # 출발역 승강장 면적 조회(Redis)
    platform_area = helper.get_platform_area(start_station_id, start_direction)
    
    # ---------------------------------------------------------
    # [수정 1] 출발역 하차 인원 조회 (안전하게 get 사용)
    # ---------------------------------------------------------
    start_stat_data = helper.get_statistical_data(start_station_id, time_slot, day_of_week, start_direction)
    if start_stat_data is None:
        off_rates = [0] * 10
        print(f"⚠️ 통계 데이터가 None입니다: station={start_station_id}, time_slot={time_slot}")
    else:
        off_rates = start_stat_data.get('offRates', [0] * 10)

    # 출발역 승강장 혼잡도 계산
    cal_congestion = CongestionCalculation()
    start_data["stationCongestions"] = cal_congestion.calculate_platform_congestion(platform_counts=platform_counts, platform_area=platform_area)

    # 출발역 최적 탑승장 계산
    start_data["totalCongestions"], start_data["bestBoardings"], start_data["comfortBoarding"], boarding_possibility = \
       cal_congestion.calculate_boarding_and_possibility(
                car_congestions=start_data["carCongestions"],
                off_rates=off_rates,
                fast_boarding=start_fast_boarding
            )
    start_data["isBoardable"] = False if boarding_possibility == 0 else True
    if start_data["isBoardable"]:
        estimated_boarding_time = start_time[0]

    start_data["carCongestions"] = convert_list_to_dict(start_data["carCongestions"])
    start_data["stationCongestions"] = convert_list_to_dict(start_data["stationCongestions"])
    start_data["totalCongestions"] = convert_list_to_dict(start_data["totalCongestions"])
    start_data["bestBoardings"] = convert_list_to_dict(start_data["bestBoardings"])
    start_data["comfortBoarding"] = convert_list_to_dict(start_data["comfortBoarding"])

    start_station_datas.append(start_data)

    # 출발역 시간2,3 탑승가능성 조회(Redis)
    start_data["carCongestions"] = []
    start_data["stationCongestions"] = []
    start_data["totalCongestions"] = []
    start_data["bestBoardings"] = []
    start_data["comfortBoarding"] = []
    
    for time in start_time[1:]:
        next_start_data = {}  # 새 딕셔너리 생성
        next_start_data['stationId'] = start_station_id
        next_start_data['direction'] = start_direction
        next_start_data["carCongestions"] = []
        next_start_data["stationCongestions"] = []
        next_start_data["totalCongestions"] = []
        next_start_data["bestBoardings"] = []
        next_start_data["comfortBoarding"] = []

        hh, mm = map(int, time.split(':'))
        time_slot = str(hh).zfill(2) + str(mm).zfill(2)
        
        # ---------------------------------------------------------
        # [수정 2] 탑승 가능성 조회 (안전하게 get 사용)
        # ---------------------------------------------------------
        stat_data_next = helper.get_statistical_data(start_station_id, time_slot, day_of_week, start_direction)
        if stat_data_next is None:
            isboardable = [False]
            print(f"⚠️ 통계 데이터가 None입니다: station={start_station_id}, time_slot={time_slot}")
        else:
            isboardable = stat_data_next.get('isBoardable', [False])

        # 출발역 최적 탑승장 계산
        start_data["isBoardable"] = any(isboardable)

        if start_data["isBoardable"] and estimated_boarding_time == "":
            estimated_boarding_time = time
        
        start_station_datas.append(start_data)


    transfer_stations_meta = []

    for transfer_station_id, transfer_direction, times, fast_boarding in zip(transfer_station_ids, transfer_directions, transfer_times, transfer_fast_boardings):
        if not times or len(times) == 0:
            print(f"⚠️ 환승역 {transfer_station_id}의 시간 데이터가 비어있습니다.")
            continue

        transfer_station_datas = []

        for time in times:
            transfer_data = {}
            transfer_data['stationId'] = transfer_station_id
            transfer_data['direction'] = transfer_direction

            hh, mm = map(int, time.split(':'))
            time_slot = str(hh).zfill(2) + str(mm).zfill(2)

            data = helper.get_statistical_data(transfer_station_id, time_slot, day_of_week, transfer_direction)
            
            # ---------------------------------------------------------
            # [수정 3] 환승역 데이터 조회 (전부 get으로 변경)
            # ---------------------------------------------------------
            # 환승역 열차 혼잡도 조회(Redis) - 없으면 0으로 채운 리스트
            if data is None:
                print(f"⚠️ 환승역 통계 데이터가 None입니다: station={transfer_station_id}, time_slot={time_slot}")
                transfer_data['carCongestions'] = convert_list_to_dict([0]*10)
                transfer_data['stationCongestions'] = convert_list_to_dict([0]*10)
                transfer_data['totalCongestions'] = convert_list_to_dict([0]*10)
                transfer_data['isBoardable'] = False
                transfer_data['bestBoardings'] = convert_list_to_dict([])
                transfer_data['comfortBoarding'] = convert_list_to_dict([0]*10)
            else:
                transfer_data['carCongestions'] = convert_list_to_dict(data.get('carCongestions', [0]*10))
                transfer_data['stationCongestions'] = convert_list_to_dict(data.get('stationCongestions', [0]*10))
                transfer_data['totalCongestions'] = convert_list_to_dict(data.get('totalCongestions', [0]*10))
                transfer_data['isBoardable'] = any(data.get('isBoardable', [False]))
                transfer_data['bestBoardings'] = convert_list_to_dict(calculate.calculate_best_boarding(data.get('isBoardable', [False]), fast_boarding))
                transfer_data['comfortBoarding'] = convert_list_to_dict(data.get('comfortBoarding', [0]*10))

            transfer_station_datas.append(transfer_data)

        transfer_stations_meta.append(transfer_station_datas)
    
    end_station_data = {"stationId": end_station_id}

    helper.save_full_route_json(
        redis_key=f'path:full:{start_station_id}:{end_station_id}:{day_of_week}:{start_time[0].replace(":", "")}',
        estimated_boarding_time=estimated_boarding_time,
        boarding_probability=boarding_possibility,
        start_station_datas=start_station_datas,
        transfer_stations_meta=transfer_stations_meta,
        end_station_data=end_station_data,
    )

    route = []
    route.append(start_station_id)
    route.extend(transfer_station_ids) # 변수명 오타 수정 (transfer_station_id -> transfer_station_ids)
    route.append(end_station_id)

    departure_time = []
    departure_time.append(start_time[0])
    departure_time.append(estimated_boarding_time)
    boarding_probability = []
    if estimated_boarding_time == start_time[0]:
        boarding_probability.append(boarding_possibility)
        boarding_probability.append(boarding_possibility)
    else:
        boarding_probability.append(0)
        boarding_probability.append(boarding_possibility)

    helper.save_route_list_json(
        redis_key=f'path:pred:{start_station_id}:{end_station_id}:{day_of_week}:{start_time[0].replace(":", "")}',
        routes=route,
        departure_times=departure_time,
        boarding_probability=boarding_probability
    )

# =================================================================
# 특정 방향(상행/하행) 데이터 생성
# =================================================================
def get_station_data_by_direction(
    direction: int,
    times: list,
    station_id: int,
    day_of_week: int,
    helper,
    cal_congestion,
    df_congestion
):
    """
    특정 방향(direction)에 대한 데이터를 처리하여 리스트로 반환하는 함수
    direction: 0 (상행/내선/Left), 1 (하행/외선/Right)
    """
    datas = []
    
    if not times:
        return datas

    # ---------------------------------------------------------
    # 1. 첫 번째 시간대 (실시간/CSV 기반) 처리
    # ---------------------------------------------------------
    first_time_str = times[0]
    t_hh, t_mm = map(int, first_time_str.split(':'))
    time_slot = str(t_hh).zfill(2) + str(t_mm).zfill(2)

    current_data = {'direction': direction}

    # CSV 필터링
    df_filtered = df_congestion[
        (df_congestion['dow'] == day_of_week) &
        (df_congestion['stationCode'] == station_id) &
        (df_congestion['updnLine'] == direction) &
        (df_congestion['hh'] == t_hh) & 
        (df_congestion['mm'] == t_mm)   
    ]

    # 열차 혼잡도 (CSV or Default)
    if not df_filtered.empty:
        current_data["carCongestions"] = ast.literal_eval(df_filtered['congestionCar'].iloc[0])
    else:
        current_data["carCongestions"] = [0] * 10

    # 출발역 승강장 인원 조회 (MQTT)
    platform_counts = helper.get_current_platform_data()
    
    # 출발역 승강장 면적 조회 (Redis)
    platform_area = helper.get_platform_area(station_id, direction)
    
    # ---------------------------------------------------------
    # [수정] 출발역 하차 인원 조회 (안전하게 get 사용)
    # ---------------------------------------------------------
    stat_data = helper.get_statistical_data(station_id, time_slot, day_of_week, direction)
    if stat_data is None:
        off_rates = [0] * 10
    else:
        off_rates = stat_data.get('offRates', [0] * 10)

    # 승강장 혼잡도 계산
    current_data["stationCongestions"] = cal_congestion.calculate_platform_congestion(
        platform_counts=platform_counts, 
        platform_area=platform_area
    )

    # 탑승 가능성 계산
    boarding_possibility = cal_congestion.calculate_possibility(
        car_congestions=current_data["carCongestions"],
        off_rates=off_rates
    )
    current_data["isBoardable"] = False if boarding_possibility == 0 else True
    
    # 리스트를 딕셔너리로 변환
    current_data["carCongestions"] = convert_list_to_dict(current_data["carCongestions"])

    datas.append(current_data)

    # ---------------------------------------------------------
    # 2. 이후 시간대 (통계/Redis 기반) 처리
    # ---------------------------------------------------------
    for time in times[1:]:
        next_data = {'direction': direction}
        
        t_hh, t_mm = map(int, time.split(':'))
        time_slot = str(t_hh).zfill(2) + str(t_mm).zfill(2)

        # 통계 데이터 조회 (Redis)
        stat_data = helper.get_statistical_data(station_id, time_slot, day_of_week, direction)
        
        # ---------------------------------------------------------
        # [수정 5] 데이터 없을 경우 안전한 기본값 사용
        # ---------------------------------------------------------
        # 열차 혼잡도 (없으면 0 리스트)

        if stat_data is None:
            next_data['carCongestions'] = convert_list_to_dict([0]*10)
            next_data["isBoardable"] = False
            print(f"⚠️ 통계 데이터가 None입니다: station={station_id}, time_slot={time_slot}")
        else:
            car_congestions = stat_data.get('carCongestions', [0]*10)
            next_data['carCongestions'] = convert_list_to_dict(car_congestions)
            # 탑승 가능성 (없으면 빈 리스트 -> any() 결과 False)
            isboardable_list = stat_data.get('isBoardable', []) 
            next_data["isBoardable"] = any(isboardable_list)
        
        datas.append(next_data)

    return datas

def station_status_calculate_and_save(day_of_week, station_id, left_times, right_times):
    '''
    Args:
        day_of_week (str): 
            요일
            예시: "MON"

        station_id (int): 
            역 ID
            예시: 101
        left_times (list[str]): 
            역 왼쪽에서 탈 수 있는 열차들의 시각 (hh:mm)
            예시: ["02:00", "03:20", "05:20"]
        
        right_times (list[str]): 
            역 오른쪽에서 탈 수 있는 열차들의 시각 (hh:mm)
            예시: ["02:10", "03:30", "05:40"]
    
    Saves:
        start_car_congestions (list[list[int]]): 
            출발역의 차량별 혼잡도
            예시: [
                [10, 20, 30, 40, 50, 60, 70, 80, 90, 100],
                [10, 20, 30, 40, 50, 60, 70, 80, 90, 100],
                [10, 20, 30, 40, 50, 60, 70, 80, 90, 100],
            ]
        start_isboardable (list[bool]):
            출발역의 시간별 탑승 가능 여부
            예시: [True, False, True]
    '''
    station_id = int(station_id)
    
    helper = SubwayHelper()
    cal_congestion = CongestionCalculation()

    # CSV는 한 번만 로드하여 재사용 (성능 최적화)
    df_start = pd.read_csv(f'../Congestion_Algorithm/data/tmap_puzzle_filtering_car.csv')

    # 1. Left Data (Direction 0) 생성
    left_datas = get_station_data_by_direction(
        direction=0,
        times=left_times,
        station_id=station_id,
        day_of_week=day_of_week,
        helper=helper,
        cal_congestion=cal_congestion,
        df_congestion=df_start
    )

    # 2. Right Data (Direction 1) 생성
    right_datas = get_station_data_by_direction(
        direction=1,
        times=right_times, # right_times 변수가 있다고 가정
        station_id=station_id,
        day_of_week=day_of_week,
        helper=helper,
        cal_congestion=cal_congestion,
        df_congestion=df_start
    )

    helper.save_for_station_board(
        redis_key=f'stat:near:{station_id}:{day_of_week}:{left_times[0].replace(":", "")}',
        station_id=station_id,
        up_bound=right_datas, # direction 1
        down_bound=left_datas # direction 0
    )


if __name__ == "__main__":
    # process_statistical_data()
    # process_platform_area_data()
    print("Statistical data and platform area data processing completed.")

    # optimized_path_calculate_and_save(
    #     day_of_week="MON",
    #     start_station_id=221,
    #     start_direction=1,
    #     start_time=["18:00", "18:10", "18:20"],
    #     start_fast_boarding=[1, 4],
    #     transfer_station_ids=[744],
    #     transfer_directions=[1],
    #     transfer_times=[["18:30", "18:40", "18:50"]],
    #     transfer_fast_boardings=[[2, 1]],
    #     end_station_id=999,
    # )
    # station_status_calculate_and_save(
    #     day_of_week="MON",
    #     station_id=221,
    #     left_times=["18:00", "18:10", "18:20"],
    #     right_times=["18:00", "18:10", "18:20"]
    # )

    print("Optimized path calculation and saving completed.")