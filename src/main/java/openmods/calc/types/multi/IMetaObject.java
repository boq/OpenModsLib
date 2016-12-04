package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.ICallable;

public interface IMetaObject {

	public boolean has(String slot);

	public ICallable<TypedValue> get(String slot);

	public Optional<ICallable<TypedValue>> getOptional(String slot);

}
