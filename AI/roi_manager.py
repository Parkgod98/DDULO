import cv2
import yaml
import numpy as np

class ROIManager:
    def __init__(self, config_file='config.yaml'):
        self.config_file = config_file
        self.rois = {}
        self.load_config()

    def load_config(self):
        try:
            with open(self.config_file, 'r') as f:
                data = yaml.load(f, Loader=yaml.UnsafeLoader)
                self.rois = data.get('ROI_SETTINGS', {})
            print(f"✅ ROI 설정 로드 완료: {len(self.rois)}개 구역")
        except FileNotFoundError:
            print("⚠️ 설정 파일이 없습니다.")
            self.rois = {}

    def get_anchor_point(self, bbox):
        """
        [수정됨] 박스 -> 발 좌표 변환 (측면 비율 보정 + 바닥 클램핑 포함)
        - 함수명: get_anchor_point 유지
        - 인수: bbox 하나만 받음
        - 높이: 형님이 1080으로 맞춘다고 하셨으니 1080 기준으로 고정
        """
        x1, y1, x2, y2 = map(int, bbox)
        
        # 형님이 알아서 맞춘다고 하신 그 높이
        IMG_HEIGHT = 1080 
        
        # 1. [맨 앞줄] 발이 화면 끝에 닿아있으면 그냥 바닥으로 침
        if y2 >= IMG_HEIGHT - 10:
            return ((x1 + x2) // 2, IMG_HEIGHT - 50)

        w = x2 - x1
        h = y2 - y1
        if w == 0: return ((x1 + x2) // 2, y2-50)

        # 2. [비율 보정] 측면 승객 가림 현 상 해결
        current_ratio = h / w 
        
        # 측면 비율(2.5)보다 작으면 상반신만 보인다고 판단 -> 다리 길이 강제 늘림
        if current_ratio < 2.0:
            # 측면(Side)은 얇으니까 너비 대비 키를 3.8배로 잡음
            estimated_total_height = int(w * 2.5) 
            estimated_y2 = y1 + estimated_total_height
            
            # 3. [Clamping] 늘린 다리가 화면 밖으로 나가면 1079에서 멈춤
            final_y = min(estimated_y2, IMG_HEIGHT - 50)
            
            return ((x1 + x2) // 2, final_y)

        # 정상적인 경우 (그냥 발 위치 리턴)
        return ((x1 + x2) // 2, y2-50)

    def check_entry(self, bbox):
        """Task 48: 좌표가 어느 구역에 있는지 확인"""
        point = self.get_anchor_point(bbox)
        
        for name, region_points in self.rois.items():
            pts_np = np.array(region_points, np.int32)
            if cv2.pointPolygonTest(pts_np, point, False) >= 0:
                return name, point # 포함된 구역 이름 리턴
        
        return None, point

    def draw_rois(self, frame, zone_counts=None):
        """
        [수정됨] 구역별 인원수(Count) 시각화
        zone_counts: {'1-1': 3, '1-2': 5} 형태의 딕셔너리
        """
        if not self.rois: return frame
        if zone_counts is None: zone_counts = {}

        overlay = frame.copy()
        output = frame.copy()
        alpha = 0.3

        for name, points in self.rois.items():
            pts = np.array(points, np.int32).reshape((-1, 1, 2))
            
            # 현재 구역의 인원수 가져오기 (없으면 0)
            count = zone_counts.get(name, 0)

            # 인원이 많으면 색을 진하게 할 수도 있지만, 일단은 통일
            # 인원 0명: 회색, 1명 이상: 초록색
            if count > 0:
                color = (0, 255, 0) # Green (사람 있음)
                thickness = 2
            else:
                color = (100, 100, 100) # Gray (사람 없음)
                thickness = 1

            # 영역 그리기
            cv2.fillPoly(overlay, [pts], color)
            cv2.polylines(output, [pts], True, color, thickness, cv2.LINE_AA)
            
            # ★ 핵심: 구역 이름 + 인원수 표시
            # 예: "1-1 (3)"
            label = f"{name} : {count}"
            
            # 텍스트 위치 (구역의 첫 번째 점)
            text_pos = tuple(points[0])
            
            # 글씨 잘 보이게 검은 테두리 + 흰 글씨
            cv2.putText(output, label, text_pos, cv2.FONT_HERSHEY_SIMPLEX, 
                       0.8, (0, 0, 0), 4, cv2.LINE_AA)
            cv2.putText(output, label, text_pos, cv2.FONT_HERSHEY_SIMPLEX, 
                       0.8, (255, 255, 255), 2, cv2.LINE_AA)

        cv2.addWeighted(overlay, alpha, output, 1 - alpha, 0, output)
        return output
