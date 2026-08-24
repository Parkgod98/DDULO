import argparse
import csv
import time
from detector import Detector
import cv2


def run_for_threshold(source, conf, frames):
    det = Detector(model_path='models/yolov8n_best.engine', conf_threshold=conf)
    cap = cv2.VideoCapture(source)
    if not cap.isOpened():
        raise RuntimeError(f"Cannot open source: {source}")

    counts = []
    timings = []
    for i in range(frames):
        ret, frame = cap.read()
        if not ret:
            break
        t0 = time.time()
        _, detections = det.detect_frame(frame)
        t1 = time.time()
        counts.append(len(detections))
        timings.append(t1 - t0)

    cap.release()
    return counts, timings


def summarize_counts(counts, timings):
    import statistics
    if not counts:
        return {}
    return {
        'frames': len(counts),
        'avg_count': statistics.mean(counts),
        'median_count': statistics.median(counts),
        'std_count': statistics.pstdev(counts),
        'pct_zero': sum(1 for c in counts if c == 0) / len(counts),
        'avg_time': statistics.mean(timings) if timings else 0.0
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', default=0, help='Video source (0 for webcam or path)')
    parser.add_argument('--start', type=float, default=0.2)
    parser.add_argument('--end', type=float, default=0.8)
    parser.add_argument('--step', type=float, default=0.05)
    parser.add_argument('--frames', type=int, default=120)
    parser.add_argument('--out', default='tune_results.csv')
    args = parser.parse_args()

    thresholds = []
    v = args.start
    while v <= args.end + 1e-9:
        thresholds.append(round(v, 3))
        v += args.step

    results = []
    for conf in thresholds:
        print(f"Running threshold={conf} ...")
        counts, timings = run_for_threshold(args.source, conf, args.frames)
        stats = summarize_counts(counts, timings)
        stats['threshold'] = conf
        results.append(stats)
        print(f" -> frames={stats.get('frames',0)} avg={stats.get('avg_count'):.2f} pct_zero={stats.get('pct_zero'):.2f} avg_time={stats.get('avg_time'):.3f}s")

    # write csv
    keys = ['threshold', 'frames', 'avg_count', 'median_count', 'std_count', 'pct_zero', 'avg_time']
    with open(args.out, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=keys)
        writer.writeheader()
        for r in results:
            writer.writerow({k: r.get(k, '') for k in keys})

    print(f"Saved results to {args.out}")


if __name__ == '__main__':
    main()
