package org.pharosnet.vertx.faas.core.exceptions;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;

public class NotFoundException extends ServiceException {

    private static final int code = 404;
    private static final String message = "内容未发现！";

    public static <T> AsyncResult<T> fail(Throwable throwable) {
        return Future.failedFuture(new NotFoundException(throwable));
    }

    public static <T> AsyncResult<T> fail(String reason) {
        return Future.failedFuture(new NotFoundException(reason));
    }

    public static <T> AsyncResult<T> fail(int statusCode, String reason) {
        return Future.failedFuture(new NotFoundException(statusCode, reason));
    }


    public static <T> AsyncResult<T> fail(String reason, Throwable throwable) {
        return Future.failedFuture(new NotFoundException(reason, throwable));
    }

    public static <T> AsyncResult<T> fail(int statusCode, String reason, Throwable throwable) {
        return Future.failedFuture(new NotFoundException(statusCode, reason, throwable));
    }

    public NotFoundException(Throwable throwable) {
        this(code, throwable);
    }

    public NotFoundException(int statusCode, Throwable throwable) {
        super(statusCode, message);
        super.getDebugInfo().put("message", throwable.getMessage());
        super.getDebugInfo().put("cause", throwable);
    }

    public NotFoundException(String reason) {
        this(code, reason);
    }

    public NotFoundException(int statusCode, String reason) {
        super(statusCode, message);
        super.getDebugInfo().put("message", reason);
    }

    public NotFoundException(String reason, Throwable throwable) {
        this(code, reason, throwable);
    }

    public NotFoundException(int statusCode, String reason, Throwable throwable) {
        super(statusCode, message);
        super.getDebugInfo().put("message", reason);
        super.getDebugInfo().put("cause", throwable);
    }

}
