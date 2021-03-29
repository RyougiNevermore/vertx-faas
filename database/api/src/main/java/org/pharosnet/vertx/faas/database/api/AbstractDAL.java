package org.pharosnet.vertx.faas.database.api;

import io.vertx.core.Future;

import java.util.List;

public interface AbstractDAL<R, ID> {

    Future<Void> begin(SqlContext context);
    Future<Void> commit(SqlContext context);
    Future<Void> rollback(SqlContext context);
    Future<QueryResult> query(SqlContext context, QueryArg arg);

    Future<R> get(SqlContext context, ID id);
    Future<List<R>> get(SqlContext context, List<ID> ids);
    Future<R> insert(SqlContext context, R row);
    Future<List<R>> insert(SqlContext context, List<R> rows);
    Future<R> update(SqlContext context, R row);
    Future<List<R>> update(SqlContext context, List<R> rows);
    Future<R> delete(SqlContext context, R row);
    Future<List<R>> delete(SqlContext context, List<R> rows);
    Future<R> deleteForce(SqlContext context, R row);
    Future<List<R>> deleteForce(SqlContext context, List<R> rows);

}
