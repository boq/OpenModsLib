package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.ISymbol;
import openmods.calc.NestedSymbolMap;
import openmods.calc.SymbolMap;
import openmods.utils.Stack;

public class CompositeSymbolMap extends NestedSymbolMap<TypedValue> {

	private final TypeDomain domain;
	private final TypedValue value;
	private final ICallable<TypedValue> attrSlot;

	public CompositeSymbolMap(SymbolMap<TypedValue> parent, TypeDomain domain, TypedValue value) {
		super(parent);
		this.domain = domain;
		this.value = value;
		this.attrSlot = value.getMetaObject().get(TypedCalcConstants.SLOT_ATTRIBUTE);
	}

	@Override
	public void put(String name, ISymbol<TypedValue> symbol) {
		throw new IllegalStateException("Trying to set value in read-only frame");
	}

	private TypedValue getValue(String name, Frame<TypedValue> frame) {
		final Stack<TypedValue> stack = frame.stack();
		stack.push(value);
		stack.push(domain.create(String.class, name));
		attrSlot.call(frame, Optional.of(2), Optional.of(1));
		final TypedValue result = stack.pop();
		Preconditions.checkState(stack.isEmpty(), "Values left on stack");
		return result;
	}

	@Override
	public ISymbol<TypedValue> get(final String name) {
		return new ISymbol<TypedValue>() {

			@Override
			public TypedValue get() {
				final Frame<TypedValue> frame = FrameFactory.newLocalFrame(parent);
				return getValue(name, frame);
			}

			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				final Frame<TypedValue> subframe = FrameFactory.newLocalFrameWithSubstack(frame, 0);
				final TypedValue value = getValue(name, frame);
				value.getMetaObject().get(TypedCalcConstants.SLOT_CALL).call(subframe, argumentsCount, returnsCount);
			}
		};
	}
}
