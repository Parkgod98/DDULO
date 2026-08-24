STD_DENSITY = 4.3   # 면적당 기준 인원 (명/m^2)
TRAIN_CAPACITY = 160 # 열차 1량당 기준 인원 (명)

PLATFORM_THRESHOLD = 100.0
TOTAL_THRESHOLD = 150.0


def calculate_best_boarding(isboardings, fast_boarding):
    """
    최적 탑승칸 계산
    Args:
        isboardings (list): 탑승 가능 칸 정보
            예시: [True, True, False, True, True, False, True, True, False, True]
        fast_boarding (list): 빠른 하차/환승 탑승 칸 정보
            예시: [1, 1] # 1호차 1게이트가 빠른 하차/환승 탑승
    Returns:
        list: 최적 탑승칸
            예시 : [1, 3]
    """

    optimal_boarding_idx = (fast_boarding[0] - 1) * 4 + (fast_boarding[1] - 1)
    visited = [0]*len(isboardings)

    queue = [optimal_boarding_idx] # 탑승칸 인덱스
    visited[optimal_boarding_idx] = 1

    while queue:
        idx = queue.pop(0)

        if isboardings[idx]:
            return [idx // 4 + 1, idx % 4 + 1]
        # 좌우 탐색
        for neighbor in [idx -1, idx +1]:
            if 0 <= neighbor < len(isboardings) and not visited[neighbor]:
                visited[neighbor] = 1
                queue.append(neighbor)
                
    return []

class CongestionCalculation():
    def __init__(self):
        pass

    def calculate_platform_congestion(self, platform_counts, platform_area):
        """
        승강장 혼잡도 계산 (게이트별)

        Args:
            platform_counts (dict): 게이트별 대기인원 수
                예시: {
                    "1-1": 50,  # 1호차 1번 게이트에 50명 대기
                    "1-2": 30,
                    ...
                }
            platform_area (list): 게이트별 면적(m^2)
                예시 : [
                    [50.0, 40.0, 50.0, 40.0],
                    [50.0, 40.0, 50.0, 40.0],
                    ...
                ]

        Returns:
            list: 게이트별 혼잡도 정보
                예시: [
                    [70.0, 80.0, 90.0, 100.0],  # 1호차
                    [110.0, 120.0, 130.0, 140.0],  # 2호차
                    ...
                ]
        """
        # self.platform_counts = platform_counts
        self.platform_area = platform_area
        self.platform_counts = []
        
        results = []
        for car_idx, car_area in enumerate(platform_area):
            car_congestion = []
            for door_idx, area in enumerate(car_area):
                gate_key = f"{car_idx + 1}-{door_idx + 1}"
                count = platform_counts.get(gate_key, 0)
                self.platform_counts.append(count)
                
                # 공식: (인원 / 면적) / 4.3 * 100
                density = count / area
                congestion = (density / STD_DENSITY) * 100
                
                car_congestion.append(round(congestion, 2))
            results.append(car_congestion)

        self.platform_congestion = results
        return results

    def calculate_total_congestion(self, car_congestions, off_rates):
        """
        차량별 종합 혼잡도 계산

        Args:
            car_congestions (list): 차량별 혼잡도 (%)
                예시: [80, 90, 100, 110, 120, 130, 140, 150, 160, 170]
            off_rates (list): 과거 하차 비율 (%)
                예시: [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]

        Returns:
            list: 차량별 혼잡도 정보
                예시: [
                    [80, 90, 100, 110],  # 1호차
                    [120, 130, 140, 150],  # 2호차
                    ...
                ]
        """
        # 종합 혼잡도 공식 적용
        # 공식: 열차혼잡도 * (1 - (과거하차비율/100)) + 승강장 인원 * (100 / 160)
        total_congestion_list = []
        platform_iter = [iter(self.platform_counts)] * 4
        platform_groups = zip(*platform_iter)

        for car_congestion, off_rate, doors in zip(car_congestions, off_rates, platform_groups):
            total_congestion = [
                car_congestion * (1 - (off_rate / 100.0)) + int(count * (100 / TRAIN_CAPACITY)) for count in doors
            ]
            total_congestion_list.append(total_congestion)

        self.total_congestion = total_congestion_list
        return total_congestion_list


    def calculate_boarding_and_possibility(self, car_congestions, off_rates, fast_boarding):
        """
        최적 탑승칸, 쾌적 탑승칸, 탑승 가능성 계산

        Args:
            car_congestions (list): 차량별 혼잡도 (%)
                예시: [80, 90, 100, 110, 120, 130, 140, 150, 160, 170]
            fast_boarding (list): 빠른 하차/환승 탑승 칸 정보
                예시: [1, 1] # 1호차 1게이트가 빠른 하차/환승 탑승 칸
            
        Returns:
            list: 최적 탑승칸
                예시 : [1, 3]
            list: 쾌적 탑승칸
                예시 : [2, 4]
            float : 탑승 가능성(%)
                예시 : 85
        """
        self.calculate_total_congestion(car_congestions=car_congestions, off_rates=off_rates)

        isboardings = [] # 탑승 가능 칸 10*4
        comfortable_boarding = [] # 쾌적 탑승 칸
        comfortable_congestion = float('inf')

        for car_no, congestion in enumerate(self.total_congestion):
            for door_no, cong_value in enumerate(congestion):
                if cong_value > TOTAL_THRESHOLD or self.platform_congestion[car_no][door_no] > PLATFORM_THRESHOLD:
                    isboardings.append(False)
                else:
                    isboardings.append(True)
                    if cong_value < comfortable_congestion:
                        comfortable_congestion = cong_value
                        comfortable_boarding = [car_no + 1, door_no + 1]
        
        # 탑승할 수 있는 칸이 하나도 없으면 None 반환
        if comfortable_congestion == float('inf'):
            return [], [], [], 0
        
        # 최적 탑승칸 계산
        # fast_boarding에서 가장 가까운 탑승가능칸 선택, 양옆으로 이동, bfs 탐색
        optimal_boarding = calculate_best_boarding(isboardings, fast_boarding)

        # 탑승 가능성 계산
        # (1- comfortable_congestion/TOTAL_THRESHOLD) * 100
        boarding_possibility = round((1 - comfortable_congestion / TOTAL_THRESHOLD) * 100, 2)

        return self.total_congestion, optimal_boarding, comfortable_boarding, boarding_possibility
    
    def calculate_possibility(self, car_congestions, off_rates):
        """
        탑승 가능성 계산

        Args:
            car_congestions (list): 차량별 혼잡도 (%)
                예시: [80, 90, 100, 110, 120, 130, 140, 150, 160, 170]
            
        Returns:
            float : 탑승 가능성(%)
                예시 : 85
        """
        self.calculate_total_congestion(car_congestions=car_congestions, off_rates=off_rates)

        isboardings = [] # 탑승 가능 칸 10*4
        comfortable_congestion = float('inf')

        for car_no, congestion in enumerate(self.total_congestion):
            for door_no, cong_value in enumerate(congestion):
                if cong_value > TOTAL_THRESHOLD or self.platform_congestion[car_no][door_no] > PLATFORM_THRESHOLD:
                    isboardings.append(False)
                else:
                    isboardings.append(True)
                    if cong_value < comfortable_congestion:
                        comfortable_congestion = cong_value
        
        # 탑승할 수 있는 칸이 하나도 없으면 None 반환
        if comfortable_congestion == float('inf'):
            return 0

        # 탑승 가능성 계산
        # (1- comfortable_congestion/TOTAL_THRESHOLD) * 100
        boarding_possibility = round((1 - comfortable_congestion / TOTAL_THRESHOLD) * 100, 2)

        return boarding_possibility