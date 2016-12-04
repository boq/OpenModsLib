package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import openmods.calc.ICallable;

public class MappedMetaObject implements IMetaObject {

	private final Map<String, ICallable<TypedValue>> slots;

	public MappedMetaObject(Map<String, ICallable<TypedValue>> slots) {
		this.slots = ImmutableMap.copyOf(slots);
	}

	@Override
	public boolean has(String slot) {
		return slots.containsKey(slot);
	}

	@Override
	public ICallable<TypedValue> get(String slot) {
		final ICallable<TypedValue> slotCallable = slots.get(slot);
		Preconditions.checkState(slotCallable != null, "No slot: %s", slot);
		return slotCallable;
	}

	@Override
	public Optional<ICallable<TypedValue>> getOptional(String slot) {
		final ICallable<TypedValue> slotCallable = slots.get(slot);
		if (slotCallable == null) return Optional.absent();
		return Optional.of(slotCallable);
	}

	public static class Builder {
		private Builder() {}

		private final ImmutableMap.Builder<String, ICallable<TypedValue>> slots = ImmutableMap.builder();

		public Builder put(String slot, ICallable<TypedValue> slotCallable) {
			slots.put(slot, slotCallable);
			return this;
		}

		public IMetaObject build() {
			return new MappedMetaObject(slots.build());
		}
	}

	public static Builder builder() {
		return new Builder();
	}

}
