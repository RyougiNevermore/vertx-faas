package org.pharosnet.vertx.faas.engine.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.pharosnet.vertx.faas.core.components.MessageConsumerRegister;
import org.pharosnet.vertx.faas.engine.http.config.HttpConfig;
import org.pharosnet.vertx.faas.engine.http.router.AbstractHttpRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(HttpVerticle.class);

    public HttpVerticle(MessageConsumerRegister register, AbstractHttpRouter httpRouter) {
        this.register = register;
        this.httpRouter = httpRouter;
    }

    private Http http;
    private final AbstractHttpRouter httpRouter;
    private final MessageConsumerRegister register;

    public void register() {
        if (this.register == null) {
            return;
        }
        register.register(this.vertx);
    }

    public Future<Void> unregister() {
        if (this.register == null) {
            return Future.succeededFuture();
        }
        return this.register.unregister();
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("NATIVE 开启 {}", vertx.isNativeTransportEnabled());
        }
        String basePackage = this.config().getJsonObject("_faasOptions").getString("basePackage");
        this.http = new Http(this.vertx, basePackage, HttpConfig.mapFrom(this.config().getJsonObject("http")));

        this.http.run(this.httpRouter)
                .onSuccess(r -> {
                    this.register();
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("启动HTTP服务失败。", e);
                    promise.fail("启动HTTP服务失败。");
                });
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        this.unregister()
                .compose(r -> this.http.close())
                .onSuccess(r -> {
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("关闭HTTP服务失败。", e);
                    promise.fail("关闭HTTP服务失败。");
                });

    }

}
