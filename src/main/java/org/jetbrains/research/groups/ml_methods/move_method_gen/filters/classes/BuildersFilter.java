package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class BuildersFilter implements Predicate<PsiClass> {
    @Override
    public boolean test(final @NotNull PsiClass psiClass) {
        String name = psiClass.getName();
        return name == null || !name.endsWith("Builder");
    }
}
