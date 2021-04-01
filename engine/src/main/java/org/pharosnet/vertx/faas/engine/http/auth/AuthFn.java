package org.pharosnet.vertx.faas.engine.http.auth;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface AuthFn {

    static AuthFn proxy(Vertx vertx) {
        return new AuthFnImpl(AuthService.proxy(vertx));
    }

    Future<String> generateToken(JsonObject claims);

}
