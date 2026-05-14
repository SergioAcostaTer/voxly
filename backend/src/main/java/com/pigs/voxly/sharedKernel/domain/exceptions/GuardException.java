package com.pigs.voxly.sharedKernel.domain.exceptions;

public final class GuardException extends DomainException {

    private final String parameterName;

    public GuardException(String parameterName, String message) {
        super(message);
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public static GuardException forParam(String parameterName, String violation) {
        return new GuardException(parameterName, "'%s' %s.".formatted(parameterName, violation));
    }
}
