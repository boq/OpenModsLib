package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.utils.Stack;

public class SlotCallerFunction extends FixedCallable<TypedValue> {

	private final String slot;

	public SlotCallerFunction(String slot, int args) {
		super(args + 1, 1); // self + args
		this.slot = slot;
	}

	@Override
	public void call(Frame<TypedValue> frame) {
		final Stack<TypedValue> stack = frame.stack().substack(argCount);
		final TypedValue self = stack.peek(argCount);
		final ICallable<TypedValue> slotCallable = self.getMetaObject().get(slot);
		slotCallable.call(FrameFactory.newLocalFrame(frame, stack), Optional.of(argCount), Optional.of(1));
	}

}
