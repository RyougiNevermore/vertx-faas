package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.codegen.format.CamelCase;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.lang.model.element.Modifier;
import java.util.List;

public class DALInsertGenerator {

    public DALInsertGenerator(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    private final DatabaseType databaseType;

    public void generate(DALModel dalModel, TypeSpec.Builder typeBuilder) {

        String sql = this.generateSQL(dalModel.getTableModel());

        // sql
        FieldSpec.Builder staticSqlField = FieldSpec.builder(
                ClassName.get(String.class), "_insertSQL",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$S", sql);
        typeBuilder.addField(staticSqlField.build());

        typeBuilder.addMethod(this.generateOne(dalModel).build());
        typeBuilder.addMethod(this.generateBatch(dalModel).build());

    }

    public String generateSQL(TableModel tableModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO").append(" ");
        if (tableModel.getTable().schema().length() > 0) {
            builder.append(tableModel.getTable().schema().toUpperCase()).append(".");
        }
        builder.append(tableModel.getTable().name()).append(" ");
        StringBuilder columns = new StringBuilder();
        StringBuilder args = new StringBuilder();
        int pos = 1;
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            columns.append(", ").append(columnModel.getColumn().name());
            if (databaseType.equals(DatabaseType.MYSQL)) {
                args.append(", ?");
            } else {
                args.append(", $").append(pos);
                pos++;
            }
        }
        builder.append("(").append(columns.toString().substring(2)).append(")");
        builder.append(" ").append("VALUES").append(" ").append("(").append(columns.toString().substring(2)).append(")");
        return builder.toString().toUpperCase();
    }

    public MethodSpec.Builder generateOne(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(dalModel.getTableClassName(), "row")
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(Future.class),
                                dalModel.getTableClassName()
                        )
                )
                .addStatement("$T promise = $T.promise()",
                        ParameterizedTypeName.get(
                                ClassName.get(Promise.class),
                                dalModel.getTableClassName()
                        ),
                        ClassName.get(Promise.class)
                )
                .addStatement("$T resultPromise = $T.promise()",
                        ParameterizedTypeName.get(
                                ClassName.get(Promise.class),
                                ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult")
                        ),
                        ClassName.get(Promise.class)
                )
                .addCode("resultPromise.future()\n")
                .addCode("\t.onSuccess(result -> {\n")
                .addCode("\t\tpromise.complete(row);\n")
                .addCode("\t})\n")
                .addCode("\t.onFailure(e -> {\n")
                .addCode("\t\tlog.error(\"insert failed, sql = {}, row = {}\", _insertSQL, row.toJson().encode(), e);\n")
                .addCode("\t\tpromise.fail(e);\n")
                .addCode("\t})\n")
                .addCode("\n")
                .addCode("$T args = new $T();\n", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class));
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            methodBuild.addCode(String.format("args.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(columnModel.getFieldName()))));
        }
        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_insertSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(false);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n")
                .addCode("this.service.query(context, arg, resultPromise);\n")
                .addCode("return promise.future();");

        return methodBuild;
    }

    public MethodSpec.Builder generateBatch(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), dalModel.getTableClassName()), "rows")
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(Future.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(List.class),
                                        dalModel.getTableClassName()
                                )
                        )
                )
                .addStatement("$T promise = $T.promise()",
                        ParameterizedTypeName.get(
                                ClassName.get(Promise.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(List.class),
                                        dalModel.getTableClassName()
                                )
                        ),
                        ClassName.get(Promise.class)
                )
                .addStatement("$T resultPromise = $T.promise()",
                        ParameterizedTypeName.get(
                                ClassName.get(Promise.class),
                                ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryResult")
                        ),
                        ClassName.get(Promise.class)
                )
                .addCode("resultPromise.future()\n")
                .addCode("\t.onSuccess(result -> {\n")
                .addCode("\t\tpromise.complete(rows);\n")
                .addCode("\t})\n")
                .addCode("\t.onFailure(e -> {\n")
                .addCode("\t\tlog.error(\"insert batch failed, sql = {}, row = {}\", _insertSQL, $T.encode(rows), e);\n", ClassName.get(Json.class))
                .addCode("\t\tpromise.fail(e);\n")
                .addCode("\t})\n")
                .addCode("\n")
                .addCode("$T args = new $T();\n", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class))
                .addCode("for ($T row : rows) {\n", dalModel.getTableClassName())
                .addCode("\t$T arg = new $T();\b", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class));
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            methodBuild.addCode(String.format("\targ.add(row.get%s());\n", CamelCase.INSTANCE.format(List.of(columnModel.getFieldName()))));
        }
        methodBuild
                .addCode("\targs.add(arg);\n")
                .addCode("}\n")
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_insertSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(true);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n")
                .addCode("this.service.query(context, arg, resultPromise);\n")
                .addCode("return promise.future();");
        return methodBuild;
    }

}
