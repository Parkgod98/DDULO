import pandas as pd
import numpy as np
import ast

PLATFORM_AREA = 30.24
CAR_CAPACITY = 160
STD_DENSITY = 4.3

PLATFORM_THRESHOLD = 100.0
TOTAL_THRESHOLD = 150.0

print("[1] Loading Data...")
try:
    base_path = 'Congestion_Algorithm/data/'
    car_data = pd.read_csv(base_path + 'tmap_puzzle_filtering_car.csv', encoding='utf-8')
    get_off_data = pd.read_csv(base_path + 'tmap_puzzle_filtering_get-off.csv', encoding='utf-8')
    avg_get_in_data = pd.read_csv(base_path + 'average_subway_get-on-and-off_by_hour.csv', encoding='utf-8')
    print(f" -> Done (Car: {car_data.shape}, Get-off: {get_off_data.shape})")
except Exception as e:
    print(f"[Error] {e}")
    exit()

def safe_literal_eval(val):
    if isinstance(val, str):
        try:
            return ast.literal_eval(val)
        except (ValueError, SyntaxError):
            return []
    return val

if 'congestionCar' in car_data.columns:
    car_data['congestionCar'] = car_data['congestionCar'].apply(safe_literal_eval)

if 'getOffCarRate' in get_off_data.columns:
    get_off_data['getOffCarRate'] = get_off_data['getOffCarRate'].apply(safe_literal_eval)

def calculate_average_by_group(df, value_col):
    group_cols = [
        'subwayLine', 'stationName', 'stationCode', 'updnLine', 
        'dow', 'hh', 'mm', 'prevStationName', 'prevStationCode'
    ]
    existing_cols = [c for c in group_cols if c in df.columns]

    if not existing_cols:
        return pd.DataFrame()

    def list_mean(series):
        matrix = np.array(series.tolist())
        if matrix.ndim == 1: 
            return matrix.tolist()
        return np.mean(matrix, axis=0).tolist()

    return df.groupby(existing_cols)[value_col].apply(list_mean).reset_index()

print("[2] Processing Groups...")
df_car_avg = calculate_average_by_group(car_data, 'congestionCar') if 'congestionCar' in car_data.columns else pd.DataFrame()
df_getoff_avg = calculate_average_by_group(get_off_data, 'getOffCarRate') if 'getOffCarRate' in get_off_data.columns else pd.DataFrame()

merge_keys = [c for c in df_car_avg.columns if c in df_getoff_avg.columns and c != 'congestionCar' and c != 'getOffCarRate']

df_merged = pd.merge(df_car_avg, df_getoff_avg, on=merge_keys, how='inner')

print("[3] Matching Next Station...")
df_next_lookup = df_merged[['dow', 'hh', 'mm', 'updnLine', 'prevStationCode', 'congestionCar']].copy()
df_next_lookup.rename(columns={'congestionCar': 'next_station_congestion'}, inplace=True)

df_final = pd.merge(
    df_merged,
    df_next_lookup,
    left_on=['dow', 'hh', 'mm', 'updnLine', 'stationCode'],
    right_on=['dow', 'hh', 'mm', 'updnLine', 'prevStationCode'],
    how='left',
    suffixes=('', '_next_dup')
)
df_final = df_final.loc[:, ~df_final.columns.str.endswith('_next_dup')]
# df_final = df_final.dropna(subset=['next_station_congestion'])

print("[4] Calculating Congestion...")

df_final['totalCongestion'] = df_final['next_station_congestion'].apply(
    lambda x: [[val, val, val, val] for val in x] if isinstance(x, list) else []
)

def calc_platform_congestion(row):
    if not isinstance(row['next_station_congestion'], list) or not row['congestionCar']:
        return [], []

    curr = np.array(row['congestionCar'])
    next_s = np.array(row['next_station_congestion'])
    off_rate = np.array(row['getOffCarRate'])

    if len(curr) != len(next_s):
        return [], []

    rem_ratio = curr * (1 - (off_rate / 100.0))
    boarding = np.maximum(next_s - rem_ratio, 0)
    # boarding==0이면, 탑승인원=하차인원
    boarding = np.where(boarding == 0, curr * (off_rate / 100.0), boarding)
    people = boarding * (CAR_CAPACITY / 100.0)
    
    # avg_get_in_data에서 해당 호선, 역명에 맞는 평균 승차 인원 가져오기
    avg_row = avg_get_in_data[(avg_get_in_data['호선'] == row['subwayLine']) & 
                               (avg_get_in_data['역명'] == row['stationName'])]
    if not avg_row.empty:
        avg_value = avg_row[f'avg_{row["hh"]}'].values[0]
    else:
        avg_value = 0
    
    # 조건에 따라 요소별로 congestion 계산
    congestion = np.where(
        (next_s > TOTAL_THRESHOLD) & (avg_value > people),
        avg_value,
        (people / PLATFORM_AREA / STD_DENSITY) * 100
    )

    # return np.round(people, 2).tolist(), np.round(congestion, 2).tolist()
    people_nested = [[int(np.ceil(p/4))] * 4 for p in people] # 문당 인원으로 나눔 (올림)
    congestion_nested = [[int(np.ceil(c))] * 4 for c in congestion] # 혼잡도는 값 유지 (올림)

    return people_nested, congestion_nested

df_final[['platformPeople', 'platformCongestion']] = df_final.apply(calc_platform_congestion, axis=1, result_type='expand')

'''
탑승가능성 & 가장 쾌적한 칸 계산하기
1. platformCongestion이 PLATFORM_THRESHOLD 미만일경우 탑승가능
2. totalCongestion이 TOTAL_THRESHOLD 미만일경우 탑승가능
3. 칸별로 탑승가능 여부 확인
쾌적한 칸: totalCongestion이 가장 낮은 칸

Returns:
    list: 차량별 탑승 가능성 정보
            예시: [True, True, False, True, True, False, True, True, False, True]
'''
def determine_boardable(row):
    platform_cong = row['platformCongestion']
    total_cong = row['totalCongestion']

    comfort_boarding = [] # [칸, 문] 번호 저장용
    comfort_value = float('inf')
    boardable_list = [] # 1차원 리스트 (총 40개 원소)

    if not isinstance(platform_cong, list) or not isinstance(total_cong, list):
        return [], []

    # 1. 칸(car_idx)과 문(door_idx)을 모두 순회할 수 있도록 중첩 루프 사용
    for car_idx, (p_car, t_car) in enumerate(zip(platform_cong, total_cong)):
        # p_car와 t_car는 각각 4개의 문 데이터를 가진 리스트
        for door_idx, (p_val, t_val) in enumerate(zip(p_car, t_car)):
            
            # 탑승 가능 여부 판단 (문 단위)
            is_ok = p_val < PLATFORM_THRESHOLD and t_val < TOTAL_THRESHOLD
            boardable_list.append(is_ok)

            # 쾌적 탑승칸 계산 (탑승 가능한 칸 중 가장 혼잡도가 낮은 문 선택)
            if is_ok and t_val < comfort_value:
                comfort_value = t_val
                # 요청하신 대로 [칸 번호, 문 번호] 형식으로 저장 (1부터 시작)
                comfort_boarding = [car_idx + 1, door_idx + 1]

    return comfort_boarding, boardable_list

# isBoardable, comfortBoarding
df_final[['comfortBoarding', 'isBoardable']] = df_final.apply(determine_boardable, axis=1, result_type='expand')

output_path = base_path + 'past_average_congestion.csv'
df_final.to_csv(output_path, index=False, encoding='utf-8-sig')

print(f"[Complete] Saved to {output_path}")
print(df_final[['stationCode', 'congestionCar', 'platformCongestion', 'totalCongestion']].head())