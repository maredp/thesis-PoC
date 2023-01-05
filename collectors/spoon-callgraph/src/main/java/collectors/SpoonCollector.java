package collectors;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import spoon.DecompiledResource;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SpoonCollector {
    private int controllerCount;
    HashMap<String, Controller> controllerSequences;
    Decomposition decomposition;
    private String projectName;

    // all project classes
    protected Set<String> allEntities;

    // in FenixFramework: allDomainEntities = all classes that extend .*_Base
    // in collectors.JPA: allDomainEntities = all @Entities @Embeddable @MappedSuperClass
    protected Set<String> allDomainEntities;
    protected HashSet<CtClass> controllers;
    private List<Access> controllerAccesses;
    protected Map<String, List<CtType>> abstractClassesAndInterfacesImplementorsMap;
    private boolean includeLocationOfAccess;

    protected Factory factory;
    protected Launcher launcher;

    /* ------- TO REMOVE ----------- */
    protected int repositoryCount = 0;
    private int controllerMethodsCount = 0;

    public SpoonCollector(
            int launcherChoice,
            String repoName,
            String projectPath,
            boolean includeLocationOfAccess) throws IOException {
        File decompiledDir = new File(Constants.DECOMPILED_SOURCES_PATH);
        if (decompiledDir.exists()) {
            FileUtils.deleteDirectory(decompiledDir);
        }

        controllerSequences = new HashMap<>();
        decomposition = new Decomposition(new HashMap<>());
        controllers = new HashSet<>();
        allEntities = new HashSet<>();
        allDomainEntities = new TreeSet<>();
        abstractClassesAndInterfacesImplementorsMap = new HashMap<>();

        this.projectName = repoName;
        this.includeLocationOfAccess = includeLocationOfAccess;

        switch (launcherChoice) {
            case Constants.LAUNCHER:
                launcher = new Launcher();
                launcher.addInputResource(projectPath);
                break;

            case Constants.MAVEN_LAUNCHER:
                launcher = new MavenLauncher(projectPath, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
                break;

            case Constants.JAR_LAUNCHER:
                launcher = new Launcher();
                File projectDir = new File(projectPath);
                String[] extensions = new String[]{"jar"};
                Collection<File> packageFiles = FileUtils.listFiles(
                        projectDir,
                        extensions,
                        true
                );

                for (File packageFile : packageFiles) {
                    String packagePath = packageFile.getPath();
                    String decompiledPackageSourcesDir = Constants.DECOMPILED_SOURCES_PATH
                            + packagePath.substring(packagePath.lastIndexOf("/")+1)
                            + File.separator;
                    new File(decompiledPackageSourcesDir).mkdirs();

                    new DecompiledResource(
                            packagePath,
                            null,
                            null,
                            decompiledPackageSourcesDir
                    );
                    launcher.addInputResource(decompiledPackageSourcesDir);
                }
                break;
            default:
                System.exit(1);
                break;
        }

        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.getEnvironment().setCommentEnabled(false);
    }

    public void run() throws IOException {
        factory = launcher.getFactory();

        System.out.println("Processing Project: " + projectName);
        long startTime = System.currentTimeMillis();
        collectControllersAndEntities();

        controllerCount = 0;
        for (CtClass controller : controllers) {
            controllerCount++;
            processController(controller);
        }

        long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        float elapsedTimeSec = elapsedTimeMillis/1000F;
        System.out.println("Complete. Elapsed time: " + elapsedTimeSec + " seconds");

        storeJsonFile(Constants.COLLECTION_SAVE_PATH + projectName, projectName, controllerSequences);

        File file = new File(Constants.DECOMPILED_SOURCES_PATH);
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }

        /* ------ TO REMOVE ------- */
        File testData = new File(Constants.TEST_DATA_PATH);
        if (!testData.exists()) testData.createNewFile();
        String s0 = "Project: " + projectName;
        String s1 = "#Controllers: " + controllers.size();
        String s15 = "#ControllerMethods: " + controllerMethodsCount;
        String s2 = "#Entities: " + allEntities.size();
        String s3 = "#DomainEntities: " + allDomainEntities.size();
        String s4 = "#Repositories: " + repositoryCount;
        StringBuilder sb = new StringBuilder()
                .append("---------------------------\n")
                .append(s0)
                .append("\n")
                .append(s1)
                .append("\n")
                .append(s15)
                .append("\n")
                .append(s2)
                .append("\n")
                .append(s3)
                .append("\n")
                .append(s4)
                .append("\n");
        Files.write(Paths.get(testData.getPath()), sb.toString().getBytes(), StandardOpenOption.APPEND);
    }

    private void processController(CtClass controller) {
        for (Object cM : controller.getMethods()) {
            CtMethod controllerMethod = (CtMethod) cM;

            //Check if the controller method return type == ActionForward
            if (controller.getSuperclass() != null && controller.getSuperclass().getSimpleName().contains("DispatchAction")) {
                if (!controllerMethod.getType().toString().contains("ActionForward"))
                    continue;
            } else {
                List<CtAnnotation<? extends Annotation>> annotations = controllerMethod.getAnnotations();
                if (!existsAnnotation(annotations, "RequestMapping") &&
                    !existsAnnotation(annotations, "GetMapping") &&
                    !existsAnnotation(annotations, "PostMapping") &&
                    !existsAnnotation(annotations, "PatchMapping") &&
                    !existsAnnotation(annotations, "PutMapping") &&
                    !existsAnnotation(annotations, "DeleteMapping")
                ) {
                    continue;
                }
            }

            String controllerFullName = controller.getSimpleName() + "." + controllerMethod.getSimpleName();
            System.out.println("Processing Controller: " + controllerFullName + "   " + controllerCount + "/" + controllers.size());
            controllerAccesses = new ArrayList<>();
            Stack<SourcePosition> methodStack = new Stack<>();

            controllerMethodsCount++;
            methodCallDFS(controllerMethod, null, methodStack);

            if (controllerAccesses.size() > 0) {
                controllerSequences.put(controllerFullName, new Controller(controllerAccesses));
            }
        }
    }

    protected abstract void methodCallDFS(CtExecutable callerMethod, CtAbstractInvocation prevCalleeLocation, Stack<SourcePosition> methodStack);

    protected abstract void collectControllersAndEntities();

    protected boolean existsAnnotation(List<CtAnnotation<? extends Annotation>> annotations, String annotationName) {
        for (CtAnnotation<? extends Annotation> a : annotations) {
            if ( a.getAnnotationType().getSimpleName().equals(annotationName) )
                return true;
        }
        return false;
    }

    protected boolean fqnMatches(CtType<?> clazz, List<String> input) {
        return input.stream()
                .anyMatch(regex -> clazz.getQualifiedName().matches(regex));
    }

    protected String findFirstFqnMatch(CtType<?> clazz, String input) {
        Matcher matcher = Pattern.compile(input).matcher(clazz.getPosition().getFile().getPath());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private void storeJsonFile(String filepath, String fileName, HashMap<String, Controller> controllerSequences) {
        try {
            File filePath = new File(filepath);
            if (!filePath.exists()) {
                filePath.mkdirs();
            }

            ObjectMapper mapper = new ObjectMapper();
            // Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
            DefaultPrettyPrinter.Indenter indenter =
                    new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentObjectsWith(indenter);
            printer.indentArraysWith(indenter);

            mapper.writer(printer).writeValue(new FileOutputStream(filepath+"/"+ "functionalities.json"), controllerSequences);
            mapper.writer(printer).writeValue(new FileOutputStream(filepath+"/"+ "entities.json"), allDomainEntities);
            mapper.writer(printer).writeValue(new FileOutputStream(filepath+"/"+ "decomposition.json"), decomposition);

            System.out.println("Files created at: " + filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected List<CtMethod> getExplicitImplementationsOfAbstractMethod(CtMethod abstractMethod) throws UnkownMethodException {
        List<CtMethod> explicitMethods = new ArrayList<>();
        CtType<?> declaringType = abstractMethod.getDeclaringType();
        List<CtType> implementors = abstractClassesAndInterfacesImplementorsMap.get(declaringType.getSimpleName());
        if (implementors != null) {
            for (CtType ctImplementorType : implementors) {
                List<CtMethod> methodsByName = ctImplementorType.getMethodsByName(abstractMethod.getSimpleName());
                if (methodsByName.size() > 0) {
                    for (CtMethod ctM : methodsByName) {
                        if (isEqualMethodSignature(abstractMethod.getReference(), ctM.getReference())) {
                            explicitMethods.add(ctM);
                        }
                    }
                }
            }
        }

        return explicitMethods;
    }

    protected boolean isEqualMethodSignature(CtExecutableReference method1, CtExecutableReference method2) throws UnkownMethodException {
        // compare return type
        try {
            if (method1.getType().equals(method2.getType())) {
                // compare parameters type
                List ctMParams1 = method1.getParameters();
                List ctMParams2 = method2.getParameters();
                if (ctMParams1.size() != ctMParams2.size())
                    return false;
                for (int i = 0; i < ctMParams1.size(); i++) {
                    CtTypeReference ctMParam1 = (CtTypeReference) ctMParams1.get(i);
                    CtTypeReference ctMParam2 = (CtTypeReference) ctMParams2.get(i);
                    if (!ctMParam1.equals(ctMParam2)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("isEqualMethodSignature Unknown method: " + method1.toString() + " " + method2.toString());
            throw new UnkownMethodException();
        }
    }

    protected void addEntitiesSequenceAccess(String simpleName, String mode, String location) {
        if (includeLocationOfAccess) {
            controllerAccesses.add(new Access(mode, simpleName, location));
        } else {
            addEntitiesSequenceAccess(simpleName, mode);
        }
    }

    protected void addEntitiesSequenceAccess(String simpleName, String mode) {
        controllerAccesses.add(new Access(mode, simpleName));
    }

    protected void addClusterEntity(String clusterName, String entityName) {
        HashMap<String, List<String>> clusters = decomposition.getC();
        if (clusters.containsKey(clusterName)) {
            clusters.get(clusterName).add(entityName);
        } else {
            clusters.put(clusterName, new ArrayList<>(Arrays.asList(entityName)));
        }
    }
}
