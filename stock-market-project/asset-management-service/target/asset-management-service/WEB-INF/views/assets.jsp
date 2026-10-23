<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Assets</title>
</head>
<body>
    <h1>Assets</h1>
    <ul>
        <c:forEach var="asset" items="${assets}">
            <li>${asset.symbol} - ${asset.name} - quantity: ${asset.quantity}</li>
        </c:forEach>
    </ul>
</body>
</html>
