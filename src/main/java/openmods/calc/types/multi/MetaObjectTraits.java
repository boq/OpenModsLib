package openmods.calc.types.multi;

import java.math.BigInteger;
import openmods.calc.ICallable;
import openmods.calc.UnaryFunction;

public class MetaObjectTraits {

	public static class NoLogicValueException extends RuntimeException {
		private static final long serialVersionUID = -5318443217371834267L;

		public NoLogicValueException(TypedValue value) {
			super(String.format("Value %s is neither true or false", value));
		}
	}

	public abstract static class BoolAdapter extends UnaryFunction<TypedValue> {
		@Override
		protected TypedValue call(TypedValue value) {
			final boolean result = getResult(value);
			return value.domain.create(Boolean.class, result);
		}

		protected abstract boolean getResult(TypedValue value);
	}

	public abstract static class TypedBoolAdapter<T> extends BoolAdapter {
		private final Class<T> type;

		public TypedBoolAdapter(Class<T> type) {
			this.type = type;
		}

		@Override
		protected boolean getResult(TypedValue value) {
			return getResult(value.as(type));
		}

		protected abstract boolean getResult(T value);
	}

	public static final ICallable<TypedValue> ALWAYS_TRUE = new UnaryFunction<TypedValue>() {
		@Override
		protected TypedValue call(TypedValue value) {
			return value.domain.create(Boolean.class, Boolean.TRUE);
		}
	};

	public static final ICallable<TypedValue> ALWAYS_FALSE = new UnaryFunction<TypedValue>() {
		@Override
		protected TypedValue call(TypedValue value) {
			return value.domain.create(Boolean.class, Boolean.FALSE);
		}
	};

	public abstract static class IntAdapter extends UnaryFunction<TypedValue> {

		@Override
		protected TypedValue call(TypedValue value) {
			final int result = getResult(value);
			return value.domain.create(BigInteger.class, BigInteger.valueOf(result));
		}

		protected abstract int getResult(TypedValue value);
	}

	public abstract static class TypedIntAdapter<T> extends IntAdapter {
		private final Class<T> type;

		public TypedIntAdapter(Class<T> type) {
			this.type = type;
		}

		@Override
		protected int getResult(TypedValue value) {
			return getResult(value.as(type));
		}

		protected abstract int getResult(T value);
	}

	public abstract static class StringAdapter extends UnaryFunction<TypedValue> {

		@Override
		protected TypedValue call(TypedValue value) {
			final String result = getResult(value);
			return value.domain.create(String.class, result);
		}

		protected abstract String getResult(TypedValue value);
	}

	public abstract static class TypedStringAdapter<T> extends StringAdapter {
		private final Class<T> type;

		public TypedStringAdapter(Class<T> type) {
			this.type = type;
		}

		@Override
		protected String getResult(TypedValue value) {
			return getResult(value.as(type));
		}

		protected abstract String getResult(T value);
	}
}
