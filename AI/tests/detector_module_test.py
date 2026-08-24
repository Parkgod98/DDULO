import cv2
from detector import Detector

def main():
    print("🧪 Detector 모듈 테스트 시작")
    
    # 1. 클래스 생성 (사람만 잡는지 확인)
    det = Detector(model_path="yolov8n_best.engine", conf_threshold=0.5)

    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("카메라 안 됨")
        return

    print("q를 누르면 종료됩니다.")

    while True:
        ret, frame = cap.read()
        if not ret: break

        # 2. 모듈 사용 (깔끔!)
        result_img, infos = det.detect_frame(frame)

        # 3. 데이터 잘 넘어오는지 로그 확인
        if infos:
            print(f"👥 감지된 사람 수: {len(infos)}명")
            for info in infos:
                print(f"   -> 좌표: {info['bbox']}, 신뢰도: {info['conf']}")

        cv2.imshow("Module Test", result_img)
        if cv2.waitKey(1) == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()