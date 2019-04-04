package org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions;

import org.jetbrains.annotations.NotNull;

public class UnexpectedEmptyContext extends Exception {
    public UnexpectedEmptyContext(final @NotNull String message) {
        super(message);
    }
}
