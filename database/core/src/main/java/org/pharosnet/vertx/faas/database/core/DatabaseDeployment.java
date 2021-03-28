package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;

public class DatabaseDeployment extends ComponentDeployment {

    public DatabaseDeployment() {
        this(null);
    }

    public DatabaseDeployment(ServiceDiscovery discovery) {
        super(new DatabaseMessageConsumerRegister());
        this.discovery = discovery;
    }

    private final ServiceDiscovery discovery;

    @Override
    public Future<String> deploy(Vertx vertx, JsonObject config) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(config);
        return vertx.deployVerticle(new DatabaseVerticle(super.getRegister(), this.discovery), deploymentOptions);
    }

}
