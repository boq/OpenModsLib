package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import openmods.calc.ICallable;
import openmods.reflection.TypeVariableHolder;

public class TypeDomain {

	private interface RawConverter {
		public Object convert(Object value);
	}

	public static class TypeVariableHolders {
		@TypeVariableHolder(IConverter.class)
		public static class Converter {
			public static TypeVariable<?> S;
			public static TypeVariable<?> T;
		}
	}

	private static class WrappedConverter<S, T> implements RawConverter {
		private final Class<? extends S> source;

		private final IConverter<S, T> converter;

		public WrappedConverter(Class<? extends S> source, IConverter<S, T> converter) {
			this.source = source;
			this.converter = converter;
		}

		@Override
		public Object convert(Object value) {
			S input = source.cast(value);
			T result = converter.convert(input);
			return result;
		}
	}

	private static class CastConverter<T> implements RawConverter {
		private final Class<? extends T> target;

		public CastConverter(Class<? extends T> target) {
			this.target = target;
		}

		@Override
		public Object convert(Object value) {
			return target.cast(value);
		}

	}

	private static class EmptyMetaObject implements IMetaObject {

		public static final IMetaObject INSTANCE = new EmptyMetaObject();

		@Override
		public boolean has(String slot) {
			return false;
		}

		@Override
		public ICallable<TypedValue> get(String slot) {
			throw new IllegalStateException("No slot: " + slot);
		}

		@Override
		public Optional<ICallable<TypedValue>> getOptional(String slot) {
			return Optional.absent();
		}

	}

	private static class TypeInfo {
		public final String name;
		public final IMetaObject defaultMetaObject;

		public TypeInfo(String name, IMetaObject defaultMetaObject) {
			this.name = name;
			this.defaultMetaObject = defaultMetaObject;
		}

	}

	private final Map<Class<?>, TypeInfo> allowedTypes = Maps.newIdentityHashMap();

	private final Table<Class<?>, Class<?>, RawConverter> converters = HashBasedTable.create();

	public TypeDomain registerType(Class<?> type) {
		registerType(type, type.getSimpleName());
		return this;
	}

	public TypeDomain registerType(Class<?> type, String shortName) {
		registerType(type, shortName, EmptyMetaObject.INSTANCE);
		return this;
	}

	public TypeDomain registerType(Class<?> type, String shortName, IMetaObject defaultMetaObject) {
		allowedTypes.put(type, new TypeInfo(shortName, defaultMetaObject));
		return this;
	}

	public boolean isKnownType(Class<?> type) {
		return allowedTypes.containsKey(type);
	}

	public void checkIsKnownType(Class<?> type) {
		Preconditions.checkState(allowedTypes.containsKey(type), "Type '%s' is not allowed in domain", type);
	}

	public String getName(Class<?> type) {
		final TypeInfo typeInfo = allowedTypes.get(type);
		Preconditions.checkState(typeInfo != null, "Type %s is not registered", type);
		return typeInfo.name;
	}

	public Optional<String> tryGetName(Class<?> type) {
		final TypeInfo typeInfo = allowedTypes.get(type);
		if (typeInfo == null) return Optional.absent();
		return Optional.of(typeInfo.name);
	}

	public IMetaObject getDefaultMetaObject(Class<?> type) {
		final TypeInfo typeInfo = allowedTypes.get(type);
		Preconditions.checkState(typeInfo != null, "Type %s is not registered", type);
		return typeInfo.defaultMetaObject;
	}

	public <T> TypeDomain registerCast(Class<? extends T> source, Class<T> target) {
		checkIsKnownType(source);
		checkIsKnownType(target);
		final RawConverter prev = converters.put(source, target, new CastConverter<T>(target));
		Preconditions.checkState(prev == null, "Duplicate registration for types (%s,%s)", source, target);
		return this;
	}

	public <S, T> TypeDomain registerConverter(Class<? extends S> source, Class<? extends T> target, IConverter<S, T> converter) {
		checkIsKnownType(source);
		checkIsKnownType(target);
		final RawConverter prev = converters.put(source, target, new WrappedConverter<S, T>(source, converter));
		Preconditions.checkState(prev == null, "Duplicate registration for types (%s,%s)", source, target);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <S, T> TypeDomain registerConverter(IConverter<S, T> converter) {
		final TypeToken<?> converterType = TypeToken.of(converter.getClass());
		final Class<S> sourceType = (Class<S>)converterType.resolveType(TypeVariableHolders.Converter.S).getRawType();
		final Class<T> targetType = (Class<T>)converterType.resolveType(TypeVariableHolders.Converter.T).getRawType();
		return registerConverter(sourceType, targetType, converter);
	}

	private RawConverter getConverter(TypedValue value, Class<?> type) {
		final RawConverter converter = converters.get(value.type, type);
		Preconditions.checkArgument(converter != null, "No known conversion from %s to %s", value.type, type);
		return converter;
	}

	public boolean hasConversion(Class<?> from, Class<?> to) {
		return converters.contains(from, to);
	}

	public void checkConversion(Class<?> from, Class<?> to) {
		Preconditions.checkArgument(hasConversion(from, to), "No known conversion from %s to %s", from, to);
	}

	public TypedValue convert(TypedValue value, Class<?> type) {
		Preconditions.checkArgument(value.domain == this, "Mixed domain");
		if (value.type == type) return value;
		final RawConverter converter = getConverter(value, type);
		final Object convertedValue = converter.convert(value.value);
		return new TypedValue(this, type, convertedValue);
	}

	public <T> T unwrap(TypedValue value, Class<T> type) {
		Preconditions.checkArgument(value.domain == this, "Mixed domain");
		if (value.type == type) return type.cast(value.value);
		final RawConverter converter = getConverter(value, type);
		final Object convertedValue = converter.convert(value.value);
		return type.cast(convertedValue);
	}

	public enum Coercion {
		TO_LEFT, TO_RIGHT, INVALID;
	}

	private static final Map<Coercion, Coercion> inverses = Maps.newEnumMap(Coercion.class);

	static {
		inverses.put(Coercion.TO_LEFT, Coercion.TO_RIGHT);
		inverses.put(Coercion.TO_RIGHT, Coercion.TO_LEFT);
		inverses.put(Coercion.INVALID, Coercion.INVALID);
	}

	private final Table<Class<?>, Class<?>, Coercion> coercionRules = HashBasedTable.create();

	public TypeDomain registerCoercionRule(Class<?> left, Class<?> right, Coercion rule) {
		checkIsKnownType(left);
		checkIsKnownType(right);
		Preconditions.checkArgument(left != right);
		if (rule == Coercion.TO_LEFT) {
			checkConversion(right, left);
		} else if (rule == Coercion.TO_RIGHT) {
			checkConversion(left, right);
		}

		final Coercion prev = coercionRules.put(left, right, rule);
		Preconditions.checkState(prev == null || prev == rule, "Duplicate coercion rule for (%s,%s): %s -> %s", left, right, rule);
		return this;
	}

	public TypeDomain registerSymmetricCoercionRule(Class<?> left, Class<?> right, Coercion rule) {
		registerCoercionRule(left, right, rule);
		registerCoercionRule(right, left, inverses.get(rule));
		return this;
	}

	public Coercion getCoercionRule(Class<?> left, Class<?> right) {
		if (left == right) return Coercion.TO_LEFT;
		final Coercion result = coercionRules.get(left, right);
		return result != null? result : Coercion.INVALID;
	}

	public <T> TypedValue create(Class<T> type, T value) {
		checkIsKnownType(type);
		return new TypedValue(this, type, value);
	}

	public <T> TypedValue create(Class<T> type, T value, IMetaObject metaObject) {
		checkIsKnownType(type);
		return new TypedValue(this, type, value, metaObject);
	}

	public <T> TypedValue castAndCreate(Class<T> type, Object value) {
		return create(type, type.cast(value));
	}
}
