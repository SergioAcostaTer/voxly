package com.pigs.voxly.sharedKernel.domain.results;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ResultT<T> {

    private final boolean success;
    private final T value;
    private final List<Error> errors;

    private ResultT(T value) {
        this.success = true;
        this.value = value;
        this.errors = List.of();
    }

    private ResultT(Error error) {
        this.success = false;
        this.value = null;
        this.errors = List.of(error);
    }

    private ResultT(List<Error> errors) {
        this.success = false;
        this.value = null;
        this.errors = List.copyOf(errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getValue() {
        if (isFailure()) {
            throw new IllegalStateException("Cannot access value of a failed result. Error: " + getError());
        }
        return value;
    }

    public Error getError() {
        return errors.isEmpty() ? Error.NONE : errors.getFirst();
    }

    public List<Error> getErrors() {
        return errors;
    }

    public Optional<T> toOptional() {
        return isSuccess() ? Optional.ofNullable(value) : Optional.empty();
    }

    public T getValueOrDefault(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    public Result toResult() {
        return isSuccess() ? Result.success() : Result.failure(errors.toArray(Error[]::new));
    }

    // --- Factory methods ---

    public static <T> ResultT<T> success(T value) {
        return new ResultT<>(value);
    }

    public static <T> ResultT<T> failure(Error error) {
        return new ResultT<>(error);
    }

    public static <T> ResultT<T> failure(List<Error> errors) {
        return new ResultT<>(errors);
    }

    public static <T> ResultT<T> from(T value, Error errorIfNull) {
        return value != null ? success(value) : failure(errorIfNull);
    }

    // --- Functional operations ---

    public <R> ResultT<R> map(Function<T, R> mapper) {
        return isSuccess() ? ResultT.success(mapper.apply(value)) : ResultT.failure(errors);
    }

    public <R> ResultT<R> flatMap(Function<T, ResultT<R>> mapper) {
        return isSuccess() ? mapper.apply(value) : ResultT.failure(errors);
    }

    public Result flatMapToResult(Function<T, Result> mapper) {
        return isSuccess() ? mapper.apply(value) : Result.failure(errors.toArray(Error[]::new));
    }

    public ResultT<T> ensure(Predicate<T> predicate, Error error) {
        if (isFailure()) return this;
        return predicate.test(value) ? this : failure(error);
    }

    public <R> R match(Function<T, R> onSuccess, Function<List<Error>, R> onFailure) {
        return isSuccess() ? onSuccess.apply(value) : onFailure.apply(errors);
    }

    public ResultT<T> onSuccess(Consumer<T> action) {
        if (isSuccess()) action.accept(value);
        return this;
    }

    public ResultT<T> onFailure(Consumer<List<Error>> action) {
        if (isFailure()) action.accept(errors);
        return this;
    }

    @Override
    public String toString() {
        return isSuccess()
                ? "Success: " + value
                : "Failure: " + errors.stream().map(Error::toString).collect(Collectors.joining(", "));
    }
}
