# SQL4ObjQuery

SQL4ObjQuery = SQL Query For Java Objects.

It provides the ability for a developer to apply an simple SQL statement to a collection of Java objects.


## Inspiration

This tool is mainly inspired by josql and SimpleDB. I reinvent the wheel for the following two reasons:

    1. Learn to implement the relational database
    2. Try Java8 Stream API

## Features

* SQL Query For Java Objects.
* support method invoke

## Quick Start

```java
public static void main(String[] args) throws Throwable {
	// Get a list of java.io.File objects.
	File dir =new File("/Users/xx/Desktop/db/sql4object/src/main/java/fencer911/sql4object");
	List<File> files = Arrays.asList(dir.listFiles());
	String sql="SELECT * FROM File f WHERE f.name like '%p%.java' order by f.name ";
	sql="SELECT * FROM java.io.File  WHERE name like '%p%.java' and length()>1241 order by name desc  ";
	Stream<File> stream=new SQL4ObjQuery<File>().execute(sql,files);
	
    System.out.println("stream.forEach");
    List<File> list=stream.collect(Collectors.toList());
    list.forEach(f->{
	    System.out.println(f.getName()+","+f.length());
    });
}
```