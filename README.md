# vertx-faas

重构：
基于vertx的风格重构模块结构。

todo list

database:

SqlContext, create from context 时直接用原context（return (SqlContext) context），不再fork一个出来。
begin，commit，rollback返回 SqlContext。同时设置txId，isLocal，remoteHostId。
core中不再存tx-host缓存。直接用context中的hostId。

cachedTransaction 改为async，使用vertx的代理executor。


codegen：
重构，使用vertx的codegen方案，such as service proxy的用法。
dataObject，见demo中的用法。增加时间的支持。

tx fn:
增加 tx interceptor，

interceptor：
使用vertx的ServiceAuthInterceptor，在ServiceBinder掉addInterceptor


class base；

proxy：
？
使用eventbus 监听 vertx.discovery.usage 获取 Record。

Circuit Breaker Fn：
在fnService中增加。

rabbitMQ && kafka：
单独一个Messaging模块

DDD：
单独一个模块，