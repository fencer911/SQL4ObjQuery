package fencer911.sql4object;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.util.JdbcConstants;
/**
 * SQL4ObjQuery=SQL Query For Objects
 * @author fencer911
 * 
 * This class provides the ability for a developer to apply an simple SQL statement
 * (using suitable syntax) to a collection of Java objects.
 * <p>
 * Basic usage:
 * <pre>
 *   List<T> collection;
 *   Stream<T> stream=new SqlObjQuery().execute(SQL,collection);
 * </pre>
 * <p>
 * An example statement would look like:
 * <pre>
 *   SELECT lastModified,
 *          name
 *   FROM   java.io.File
 *   WHERE  name LIKE '%.html'
 * </pre>
 * <p>
 * The SQL4ObjQuery functionality is small and simple, whilst basic queries like the one above are
 * perfectly possible, but complex queries are not support, for example:group,having,in.
 * Maybe support in the future.
 * @param <T>
 */
public class SQL4ObjQuery<T> {
	//Predicate operator
	private static String OP_like="like";
	private static String OP_or="or";
	private static String OP_and="and";
	
	private static String ASC="asc";
	private static String DESC="desc";
	
	private  static String processLikeStr(String likeExpr) {
		return likeExpr.replace(".", "\\.").replace("?", ".").replace("%", ".*");
	}
	//the objects of the parse SQL generated 
	private SQLSelectQueryBlock sqlSelectQueryBlock;
	private SQLExprTableSource table;
	private SQLExpr where;
	private Stream<T> stream;
	
	private  ExpressionParser parser;
	/**
	 * execute query
	 * @param sql  format:select * from T t where t.xx=XXX
	 * @param dataList data which query on
	 * @return T's Stream
	 * @throws Throwable sometime not support SQL eg:parse error,join clause,group clause will ignore
	 */
	public   Stream<T> execute(String sql,Collection<T>  dataList) throws Throwable {
		System.out.println("query Collection size="+dataList.size());
        // parse
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        // only support one select statement
        SQLStatement statement = statements.get(0);
        SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) statement;
        SQLSelectQuery sqlSelectQuery     = sqlSelectStatement.getSelect().getQuery();
        // not union select statement
        if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
            this.sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectQuery;
            //get table info 
            SQLTableSource from = sqlSelectQueryBlock.getFrom();
            if (!(from instanceof SQLExprTableSource)) {
            	throw new Throwable("Sql not support! only one table");
            }
            this.parser = new SpelExpressionParser();
            this.table=(SQLExprTableSource)from;
            this.where = sqlSelectQueryBlock.getWhere();
            this.stream=dataList.stream();
            
        	// See if we have any expressions that are to be executed on   the Stream<T>.
            this.evalWhereClause();
        	// Now perform the order by.
            this.evalOrderByClause (sqlSelectQueryBlock.getOrderBy());
        	// Finally, if we have a limit clause, restrict the size of Stream<T> returned...
            this.evalLimitClause ();
        } 
        return this.stream;
    }
	private void evalLimitClause() {
        SQLLimit limit = sqlSelectQueryBlock.getLimit();
        if(limit==null){
        	return;
        }
        SQLIntegerExpr offset=(SQLIntegerExpr)limit.getOffset();
        SQLIntegerExpr rowCount=(SQLIntegerExpr)limit.getRowCount();
        this.stream=this.stream.skip(offset.getNumber().longValue()).limit(rowCount.getNumber().longValue());		
	}
	private void evalOrderByClause(SQLOrderBy orderBy) {
        SQLExpr    exprOfOrder=orderBy.getItems().get(0).getExpr();
        StringBuilder  owner=new StringBuilder("");
        if(exprOfOrder  instanceof  SQLIdentifierExpr ){
        	owner.append(""+this.table.getName().getSimpleName());
        }else if(exprOfOrder  instanceof SQLPropertyExpr ){
        	owner.append(this.table.getAlias());
        }
        SQLSelectOrderByItem orderitem=(SQLSelectOrderByItem)exprOfOrder.getParent();
        SQLOrderingSpecification orderTypeSpeci=orderitem.getType();
        String orderType=ASC;
        if(orderTypeSpeci!=null&&!orderTypeSpeci.name_lcase.isEmpty()){
        	orderType=orderTypeSpeci.name_lcase;
        }
        StringBuilder orderExpr=new StringBuilder("#"+owner.toString()+"."+exprOfOrder.toString());
        Function<T,String> funcOfSort=(T x)->processExpr(owner.toString(),x,orderExpr.toString(),String.class);
        Comparator<T> comparator=null;
        if(DESC.equals(orderType)){
        	comparator=Comparator.comparing(funcOfSort,Comparator.reverseOrder());
        }else{
        	comparator=Comparator.comparing(funcOfSort);
        }
        //if support more order item, use comparator.thenComparing(Class::propertie2))
        stream=stream.sorted(comparator);   	
		
	}
	/**
	 *  Expression convert to predicate filter
	 */
	public void evalWhereClause(){
        if (this.where instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr   sqlBinaryOpExpr = (SQLBinaryOpExpr) where;
            //build filter
            Predicate<T> predicate=processExpression(sqlBinaryOpExpr);
            stream=stream.filter(predicate);              
        } 
	}
	private Predicate<T> processExpression(SQLBinaryOpExpr binaryExpr){
        SQLExpr           left            = binaryExpr.getLeft();
        SQLBinaryOperator operator        = binaryExpr.getOperator();
        SQLExpr           right           = binaryExpr.getRight();
        
        StringBuilder  owner=new StringBuilder("");
        if(left  instanceof  SQLIdentifierExpr||left  instanceof  SQLMethodInvokeExpr ){
        	owner.append(""+table.getName().getSimpleName());
        }else if(left  instanceof  SQLPropertyExpr ){
        	owner.append(table.getAlias());
        }
        StringBuilder spel=new StringBuilder("#").append(owner).append("."+left.toString());
        if(operator.name_lcase.equals(OP_and)){
        	if(left  instanceof  SQLBinaryOpExpr){
        		return processExpression((SQLBinaryOpExpr)left).and(processExpression((SQLBinaryOpExpr)right));
        	}
        }else  if(operator.name_lcase.equals(OP_or)){
        	if(left  instanceof  SQLBinaryOpExpr){
        		return processExpression((SQLBinaryOpExpr)left).or(processExpression((SQLBinaryOpExpr)right));
        	}
        }else {
        	if(OP_like.equals(operator.name_lcase)){
        		spel.append(" matches ").append(processLikeStr(right.toString()));
	        }else{
	        	spel.append(" ").append(operator.name_lcase).append(" ").append(right.toString());
	        }
            Predicate<T> predicate=(T t)->processExpr(owner.toString(),t,spel.toString(),Boolean.class);
            return predicate;
        }
		return null;
	}

	private <T> T processExpr(String owner,Object obj,String expr,Class<T> expectedResultType){
	    StandardEvaluationContext context = new StandardEvaluationContext();
	    context.setVariable(owner, obj);
	    T result = parser.parseExpression(expr).getValue(context,expectedResultType);
	    System.out.println("expr={"+expr+"},result="+result);
	    return result;
	}
	public static void main(String[] args) throws Throwable {
		// Get a list of java.io.File objects.
		File dir =new File("/Users/xxx/Desktop/db/sql4object/src/main/java/fencer911/sql4object");
		List<File> files = Arrays.asList(dir.listFiles());
		String sql="SELECT * FROM File f WHERE f.name like '%p%.java' order by f.name ";
		sql="SELECT * FROM java.io.File  WHERE name like '%p%.java' and length()>1241 order by name desc  ";
		Stream<File> stream=new SQL4ObjQuery().execute(sql,files);
		
        System.out.println("stream.forEach");
        List list=(List)stream.collect(Collectors.toList());
        list.forEach(t->{
		    File f = (File) t;
		    System.out.println(f.getName()+","+f.length());
        });
	}
	
}
