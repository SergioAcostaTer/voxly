package com.pigs.voxly.sharedKernel.domain.results;

public final class DomainErrors {

    private DomainErrors() {
    }

    public static Error notFound(String entityName, Object id) {
        return Error.notFound(
                entityName + ".NotFound",
                "%s with ID '%s' was not found".formatted(entityName, id));
    }

    public static Error valueIsRequired(String valueName) {
        return Error.validation(
                "General.ValueIsRequired",
                "'%s' is required".formatted(valueName));
    }

    public static Error invalidValue(String valueName) {
        return Error.validation(
                "General.InvalidValue",
                "'%s' has an invalid value".formatted(valueName));
    }

    public static Error unexpected(String message) {
        return Error.unexpected("General.Unexpected", message);
    }

    public static Error conflict(String message) {
        return Error.conflict("General.Conflict", message);
    }
}
