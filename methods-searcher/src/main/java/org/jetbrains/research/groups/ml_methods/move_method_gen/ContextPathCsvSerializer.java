package org.jetbrains.research.groups.ml_methods.move_method_gen;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.ExtractFeaturesTask;
import JavaExtractor.FeaturesEntities.ProgramFeatures;
import com.github.javaparser.ParseException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions.UnexpectedEmptyContext;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;
import org.kohsuke.args4j.CmdLineException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.CsvSerializer.Headers.*;

public class ContextPathCsvSerializer {
    private static final @NotNull
    ContextPathCsvSerializer INSTANCE = new ContextPathCsvSerializer();

    private static final @NotNull String METHODS_FILE_NAME = "methods.csv";

    private static final @NotNull String CLASSES_FILE_NAME = "classes.csv";

    private static final @NotNull String POINTS_FILE_NAME = "points.csv";

    private static final @NotNull CSVFormat METHODS_FILE_FORMAT = CSVFormat.RFC4180.withHeader(ID.toString(), NAME.toString(), "context", FILE.toString(), OFFSET.toString(), CONTAINING_CLASS_ID.toString(), TARGET_IDS.toString());

    private static final @NotNull CSVFormat CLASSES_FILE_FORMAT = CSVFormat.RFC4180.withHeader(ID.toString(), NAME.toString(), "methods", FILE.toString(), OFFSET.toString());

    private static final int MAX_PATH_LENGTH = 8;

    private static final int MAX_PATH_WIDTH = 2;

    private ContextPathCsvSerializer() {
    }

    public static @NotNull
    ContextPathCsvSerializer getInstance() {
        return INSTANCE;
    }

    public void serialize(
        final @NotNull ContextPathDataset dataset,
        final @NotNull Path targetDir
    ) throws IOException, CmdLineException, ParseException, UnexpectedEmptyContext {
        targetDir.toFile().mkdirs();

        try (
            BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(METHODS_FILE_NAME), CREATE_NEW);
            CSVPrinter csvPrinter = new CSVPrinter(writer, METHODS_FILE_FORMAT)
        ) {
            List<PsiMethod> methods = dataset.getMethods();
            for (int methodId = 0; methodId < methods.size(); methodId++) {
                PsiMethod method = methods.get(methodId);

                CommandLineValues cmdValues = new CommandLineValues(
                    "--max_path_length", Integer.toString(MAX_PATH_LENGTH),
                    "--max_path_width", Integer.toString(MAX_PATH_WIDTH)
                );

                ExtractFeaturesTask extractTask =
                    new ExtractFeaturesTask(cmdValues, removeDanglingOneLineComments(method.getText()));

                ArrayList<ProgramFeatures> methodsContexts = new ArrayList<>(
                    extractTask.extractSingleFile()
                        .stream()
                        .filter(it -> it.getName().equals(splitName(method)))
                        .limit(1)
                        .collect(Collectors.toList())
                );


                String pathContext = extractTask.featuresToString(methodsContexts);
                if (pathContext.isEmpty()) {
                    throw new UnexpectedEmptyContext(MethodUtils.fullyQualifiedName(method));
                }

                csvPrinter.printRecord(
                    methodId,
                    MethodUtils.fullyQualifiedName(method),
                    pathContext,
                    getPathToContainingFile(method),
                    method.getNode().getStartOffset(),
                    dataset.getIdOfContainingClass(method),
                    dataset.getIdsOfTargetClasses(method).stream().map(Object::toString).collect(Collectors.joining(" "))
                );
            }
        }

        try (
            BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(CLASSES_FILE_NAME), CREATE_NEW);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CLASSES_FILE_FORMAT)
        ) {
            List<PsiClass> classes = dataset.getClasses();
            for (int classId = 0; classId < classes.size(); classId++) {
                PsiClass clazz = classes.get(classId);

                csvPrinter.printRecord(
                        classId,
                        clazz.getQualifiedName(),
                        dataset.getIdsOfMethodsIn(clazz).stream().map(Object::toString).collect(Collectors.joining(" ")),
                        getPathToContainingFile(clazz),
                        clazz.getNode().getStartOffset()
                );
            }
        }

        try (
            BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(POINTS_FILE_NAME), CREATE_NEW);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180)
        ) {
            for (ContextPathDataset.Point point : dataset.getPoints()) {
                csvPrinter.printRecord(
                    point.getMethodId(),
                    point.getClassId(),
                    point.getLabel()
                );
            }
        }
    }

    private @NotNull String removeDanglingOneLineComments(final @NotNull String code) {
        return code.replaceAll("//.*?\\z", "");
    }

    private @NotNull String splitName(final @NotNull PsiMethod method) {
        return Common.splitToSubtokens(method.getName())
            .stream()
            .collect(Collectors.joining(Common.internalSeparator));
    }

    private @NotNull Path getPathToContainingFile(final @NotNull PsiElement element) {
        return Paths.get(element.getProject().getBasePath()).toAbsolutePath().normalize().relativize(
            Paths.get(element.getContainingFile().getVirtualFile().getCanonicalPath()).toAbsolutePath().normalize()
        );
    }
}
