package com.pigs.voxly.sharedKernel.domain.results;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Result {

    private final boolean success;
    private final List<Error> errors;

    protected Result(boolean success, Error error) {
        this.success = success;
        this.errors = error.equals(Error.NONE) ? List.of() : List.of(error);
    }

    protected Result(boolean success, List<Error> errors) {
        this.success = success;
        this.errors = List.copyOf(errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public Error getError() {
        return errors.isEmpty() ? Error.NONE : errors.getFirst();
    }

    public List<Error> getErrors() {
        return errors;
    }

    // --- Factory methods ---

    public static Result success() {
        return new Result(true, Error.NONE);
    }

    public static Result failure(Error error) {
        return new Result(false, error);
    }

    public static Result failure(Error... errors) {
        return new Result(false, List.of(errors));
    }

    public static Result create(boolean condition, Error error) {
        return condition ? success() : failure(error);
    }

    // --- Functional operations ---

    public <T> T match(Supplier<T> onSuccess, Function<List<Error>, T> onFailure) {
        return success ? onSuccess.get() : onFailure.apply(errors);
    }

    public Result onSuccess(Runnable action) {
        if (success) action.run();
        return this;
    }

    public Result onFailure(Consumer<List<Error>> action) {
        if (!success) action.accept(errors);
        return this;
    }

    public <T> ResultT<T> map(Supplier<T> factory) {
        return success ? ResultT.success(factory.get()) : ResultT.failure(errors);
    }

    public static Result combine(Result... results) {
        List<Error> allErrors = Arrays.stream(results)
                .filter(Result::isFailure)
                .flatMap(r -> r.getErrors().stream())
                .toList();

        return allErrors.isEmpty() ? success() : new Result(false, allErrors);
    }

    @Override
    public String toString() {
        return success
                ? "Success"
                : "Failure: " + errors.stream().map(Error::toString).collect(Collectors.joining(", "));
    }
}
