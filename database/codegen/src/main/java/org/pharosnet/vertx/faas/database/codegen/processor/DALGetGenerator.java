package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

public class DALGetGenerator {

    public DALGetGenerator(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    private final DatabaseType databaseType;

    public void generate(DALModel dalModel, TypeSpec.Builder typeBuilder) {

        String sql = this.generateSQL(dalModel.getTableModel());

        // sql
        FieldSpec.Builder staticSqlField = FieldSpec.builder(
                ClassName.get(String.class), "_getSQL",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("$S", sql);
        typeBuilder.addField(staticSqlField.build());

        typeBuilder.addMethod(this.generateOne(dalModel).build());
        typeBuilder.addMethod(this.generateBatch(dalModel).build());

    }

    public String generateSQL(TableModel tableModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT").append(" ");
        StringBuilder columns = new StringBuilder();
        String idColumnName = "";
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            columns.append(", ").append(columnModel.getColumn().name());
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idColumnName = columnModel.getColumn().name();
            }
        }
        builder.append(columns.toString().substring(2)).append(" ");
        builder.append("FROM").append(" ");
        if (tableModel.getTable().schema().length() > 0) {
            builder.append(tableModel.getTable().schema().toUpperCase()).append(".");
        }
        builder.append(tableModel.getTable().name()).append(" ");
        builder.append("WHERE").append(" ").append(idColumnName).append(" = ");
        if (databaseType.equals(DatabaseType.MYSQL)) {
            builder.append("?");
        } else {
            builder.append("$1");
        }
        return builder.toString().toUpperCase();
    }

    public MethodSpec.Builder generateOne(DALModel dalModel) {
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(dalModel.getIdClassName(), "id")
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
                .addCode("\t\t$T _rows = result.getRows();\n",
                        ParameterizedTypeName.get(
                                ClassName.get(List.class),
                                ClassName.get(JsonObject.class)
                        ))
                .addCode("\t\tif (_rows == null || _rows.isEmpty()) {\n")
                .addCode("\t\t\tpromise.complete();\n")
                .addCode("\t\t\treturn;\n")
                .addCode("\t\t}")
                .addCode("\t\t$T mapper = new $T();\n", dalModel.getTableMapperClassName(), dalModel.getTableMapperClassName())
                .addCode("\t\tpromise.complete(mapper.map(_rows.get(0)));\n")
                .addCode("\t})\n")
                .addCode("\t.onFailure(e -> {\n")
                .addCode("\t\tlog.error(\"get failed, sql = {}, id = {}\", _getSQL, id, e);\n")
                .addCode("\t\tpromise.fail(e);\n")
                .addCode("\t})\n")
                .addCode("\n")
                .addCode("$T args = new $T();\n", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class))
                .addCode("args.add(id);\n");

        methodBuild
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_getSQL);\n")
                .addCode("arg.setArgs(args);\n")
                .addCode("arg.setBatch(false);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n")
                .addCode("this.service.query(context, arg, resultPromise);\n")
                .addCode("return promise.future();");
        return methodBuild;
    }

    public MethodSpec.Builder generateBatch(DALModel dalModel) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT").append(" ");
        StringBuilder columns = new StringBuilder();
        String idColumnName = "";
        for (ColumnModel columnModel : dalModel.getTableModel().getColumnModels()) {
            columns.append(", ").append(columnModel.getColumn().name());
            if (columnModel.getKind().equals(ColumnKind.ID)) {
                idColumnName = columnModel.getColumn().name();
            }
        }
        builder.append(columns.toString().substring(2)).append(" ");
        builder.append("FROM").append(" ");
        if (dalModel.getTableModel().getTable().schema().length() > 0) {
            builder.append(dalModel.getTableModel().getTable().schema().toUpperCase()).append(".");
        }
        builder.append(dalModel.getTableModel().getTable().name()).append(" ");
        builder.append("WHERE").append(" ").append(idColumnName).append(" IN (#ids#)");
        String getBatchSQL = builder.toString();
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("org.pharosnet.vertx.faas.database.api", "SqlContext"), "context")
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), dalModel.getIdClassName()), "ids")
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(Future.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(List.class),
                                        dalModel.getTableClassName()
                                )
                        )
                )
                .addCode("String _getBatchSQL = $S;\n", getBatchSQL)
                .addCode("$T idsBuilder = new $T();\n", ClassName.get(StringBuilder.class), ClassName.get(StringBuilder.class))
                .addCode("for ($T id : ids) {\n", dalModel.getIdClassName());

        if (dalModel.getIdClassName().equals(ClassName.get(String.class))) {
            methodBuild.addCode("\tidsBuilder.append(\", \").append(\"'\").append(id).append(\"'\");\n");
        } else {
            methodBuild.addCode("\tidsBuilder.append(\", \").append(id);\n");
        }
        methodBuild.addCode("}\n");
        methodBuild.addCode("_getBatchSQL = _getBatchSQL.replace(\"#ids\", idsBuilder.toString().substring(2));\n");

        methodBuild.addStatement("$T promise = $T.promise()",
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
                .addCode("\t\t$T _rows = result.getRows();\n",
                        ParameterizedTypeName.get(
                                ClassName.get(List.class),
                                ClassName.get(JsonObject.class)
                        ))
                .addCode("\t\tif (_rows == null || _rows.isEmpty()) {\n")
                .addCode("\t\t\tpromise.complete();\n")
                .addCode("\t\t\treturn;\n")
                .addCode("\t\t}")
                .addCode("\t\t$T mapper = new $T();\n", dalModel.getTableMapperClassName(), dalModel.getTableMapperClassName())

                .addCode("\t\t$T values = new $T(_rows.size());\n",
                        ParameterizedTypeName.get(
                                ClassName.get(List.class),
                                dalModel.getTableClassName()
                        ), ParameterizedTypeName.get(ClassName.get(ArrayList.class)))
                .addCode("\t\tfor ($T _row : _rows) {\n", ClassName.get(JsonObject.class))
                .addCode("\t\t\tif (_row == null) {\n")
                .addCode("\t\t\t\tcontinue;\n")
                .addCode("\t\t\t}\n")
                .addCode("\t\t\t$T value = mapper.map(_row);\n", dalModel.getTableClassName())
                .addCode("\t\t\tvalues.add(value);\n")
                .addCode("\t\t}\n")
                .addCode("\t\tpromise.complete(values);\n")
                .addCode("\t})\n")
                .addCode("\t.onFailure(e -> {\n")
                .addCode("\t\tlog.error(\"get batch failed, sql = {}\", _getBatchSQL, e);\n")
                .addCode("\t\tpromise.fail(e);\n")
                .addCode("\t})\n")
                .addCode("\n")
                .addCode("$T arg = new $T();\n", ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"), ClassName.get("org.pharosnet.vertx.faas.database.api", "QueryArg"))
                .addCode("arg.setQuery(_getBatchSQL);\n")
                .addCode("arg.setBatch(false);\n")
                .addCode("arg.setSlaverMode(false);\n")
                .addCode("arg.setNeedLastInsertedId(false);\n")
                .addCode("this.service.query(context, arg, resultPromise);\n")
                .addCode("return promise.future();");
        return methodBuild;
    }

}
