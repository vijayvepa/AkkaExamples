package common;

import akka.grpc.GrpcServiceException;
import io.grpc.Status;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class GrpcUtils {
  public static <TModel, TProto> CompletionStage<TProto> handleNotFound(
      CompletionStage<TModel> get,
      Function<TModel, Boolean> isEmpty,
      Function<TModel, TProto> toProto,
      String description)
  {
    return get.thenApply(summary -> {
      if (isEmpty.apply(summary)) {
        throw new GrpcServiceException(Status.NOT_FOUND.withDescription(description));
      }
      return toProto.apply(summary);
    });
  }

  public static  <T> CompletionStage<T> convertError(CompletionStage<T> response) {
    return response.exceptionally(ex -> {
      if (ex instanceof TimeoutException) {
        throw new GrpcServiceException(Status.UNAVAILABLE.withDescription("Operation timed out"));
      }
      throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()));
    });
  }
}
