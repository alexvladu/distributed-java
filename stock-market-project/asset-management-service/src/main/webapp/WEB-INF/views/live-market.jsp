<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="refresh" content="5">
    <title>Live Market</title>
    <style>
        body { font-family: sans-serif; margin: 2rem; }
        table { border-collapse: collapse; min-width: 42rem; }
        th, td { border: 1px solid #ccc; padding: .6rem .8rem; text-align: left; }
        th { background: #f2f2f2; }
        .empty { color: #666; }
    </style>
</head>
<body>
    <h1>Live Market</h1>
    <p>Prices are received from Kafka and refreshed automatically.</p>
    <table>
        <thead>
            <tr>
                <th>Symbol</th>
                <th>Company</th>
                <th>Price</th>
                <th>Quantity</th>
                <th>Last update</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="stock" items="${stocks}">
                <tr>
                    <td>${stock.symbol}</td>
                    <td>${stock.name}</td>
                    <td>${stock.price}</td>
                    <td>${stock.quantity}</td>
                    <td>${consumer.lastUpdated(stock.symbol)}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty stocks}">
                <tr><td class="empty" colspan="5">Waiting for stock updates from Kafka...</td></tr>
            </c:if>
        </tbody>
    </table>
    <p><a href="${pageContext.request.contextPath}/assets">View assets</a></p>
</body>
</html>
