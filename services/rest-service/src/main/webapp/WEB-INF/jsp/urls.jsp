<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>URL Shortener - All URLs</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: Arial, sans-serif;
            background: #f5f5f5;
            padding: 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        .header {
            background: white;
            padding: 20px;
            margin-bottom: 20px;
            border: 1px solid #ddd;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .header h1 {
            font-size: 24px;
            color: #333;
        }

        .nav a {
            color: #0066cc;
            text-decoration: none;
            margin-left: 20px;
        }

        .nav a:hover {
            text-decoration: underline;
        }

        .message {
            padding: 10px;
            margin-bottom: 20px;
            border: 1px solid;
        }

        .message.error {
            background: #ffebee;
            color: #c62828;
            border-color: #c62828;
        }

        .message.success {
            background: #e8f5e9;
            color: #2e7d32;
            border-color: #2e7d32;
        }

        .table-container {
            background: white;
            border: 1px solid #ddd;
        }

        .table-header {
            padding: 20px;
            border-bottom: 1px solid #ddd;
        }

        .table-header h2 {
            color: #333;
            font-size: 18px;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #666;
        }

        .empty-state h2 {
            margin-bottom: 10px;
        }

        .empty-state a {
            color: #0066cc;
            text-decoration: none;
        }

        .empty-state a:hover {
            text-decoration: underline;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        thead {
            background: #f9f9f9;
        }

        th {
            text-align: left;
            padding: 12px 15px;
            font-weight: bold;
            color: #333;
            border-bottom: 1px solid #ddd;
        }

        th.sortable {
            cursor: pointer;
            user-select: none;
        }

        th.sortable:hover {
            background: #f0f0f0;
        }

        .sort-indicator {
            display: inline-flex;
            flex-direction: column;
            margin-left: 5px;
            font-size: 8px;
            line-height: 6px;
            vertical-align: middle;
        }

        .sort-indicator.active {
            color: #0066cc;
            font-size: 10px;
        }

        .sort-indicator.inactive {
            color: #999;
        }

        td {
            padding: 12px 15px;
            border-bottom: 1px solid #f0f0f0;
        }

        tbody tr:hover {
            background: #f9f9f9;
        }

        .url-cell {
            max-width: 300px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .url-cell a {
            color: #333;
            text-decoration: none;
        }

        .url-cell a:hover {
            color: #0066cc;
            text-decoration: underline;
        }

        .short-url {
            color: #0066cc;
            text-decoration: none;
        }

        .short-url:hover {
            text-decoration: underline;
        }

        .badge {
            display: inline-block;
            padding: 3px 8px;
            font-size: 11px;
            font-weight: bold;
        }

        .badge-success {
            background: #e8f5e9;
            color: #2e7d32;
        }

        .badge-danger {
            background: #ffebee;
            color: #c62828;
        }

        .action-buttons {
            display: flex;
            gap: 5px;
        }

        .btn-small {
            padding: 5px 10px;
            font-size: 12px;
            border: none;
            cursor: pointer;
        }

        .btn-success {
            background: #28a745;
            color: white;
        }

        .btn-success:hover {
            background: #218838;
        }

        .btn-danger {
            background: #dc3545;
            color: white;
        }

        .btn-danger:hover {
            background: #c82333;
        }

        .pagination {
            display: flex;
            justify-content: center;
            gap: 5px;
            padding: 20px;
            border-top: 1px solid #ddd;
        }

        .pagination button {
            padding: 8px 12px;
            border: 1px solid #ddd;
            background: white;
            cursor: pointer;
            font-size: 13px;
        }

        .pagination button:hover:not(:disabled) {
            background: #0066cc;
            color: white;
            border-color: #0066cc;
        }

        .pagination button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .pagination button.active {
            background: #0066cc;
            color: white;
            border-color: #0066cc;
        }

        .pagination span {
            padding: 8px;
            color: #999;
        }
    </style>
</head>
<body>

    <div class="container">
        <div class="header">
            <h1>URL Shortener</h1>
            <nav class="nav">
                <a href="/">Home</a>
                <a href="/urls">All URLs</a>
            </nav>
        </div>

        <div id="messageContainer"></div>

        <div class="table-container">
            <div class="table-header">
                <h2>All Shortened URLs</h2>
            </div>

            <div id="emptyState" class="empty-state" style="display: none;">
                <h2>No URLs Found</h2>
                <p>Start by creating your first short URL</p>
                <a href="/">Go to Home</a>
            </div>
            <table id="urlsTable">
                <thead>
                    <tr>
                        <th class="sortable" onclick="sortTable('shortCode')">
                            Short Code
                            <c:choose>
                                <c:when test="${sortBy == 'shortCode'}">
                                    <span class="sort-indicator active">${sortDirection == 'asc' ? '▲' : '▼'}</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="sort-indicator inactive">
                                        <span>▲</span>
                                        <span>▼</span>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </th>
                        <th>Original URL</th>
                        <th>Short URL</th>
                        <th class="sortable" onclick="sortTable('clicks')">
                            Clicks
                            <c:choose>
                                <c:when test="${sortBy == 'clicks'}">
                                    <span class="sort-indicator active">${sortDirection == 'asc' ? '▲' : '▼'}</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="sort-indicator inactive">
                                        <span>▲</span>
                                        <span>▼</span>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </th>
                        <th>Status</th>
                        <th class="sortable" onclick="sortTable('expiresAt')">
                            ExpiresAt
                            <c:choose>
                                <c:when test="${sortBy == 'expiresAt'}">
                                    <span class="sort-indicator active">${sortDirection == 'asc' ? '▲' : '▼'}</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="sort-indicator inactive">
                                        <span>▲</span>
                                        <span>▼</span>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="urlsTableBody">
                    <c:forEach items="${urls}" var="url">
                    <tr>
                        <td><strong>${url.shortCode}</strong></td>
                        <td class="url-cell"><a href="${url.longUrl}" target="_blank">${url.longUrl}</a></td>
                        <td class="url-cell"><a href="/${url.shortCode}" target="_blank">${url.shortUrl}</a></td>
                        <td><strong>${url.clicks}</strong></td>
                        <td>
                            <c:choose>
                                <c:when test="${url.expired}">
                                    <span class="badge badge-danger">Expired</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-success">Active</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <fmt:parseDate value="${url.expiresAt}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedDate" type="both" />
                            <fmt:formatDate value="${parsedDate}" pattern="MMM dd, yyyy HH:mm" />
                        </td>
                        <td>
                            <div class="action-buttons">
                                <button class="btn-small btn-success" onclick="copyToClipboard(event, '${url.shortUrl}')">Copy</button>
                                <button class="btn-small btn-danger" onclick="deleteUrl(event, '${url.shortCode}')">Delete</button>
                            </div>
                        </td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="pagination" id="pagination">
                <c:if test="${totalPages > 1}">
                    <c:choose>
                        <c:when test="${currentPage > 0}">
                            <a href="/urls?page=${currentPage - 1}&size=${pageSize}&sortBy=${sortBy}&sortDirection=${sortDirection}">
                                <button>Previous</button>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <button disabled>Previous</button>
                        </c:otherwise>
                    </c:choose>

                    <c:forEach begin="0" end="${totalPages - 1}" var="i">
                        <c:choose>
                            <c:when test="${i == currentPage}">
                                <button class="active">${i + 1}</button>
                            </c:when>
                            <c:otherwise>
                                <a href="/urls?page=${i}&size=${pageSize}&sortBy=${sortBy}&sortDirection=${sortDirection}">
                                    <button>${i + 1}</button>
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>

                    <c:choose>
                        <c:when test="${currentPage < totalPages - 1}">
                            <a href="/urls?page=${currentPage + 1}&size=${pageSize}&sortBy=${sortBy}&sortDirection=${sortDirection}">
                                <button>Next</button>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <button disabled>Next</button>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </div>
        </div>
    </div>

    <script>
    function sortTable(column) {
        const currentSortBy = '${sortBy}';
        const currentSortDirection = '${sortDirection}';
        const currentPage = ${currentPage};
        const pageSize = ${pageSize};

        let url = '/urls?page=' + currentPage + '&size=' + pageSize;

        if (column === currentSortBy) {
            if (currentSortDirection === 'asc') {
                url += '&sortBy=' + column + '&sortDirection=desc';
            }
        } else {
            url += '&sortBy=' + column + '&sortDirection=asc';
        }

        window.location.href = url;
    }

    function copyToClipboard(event, shortUrl) {
        navigator.clipboard.writeText(shortUrl).then(function() {
            const btn = event.target;
            const originalText = btn.textContent;
            btn.textContent = 'Copied!';
            setTimeout(function() {
                btn.textContent = originalText;
            }, 2000);
        }).catch(function(err) {
            alert('Failed to copy: ' + err.message);
        });
    }

    function deleteUrl(event, shortCode) {
        if (!confirm('Do you want to delete the URL with short code: ' + shortCode + '?')) {
            return;
        }

        const btn = event.target;
        btn.disabled = true;
        btn.textContent = 'Deleting...';

        fetch('/api/urls/' + shortCode, {
            method: 'DELETE'
        })
        .then(function(response) {
            if (response.ok) {
                window.location.reload();
            } else {
                alert('Failed to delete URL');
                btn.disabled = false;
                btn.textContent = 'Delete';
            }
        })
        .catch(function(err) {
            alert('Error: ' + err.message);
            btn.disabled = false;
            btn.textContent = 'Delete';
        });
    }
    </script>

</body>
</html>
