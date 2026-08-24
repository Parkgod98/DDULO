# api_server.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import uvicorn

# 기존 모듈 import
from main_logic import process_platform_area_data, process_statistical_data
from main_logic import optimized_path_calculate_and_save, station_status_calculate_and_save

app = FastAPI()

# 요청 데이터 구조 정의 (DTO)
class OptimizedPathRequest(BaseModel):
    day_of_week: str
    start_station_id: int
    start_direction: int
    start_time: List[str]
    start_fast_boarding: List[int]
    transfer_station_ids: List[int]
    transfer_directions: List[int]
    transfer_times: List[List[str]]
    transfer_fast_boardings: List[List[int]]
    end_station_id: int

class StationStatusRequest(BaseModel):
    day_of_week: str
    station_id: int
    left_times: List[str]
    right_times: List[str]

@app.post("/calculate/path")
def calculate_path(req: OptimizedPathRequest):
    try:
        optimized_path_calculate_and_save(
            day_of_week=req.day_of_week,
            start_station_id=req.start_station_id,
            start_direction=req.start_direction,
            start_time=req.start_time,
            start_fast_boarding=req.start_fast_boarding,
            transfer_station_ids=req.transfer_station_ids,
            transfer_directions=req.transfer_directions,
            transfer_times=req.transfer_times,
            transfer_fast_boardings=req.transfer_fast_boardings,
            end_station_id=req.end_station_id
        )
        return {"status": "success", "message": "Path calculation saved to Redis"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/calculate/station")
def calculate_status(req: StationStatusRequest):
    try:
        station_status_calculate_and_save(
            day_of_week=req.day_of_week,
            station_id=req.station_id,
            left_times=req.left_times,
            right_times=req.right_times
        )
        return {"status": "success", "message": "Station status saved to Redis"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    process_statistical_data()
    process_platform_area_data()
    uvicorn.run(app, host="0.0.0.0", port=8001)