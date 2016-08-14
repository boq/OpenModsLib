package openmods.calc;

import com.google.common.base.Preconditions;

public class SymbolGet<E> implements IExecutable<E> {

	private final String id;

	public SymbolGet(String id) {
		this.id = id;
	}

	@Override
	public void execute(ICalculatorFrame<E> frame) {
		final ISymbol<E> symbol = frame.getSymbol(id);
		Preconditions.checkNotNull(symbol, "Unknown symbol: %s", id);

		try {
			symbol.get(frame);
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute symbol '" + id + "'", e);
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SymbolGet) {
			final SymbolGet<?> other = (SymbolGet<?>)obj;
			return other.id.equals(this.id);
		}
		return false;
	}

	@Override
	public String toString() {
		return "@" + id;
	}

	@Override
	public String serialize() {
		return "@" + id;
	}
}