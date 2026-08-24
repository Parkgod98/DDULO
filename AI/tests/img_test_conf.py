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


class ControlledTestLogger:
    def __init__(self):
        self.all_results = []
        self.csv_filename = os.path.join("imgCSVs", f"img_test_controlled_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv")
        
    def log_result(self, conf_threshold, image_name, results):
        """테스트 결과 기록"""
        result_data = {
            'conf_threshold': conf_threshold,
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
            'conf_threshold', 'image_name', 'timestamp', 'total_people', 'processing_time_ms',
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
        print("📊 통제 테스트 결과 요약")
        print("=" * 80)
        
        # 신뢰도별 그룹화
        conf_groups = {}
        for result in self.all_results:
            conf = result['conf_threshold']
            if conf not in conf_groups:
                conf_groups[conf] = []
            conf_groups[conf].append(result)
        
        # 신뢰도별 통계
        print(f"{'신뢰도':<8} {'평균인원':<8} {'평균신뢰도':<10} {'평균시간(ms)':<12} {'테스트수':<8}")
        print("-" * 80)
        
        baseline_avg = 0
        
        for conf in sorted(conf_groups.keys()):
            results = conf_groups[conf]
            avg_people = sum(r['total_people'] for r in results) / len(results)
            avg_conf = sum(r['avg_confidence'] for r in results) / len(results)
            avg_time = sum(r['processing_time_ms'] for r in results) / len(results)
            
            print(f"{conf:<8} {avg_people:<8.1f} {avg_conf:<10.3f} {avg_time:<12.1f} {len(results):<8}")
            
            if conf == 0.35:
                baseline_avg = avg_people
        
        # 기준 대비 비교
        print("\n🎯 기준(0.35) 대비 비교:")
        print("-" * 40)
        for conf in sorted(conf_groups.keys()):
            if conf != 0.35:
                results = conf_groups[conf]
                avg_people = sum(r['total_people'] for r in results) / len(results)
                diff = avg_people - baseline_avg
                change = "▲" if diff > 0 else "▼" if diff < 0 else "→"
                print(f"신뢰도 {conf}: {avg_people:.1f}명 ({change}{abs(diff):.1f})")
        
        # 추천
        print(f"\n💡 추천:")
        if baseline_avg > 0:
            stable_results = [r for r in self.all_results if r['total_people'] == r['unique_track_ids']]
            if stable_results:
                conf_counts = {}
                for r in stable_results:
                    conf = r['conf_threshold']
                    conf_counts[conf] = conf_counts.get(conf, 0) + 1
                
                best_conf = max(conf_counts, key=conf_counts.get)
                print(f"   가장 안정적인 신뢰도: {best_conf}")
                print(f"   이유: 객체수와 고유ID가 일치하는 경우가 가장 많음")


def process_single_image(image_path, detector, roi_man, conf_threshold, save_image=False):
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
        info_text = f"Conf:{conf_threshold} Total: {total_waiting}"
        cv2.putText(display_frame, info_text, (35, 65), 
                   cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 255), 3, cv2.LINE_AA)
        
        # 이미지 저장
        name, ext = os.path.splitext(os.path.basename(image_path))
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        output_filename = f"{name}_{timestamp}_c{conf_threshold}{ext}"
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


def controlled_accuracy_test():
    """통제된 정확도 테스트 메인 함수"""
    print("🎯 통제 정확도 테스트 시작")
    print("=" * 50)
    
    # 설정 파일 로드
    try:
        with open('img_test.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
    except FileNotFoundError:
        print("❌ img_test.yaml 파일이 없습니다.")
        return
    
    print(f"📋 테스트 설정: {config['test_config']['description']}")
    print(f"🔍 테스트 신뢰도: {config['test_config']['confidence_thresholds']}")
    
    # 초기화
    roi_man = ROIManager(config_file='../config.yaml')
    logger = ControlledTestLogger()
    
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
    
    # 각 신뢰도별로 테스트
    for conf_threshold in config['test_config']['confidence_thresholds']:
        print(f"\n🔄 신뢰도 {conf_threshold} 테스트 중...")
        print("-" * 40)
        
        # Detector 생성
        model_path = config['test_config']['model']['path']
        if not os.path.exists(model_path):
            print(f"🔄 모델 다운로드 중...")
            from ultralytics import YOLO
            YOLO('yolov8m.pt')
            print("✅ 모델 다운로드 완료")
        
        det = Detector(model_path=model_path, conf_threshold=conf_threshold)
        
        # 각 이미지 처리
        for i, image_file in enumerate(image_files):
            input_path = os.path.join(input_folder, image_file)
            
            print(f"  [{i+1}/{len(image_files)}] {image_file}")
            
            # 이미지 처리
            result = process_single_image(input_path, det, roi_man, conf_threshold, save_image=True)
            
            if result:
                # 결과 기록
                logger.log_result(conf_threshold, image_file, result)
                print(f"    ✅ 인원: {result['total_people']}명, 시간: {result['processing_time_ms']:.1f}ms")
            else:
                print(f"    ❌ 실패")
    
    # CSV 저장 및 요약
    logger.save_csv()
    logger.print_summary()


if __name__ == "__main__":
    controlled_accuracy_test()
