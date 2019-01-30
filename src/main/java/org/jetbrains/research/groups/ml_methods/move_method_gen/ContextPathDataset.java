package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods.ConstructorsFilter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods.EmptyMethodsFilter;

import java.util.*;
import java.util.stream.Collectors;

public class ContextPathDataset {
    private final @NotNull List<PsiMethod> methods;

    private final @NotNull List<PsiClass> classes;

    private final @NotNull List<Point> points;

    private final @NotNull
    Map<PsiMethod, Integer> idOfMethod = new HashMap<>();

    public ContextPathDataset(final @NotNull Dataset dataset) {
        classes = dataset.getClasses();

        Set<PsiMethod> allMethods =
            classes.stream()
                .flatMap(it -> Arrays.stream(it.getMethods()))
                .filter(new EmptyMethodsFilter())
                .filter(new ConstructorsFilter())
                .collect(Collectors.toSet());

        methods = new ArrayList<>();

        {
            int methodId = 0;
            for (PsiMethod method : allMethods) {
                methods.add(method);
                idOfMethod.put(method, methodId);
                methodId++;
            }
        }

        points = new ArrayList<>();

        for (Dataset.Method method : dataset.getMethods()) {
            int methodId = idOfMethod.get(method.getPsiMethod());

            points.add(new Point(methodId, method.getIdOfContainingClass(), 1));

            for (int targetId : method.getIdsOfPossibleTargets()) {
                points.add(new Point(methodId, targetId, 0));
            }
        }
    }

    public List<PsiClass> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<PsiMethod> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public List<Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public List<Integer> getIdsOfMethodsIn(final @NotNull PsiClass psiClass) {
        List<Integer> idsOfMethods = new ArrayList<>();
        for (PsiMethod method : psiClass.getMethods()) {
            if (!idOfMethod.containsKey(method)) {
                continue;
            }

            idsOfMethods.add(idOfMethod.get(method));
        }

        return idsOfMethods;
    }

    public class Point {
        private final int methodId;

        private final int classId;

        private final int label;

        public Point(final int methodId, final int classId, final int label) {
            this.methodId = methodId;
            this.classId = classId;
            this.label = label;
        }

        public int getMethodId() {
            return methodId;
        }

        public int getClassId() {
            return classId;
        }

        public int getLabel() {
            return label;
        }
    }
}
