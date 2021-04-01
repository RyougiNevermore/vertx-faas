package org.pharosnet.vertx.faas.engine.http.auth;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class AuthFnImpl implements AuthFn {

    public AuthFnImpl(AuthService service) {
        this.service = service;
    }

    private final AuthService service;

    @Override
    public Future<String> generateToken(JsonObject claims) {
        Promise<String> promise = Promise.promise();
        this.service.generateToken(claims, promise);
        return promise.future();
    }
}
