package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.pharosnet.vertx.faas.database.codegen.annotations.Arg;
import org.pharosnet.vertx.faas.database.codegen.annotations.Query;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

public class DALMethodModel {

    public DALMethodModel(ExecutableElement methodElement) throws Exception {
        this.query = methodElement.getAnnotation(Query.class);
        TypeName returnType = TypeName.get(methodElement.getReturnType());
        if (!returnType.toString().startsWith("io.vertx.core.Future")) {
            throw new Exception(String.format("%s 函数的返回值不是io.vertx.core.Future。", methodElement.getSimpleName()));
        }
        this.returnClassName = (ParameterizedTypeName) returnType;
        this.returnElementClassName = (ClassName) this.returnClassName.typeArguments.get(0);
        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters == null || parameters.size() == 0) {
            throw new Exception(String.format("%s 函数的参数为空。", methodElement.getSimpleName()));
        }

        this.paramModels = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            if (i == 0) {
                if (!TypeName.get(parameter.asType()).toString().equals("org.pharosnet.vertx.faas.database.api.SqlContext")) {
                    throw new Exception(String.format("%s 函数的第一个参数不是org.pharosnet.vertx.faas.database.api.SqlContext。", methodElement.getSimpleName()));
                }
            } else {
                if (parameter.getAnnotation(Arg.class) == null) {
                    throw new Exception(String.format("%s 函数参数没有@Arg。", methodElement.getSimpleName()));
                }
            }

            this.paramModels.add(new DALMethodParamModel(parameter));
        }
        if (this.paramModels.isEmpty()) {
            throw new Exception(String.format("%s 函数没有参数。", methodElement.getSimpleName()));
        }
    }

    private Query query;
    private ParameterizedTypeName returnClassName;
    private ClassName returnElementClassName;
    private List<DALMethodParamModel> paramModels;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public ParameterizedTypeName getReturnClassName() {
        return returnClassName;
    }

    public void setReturnClassName(ParameterizedTypeName returnClassName) {
        this.returnClassName = returnClassName;
    }

    public List<DALMethodParamModel> getParamModels() {
        return paramModels;
    }

    public void setParamModels(List<DALMethodParamModel> paramModels) {
        this.paramModels = paramModels;
    }

    public ClassName getReturnElementClassName() {
        return returnElementClassName;
    }

    public void setReturnElementClassName(ClassName returnElementClassName) {
        this.returnElementClassName = returnElementClassName;
    }
}
