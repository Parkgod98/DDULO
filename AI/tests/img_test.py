import cv2
import os
import time
import csv
from datetime import datetime
import sys

# 상위 디렉토리 경로 추가 (main.py와 동일한 모듈 사용)
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from detector import Detector
from roi_manager import ROIManager


class ImageTestLogger:
    def __init__(self):
        self.csv_data = []
        self.csv_filename = f"img_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        self.csv_path = os.path.join("imgCSVs", self.csv_filename)
        
    def log_result(self, image_name, results):
        """테스트 결과를 CSV 데이터로 기록"""
        self.csv_data.append(results)
        
    def save_csv(self):
        """CSV 파일로 저장"""
        if not self.csv_data:
            print("⚠️ 저장할 데이터가 없습니다.")
            return
            
        # CSV 헤더
        headers = [
            'image_name', 'timestamp', 'total_people', 'processing_time_ms',
            'zone_1_1_count', 'zone_1_2_count', 'zone_1_3_count', 'zone_1_4_count',
            'tracked_objects', 'unique_track_ids', 'avg_confidence'
        ]
        
        try:
            with open(self.csv_path, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.DictWriter(csvfile, fieldnames=headers)
                writer.writeheader()
                writer.writerows(self.csv_data)
            print(f"✅ CSV 저장 완료: {self.csv_path}")
        except Exception as e:
            print(f"❌ CSV 저장 실패: {e}")


def process_single_image(image_path, detector, roi_man):
    """단일 이미지 처리 로직"""
    start_time = time.time()
    
    # 이미지 읽기
    frame = cv2.imread(image_path)
    if frame is None:
        print(f"❌ 이미지 로드 실패: {image_path}")
        return None
    
    # 객체 탐지 및 추적
    _, tracked_objects = detector.detect_and_track_frame(frame)
    
    # 구역별 인원 집계
    zone_counts = {name: 0 for name in roi_man.rois.keys()}
    zone_track_ids = {name: set() for name in roi_man.rois.keys()}
    total_waiting = 0
    total_confidence = 0
    
    for obj in tracked_objects:
        bbox = obj['bbox']
        track_id = obj['track_id']
        confidence = obj.get('conf', 0.0)
        
        zone_name, _ = roi_man.check_entry(bbox)
        if zone_name:
            if track_id not in zone_track_ids[zone_name]:
                zone_track_ids[zone_name].add(track_id)
                zone_counts[zone_name] += 1
                total_waiting += 1
                total_confidence += confidence
    
    # 처리된 화면 그리기
    display_frame = roi_man.draw_rois(frame, zone_counts)
    
    for obj in tracked_objects:
        x1, y1, x2, y2 = obj['bbox']
        track_id = obj['track_id']
        
        # 바운딩 박스 그리기
        cv2.rectangle(display_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
        
        # Track ID 표시
        label = f"ID:{track_id}"
        cv2.putText(display_frame, label, (x1, y1 - 10), 
                   cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
    
    # UI 오버레이
    cv2.rectangle(display_frame, (20, 20), (400, 80), (0, 0, 0), -1)
    info_text = f"Total People: {total_waiting}"
    cv2.putText(display_frame, info_text, (35, 65), 
               cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 255), 3, cv2.LINE_AA)
    
    processing_time = (time.time() - start_time) * 1000  # ms 단위
    
    # 결과 데이터 준비
    results = {
        'image_name': os.path.basename(image_path),
        'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'total_people': total_waiting,
        'processing_time_ms': round(processing_time, 2),
        'zone_1_1_count': zone_counts.get('1-1', 0),
        'zone_1_2_count': zone_counts.get('1-2', 0),
        'zone_1_3_count': zone_counts.get('1-3', 0),
        'zone_1_4_count': zone_counts.get('1-4', 0),
        'tracked_objects': len(tracked_objects),
        'unique_track_ids': len(set([obj['track_id'] for obj in tracked_objects])),
        'avg_confidence': round(total_confidence / len(tracked_objects), 3) if tracked_objects else 0
    }
    
    return display_frame, results


def batch_process_images():
    """전체 배치 처리 메인 함수"""
    print("🚀 [Image Test Start] 이미지 배치 처리 테스트")
    print("=" * 50)
    
    # 1. 핵심 부품 장착
    roi_man = ROIManager(config_file='../config.yaml')
    
    # 모델 파일 확인 및 자동 다운로드
    model_path = '../models/yolov8m.pt'
    if not os.path.exists(model_path):
        print("🔄 YOLOv8m 모델 다운로드 중...")
        from ultralytics import YOLO
        YOLO('yolov8m.pt')  # 자동 다운로드
        print("✅ 모델 다운로드 완료")
    
    det = Detector(model_path=model_path, conf_threshold=0.25)
    
    # 2. 로거 초기화
    logger = ImageTestLogger()
    
    # 3. 입력 폴더 확인
    input_folder = "imgInput"
    output_folder = "imgOutput"
    
    if not os.path.exists(input_folder):
        print(f"❌ 입력 폴더가 없습니다: {input_folder}")
        return
    
    os.makedirs(output_folder, exist_ok=True)
    
    # 4. 이미지 파일 목록 가져오기
    image_extensions = ['.jpg', '.jpeg', '.png', '.bmp', '.tiff']
    image_files = []
    
    for file in os.listdir(input_folder):
        if any(file.lower().endswith(ext) for ext in image_extensions):
            image_files.append(file)
    
    if not image_files:
        print(f"❌ {input_folder} 폴더에 이미지가 없습니다.")
        return
    
    print(f"📁 {len(image_files)}개 이미지 발견")
    
    # 5. 이미지 처리 루프
    processed_count = 0
    total_processing_time = 0
    
    for image_file in image_files:
        input_path = os.path.join(input_folder, image_file)
        
        # 출력 파일명: 이미지이름_YYYYMMDD_HHMMSS.확장자
        name, ext = os.path.splitext(image_file)
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        output_filename = f"{name}_{timestamp}{ext}"
        output_path = os.path.join(output_folder, output_filename)
        
        print(f"🔄 처리 중: {image_file}")
        
        # 이미지 처리
        result = process_single_image(input_path, det, roi_man)
        
        if result:
            processed_frame, results_data = result
            
            # 처리된 이미지 저장
            cv2.imwrite(output_path, processed_frame)
            
            # CSV 데이터 기록
            logger.log_result(image_file, results_data)
            
            processed_count += 1
            total_processing_time += results_data['processing_time_ms']
            
            print(f"   ✅ 완료 - 인원: {results_data['total_people']}명, "
                  f"처리시간: {results_data['processing_time_ms']:.2f}ms")
            print(f"   📁 저장: {output_filename}")
        else:
            print(f"   ❌ 실패")
    
    # 6. CSV 저장
    logger.save_csv()
    
    # 7. 통계 출력
    print("\n" + "=" * 50)
    print("📊 처리 결과 요약")
    print(f"   처리된 이미지: {processed_count}/{len(image_files)}")
    print(f"   평균 처리 시간: {total_processing_time/processed_count:.2f}ms")
    print(f"   총 처리 시간: {total_processing_time:.2f}ms")
    print(f"   CSV 파일: {logger.csv_path}")
    
    if processed_count > 0:
        total_people = sum(data['total_people'] for data in logger.csv_data)
        print(f"   총 탐지 인원: {total_people}명")
        print(f"   평균 인원/이미지: {total_people/processed_count:.1f}명")


if __name__ == "__main__":
    batch_process_images()
