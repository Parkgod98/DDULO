import os
import redis
import logging
from dotenv import load_dotenv

# .env 로드
load_dotenv()

# 로거 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("RedisClient")

class RedisManager:
    _instance = None
    
    # [설정] .env에서 읽어오기
    REDIS_CONFIG = {
        'host': os.getenv("REDIS_HOST", "localhost"),
        'port': int(os.getenv("REDIS_PORT", 6379)),
        'password': os.getenv("REDIS_PASSWORD", None),
        'db': int(os.getenv("REDIS_DB", 0)),
        'decode_responses': True,
        'socket_timeout': 5
    }

    def __new__(cls):
        """Singleton 패턴"""
        if not cls._instance:
            cls._instance = super(RedisManager, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return

        try:
            # 커넥션 풀 생성
            self.pool = redis.ConnectionPool(**self.REDIS_CONFIG)
            self.client = redis.StrictRedis(connection_pool=self.pool)
            
            # 접속 테스트
            self.client.ping()
            logger.info(f"✅ Redis Connected: {self.REDIS_CONFIG['host']}:{self.REDIS_CONFIG['port']}")
            self._initialized = True

        except Exception as e:
            logger.error(f"❌ Redis Connection Error: {e}")
            self.client = None

    def get_client(self):
        """Redis 클라이언트 객체 반환"""
        if self.client is None:
            logger.warning("⚠️ Redis client lost. Reconnecting...")
            self._initialized = False
            self.__init__()
        return self.client