package com.pigs.voxly.api.shared;

import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.ErrorType;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public final class ResultMapper {

    private ResultMapper() {
    }

    public static ResponseEntity<ApiResponse<Void>> toResponse(Result result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.ok());
        }
        return toErrorResponse(result.getErrors());
    }

    public static <T> ResponseEntity<ApiResponse<T>> toResponse(ResultT<T> result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.ok(result.getValue()));
        }
        return toErrorResponse(result.getErrors());
    }

    public static <T> ResponseEntity<ApiResponse<T>> toCreatedResponse(ResultT<T> result) {
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result.getValue()));
        }
        return toErrorResponse(result.getErrors());
    }

    public static <T> ApiResponse<T> extractErrors(ResultT<?> result) {
        List<ApiResponse.ApiError> apiErrors = result.getErrors().stream()
                .map(e -> new ApiResponse.ApiError(e.getCode(), e.getMessage()))
                .toList();
        return ApiResponse.error(apiErrors);
    }

    private static <T> ResponseEntity<ApiResponse<T>> toErrorResponse(List<Error> errors) {
        HttpStatus status = mapErrorTypeToStatus(errors.getFirst().getType());

        List<ApiResponse.ApiError> apiErrors = errors.stream()
                .map(e -> new ApiResponse.ApiError(e.getCode(), e.getMessage()))
                .toList();

        return ResponseEntity.status(status).body(ApiResponse.error(apiErrors));
    }

    private static HttpStatus mapErrorTypeToStatus(ErrorType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNEXPECTED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
