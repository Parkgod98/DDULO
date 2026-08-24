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


class VideoConfTestLogger:
    def __init__(self):
        self.all_results = []
        self.csv_filename = os.path.join("vdCSVs", f"vd_test_conf_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv")
        
    def log_result(self, conf_threshold, video_name, results):
        """테스트 결과 기록"""
        result_data = {
            'conf_threshold': conf_threshold,
            'video_name': video_name,
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
            'conf_threshold', 'video_name', 'timestamp', 'total_frames', 'avg_people_per_frame',
            'max_people', 'min_people', 'avg_processing_time_ms', 'total_processing_time_s',
            'fps', 'zone_1_1_avg', 'zone_1_2_avg', 'zone_1_3_avg', 'zone_1_4_avg'
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
        print("📊 영상 신뢰도 테스트 결과 요약")
        print("=" * 80)
        
        # 신뢰도별 그룹화
        conf_groups = {}
        for result in self.all_results:
            conf = result['conf_threshold']
            if conf not in conf_groups:
                conf_groups[conf] = []
            conf_groups[conf].append(result)
        
        # 신뢰도별 통계
        print(f"{'신뢰도':<8} {'평균인원':<8} {'최대인원':<8} {'평균FPS':<8} {'처리시간(ms)':<12} {'테스트수':<8}")
        print("-" * 80)
        
        baseline_avg = 0
        
        for conf in sorted(conf_groups.keys()):
            results = conf_groups[conf]
            avg_people = sum(r['avg_people_per_frame'] for r in results) / len(results)
            max_people = max(r['max_people'] for r in results)
            avg_fps = sum(r['fps'] for r in results) / len(results)
            avg_time = sum(r['avg_processing_time_ms'] for r in results) / len(results)
            
            print(f"{conf:<8} {avg_people:<8.1f} {max_people:<8} {avg_fps:<8.1f} {avg_time:<12.1f} {len(results):<8}")
            
            if conf == 0.25:
                baseline_avg = avg_people
        
        # 기준 대비 비교
        print("\n🎯 기준(0.25) 대비 비교:")
        print("-" * 40)
        for conf in sorted(conf_groups.keys()):
            if conf != 0.25:
                results = conf_groups[conf]
                avg_people = sum(r['avg_people_per_frame'] for r in results) / len(results)
                diff = avg_people - baseline_avg
                change = "▲" if diff > 0 else "▼" if diff < 0 else "→"
                print(f"신뢰도 {conf}: {avg_people:.1f}명 ({change}{abs(diff):.1f})")
        
        # 추천
        print(f"\n💡 추천:")
        stable_results = [r for r in self.all_results if r['fps'] >= 15]  # 15FPS 이상만 고려
        if stable_results:
            conf_counts = {}
            for r in stable_results:
                conf = r['conf_threshold']
                conf_counts[conf] = conf_counts.get(conf, 0) + 1
            
            best_conf = max(conf_counts, key=conf_counts.get)
            print(f"   가장 안정적인 신뢰도: {best_conf}")
            print(f"   이유: 15FPS 이상 처리 가능한 경우가 가장 많음")
        else:
            print("   15FPS 이상의 신뢰도가 없습니다. 더 높은 신뢰도를 고려해보세요.")


def process_single_video_conf(video_path, detector, roi_man, conf_threshold, save_video=True):
    """단일 영상 처리 로직 (신뢰도 조정)"""
    start_time = time.time()
    
    # 비디오 캡처
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"❌ 비디오 열기 실패: {video_path}")
        return None
    
    # 비디오 정보
    fps = cap.get(cv2.CAP_PROP_FPS)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    
    print(f"  📹 {total_frames}프레임, {fps:.1f}FPS, {width}x{height}")
    
    # 비디오 라이터 설정
    video_writer = None
    if save_video:
        name, ext = os.path.splitext(os.path.basename(video_path))
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        output_filename = f"{name}_{timestamp}_c{conf_threshold}.mp4"
        output_path = os.path.join("vdOutput", output_filename)
        
        fourcc = cv2.VideoWriter_fourcc(*'mp4v')
        video_writer = cv2.VideoWriter(output_path, fourcc, fps, (width, height))
    
    # 프레임 처리 변수
    frame_count = 0
    total_people = 0
    max_people = 0
    min_people = float('inf')
    processing_times = []
    zone_totals = {'1-1': 0, '1-2': 0, '1-3': 0, '1-4': 0}
    
    # 프레임별 처리
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
        frame_start = time.time()
        
        # 객체 탐지 및 추적
        _, tracked_objects = detector.detect_and_track_frame(frame)
        
        # 구역별 인원 집계
        zone_counts = {name: 0 for name in roi_man.rois.keys()}
        zone_track_ids = {name: set() for name in roi_man.rois.keys()}
        frame_people = 0
        
        for obj in tracked_objects:
            bbox = obj['bbox']
            track_id = obj['track_id']
            
            # track_id가 -1이면 추적 ID가 없는 경우, 건너뛰기
            if track_id == -1:
                continue
                
            zone_name, _ = roi_man.check_entry(bbox)
            if zone_name:
                if track_id not in zone_track_ids[zone_name]:
                    zone_track_ids[zone_name].add(track_id)
                    zone_counts[zone_name] += 1
                    frame_people += 1
        
        # 통계 업데이트
        total_people += frame_people
        max_people = max(max_people, frame_people)
        min_people = min(min_people, frame_people)
        
        for zone_name, count in zone_counts.items():
            zone_totals[zone_name] += count
        
        # 처리 시간 기록
        frame_time = (time.time() - frame_start) * 1000
        processing_times.append(frame_time)
        
        # 처리된 화면 그리기
        if save_video and video_writer:
            display_frame = roi_man.draw_rois(frame, zone_counts)
            
            for obj in tracked_objects:
                x1, y1, x2, y2 = obj['bbox']
                track_id = obj['track_id']
                
                # track_id가 -1이면 표시하지 않기
                if track_id == -1:
                    continue
                
                # 바운딩 박스 그리기
                cv2.rectangle(display_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
                
                # Track ID 표시
                label = f"ID:{track_id}"
                cv2.putText(display_frame, label, (x1, y1 - 10), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            
            # UI 오버레이
            cv2.rectangle(display_frame, (20, 20), (450, 100), (0, 0, 0), -1)
            info_text1 = f"Conf:{conf_threshold} Frame:{frame_count+1}/{total_frames}"
            info_text2 = f"People:{frame_people} FPS:{1000/frame_time:.1f}"
            cv2.putText(display_frame, info_text1, (35, 45), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 255), 2, cv2.LINE_AA)
            cv2.putText(display_frame, info_text2, (35, 75), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 255), 2, cv2.LINE_AA)
            
            video_writer.write(display_frame)
        
        frame_count += 1
        
        # 진행률 표시
        if frame_count % 30 == 0:
            progress = (frame_count / total_frames) * 100
            print(f"    진행률: {progress:.1f}% ({frame_count}/{total_frames})")
    
    # 자원 해제
    cap.release()
    if video_writer:
        video_writer.release()
    
    total_processing_time = time.time() - start_time
    
    # 결과 데이터 준비
    results = {
        'total_frames': frame_count,
        'avg_people_per_frame': round(total_people / frame_count, 2) if frame_count > 0 else 0,
        'max_people': max_people,
        'min_people': min_people if min_people != float('inf') else 0,
        'avg_processing_time_ms': round(sum(processing_times) / len(processing_times), 2) if processing_times else 0,
        'total_processing_time_s': round(total_processing_time, 2),
        'fps': round(frame_count / total_processing_time, 2) if total_processing_time > 0 else 0,
        'zone_1_1_avg': round(zone_totals['1-1'] / frame_count, 2) if frame_count > 0 else 0,
        'zone_1_2_avg': round(zone_totals['1-2'] / frame_count, 2) if frame_count > 0 else 0,
        'zone_1_3_avg': round(zone_totals['1-3'] / frame_count, 2) if frame_count > 0 else 0,
        'zone_1_4_avg': round(zone_totals['1-4'] / frame_count, 2) if frame_count > 0 else 0
    }
    
    if save_video and video_writer:
        print(f"    ✅ 영상 저장: {output_path}")
    
    return results


def controlled_video_conf_test():
    """통제된 영상 신뢰도 테스트 메인 함수"""
    print("🎯 통제 영상 신뢰도 테스트 시작")
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
    logger = VideoConfTestLogger()
    
    # 출력 폴더 생성
    os.makedirs("vdOutput", exist_ok=True)
    os.makedirs("vdCSVs", exist_ok=True)
    
    # 입력 폴더 확인
    input_folder = "vdInput"
    if not os.path.exists(input_folder):
        print(f"❌ 입력 폴더가 없습니다: {input_folder}")
        return
    
    # 비디오 파일 목록 가져오기
    video_extensions = ['.mp4', '.avi', '.mov', '.mkv', '.wmv']
    video_files = []
    
    for file in os.listdir(input_folder):
        if any(file.lower().endswith(ext) for ext in video_extensions):
            video_files.append(file)
    
    if not video_files:
        print(f"❌ {input_folder} 폴더에 비디오가 없습니다.")
        return
    
    print(f"📁 {len(video_files)}개 비디오 발견")
    
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
        
        # 각 비디오 처리
        for i, video_file in enumerate(video_files):
            input_path = os.path.join(input_folder, video_file)
            
            print(f"  [{i+1}/{len(video_files)}] {video_file}")
            
            # 비디오 처리
            result = process_single_video_conf(input_path, det, roi_man, conf_threshold, save_video=True)
            
            if result:
                # 결과 기록
                logger.log_result(conf_threshold, video_file, result)
                print(f"    ✅ 평균인원: {result['avg_people_per_frame']:.1f}명, FPS: {result['fps']:.1f}")
            else:
                print(f"    ❌ 실패")
    
    # CSV 저장 및 요약
    logger.save_csv()
    logger.print_summary()


if __name__ == "__main__":
    controlled_video_conf_test()
