<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Jsp Test</title>
</head>
<body>
Java:
<% out.println(3*100 + 2*10 + 1 );%>
<br/>
Date:
<%
out.println(new java.util.Date());
%>
</body>
</html>