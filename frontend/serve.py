#!/usr/bin/env python3
"""前端开发服务器。静态文件从当前目录提供，/api/* 代理到后端 8080。"""
import http.server
import os
import socketserver
import sys
import urllib.request
import urllib.error

os.chdir(os.path.dirname(os.path.abspath(__file__)))

DEFAULT_PORT = 3000
BACKEND = "http://localhost:8080"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_PORT


class ProxyHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith("/api/"):
            self._proxy("GET")
        else:
            super().do_GET()

    def do_POST(self):
        if self.path.startswith("/api/"):
            self._proxy("POST")
        else:
            super().do_POST()

    def _proxy(self, method):
        url = BACKEND + self.path
        body = None
        if method == "POST":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length) if length else None

        req = urllib.request.Request(url, data=body, method=method)
        for k, v in self.headers.items():
            if k.lower() in ("host", "content-length"):
                continue
            req.add_header(k, v)

        try:
            with urllib.request.urlopen(req) as resp:
                content_type = resp.headers.get("Content-Type", "text/plain")
                self.send_response(resp.status)
                self.send_header("Content-Type", content_type)
                self.end_headers()
                if "text/event-stream" in content_type:
                    # SSE 流式转发，逐块写入，不缓冲
                    while chunk := resp.read1(4096):
                        self.wfile.write(chunk)
                        self.wfile.flush()
                else:
                    self.wfile.write(resp.read())
        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            self.end_headers()
            self.wfile.write(e.read())
        except urllib.error.URLError as e:
            self.send_response(502)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"Backend unreachable")


with socketserver.TCPServer(("", PORT), ProxyHandler) as httpd:
    print(f"前端: http://localhost:{PORT}  |  后端代理: {BACKEND}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止")
