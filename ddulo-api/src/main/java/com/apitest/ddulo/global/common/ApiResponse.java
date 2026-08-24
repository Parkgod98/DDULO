package com.apitest.ddulo.global.common;

import com.apitest.ddulo.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "API 공통 응답 DTO")
public class ApiResponse<T> {
    @Schema(description = "결과 상태")
    private final String result; // SUCCESS, FAIL
    @Schema(description = "에러 메시지")
    private final String message;
    @Schema(description = "응답 데이터")
    private final T data;
    @Schema(description = "에러 코드")
    private final ErrorCode errorCode;

    // 성공 시
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data, null);
    }
    
    // 성공이지만 데이터는 없을 때 (예: 삭제 성공)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("SUCCESS", null, null, null);
    }

    // 실패 시 (ErrorCode 사용)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>("FAIL", errorCode.getMessage(), null, errorCode);
    }

    // 실패 시 (메시지 직접 입력)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>("FAIL", message, null, errorCode);
    }

    private ApiResponse(String result, String message, T data, ErrorCode errorCode) {
        this.result = result;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }
}
