package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.types.multi.Cons;
import openmods.calc.types.multi.IComposite;
import openmods.calc.types.multi.Symbol;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypedBinaryOperator;
import openmods.calc.types.multi.TypedFunction;
import openmods.calc.types.multi.TypedUnaryOperator;
import openmods.calc.types.multi.TypedValue;
import openmods.calc.types.multi.TypedValueCalculatorFactory;
import openmods.math.Complex;
import openmods.reflection.MethodAccess;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Test;

public class TypedValueCalculatorTest {

	static {
		final TypeVariableHolderHandler filler = new TypeVariableHolderHandler();
		filler.fillHolders(TypedBinaryOperator.TypeVariableHolders.class);
		filler.fillHolders(TypedUnaryOperator.TypeVariableHolders.class);
		filler.fillHolders(TypeDomain.TypeVariableHolders.class);
		filler.fillHolders(MethodAccess.TypeVariableHolders.class);
		filler.fillHolders(TypedFunction.class);
	}

	private final Calculator<TypedValue, ExprType> sut = TypedValueCalculatorFactory.create();

	public CalcCheck<TypedValue> prefix(String value) {
		return CalcCheck.create(sut, value, ExprType.PREFIX);
	}

	public CalcCheck<TypedValue> infix(String value) {
		return CalcCheck.create(sut, value, ExprType.INFIX);
	}

	public CalcCheck<TypedValue> postfix(String value) {
		return CalcCheck.create(sut, value, ExprType.POSTFIX);
	}

	public CalcCheck<TypedValue> compiled(IExecutable<TypedValue> expr) {
		return CalcCheck.create(sut, expr);
	}

	private final TypedValue NULL = sut.environment.nullValue();

	private final TypeDomain domain = NULL.domain;

	private TypedValue s(String value) {
		return domain.create(String.class, value);
	}

	private TypedValue sym(String value) {
		return domain.create(Symbol.class, Symbol.get(value));
	}

	private TypedValue i(long value) {
		return domain.create(BigInteger.class, BigInteger.valueOf(value));
	}

	private TypedValue d(double value) {
		return domain.create(Double.class, value);
	}

	private TypedValue b(boolean value) {
		return domain.create(Boolean.class, value);
	}

	private TypedValue nil() {
		return sut.environment.nullValue();
	}

	private TypedValue cons(TypedValue car, TypedValue cdr) {
		return domain.create(Cons.class, new Cons(car, cdr));
	}

	private final TypedValue TRUE = b(true);

	private final TypedValue FALSE = b(false);

	private TypedValue c(double re, double im) {
		return domain.create(Complex.class, Complex.cartesian(re, im));
	}

	@Test
	public void testBasicPrefix() {
		prefix("(+ 1 2)").expectResult(i(3)).expectEmptyStack();
		prefix("(* 2 3)").expectResult(i(6)).expectEmptyStack();
		prefix("(- 1)").expectResult(i(-1)).expectEmptyStack();
		prefix("(* (- 1) (+ 2 3))").expectResult(i(-5)).expectEmptyStack();
		prefix("(/ 10 2)").expectResult(d(5.0)).expectEmptyStack();
		prefix("(** 2 5)").expectResult(i(32)).expectEmptyStack();
		prefix("(| 0b010 0b101)").expectResult(i(7)).expectEmptyStack();
	}

	@Test
	public void testPrefixFunctions() {
		prefix("(max 1)").expectResult(i(1)).expectEmptyStack();
		prefix("(max 1 2)").expectResult(i(2)).expectEmptyStack();
		prefix("(max 1 2 3)").expectResult(i(3)).expectEmptyStack();

		prefix("(max 1 2.0 3)").expectResult(i(3)).expectEmptyStack();
		prefix("(max true 2 3.0)").expectResult(d(3)).expectEmptyStack();

		prefix("(min true 2 3.0)").expectResult(b(true)).expectEmptyStack();

		prefix("(sum 1 2.0 3)").expectResult(d(6.0)).expectEmptyStack();
		prefix("(sum 'a' 'b' 'c')").expectResult(s("abc")).expectEmptyStack();

		prefix("(avg 1 2 3)").expectResult(d(2.0)).expectEmptyStack();
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(i(3)).expectEmptyStack();
		postfix("0.5 0.5 +").expectResult(d(1)).expectEmptyStack();
		postfix("0.25 0.25 +").expectResult(d(0.5)).expectEmptyStack();
		postfix("2 3 *").expectResult(i(6)).expectEmptyStack();
		postfix("10 2 /").expectResult(d(5.0)).expectEmptyStack();
		postfix("10 2 //").expectResult(i(5)).expectEmptyStack();
		postfix("1 2 +").expectResult(i(3)).expectEmptyStack();
		postfix("true true &&").expectResult(TRUE).expectEmptyStack();
		postfix("false true &&").expectResult(FALSE).expectEmptyStack();
		postfix("'abc' 'def' +").expectResult(s("abcdef")).expectEmptyStack();
		postfix("'abc' 'def' <=").expectResult(TRUE).expectEmptyStack();
	}

	@Test
	public void testCoercionPostfix() {
		postfix("0.5 1 +").expectResult(d(1.5)).expectEmptyStack();

		postfix("2 5.0 **").expectResult(d(32)).expectEmptyStack();
		postfix("2 5 **").expectResult(i(32)).expectEmptyStack();

		postfix("true 2 +").expectResult(i(3)).expectEmptyStack();
		postfix("true 2.0 +").expectResult(d(3.0)).expectEmptyStack();
	}

	@Test
	public void testArithmeticInfix() {
		infix("1 + 2").expectResult(i(3)).expectEmptyStack();
		infix("2 * 3").expectResult(i(6)).expectEmptyStack();
		infix("10 / 2").expectResult(d(5.0)).expectEmptyStack();

		infix("10 // 2").expectResult(i(5)).expectEmptyStack();
		infix("10.7 // 2").expectResult(d(5.0)).expectEmptyStack();
		infix("-2.3 // 3").expectResult(d(-1.0)).expectEmptyStack();

		infix("2 ** 5").expectResult(i(32)).expectEmptyStack();
		infix("2 ** 0").expectResult(i(1)).expectEmptyStack();
		infix("2 ** -5").expectResult(d(1.0 / 32.0)).expectEmptyStack();

		infix("5 % 2").expectResult(i(1)).expectEmptyStack();
		infix("5.125 % 1").expectResult(d(0.125)).expectEmptyStack();
		infix("5 % 2.0").expectResult(d(1.0)).expectEmptyStack();
		infix("5.125 % 1.0").expectResult(d(0.125)).expectEmptyStack();

		infix("-true").expectResult(i(-1)).expectEmptyStack();
		infix("2*true").expectResult(i(2)).expectEmptyStack();
		infix("2*-true").expectResult(i(-2)).expectEmptyStack();
		infix("2true").expectResult(i(2)).expectEmptyStack();
		infix("2(true)").expectResult(i(2)).expectEmptyStack();

		infix("-2*-3*10**3").expectResult(i(6000)).expectEmptyStack();
		infix("-2*-3*10**+3").expectResult(i(6000)).expectEmptyStack();
		infix("-2*-3*10**-3").expectResult(d(6e-3)).expectEmptyStack();
		infix("2*10**2+3*10**3").expectResult(i(3200)).expectEmptyStack();
		infix("2*10**2*3*10**3").expectResult(i(600000)).expectEmptyStack();

		infix("0.1 + true").expectResult(d(1.1)).expectEmptyStack();

		infix("'abc' * 2").expectResult(s("abcabc")).expectEmptyStack();
	}

	@Test
	public void testLogicInfix() {
		infix("!true").expectResult(FALSE).expectEmptyStack();
		infix("!1").expectResult(FALSE).expectEmptyStack();
		infix("!'hello'").expectResult(FALSE).expectEmptyStack();
		infix("!''").expectResult(TRUE).expectEmptyStack();
		infix("!0").expectResult(TRUE).expectEmptyStack();

		infix("'abc' && 5").expectResult(i(5)).expectEmptyStack();
		infix("0 && 'abc'").expectResult(i(0)).expectEmptyStack();
		infix("'' && 4").expectResult(s("")).expectEmptyStack();

		infix("'abc' || 5").expectResult(s("abc")).expectEmptyStack();
		infix("'' || 5").expectResult(i(5)).expectEmptyStack();
		infix("'' || 0").expectResult(i(0)).expectEmptyStack();
	}

	@Test
	public void testBitwiseInfix() {
		infix("~true").expectResult(i(0)).expectEmptyStack();
		infix("~0b10").expectResult(i(-3)).expectEmptyStack();
		infix("0b10110 ^ 0b101101").expectResult(i(0x3B)).expectEmptyStack();
		infix("0b1010 << 0b10").expectResult(i(40)).expectEmptyStack();
	}

	@Test
	public void testCompare() {
		infix("2 < 3").expectResult(TRUE).expectEmptyStack();
		infix("3 != 3").expectResult(FALSE).expectEmptyStack();
		infix("3 <= 3").expectResult(TRUE).expectEmptyStack();

		infix("3 <=> 3").expectResult(i(0)).expectEmptyStack();
		infix("2 <=> 3").expectResult(i(-1)).expectEmptyStack();
		infix("3 <=> 2").expectResult(i(+1)).expectEmptyStack();
	}

	@Test
	public void testEquals() {
		infix("2 == 2").expectResult(b(true)).expectEmptyStack();
		infix("null == null").expectResult(b(true)).expectEmptyStack();
		infix("2 == null").expectResult(b(false)).expectEmptyStack();

		infix("2 != null").expectResult(b(true)).expectEmptyStack();
		infix("null != 2").expectResult(b(true)).expectEmptyStack();
		infix("null != null").expectResult(b(false)).expectEmptyStack();
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(i(0)).expectEmptyStack();

		infix("1 + 2 * 3").expectResult(i(7)).expectEmptyStack();
		infix("1 + (2 * 3)").expectResult(i(7)).expectEmptyStack();
		infix("(1 + 2) * 3").expectResult(i(9)).expectEmptyStack();
		infix("-(1 + 2) * 3").expectResult(i(-9)).expectEmptyStack();
		infix("(1 + 2) * -3").expectResult(i(-9)).expectEmptyStack();
		infix("--3").expectResult(i(3)).expectEmptyStack();

		infix("2 * 2 ** 2").expectResult(i(8)).expectEmptyStack();
		infix("2 * (2 ** 2)").expectResult(i(8)).expectEmptyStack();
		infix("(2 * 2) ** 2").expectResult(i(16)).expectEmptyStack();

		infix("2 == 4 || 5 <= 6").expectResult(TRUE).expectEmptyStack();
		infix("2 << 3 + 1").expectResult(i(32)).expectEmptyStack();
	}

	@Test
	public void testTypeFunctions() {
		infix("issymbol(#test)").expectResult(b(true)).expectEmptyStack();
		infix("issymbol(#+)").expectResult(b(true)).expectEmptyStack();
		infix("issymbol(#2)").expectResult(b(false)).expectEmptyStack();

		infix("type(null)").expectResult(s("<null>")).expectEmptyStack();
		infix("type(true)").expectResult(s("bool")).expectEmptyStack();
		infix("type(5)").expectResult(s("int")).expectEmptyStack();
		infix("type(5.0)").expectResult(s("float")).expectEmptyStack();
		infix("type('a')").expectResult(s("str")).expectEmptyStack();
		infix("type(I)").expectResult(s("complex")).expectEmptyStack();
		infix("type(2 + 3I)").expectResult(s("complex")).expectEmptyStack();
		infix("type(2 + 3*I)").expectResult(s("complex")).expectEmptyStack();

		infix("isint(null)").expectResult(b(false)).expectEmptyStack();
		infix("isint(true)").expectResult(b(false)).expectEmptyStack();
		infix("isint(5)").expectResult(b(true)).expectEmptyStack();
		infix("isint(5.0)").expectResult(b(false)).expectEmptyStack();
		infix("isint('hello')").expectResult(b(false)).expectEmptyStack();
		infix("isint('I')").expectResult(b(false)).expectEmptyStack();
		infix("isint(#2)").expectResult(b(true)).expectEmptyStack();

		infix("iscomplex(1)").expectResult(b(false)).expectEmptyStack();
		infix("iscomplex(I)").expectResult(b(true)).expectEmptyStack();
		infix("iscomplex(1 + I)").expectResult(b(true)).expectEmptyStack();

		infix("isnumber(null)").expectResult(b(false)).expectEmptyStack();
		infix("isnumber(true)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber(5)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber(5.0)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber(I)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber(3 + 4I)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber('hello')").expectResult(b(false)).expectEmptyStack();

		infix("int(true)").expectResult(i(1)).expectEmptyStack();
		infix("int(5)").expectResult(i(5)).expectEmptyStack();
		infix("int(5.2)").expectResult(i(5)).expectEmptyStack();
		infix("int('6')").expectResult(i(6)).expectEmptyStack();
		infix("int('29A', 16)").expectResult(i(666)).expectEmptyStack();

		infix("float(true)").expectResult(d(1)).expectEmptyStack();
		infix("float(5)").expectResult(d(5)).expectEmptyStack();
		infix("float(5.2)").expectResult(d(5.2)).expectEmptyStack();
		infix("float('6.1')").expectResult(d(6.1)).expectEmptyStack();
		infix("float('29A.1', 16)").expectResult(d(666.0625)).expectEmptyStack();

		infix("number(true)").expectResult(b(true)).expectEmptyStack();
		infix("number(5)").expectResult(i(5)).expectEmptyStack();
		infix("number(5.2)").expectResult(d(5.2)).expectEmptyStack();
		infix("number('6')").expectResult(i(6)).expectEmptyStack();
		infix("number('6.1')").expectResult(d(6.1)).expectEmptyStack();
		infix("number('29A', 16)").expectResult(i(666)).expectEmptyStack();
		infix("number('29A.1', 16)").expectResult(d(666.0625)).expectEmptyStack();
		infix("number(3I)").expectResult(c(0, 3)).expectEmptyStack();
		infix("number(3 + 4I)").expectResult(c(3, 4)).expectEmptyStack();

		infix("bool(true)").expectResult(b(true)).expectEmptyStack();
		infix("bool(5)").expectResult(b(true)).expectEmptyStack();
		infix("bool(0)").expectResult(b(false)).expectEmptyStack();
		infix("bool(I)").expectResult(b(true)).expectEmptyStack();
		infix("bool(0I)").expectResult(b(false)).expectEmptyStack();
		infix("bool('')").expectResult(b(false)).expectEmptyStack();
		infix("bool('a')").expectResult(b(true)).expectEmptyStack();
		infix("bool(null)").expectResult(b(false)).expectEmptyStack();

		infix("str(true)").expectResult(s("true")).expectEmptyStack();
		infix("str(5)").expectResult(s("5")).expectEmptyStack();
		infix("str(5.2)").expectResult(s("5.2")).expectEmptyStack();
		infix("str('aaa')").expectResult(s("aaa")).expectEmptyStack();
		infix("str(I)").expectResult(s("0.0+1.0I")).expectEmptyStack();
		infix("str(3 + 4I)").expectResult(s("3.0+4.0I")).expectEmptyStack();

		infix("parse('\"aaa\"')").expectResult(s("aaa")).expectEmptyStack();
		infix("parse('0x29A')").expectResult(i(666)).expectEmptyStack();
		infix("parse('0x29A.1')").expectResult(d(666.0625)).expectEmptyStack();
		infix("parse('100#10')").expectResult(i(100)).expectEmptyStack();
	}

	@Test
	public void testArithmeticFunctions() {
		infix("isnan(NAN)").expectResult(b(true)).expectEmptyStack();
		infix("isnan(5)").expectResult(b(false)).expectEmptyStack();

		infix("isinf(INF)").expectResult(b(true)).expectEmptyStack();
		infix("isinf(-INF)").expectResult(b(true)).expectEmptyStack();
		infix("isnan(4)").expectResult(b(false)).expectEmptyStack();

		infix("ceil(true)").expectResult(b(true)).expectEmptyStack();
		infix("ceil(2)").expectResult(i(2)).expectEmptyStack();
		infix("ceil(2.0)").expectResult(d(2)).expectEmptyStack();
		infix("ceil(2.4)").expectResult(d(3)).expectEmptyStack();

		infix("abs(true)").expectResult(b(true)).expectEmptyStack();
		infix("abs(-2)").expectResult(i(2)).expectEmptyStack();
		infix("abs(+2)").expectResult(i(2)).expectEmptyStack();
		infix("abs(2.0)").expectResult(d(2)).expectEmptyStack();
		infix("abs(-2.4)").expectResult(d(2.4)).expectEmptyStack();
		infix("abs(3+4I)").expectResult(d(5)).expectEmptyStack();

		infix("exp(false)").expectResult(d(1.0)).expectEmptyStack();
		// infix("exp(true)").expectResult(d(Math.E)).expectEmptyStack();
		// infix("exp(1)").expectResult(d(Math.E)).expectEmptyStack();

		// infix("ln(I)").expectResult(c(0, Math.PI / 2)).expectEmptyStack();

		infix("log(true)").expectResult(d(0)).expectEmptyStack();
		infix("log(1)").expectResult(d(0)).expectEmptyStack();
		infix("log(1.0)").expectResult(d(0)).expectEmptyStack();
		infix("log(10)").expectResult(d(1)).expectEmptyStack();
		infix("log(100)").expectResult(d(2)).expectEmptyStack();
		infix("log(E, E)").expectResult(d(1)).expectEmptyStack();
		infix("log(2, E) == ln(2)").expectResult(b(true)).expectEmptyStack();
	}

	@Test
	public void testDotOperator() {
		class TestComposite implements IComposite {
			private final List<String> path;

			public TestComposite() {
				this.path = ImmutableList.of();
			}

			public TestComposite(List<String> parentPath, String elem) {
				this.path = ImmutableList.<String> builder().addAll(parentPath).add(elem).build();
			}

			@Override
			public TypedValue get(TypeDomain domain, String component) {
				if (component.equals("path")) return domain.create(String.class, Joiner.on("/").join(path));
				else return domain.create(IComposite.class, new TestComposite(path, component));
			}

			@Override
			public String subtype() {
				return "nested:" + path.size();
			}
		}

		sut.environment.setGlobalSymbol("root", Constant.create(sut.environment.nullValue().domain.create(IComposite.class, new TestComposite())));
		infix("type(root)=='object'").expectResult(b(true)).expectEmptyStack();
		infix("isobject(root)").expectResult(b(true)).expectEmptyStack();
		infix("bool(root)").expectResult(b(true)).expectEmptyStack();
		infix("root.path").expectResult(s("")).expectEmptyStack();

		prefix("(== (type root) 'object')").expectResult(b(true)).expectEmptyStack();
		prefix("(isobject root)").expectResult(b(true)).expectEmptyStack();
		prefix("(. root path)").expectResult(s("")).expectEmptyStack();

		infix("isobject(root.a)").expectResult(b(true)).expectEmptyStack();
		infix("root.a.path").expectResult(s("a")).expectEmptyStack();
		prefix("(. root a path)").expectResult(s("a")).expectEmptyStack();

		infix("isobject(root.a.b)").expectResult(b(true)).expectEmptyStack();
		infix("root.a.b.path").expectResult(s("a/b")).expectEmptyStack();

		infix("root.'a'.path").expectResult(s("a")).expectEmptyStack();
		prefix("(. root 'a' path)").expectResult(s("a")).expectEmptyStack();

		infix("root.('a').path").expectResult(s("a")).expectEmptyStack();

		infix("(root.a).b.path").expectResult(s("a/b")).expectEmptyStack();
		prefix("(. (. root a) b path)").expectResult(s("a/b")).expectEmptyStack();
	}

	@Test
	public void testInfixFunctions() {
		infix("max(true, 2, 3.0)").expectResult(d(3)).expectEmptyStack();

		infix("min(true, 2, 3.0)").expectResult(b(true)).expectEmptyStack();

		infix("sum(1, 2.0, 3)").expectResult(d(6.0)).expectEmptyStack();
		infix("sum('a', 'b', 'c')").expectResult(s("abc")).expectEmptyStack();

		infix("avg(1, 2, 3)").expectResult(d(2.0)).expectEmptyStack();
	}

	@Test
	public void testPrefixModifierQuotes() {
		prefix("#()").expectResult(nil()).expectEmptyStack();
		prefix("#2").expectResult(i(2)).expectEmptyStack();
		prefix("#'hello'").expectResult(s("hello")).expectEmptyStack();
		prefix("#(1)").expectResult(cons(i(1), nil())).expectEmptyStack();
		prefix("#(1 2)").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
	}

	@Test
	public void testInfixModifierQuotes() {
		infix("#()").expectResult(nil()).expectEmptyStack();
		infix("#2").expectResult(i(2)).expectEmptyStack();
		infix("#'hello'").expectResult(s("hello")).expectEmptyStack();
		infix("#(1)").expectResult(cons(i(1), nil())).expectEmptyStack();
		infix("#(1 2)").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
	}

	@Test
	public void testPrefixSymbolQuotes() {
		prefix("(quote ())").expectResult(nil()).expectEmptyStack();
		prefix("(quote 2)").expectResult(i(2)).expectEmptyStack();
		prefix("(quote 'hello')").expectResult(s("hello")).expectEmptyStack();
		prefix("(quote (1))").expectResult(cons(i(1), nil())).expectEmptyStack();
		prefix("(quote (1 2))").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
	}

	@Test
	public void testInfixSymbolQuotes() {
		infix("quote(())").expectResult(nil()).expectEmptyStack();
		infix("quote(2)").expectResult(i(2)).expectEmptyStack();
		infix("quote('hello')").expectResult(s("hello")).expectEmptyStack();
		infix("quote((1))").expectResult(cons(i(1), nil())).expectEmptyStack();
		infix("quote((1 2))").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
	}

	@Test
	public void testCommaWhitespaceInQuotes() {
		prefix("#(1,2)").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
		prefix("(quote (1,2))").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
		infix("quote((1,2))").expectResult(cons(i(1), cons(i(2), nil()))).expectEmptyStack();
	}

	@Test
	public void testPostfixQuotes() {
		postfix("#a").expectResult(sym("a")).expectEmptyStack();
		postfix("# a").expectResult(sym("a")).expectEmptyStack();
		postfix("#a issymbol").expectResult(b(true)).expectEmptyStack();
		postfix("#abc 'abc' symbol ==").expectResult(b(true)).expectEmptyStack();

		postfix("#+").expectResult(sym("+")).expectEmptyStack();
		postfix("# +").expectResult(sym("+")).expectEmptyStack();
	}

	@Test
	public void testPrefixModifierQuotesWithSpecialTokens() {
		prefix("#+").expectResult(sym("+")).expectEmptyStack();
		prefix("#test").expectResult(sym("test")).expectEmptyStack();
		prefix("#(max)").expectResult(cons(sym("max"), nil())).expectEmptyStack();
		prefix("#(+)").expectResult(cons(sym("+"), nil())).expectEmptyStack();
		prefix("#(1 + max)").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil())))).expectEmptyStack();
	}

	@Test
	public void testInfixModifierQuotesWithSpecialTokens() {
		infix("#+").expectResult(sym("+")).expectEmptyStack();
		infix("#test").expectResult(sym("test")).expectEmptyStack();
		infix("#(max)").expectResult(cons(sym("max"), nil())).expectEmptyStack();
		infix("#(+)").expectResult(cons(sym("+"), nil())).expectEmptyStack();
		infix("#(1 + max)").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil())))).expectEmptyStack();
	}

	@Test
	public void testPrefixSymbolQuotesWithSpecialTokens() {
		prefix("(quote +)").expectResult(sym("+")).expectEmptyStack();
		prefix("(quote test)").expectResult(sym("test")).expectEmptyStack();
		prefix("(quote (max))").expectResult(cons(sym("max"), nil())).expectEmptyStack();
		prefix("(quote (+))").expectResult(cons(sym("+"), nil())).expectEmptyStack();
		prefix("(quote (1 + max))").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil())))).expectEmptyStack();
	}

	@Test
	public void testInfixSymbolQuotesWithSpecialTokens() {
		infix("quote(+)").expectResult(sym("+")).expectEmptyStack();
		infix("quote(test)").expectResult(sym("test")).expectEmptyStack();
		infix("quote((max))").expectResult(cons(sym("max"), nil())).expectEmptyStack();
		infix("quote((+))").expectResult(cons(sym("+"), nil())).expectEmptyStack();
		infix("quote((1 + max))").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil())))).expectEmptyStack();
	}

	@Test
	public void testPrefixNestedModifierQuotes() {
		prefix("#(())").expectResult(cons(nil(), nil())).expectEmptyStack();
		prefix("#((1))").expectResult(cons(cons(i(1), nil()), nil())).expectEmptyStack();
		prefix("#((1), 2)").expectResult(cons(cons(i(1), nil()), cons(i(2), nil()))).expectEmptyStack();
		prefix("#(1 (2))").expectResult(cons(i(1), cons(cons(i(2), nil()), nil()))).expectEmptyStack();
	}

	@Test
	public void testInfixNestedModifierQuotes() {
		infix("#(())").expectResult(cons(nil(), nil())).expectEmptyStack();
		infix("#((1))").expectResult(cons(cons(i(1), nil()), nil())).expectEmptyStack();
		infix("#((1), 2)").expectResult(cons(cons(i(1), nil()), cons(i(2), nil()))).expectEmptyStack();
		infix("#(1 (2))").expectResult(cons(i(1), cons(cons(i(2), nil()), nil()))).expectEmptyStack();
	}

	@Test
	public void testPrefixDottedModifierQuotes() {
		prefix("#(3 ... ())").expectResult(cons(i(3), nil())).expectEmptyStack();
		prefix("#(3 ... 2)").expectResult(cons(i(3), i(2))).expectEmptyStack();
		prefix("#(3 ... (2 ... 1))").expectResult(cons(i(3), cons(i(2), i(1)))).expectEmptyStack();
	}

	@Test
	public void testPrefixDottedSymbolQuotes() {
		prefix("(quote (3 ... ()))").expectResult(cons(i(3), nil())).expectEmptyStack();
		prefix("(quote (3 ... 2))").expectResult(cons(i(3), i(2))).expectEmptyStack();
		prefix("(quote (3 ... (2 ... 1)))").expectResult(cons(i(3), cons(i(2), i(1)))).expectEmptyStack();
	}

	@Test
	public void testInfixDottedModifierQuotes() {
		infix("#(3 ... ())").expectResult(cons(i(3), nil())).expectEmptyStack();
		infix("#(3 ... 2)").expectResult(cons(i(3), i(2))).expectEmptyStack();
		infix("#(3 ... (2 ... 1))").expectResult(cons(i(3), cons(i(2), i(1)))).expectEmptyStack();
	}

	@Test
	public void testInfixDottedSymbolQuotes() {
		infix("quote((3 ... ()))").expectResult(cons(i(3), nil())).expectEmptyStack();
		infix("quote((3 ... 2))").expectResult(cons(i(3), i(2))).expectEmptyStack();
		infix("quote((3 ... (2 ... 1)))").expectResult(cons(i(3), cons(i(2), i(1)))).expectEmptyStack();
	}

	@Test
	public void testMixedQuotes() {
		prefix("(quote #)").expectResult(sym("#"));
		prefix("#(quote 2)").expectResult(cons(sym("quote"), cons(i(2), nil())));

		infix("quote(#)").expectResult(sym("#"));
		infix("#(quote 2)").expectResult(cons(sym("quote"), cons(i(2), nil())));
	}

	@Test
	public void testArgQuotes() {
		prefix("(str (quote test))").expectResult(s("test"));
		prefix("(len (quote (a + c)))").expectResult(i(3));

		prefix("(str #test)").expectResult(s("test"));
		prefix("(len #(a + c))").expectResult(i(3));

		infix("str(quote(test))").expectResult(s("test"));
		infix("len(quote((a + c)))").expectResult(i(3));

		infix("str(#test)").expectResult(s("test"));
		infix("len(#(a + c))").expectResult(i(3));
	}

	@Test
	public void testListFunctions() {
		infix("list()").expectResult(nil()).expectEmptyStack();
		infix("list(1,2,3)").expectResult(cons(i(1), cons(i(2), cons(i(3), nil())))).expectEmptyStack();
		infix("iscons(list(1,2,3))").expectResult(b(true)).expectEmptyStack();
		infix("list(1,2,3) == cons(1, cons(2, cons(3, null)))").expectResult(b(true)).expectEmptyStack();
		prefix("(== (list 1,2,3) #(1 2 3))").expectResult(b(true));
		infix("car(cons(1, 2))").expectResult(i(1)).expectEmptyStack();
		infix("cdr(cons(1, 2))").expectResult(i(2)).expectEmptyStack();
	}

	@Test
	public void testLengthFunction() {
		infix("len('')").expectResult(i(0)).expectEmptyStack();
		infix("len('a')").expectResult(i(1)).expectEmptyStack();
		infix("len('ab')").expectResult(i(2)).expectEmptyStack();

		infix("len(list())").expectResult(i(0)).expectEmptyStack();
		infix("len(list(1))").expectResult(i(1)).expectEmptyStack();
		infix("len(list(1,2))").expectResult(i(2)).expectEmptyStack();
	}

	@Test
	public void testSymbols() {
		prefix("(== #test # test)").expectResult(b(true)).expectEmptyStack();
		prefix("(== #a # b)").expectResult(b(false)).expectEmptyStack();
		prefix("(str #test)").expectResult(s("test")).expectEmptyStack();
		prefix("(str #+)").expectResult(s("+")).expectEmptyStack();
	}

	@Test
	public void testParserSwitch() {
		infix("2 + prefix(5)").expectResult(i(7)).expectEmptyStack();
		infix("2 + prefix((+ 5 6))").expectResult(i(13)).expectEmptyStack();

		prefix("(+ 2 (infix 5))").expectResult(i(7)).expectEmptyStack();
		prefix("(+ 2 (infix 5 + 6))").expectResult(i(13)).expectEmptyStack();
	}

	@Test
	public void testNestedParserSwitch() {
		infix("infix(5 + 2)").expectResult(i(7)).expectEmptyStack();
		infix("infix(infix(5 + 2))").expectResult(i(7)).expectEmptyStack();

		prefix("(prefix (+ 2 5))").expectResult(i(7)).expectEmptyStack();
		prefix("(prefix (prefix (+ 2 5)))").expectResult(i(7)).expectEmptyStack();

		infix("prefix((infix 2 + 5))").expectResult(i(7)).expectEmptyStack();
		prefix("(infix prefix((+ 2 5)))").expectResult(i(7)).expectEmptyStack();
	}

	@Test(expected = Exception.class)
	public void testTooManyParameters() {
		infix("abs(0, 1)").execute();
	}

	@Test(expected = Exception.class)
	public void testTooFewParameters() {
		infix("gcd(0)").execute();
	}

	@Test
	public void testConsOperator() {
		infix("1:2").expectResult(cons(i(1), i(2))).expectEmptyStack();
		infix("true:'2':3").expectResult(cons(b(true), cons(s("2"), i(3)))).expectEmptyStack();

		infix("1:2:3:null == list(1,2,3)").expectResult(b(true)).expectEmptyStack();
		prefix("(== (: 1 2 3 null) (list 1 2 3))").expectResult(b(true)).expectEmptyStack();

		infix("1:2:3:4 == #(1 2 3 ... 4)").expectResult(b(true)).expectEmptyStack();
		prefix("(== (: 1 2 3 4) #(1 2 3 ... 4))").expectResult(b(true)).expectEmptyStack();
	}

	@Test
	public void testConstantEvaluatingBrackets() {
		final SymbolStub<TypedValue> stub = new SymbolStub<TypedValue>()
				.expectArgs(i(1), i(2))
				.checkArgCount()
				.setReturns(i(5), i(6), i(7))
				.checkReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<TypedValue> expr = sut.compilers.compile(ExprType.POSTFIX, "[1 2 dummy@2,3]");
		stub.checkCallCount(1);
		compiled(expr).execute().expectStack(i(5), i(6), i(7));
		stub.checkCallCount(1);
	}
}
