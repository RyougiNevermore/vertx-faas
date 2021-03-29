package org.pharosnet.vertx.faas.database.codegen.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.vertx.codegen.format.CamelCase;
import org.pharosnet.vertx.faas.database.codegen.annotations.Query;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DALModel {

    public DALModel(Types typeUtils, Map<String, TableModel> tableModelMap, TypeElement element) throws Exception {
        this.className = ClassName.get(element);
        this.implClassName = ClassName.get(this.className.packageName(), CamelCase.INSTANCE.format(List.of(this.className.simpleName(), "Impl")));

        List<? extends TypeMirror> interfaces = element.getInterfaces();
        for (TypeMirror interface_ : interfaces) {
            boolean isAbstractDAL = false;
            boolean isAbstractVAL = false;
            Element interfaceTypeElement = typeUtils.asElement(interface_);
            if (interfaceTypeElement instanceof TypeElement) {
                ClassName interfaceClassName = ClassName.get((TypeElement) interfaceTypeElement);
                isAbstractDAL = interfaceClassName.packageName().equals("org.pharosnet.vertx.faas.database.api")
                        && interfaceClassName.simpleName().equals("AbstractDAL");
                isAbstractVAL = interfaceClassName.packageName().equals("org.pharosnet.vertx.faas.database.api")
                        && interfaceClassName.simpleName().equals("AbstractVAL");
                if (isAbstractDAL) {
                    List<? extends TypeParameterElement> typeParameters = ((TypeElement) interfaceTypeElement).getTypeParameters();
                    if (typeParameters == null || typeParameters.isEmpty()) {
                        throw new Exception(String.format("%s.%s 的AbstractDAL没有设置泛型.", this.className.packageName(), this.className.simpleName()));
                    }
                    if (typeParameters.size() != 2) {
                        throw new Exception(String.format("%s.%s 的AbstractDAL泛型设置错误，只能两个.", this.className.packageName(), this.className.simpleName()));
                    }
                    TypeParameterElement tableTypeParam = typeParameters.get(0);
                    TypeElement tableTypeElement = (TypeElement) typeUtils.asElement(tableTypeParam.asType());
                    this.tableClassName = ClassName.get(tableTypeElement);
                    String tableModelKey = String.format("%s.%s", this.tableClassName.packageName(), this.tableClassName.simpleName());
                    if (!tableModelMap.containsKey(tableModelKey)) {
                        throw new Exception(String.format("%s.%s 的AbstractDAL泛型设置错误，Table 无效.", this.className.packageName(), this.className.simpleName()));
                    }
                    this.tableModel = tableModelMap.get(tableModelKey);
                    this.tableMapperClassName = tableModelMap.get(tableModelKey).getMapperClassName();

                    if (this.tableModel.getTable().view()) {
                        throw new Exception(String.format("%s.%s 的AbstractDAL泛型设置错误，@Table.view必须是false.", this.className.packageName(), this.className.simpleName()));
                    }

                    // id
                    TypeParameterElement idTypeParam = typeParameters.get(1);
                    TypeName idTypeName = TypeName.get(idTypeParam.asType());
                    this.idClassName = (ClassName) idTypeName;
                }
            }
            this.view = isAbstractVAL;
            if (isAbstractDAL || isAbstractVAL) {
                break;
            }
        }

        if (this.tableClassName == null) {
            throw new Exception(String.format("%s.%s 没有继承的AbstractDAL.", this.className.packageName(), this.className.simpleName()));
        }

        List<? extends Element> enclosedElements = element.getEnclosedElements();
        if (enclosedElements == null || enclosedElements.isEmpty()) {
            throw new Exception(String.format("%s.%s 没有函数.", this.className.packageName(), this.className.simpleName()));
        }
        this.methodModels = new ArrayList<>();
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement instanceof ExecutableElement) {
                ExecutableElement methodElement = (ExecutableElement) enclosedElement;
                if (methodElement.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                if (methodElement.getAnnotation(Query.class) == null) {
                    continue;
                }
                DALMethodModel methodModel = new DALMethodModel(methodElement);
                this.methodModels.add(methodModel);
            }
        }
        if (this.methodModels.isEmpty()) {
            throw new Exception(String.format("%s.%s 没有@Query函数.", this.className.packageName(), this.className.simpleName()));
        }
    }

    private boolean view;
    private ClassName className;
    private ClassName implClassName;

    private ClassName tableClassName;
    private ClassName idClassName;

    private ClassName tableMapperClassName;
    private TableModel tableModel;

    private List<DALMethodModel> methodModels;

    public boolean isView() {
        return view;
    }

    public void setView(boolean view) {
        this.view = view;
    }

    public ClassName getClassName() {
        return className;
    }

    public void setClassName(ClassName className) {
        this.className = className;
    }

    public ClassName getImplClassName() {
        return implClassName;
    }

    public void setImplClassName(ClassName implClassName) {
        this.implClassName = implClassName;
    }

    public ClassName getTableClassName() {
        return tableClassName;
    }

    public void setTableClassName(ClassName tableClassName) {
        this.tableClassName = tableClassName;
    }

    public ClassName getIdClassName() {
        return idClassName;
    }

    public void setIdClassName(ClassName idClassName) {
        this.idClassName = idClassName;
    }

    public ClassName getTableMapperClassName() {
        return tableMapperClassName;
    }

    public void setTableMapperClassName(ClassName tableMapperClassName) {
        this.tableMapperClassName = tableMapperClassName;
    }

    public List<DALMethodModel> getMethodModels() {
        return methodModels;
    }

    public void setMethodModels(List<DALMethodModel> methodModels) {
        this.methodModels = methodModels;
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    public void setTableModel(TableModel tableModel) {
        this.tableModel = tableModel;
    }
}
