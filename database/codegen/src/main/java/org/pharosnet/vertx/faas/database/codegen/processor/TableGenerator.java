package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.vertx.codegen.format.CamelCase;
import org.pharosnet.vertx.faas.database.codegen.DatabaseType;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;

public class TableGenerator {

    public TableGenerator(ProcessingEnvironment processingEnv, DatabaseType databaseType) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.databaseType = databaseType;
    }

    private final Messager messager;
    private final Filer filer;
    private final Types typeUtils;
    private DatabaseType databaseType;

    public TableModel generate(TypeElement element) throws Exception {
        TableModel tableModel = new TableModel(this.typeUtils, element);
        this.generateMapper(tableModel);
        return tableModel;
    }

    private void generateMapper(TableModel tableModel) throws Exception {
        String pkg = tableModel.getClassName().packageName();
        ClassName mapperClassName = tableModel.getMapperClassName();


        // map
        MethodSpec.Builder mapMethod = MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("io.vertx.core.json", "JsonObject"), "row")
                .returns(tableModel.getClassName())
                .addCode("if (row == null) {\n")
                .addCode("\treturn null;\n")
                .addCode("}\n")
                .addCode("$T value = new $T();\n", tableModel.getClassName(), tableModel.getClassName());
        for (ColumnModel columnModel : tableModel.getColumnModels()) {
            mapMethod.addCode(String.format("value.set%s(row.getString(\"%s\"));\n",
                    CamelCase.INSTANCE.format(List.of(columnModel.getFieldName())),
                    columnModel.getColumn().name()
            ));
        }
        mapMethod.addCode("return value;");

        // class
        TypeSpec typeBuilder = TypeSpec.classBuilder(mapperClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(mapMethod.build())
                .build();

        // file
        JavaFile javaFile = JavaFile.builder(pkg, typeBuilder)
                .addFileComment("Generated code from Vertx FaaS. Do not modify!")
                .indent("\t")
                .build();

        // write
        javaFile.writeTo(filer);

        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("生成 %s.%s", pkg, mapperClassName.simpleName()));
    }


}
