package net.snowyhollows.mcgregor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.squareup.javapoet.*;
import net.snowyhollows.mcgregor.api.Tree;

public class McGregor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        types = processingEnvironment.getTypeUtils();
    }

    private ClassName nameFor(ClassName type, String suffix) {
        return ClassName.get(type.packageName(), type.simpleName() + suffix);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> beans = roundEnv.getElementsAnnotatedWith(Tree.class);

        for (Element bean : beans) {
	        messager.printMessage(Diagnostic.Kind.NOTE, "generating " + bean);
	        TypeElement beanClass = (TypeElement) bean;
	        ClassName beanClassName = ClassName.get(beanClass);
	        Tree tree = beanClass.getAnnotation(Tree.class);

	        List<? extends TypeMirror> interfaces = beanClass.getInterfaces();

	        // TODO: allow any number of other interfaces and jumps over ancestors
	        DeclaredType i = (DeclaredType) interfaces.get(0);

	        List<? extends TypeMirror> typeArguments = i.getTypeArguments();

	        TypeMirror treeType = typeArguments.get(0);
	        TypeMirror modelType = typeArguments.get(1);
	        TypeMirror contextType = typeArguments.get(2);

	        messager.printMessage(Diagnostic.Kind.NOTE, "tp: " + typeArguments + " size: " + typeArguments.size());
	        messager.printMessage(Diagnostic.Kind.NOTE, "interfejses: " + interfaces);

	        ClassName builderName = nameFor(beanClassName, "Impl");
	        String packageName = beanClassName.packageName();

	        MethodSpec.Builder build = MethodSpec.methodBuilder("build")
			        .addParameter(ParameterSpec.builder(TypeName.get(treeType), "tree").build())
			        .addParameter(ParameterSpec.builder(TypeName.get(modelType), "model").build())
			        .addParameter(ParameterSpec.builder(TypeName.get(contextType), "ctx").build())
			        .addModifiers(Modifier.PUBLIC)
			        .returns(TypeName.get(treeType));

	        FileInputStream fileReader = null;
	        try {
	        	String templateName = tree.value();
	        	if (templateName.equals("##same_as_simple_name")) {
	        		templateName = uncapitalize(beanClass.getSimpleName().toString()) + ".html";
		        }
		        FileObject res = filer.getResource(StandardLocation.SOURCE_OUTPUT, "a.b.c", "kapusta.html");
		        File baseSourcesDir = new File(res.getName());
		        while (!baseSourcesDir.getName().equalsIgnoreCase("build")) {
		        	baseSourcesDir = baseSourcesDir.getParentFile();
		        }
		        baseSourcesDir = baseSourcesDir.getParentFile();

		        String relativeName = "src/main/java/" + (templateName.startsWith("/") ? templateName : packageName.replace('.','/') + "/" + templateName);
		        File resourceFile = new File(baseSourcesDir, relativeName);

		        messager.printMessage(Diagnostic.Kind.NOTE, "dummy: " + resourceFile);
		        fileReader = new FileInputStream(resourceFile);

		        ComponentDescription a = AstBuilder.process(fileReader);
		        BasicAstRenderer basicAstRenderer = new BasicAstRenderer();
		        build.addCode("return " + basicAstRenderer.render(a) + ";");

	        } catch (IOException e) {
		        throw new RuntimeException(e);
	        } finally {
	        	if (fileReader != null) {
			        try {
				        fileReader.close();
			        } catch (IOException e) {
				        throw new RuntimeException(e);
			        }
		        }
	        }


	        TypeSpec.Builder factory = TypeSpec.classBuilder(builderName)
	                .addModifiers(Modifier.PUBLIC)
	                .addSuperinterface(beanClassName)
	                .addMethod(build.build());

	        try {
		        JavaFile.builder(packageName, factory.build()).build().writeTo(filer);
	        } catch (IOException e) {
		        throw new RuntimeException(e);
	        }
        }

        return true;

    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Tree.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

	private String uncapitalize(String t) {
		return t.substring(0, 1).toLowerCase() + t.substring(1);
	}

}
