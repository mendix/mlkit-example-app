package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.MendixRuntimeException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class StaleDBObjectCleaner {

  private StaleDBObjectCleaner() {}

  public static <T> long cleanupStaleObjects(
      final Class<T> clazz,
      final String entityName,
      final String expiryFieldName,
      final long olderThan,
      final int batchSize)
      throws InterruptedException {
    return ThreadingBatchingListProcessor.process(
        clazz.getSimpleName() + "Cleanup",
        batchSize,
        retrieveStaleObjects(clazz, entityName, expiryFieldName, olderThan),
        deleteStaleObject());
  }

  private static <T> BiFunction<Integer, Long, List<T>> retrieveStaleObjects(
      final Class<T> clazz,
      final String entityName,
      final String expiryFieldName,
      final long deadline) {
    return (batchSize, total) -> {
      final IContext context = Core.createSystemContext();
      return MendixUtils.retrieveFromDatabase(
          context,
          clazz,
          batchSize,
          0,
          Collections.emptyMap(),
          0,
          "//%s[%s <= $deadline]",
          Map.of("deadline", deadline),
          entityName,
          expiryFieldName);
    };
  }

  private static <T> Consumer<T> deleteStaleObject() {
    return staleObject -> {
      // create new system contexts to avoid concurrency clashes
      final IContext context = Core.createSystemContext();
      delete(context, staleObject);
    };
  }

  private static IMendixObject delete(IContext context, Object staleObject) {
    try {
      Method getMendixObjectMethod =
          staleObject.getClass().getDeclaredMethod("delete", IContext.class);
      return (IMendixObject) getMendixObjectMethod.invoke(staleObject, context);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new MendixRuntimeException(
          String.format(
              "Could not cleaned object of type: %s, reason: %s ",
              staleObject.getClass().getSimpleName(), e.getMessage()));
    }
  }
}
