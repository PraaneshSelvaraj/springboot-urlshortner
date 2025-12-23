<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
    <title>URL Shortener</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: Arial, sans-serif;
            background: #f5f5f5;
            padding: 20px;
        }

        .container {
            max-width: 800px;
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

        .main-box {
            background: white;
            padding: 30px;
            border: 1px solid #ddd;
        }

        .main-box h2 {
            margin-bottom: 20px;
            color: #333;
        }

        .form-group {
            margin-bottom: 15px;
        }

        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #333;
        }

        .form-group input {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            font-size: 14px;
        }

        .form-group input:focus {
            outline: none;
            border-color: #0066cc;
        }

        .btn {
            background: #0066cc;
            color: white;
            border: none;
            padding: 12px 24px;
            cursor: pointer;
            font-size: 14px;
        }

        .btn:hover {
            background: #0052a3;
        }

        .btn:disabled {
            background: #ccc;
            cursor: not-allowed;
        }

        .error {
            background: #ffebee;
            color: #c62828;
            padding: 10px;
            margin-top: 15px;
            border: 1px solid #c62828;
        }

        .result {
            display: none;
            margin-top: 30px;
            padding: 20px;
            background: #f9f9f9;
            border: 1px solid #ddd;
        }

        .result.show {
            display: block;
        }

        .result h3 {
            margin-bottom: 15px;
            color: #333;
        }

        .result-item {
            margin-bottom: 15px;
        }

        .result-item label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
            color: #666;
            font-size: 12px;
        }

        .result-item .value {
            padding: 10px;
            background: white;
            border: 1px solid #ddd;
            word-break: break-all;
        }

        .result-item a {
            color: #0066cc;
            text-decoration: none;
        }

        .result-item a:hover {
            text-decoration: underline;
        }

        .copy-btn {
            background: #28a745;
            color: white;
            border: none;
            padding: 8px 16px;
            cursor: pointer;
            margin-top: 10px;
            font-size: 13px;
        }

        .copy-btn:hover {
            background: #218838;
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

        <div class="main-box">
            <h2>Shorten Your Link</h2>

            <form id="shortenForm">
                <div class="form-group">
                    <label for="longUrl">URL</label>
                    <input
                        type="url"
                        id="longUrl"
                        name="longUrl"
                        placeholder="https://example.com/very/long/url"
                        required
                    >
                </div>
                <button type="submit" id="submitBtn" class="btn">Shorten</button>
            </form>

            <div id="errorMessage"></div>

            <div id="result" class="result">
                <h3>Success</h3>

                <div class="result-item">
                    <label>Short URL</label>
                    <div class="value">
                        <a id="shortUrl" href="" target="_blank"></a>
                    </div>
                    <button class="copy-btn" onclick="copyToClipboard(event)">Copy</button>
                </div>

                <div class="result-item">
                    <label>Original URL</label>
                    <div class="value" id="originalUrl"></div>
                </div>

                <div class="result-item">
                    <label>Short Code</label>
                    <div class="value" id="shortCode"></div>
                </div>
            </div>
        </div>
    </div>

    <script>
        document.getElementById('shortenForm').addEventListener('submit', async function(e) {
            e.preventDefault();

            const longUrl = document.getElementById('longUrl').value;
            const submitBtn = document.getElementById('submitBtn');
            const errorDiv = document.getElementById('errorMessage');
            const resultDiv = document.getElementById('result');

            errorDiv.innerHTML = '';
            resultDiv.classList.remove('show');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Shortening...';

            try {
                const response = await fetch('/api/urls', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ url: longUrl })
                });

                if (!response.ok) {
                    const error = await response.text();
                    throw new Error(error || 'Failed to shorten URL');
                }

                const data = await response.json();

                document.getElementById('shortUrl').textContent = data.shortUrl;
                document.getElementById('shortUrl').href = data.shortUrl;
                document.getElementById('originalUrl').textContent = data.longUrl;
                document.getElementById('shortCode').textContent = data.shortCode;

                resultDiv.classList.add('show');
                document.getElementById('longUrl').value = '';

            } catch (error) {
                errorDiv.innerHTML = '<div class="error">Error: ' + error.message + '</div>';
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Shorten';
            }
        });

        function copyToClipboard(event) {
            const shortUrl = document.getElementById('shortUrl').textContent;
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
    </script>

</body>
</html>
