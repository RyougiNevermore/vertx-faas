package org.pharosnet.vertx.faas.codegen;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.ModuleGen;
import org.pharosnet.vertx.faas.codegen.annotation.EnableOAS;
import org.pharosnet.vertx.faas.codegen.annotation.Fn;
import org.pharosnet.vertx.faas.codegen.annotation.FnInterceptor;
import org.pharosnet.vertx.faas.codegen.generators.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;

@SupportedAnnotationTypes({
        "org.pharosnet.vertx.faas.codegen.annotation.EnableOAS",
        "org.pharosnet.vertx.faas.codegen.annotation.Fn",
        "io.vertx.codegen.annotations.ModuleGen",
})
@SupportedOptions({"codegen.output"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class FaaSCodeGenProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;
    private Types typeUtils;

    private Set<String> fns;
    private Set<String> fnImpls;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.fns = new HashSet<>();
        this.fnImpls = new HashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.generateDataObjectPlus(roundEnv);
        this.generateModuleFn(roundEnv);
        Map<String, List<Element>> moduleFnMap = this.generateModuleFnImpl(roundEnv);
        this.generateOAS(roundEnv, moduleFnMap);
        return true;
    }

    private void generateOAS(RoundEnvironment roundEnv, Map<String, List<Element>> moduleFnMap) {
        Set<? extends Element> oasElements = roundEnv.getElementsAnnotatedWith(EnableOAS.class);
        if (oasElements == null || oasElements.isEmpty()) {
            return;
        }
        Element oasElement = oasElements.iterator().next();
        EnableOAS enableOAS = oasElement.getAnnotation(EnableOAS.class);
        try {
            new OASGenerator(this.messager, this.elementUtils, this.filer).generate(moduleFnMap, enableOAS);
        } catch (Exception exception) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成 OpenAPI 失败。");
            messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    private void generateModuleFn(RoundEnvironment roundEnv) {

        Set<? extends Element> moduleElements = roundEnv.getElementsAnnotatedWith(ModuleGen.class);
        if (moduleElements == null || moduleElements.isEmpty()) {
            return;
        }
        for (Element moduleElement : moduleElements) {
            String packageName = elementUtils.getPackageOf(moduleElement).getQualifiedName().toString();
            ModuleGen moduleGen = moduleElement.getAnnotation(ModuleGen.class);
            List<Element> fnElements = new ArrayList<>();
            List<? extends Element> classElements = elementUtils.getPackageOf(moduleElement).getEnclosedElements();
            for (Element classElement : classElements) {
                boolean isType = false;
                if (classElement instanceof TypeElement) {
                    isType = true;
                }
                if (!isType) {
                    continue;
                }
                TypeElement typeElement = (TypeElement) classElement;
                Fn fn = typeElement.getAnnotation(Fn.class);
                if (fn != null) {
                    String fnName = ClassName.get(typeElement).packageName() + "." + ClassName.get(typeElement).simpleName();
                    if (this.fns.contains(fnName)) {
                        continue;
                    }
                    this.fns.add(fnName);
                    fnElements.add(classElement);
                }
            }
            if (!fnElements.isEmpty()) {
                try {
                    new ModuleFnGenerator(this.messager, this.elementUtils, this.filer).generate(packageName, moduleGen, fnElements);
                } catch (Throwable exception) {
                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s:%s 模块的函数失败。", moduleGen.groupPackage(), moduleGen.name()));
                    messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                    throw new RuntimeException(exception);
                }
            }

        }

    }

    private Map<String, List<Element>> generateModuleFnImpl(RoundEnvironment roundEnv) {
        Map<String, List<Element>> moduleFnMap = new HashMap<>();
        Set<? extends Element> moduleElements = roundEnv.getElementsAnnotatedWith(ModuleGen.class);
        if (moduleElements == null || moduleElements.isEmpty()) {
            return moduleFnMap;
        }
        for (Element moduleElement : moduleElements) {
            String packageName = elementUtils.getPackageOf(moduleElement).getQualifiedName().toString();
            ModuleGen moduleGen = moduleElement.getAnnotation(ModuleGen.class);
            List<FnImpl> fnImplElements = new ArrayList<>();
            List<? extends Element> classElements = elementUtils.getPackageOf(moduleElement).getEnclosedElements();
            for (Element classElement : classElements) {
                boolean isType = false;
                if (classElement instanceof TypeElement) {
                    isType = true;
                }
                if (!isType) {
                    continue;
                }
                TypeElement typeElement = (TypeElement) classElement;

                List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
                if (interfaces == null || interfaces.isEmpty()) {
                    continue;
                }
                for (TypeMirror interface_ : interfaces) {
                    Element interfaceElement = this.typeUtils.asElement(interface_);
                    Fn interfaceFn = interfaceElement.getAnnotation(Fn.class);
                    if (interfaceFn != null) {
                        String fnName = ClassName.get(typeElement).packageName() + "." + ClassName.get(typeElement).simpleName();
                        if (this.fnImpls.contains(fnName)) {
                            break;
                        }
                        this.fnImpls.add(fnName);
                        FnInterceptor fnInterceptor = typeElement.getAnnotation(FnInterceptor.class);
                        fnImplElements.add(new FnImpl((TypeElement) interfaceElement, typeElement, interfaceFn, fnInterceptor));
                        List<Element> fetchFnElements;
                        if (moduleFnMap.containsKey(moduleGen.name())) {
                            fetchFnElements = moduleFnMap.get(moduleGen.name());
                        } else {
                            fetchFnElements = new ArrayList<>();
                        }
                        fetchFnElements.add(interfaceElement);
                        moduleFnMap.put(moduleGen.name(), fetchFnElements);
                        break;
                    }
                }
            }
            if (!fnImplElements.isEmpty()) {
                try {
                    new ModuleImplGenerator(this.messager, this.elementUtils, this.filer).generate(packageName, moduleGen, fnImplElements);
                } catch (Throwable exception) {
                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s:%s 模块的函数失败。", moduleGen.groupPackage(), moduleGen.name()));
                    messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                    throw new RuntimeException(exception);
                }
            }


        }

        return moduleFnMap;
    }

    private void generateDataObjectPlus(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(DataObject.class);
        if (elements == null || elements.isEmpty()) {
            return;
        }
        DataObjectPlusGenerator generator = new DataObjectPlusGenerator(this.messager, this.elementUtils, this.typeUtils, this.filer);
        for (Element element : elements) {
            try {
                generator.generate(element);
            } catch (Throwable exception) {
                messager.printMessage(Diagnostic.Kind.ERROR, String.format("生成 %s DataObject Json Mapper失败。", element.getSimpleName().toString()));
                messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
                throw new RuntimeException(exception);
            }

        }
    }

}
