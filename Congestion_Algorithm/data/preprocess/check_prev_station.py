import pandas as pd
import numpy as np

# 1. 데이터 로드
base_path = 'Congestion_Algorithm/data/'
try:
    car_data = pd.read_csv(base_path + 'tmap_puzzle_filtering_car.csv', encoding='utf-8')
    print("데이터 로드 완료")
except FileNotFoundError:
    print("파일을 찾을 수 없습니다.")
    exit()

# 2. 데이터 전처리 (결측치 제거)
# prevStationName이 없는 행(종점 등)이 있다면 제거하거나 제외해야 에러가 안 납니다.
car_data = car_data.dropna(subset=['prevStationName'])

# 3. 그룹화 및 유니크 값 추출
# 설명: 'stationName'으로 그룹을 짓고, 'prevStationName' 컬럼의 고유한 값(unique)들을 리스트로 만듭니다.
grouped_prev_stations = car_data.groupby('stationName')['prevStationName'].apply(
    lambda x: list(x.unique())
).reset_index()

# 4. 결과 출력
print(f"총 {len(grouped_prev_stations)}개의 역이 그룹화되었습니다.")
print(grouped_prev_stations.head(10))

# (선택) 특정 역 확인 예시
target_station = "강남역"
result = grouped_prev_stations[grouped_prev_stations['stationName'] == target_station]
if not result.empty:
    print(f"\n[{target_station}]의 이전 역 목록: {result['prevStationName'].values[0]}")