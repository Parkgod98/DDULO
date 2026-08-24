import cv2
import os
import time
import csv
import yaml
from datetime import datetime
import sys

# 상위 디렉토리 경로 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from detector import Detector
from roi_manager import ROIManager


class IoUTestLogger:
    def __init__(self):
        self.all_results = []
        self.csv_filename = os.path.join("imgCSVs", f"img_test_iou_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv")
        
    def log_result(self, iou_threshold, image_name, results):
        """테스트 결과 기록"""
        result_data = {
            'iou_threshold': iou_threshold,
            'image_name': image_name,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            **results
        }
        self.all_results.append(result_data)
        
    def save_csv(self):
        """CSV 파일로 저장"""
        if not self.all_results:
            print("⚠️ 저장할 데이터가 없습니다.")
            return
            
        # CSV 헤더
        headers = [
            'iou_threshold', 'image_name', 'timestamp', 'total_people', 'processing_time_ms',
            'zone_1_1_count', 'zone_1_2_count', 'zone_1_3_count', 'zone_1_4_count',
            'tracked_objects', 'unique_track_ids', 'avg_confidence'
        ]
        
        try:
            with open(self.csv_filename, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.DictWriter(csvfile, fieldnames=headers)
                writer.writeheader()
                writer.writerows(self.all_results)
            print(f"✅ CSV 저장 완료: {self.csv_filename}")
        except Exception as e:
            print(f"❌ CSV 저장 실패: {e}")
    
    def print_summary(self):
        """테스트 결과 요약 출력"""
        if not self.all_results:
            return
            
        print("\n" + "=" * 80)
        print("📊 IoU 테스트 결과 요약")
        print("=" * 80)
        
        # IoU별 그룹화
        iou_groups = {}
        for result in self.all_results:
            iou = result['iou_threshold']
            if iou not in iou_groups:
                iou_groups[iou] = []
            iou_groups[iou].append(result)
        
        # IoU별 통계
        print(f"{'IoU':<8} {'평균인원':<8} {'평균신뢰도':<10} {'평균시간(ms)':<12} {'테스트수':<8}")
        print("-" * 80)
        
        baseline_avg = 0
        
        for iou in sorted(iou_groups.keys()):
            results = iou_groups[iou]
            avg_people = sum(r['total_people'] for r in results) / len(results)
            avg_conf = sum(r['avg_confidence'] for r in results) / len(results)
            avg_time = sum(r['processing_time_ms'] for r in results) / len(results)
            
            print(f"{iou:<8} {avg_people:<8.1f} {avg_conf:<10.3f} {avg_time:<12.1f} {len(results):<8}")
            
            if iou == 0.45:
                baseline_avg = avg_people
        
        # 기준 대비 비교
        print("\n🎯 기준(0.45) 대비 비교:")
        print("-" * 40)
        for iou in sorted(iou_groups.keys()):
            if iou != 0.45:
                results = iou_groups[iou]
                avg_people = sum(r['total_people'] for r in results) / len(results)
                diff = avg_people - baseline_avg
                change = "▲" if diff > 0 else "▼" if diff < 0 else "→"
                print(f"IoU {iou}: {avg_people:.1f}명 ({change}{abs(diff):.1f})")
        
        # 추천
        print(f"\n💡 추천:")
        stable_results = [r for r in self.all_results if r['total_people'] == r['unique_track_ids']]
        if stable_results:
            iou_counts = {}
            for r in stable_results:
                iou = r['iou_threshold']
                iou_counts[iou] = iou_counts.get(iou, 0) + 1
            
            best_iou = max(iou_counts, key=iou_counts.get)
            print(f"   가장 안정적인 IoU: {best_iou}")
            print(f"   이유: 객체수와 고유ID가 일치하는 경우가 가장 많음")


def process_single_image_iou(image_path, model, roi_man, iou_threshold, conf_threshold=0.25, save_image=False):
    """단일 이미지 처리 로직 (IoU 조정)"""
    start_time = time.time()
    
    # 이미지 읽기
    frame = cv2.imread(image_path)
    if frame is None:
        print(f"❌ 이미지 로드 실패: {image_path}")
        return None
    
    # 객체 탐지 (IoU 조정)
    try:
        model_results = model(frame, conf=conf_threshold, iou=iou_threshold, classes=[0], verbose=False)
        
        # 결과 처리
        tracked_objects = []
        for result in model_results:
            boxes = result.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].int().tolist()
                confidence = float(box.conf[0])
                
                tracked_objects.append({
                    'bbox': [x1, y1, x2, y2],
                    'conf': confidence,
                    'track_id': len(tracked_objects)  # 임시 ID
                })
    except Exception as e:
        print(f"   ❌ 모델 추론 실패: {e}")
        return None
    
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
    if save_image:
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
        info_text = f"IoU:{iou_threshold} Total: {total_waiting}"
        cv2.putText(display_frame, info_text, (35, 65), 
                   cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 255), 3, cv2.LINE_AA)
        
        # 이미지 저장
        name, ext = os.path.splitext(os.path.basename(image_path))
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        output_filename = f"{name}_{timestamp}_iou{iou_threshold}{ext}"
        output_path = os.path.join("imgOutput", output_filename)
        cv2.imwrite(output_path, display_frame)
    
    processing_time = (time.time() - start_time) * 1000
    
    # 결과 데이터 준비
    results = {
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
    
    return results


def controlled_iou_test():
    """통제된 IoU 테스트 메인 함수"""
    print("🎯 통제 IoU 테스트 시작")
    print("=" * 50)
    
    # 테스트할 IoU 임계값들
    iou_thresholds = [0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60]
    
    print(f"🔍 테스트 IoU: {iou_thresholds}")
    
    # 초기화
    roi_man = ROIManager(config_file='../config.yaml')
    logger = IoUTestLogger()
    
    # 모델 로드
    try:
        from ultralytics import YOLO
        model = YOLO('../models/yolov8m.pt')
        print("✅ 모델 로드 완료")
    except Exception as e:
        print(f"❌ 모델 로드 실패: {e}")
        return
    
    # 입력 폴더 확인
    input_folder = "imgInput"
    if not os.path.exists(input_folder):
        print(f"❌ 입력 폴더가 없습니다: {input_folder}")
        return
    
    # 이미지 파일 목록 가져오기
    image_extensions = ['.jpg', '.jpeg', '.png', '.bmp', '.tiff']
    image_files = []
    
    for file in os.listdir(input_folder):
        if any(file.lower().endswith(ext) for ext in image_extensions):
            image_files.append(file)
    
    if not image_files:
        print(f"❌ {input_folder} 폴더에 이미지가 없습니다.")
        return
    
    print(f"📁 {len(image_files)}개 이미지 발견")
    
    # 각 IoU별로 테스트
    for iou_threshold in iou_thresholds:
        print(f"\n🔄 IoU {iou_threshold} 테스트 중...")
        print("-" * 40)
        
        # 각 이미지 처리
        for i, image_file in enumerate(image_files):
            input_path = os.path.join(input_folder, image_file)
            
            print(f"  [{i+1}/{len(image_files)}] {image_file}")
            
            # 이미지 처리
            result = process_single_image_iou(input_path, model, roi_man, iou_threshold, save_image=True)
            
            if result:
                # 결과 기록
                logger.log_result(iou_threshold, image_file, result)
                print(f"    ✅ 인원: {result['total_people']}명, 시간: {result['processing_time_ms']:.1f}ms")
            else:
                print(f"    ❌ 실패")
    
    # CSV 저장 및 요약
    logger.save_csv()
    logger.print_summary()


if __name__ == "__main__":
    controlled_iou_test()
