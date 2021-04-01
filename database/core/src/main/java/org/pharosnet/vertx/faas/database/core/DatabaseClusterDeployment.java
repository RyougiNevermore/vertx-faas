package org.pharosnet.vertx.faas.database.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import org.pharosnet.vertx.faas.core.components.ComponentDeployment;

public class DatabaseClusterDeployment extends ComponentDeployment {

    private DatabaseClusterDeployment() {
        this(null);
    }

    public DatabaseClusterDeployment(ServiceDiscovery discovery) {
        this(discovery, 0);
    }

    public DatabaseClusterDeployment(ServiceDiscovery discovery, int workers) {
        super();
        this.discovery = discovery;
        if (workers < 0) {
            workers = CpuCoreSensor.availableProcessors() * 2;
        }
        this.workers = workers;
    }

    private final int workers;
    private final ServiceDiscovery discovery;

    @Override
    public Future<String> deploy(Vertx vertx, JsonObject config) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        if (this.workers > 0) {
            deploymentOptions.setWorker(true).setWorkerPoolSize(this.workers);
        }
        deploymentOptions.setConfig(config);
        return vertx.deployVerticle(new DatabaseVerticle(this.discovery), deploymentOptions);
    }

}
