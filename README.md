# vertx-faas

todo list

database:
把QueryArg.arg 从 jsonArray<Object>，变成jsonArray<JsonObject> {JsonObject中是type：String，value: Object}
根据type进行值类型转译，当日期类型是，把String转成日期，

tx fn:
增加 tx interceptor，

interceptor：
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