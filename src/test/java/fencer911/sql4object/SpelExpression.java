package fencer911.sql4object;

import java.util.Date;

import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.sun.org.apache.xpath.internal.operations.Bool;

public class SpelExpression {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	//https://xiaobaoqiu.github.io/blog/2015/04/09/spring-spelyu-fa/
	//https://www.baeldung.com/spring-expression-language
	@Test
	public void testAnyStr(){
		ExpressionParser expressionParser = new SpelExpressionParser();
		Expression expression = expressionParser.parseExpression("'Any string 1+2'");
		String result = (String) expression.getValue();
		System.out.println(result);
	}
	@Test
	public  void testRegExpr() {
		ExpressionParser parser = new SpelExpressionParser();
	    boolean result1 = parser.parseExpression("'Predicate.java' matches 'P.*java'").getValue(boolean.class);
	    System.out.println("result1=" + result1);
	}
	public static boolean like(String str, String expr) {
	    expr = expr.toLowerCase(); // ignoring locale for now
	    expr = expr.replace(".", "\\."); // "\\" is escaped to "\" (thanks, Alan M)
	    // ... escape any other potentially problematic characters here
	    expr = expr.replace("?", ".");
	    expr = expr.replace("%", ".*");
	    str = str.toLowerCase();
	    return str.matches(expr);
	}
	@Test
	public  void testAssignValue() {
	    EvaluationContext context = new StandardEvaluationContext();
	    ExpressionParser parser = new SpelExpressionParser();
	    //1.给自定义变量赋值
	    context.setVariable("variable", "Spring SpEL");
	    String result1 = parser.parseExpression("#variable").getValue(context, String.class);
	    System.out.println("result1=" + result1);

	    //修改
	    String result2 = parser.parseExpression("#variable='ABC'").getValue(context, String.class);
	    System.out.println("result2=" + result2);
	}
	@Test
	public  void testBoolExpr(){
		ExpressionParser parser = new SpelExpressionParser();
	    boolean result1 = parser.parseExpression("1>2").getValue(boolean.class);
	    boolean result2 = parser.parseExpression("1 between {1, 2}").getValue(boolean.class);
	    System.out.println("result2=" + result2);
	}
	@Test
	public  void TType() {
		ExpressionParser parser = new SpelExpressionParser();
	    //java.lang包类访问
	    Class<String> result1 = parser.parseExpression("T(String)").getValue(Class.class);
	    System.out.println("result1=" + result1);
	    //其他包类访问
	    String expression2 = "T(com.qunar.scm.spel.SpelParser)";
	    Class<String> result2 = parser.parseExpression(expression2).getValue(Class.class);
	    System.out.println("result2=" + result2);
	    //类静态字段访问
	    int result3=parser.parseExpression("T(Integer).MAX_VALUE").getValue(int.class);
	    System.out.println("result3=" + result3);
	    //类静态方法调用
	    int result4 = parser.parseExpression("T(Integer).parseInt('1')").getValue(int.class);
	    System.out.println("result4=" + result4);
	}
	@Test
	public  void methodInvoke() {
	    ExpressionParser parser = new SpelExpressionParser();
	    // 1.访问root对象属性
	    Date date = new Date();
	    System.out.println("date=" + date);
//	    StandardEvaluationContext context = new StandardEvaluationContext(date);
//	    Boolean result2 = parser.parseExpression("'aa' matches '\\d+'").getValue(context,Boolean.class);
	    
	    StandardEvaluationContext context = new StandardEvaluationContext();
	    context.setVariable("d", date);

	    Boolean result2 = parser.parseExpression("#d.month matches '\\d+'").getValue(context,Boolean.class);
	    
	    System.out.println("result2=" + result2);
	}
}
