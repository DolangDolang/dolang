package live.dolang.api.common.exception;

import live.dolang.api.common.response.BaseResponse;
import live.dolang.api.common.response.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(BadRequestException.class)
    public BaseResponse<BaseResponseStatus> badRequestExceptionExceptionHandler(BadRequestException exception) {
        log.error("BadRequestException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public BaseResponse<BaseResponseStatus> unauthorizedExceptionHandler(UnauthorizedException exception) {
        log.error("UnauthorizedException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(ForbiddenException.class)
    public BaseResponse<BaseResponseStatus> forbiddenExceptionHandler(ForbiddenException exception) {
        log.error("ForbiddenException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(NotFoundException.class)
    public BaseResponse<BaseResponseStatus> userProfileNotFoundExceptionHandler(NotFoundException exception) {
        log.error("NotFoundException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(InternalServerException.class)
    public BaseResponse<BaseResponseStatus> internalServerExceptionHandler(InternalServerException exception) {
        log.error("InternalServerException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(DuplicateException.class)
    public BaseResponse<BaseResponseStatus> internalServerExceptionHandler(DuplicateException exception) {
        log.error("DuplicateException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(RequestParameterException.class)
    public BaseResponse<BaseResponseStatus> requestParameterExceptionHandler(RequestParameterException exception) {
        log.error("RequestParameterException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(exception.getStatus());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<BaseResponseStatus> requestParameterExceptionHandler(MissingServletRequestParameterException exception) {
        log.error("MissingServletRequestParameterException has occurred. {} {} {}", exception.getMessage(), exception.getCause(), exception.getStackTrace()[0]);
        return BaseResponse.status(new RequestParameterException(BaseResponseStatus.INVALID_PARAMETER).getStatus());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<BaseResponseStatus> exceptionHandler(Exception exception) {
        log.error(exception.getMessage(), exception);
        return BaseResponse.status(BaseResponseStatus.INTERNAL_SERVER_ERROR);
    }
}
