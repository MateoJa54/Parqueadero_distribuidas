#!/usr/bin/env python3
"""Suite QA E2E y de seguridad para el sistema de parqueadero local.

Usa solo la biblioteca estandar. Requiere que los servicios y Kong esten
levantados y que existan las cuentas qa.* creadas por seed_usuarios_roles.py.
No crea datos validos: los casos de escritura usan payloads rechazables.
"""

from __future__ import annotations

import argparse
import base64
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import asdict, dataclass
from typing import Any


@dataclass
class HttpResult:
    status: int
    headers: dict[str, str]
    body: Any
    elapsed_ms: int


@dataclass
class CaseResult:
    category: str
    name: str
    passed: bool
    expected: str
    actual: str
    detail: str = ""


class QaRunner:
    def __init__(self, gateway: str, timeout: float = 10.0) -> None:
        self.gateway = gateway.rstrip("/")
        self.timeout = timeout
        self.results: list[CaseResult] = []
        self.tokens: dict[str, str] = {}
        self.logins: dict[str, dict[str, Any]] = {}

    def request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        json_body: Any | None = None,
        raw_body: bytes | None = None,
        headers: dict[str, str] | None = None,
        base: str | None = None,
    ) -> HttpResult:
        url = (base or self.gateway).rstrip("/") + path
        request_headers = {"Accept": "application/json"}
        if token:
            request_headers["Authorization"] = f"Bearer {token}"
        if headers:
            request_headers.update(headers)
        data = raw_body
        if json_body is not None:
            data = json.dumps(json_body).encode("utf-8")
            request_headers.setdefault("Content-Type", "application/json")
        req = urllib.request.Request(url, data=data, headers=request_headers, method=method)
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as response:
                payload = response.read()
                status = response.status
                response_headers = dict(response.headers.items())
        except urllib.error.HTTPError as error:
            payload = error.read()
            status = error.code
            response_headers = dict(error.headers.items())
        except Exception as error:  # una caida/timeout es evidencia QA
            elapsed = int((time.perf_counter() - started) * 1000)
            return HttpResult(0, {}, {"transportError": str(error)}, elapsed)
        elapsed = int((time.perf_counter() - started) * 1000)
        text = payload.decode("utf-8", errors="replace")
        try:
            body: Any = json.loads(text) if text else None
        except json.JSONDecodeError:
            body = text[:500]
        return HttpResult(status, response_headers, body, elapsed)

    def check_status(
        self,
        category: str,
        name: str,
        result: HttpResult,
        expected: int | set[int],
        detail: str = "",
    ) -> bool:
        statuses = {expected} if isinstance(expected, int) else expected
        passed = result.status in statuses
        expected_text = "/".join(str(value) for value in sorted(statuses))
        suffix = detail or f"{result.elapsed_ms} ms"
        self.results.append(
            CaseResult(category, name, passed, expected_text, str(result.status), suffix)
        )
        marker = "PASS" if passed else "FAIL"
        print(f"[{marker}] [{category}] {name}: {result.status} (esperado {expected_text})")
        return passed

    def check_value(
        self,
        category: str,
        name: str,
        actual: Any,
        expected: Any,
        detail: str = "",
    ) -> bool:
        passed = actual == expected
        self.results.append(
            CaseResult(category, name, passed, repr(expected), repr(actual), detail)
        )
        marker = "PASS" if passed else "FAIL"
        print(f"[{marker}] [{category}] {name}: {actual!r} (esperado {expected!r})")
        return passed

    def login(self, alias: str, username: str, password: str) -> HttpResult:
        result = self.request(
            "POST",
            "/api/v1/auth/login",
            json_body={"username": username, "password": password},
        )
        if self.check_status("auth", f"login {alias}", result, 200):
            self.tokens[alias] = result.body["token"]
            self.logins[alias] = result.body
        return result

    @staticmethod
    def unsigned_none_token(source_token: str) -> str:
        payload = source_token.split(".")[1]
        header = base64.urlsafe_b64encode(
            json.dumps({"alg": "none", "typ": "JWT"}).encode()
        ).decode().rstrip("=")
        return f"{header}.{payload}."

    @staticmethod
    def tamper_token(source_token: str) -> str:
        parts = source_token.split(".")
        replacement = "A" if parts[1][-1] != "A" else "B"
        parts[1] = parts[1][:-1] + replacement
        return ".".join(parts)

    def run(self, rate_limit: bool) -> int:
        print("\n=== Disponibilidad y autenticacion ===")
        self.check_status(
            "availability", "frontend", self.request("GET", "/", base="http://127.0.0.1:5173"), 200
        )
        accounts = {
            "root": ("root", "Root2025"),
            "admin": ("qa.admin", "QaAdmin2025"),
            "recaudador": ("qa.recauda", "QaRecauda2025"),
            "cliente": ("qa.cliente", "QaCliente2025"),
            "invitado": ("qa.invitado", "QaInvitado2025"),
        }
        for alias, credentials in accounts.items():
            self.login(alias, *credentials)

        self.check_status(
            "auth",
            "credenciales incorrectas",
            self.request(
                "POST",
                "/api/v1/auth/login",
                json_body={"username": "root", "password": "incorrecta"},
            ),
            401,
        )
        self.check_status(
            "auth",
            "password ausente",
            self.request("POST", "/api/v1/auth/login", json_body={"username": "root"}),
            400,
        )
        self.check_status(
            "auth",
            "inyeccion SQL en username",
            self.request(
                "POST",
                "/api/v1/auth/login",
                json_body={"username": "' OR 1=1 --", "password": "x"},
            ),
            401,
        )
        self.check_status(
            "auth",
            "JSON malformado",
            self.request(
                "POST",
                "/api/v1/auth/login",
                raw_body=b'{"username":"root",',
                headers={"Content-Type": "application/json"},
            ),
            400,
        )

        if "root" not in self.tokens:
            print("No se obtuvo token root; se abortan pruebas dependientes.")
            return self.finish()

        root = self.tokens["root"]
        self.check_status("auth", "perfil con access token", self.request("GET", "/api/v1/auth/me", token=root), 200)
        self.check_status("auth", "perfil sin token", self.request("GET", "/api/v1/auth/me"), 401)
        self.check_status(
            "auth",
            "JWT manipulado",
            self.request("GET", "/api/v1/usuarios", token=self.tamper_token(root)),
            401,
        )
        self.check_status(
            "auth",
            "JWT alg none",
            self.request("GET", "/api/v1/usuarios", token=self.unsigned_none_token(root)),
            401,
        )
        refresh = self.logins["root"]["refreshToken"]
        self.check_status(
            "auth", "refresh token usado como access", self.request("GET", "/api/v1/usuarios", token=refresh), 401
        )

        print("\n=== Gateway y aislamiento por servicio ===")
        endpoints = {
            "usuarios": "/api/v1/usuarios",
            "zonas": "/api/v1/zonas",
            "espacios": "/api/v1/espacios",
            "vehiculos": "/api/vehiculos",
            "asignaciones": "/api/v1/asignaciones-vehiculos",
            "tickets": "/api/v1/tickets",
            "auditoria": "/api/v1/audit",
        }
        for service, path in endpoints.items():
            self.check_status("gateway", f"{service} autenticado", self.request("GET", path, token=root), 200)
            self.check_status("gateway", f"{service} sin token", self.request("GET", path), 401)

        direct = {
            "usuarios": ("http://localhost:8081", "/api/v1/usuarios"),
            "zonas": ("http://localhost:8080", "/api/v1/zonas"),
            "vehiculos": ("http://localhost:3000", "/api/vehiculos"),
            "asignaciones": ("http://localhost:8082", "/api/v1/asignaciones-vehiculos"),
            "tickets": ("http://localhost:8083", "/api/v1/tickets"),
            "auditoria": ("http://localhost:3002", "/api/v1/audit"),
        }
        for service, (base, path) in direct.items():
            self.check_status("direct-security", f"{service} directo sin token", self.request("GET", path, base=base), 401)
            self.check_status("direct-security", f"{service} directo con token", self.request("GET", path, base=base, token=root), 200)

        print("\n=== Autorizacion por roles ===")
        admin = self.tokens.get("admin", "")
        recauda = self.tokens.get("recaudador", "")
        cliente = self.tokens.get("cliente", "")
        invitado = self.tokens.get("invitado", "")
        self.check_status("rbac", "ADMIN lista usuarios", self.request("GET", "/api/v1/usuarios", token=admin), 200)
        self.check_status("rbac", "CLIENTE no lista usuarios", self.request("GET", "/api/v1/usuarios", token=cliente), 403)
        self.check_status("rbac", "INVITADO no lista usuarios", self.request("GET", "/api/v1/usuarios", token=invitado), 403)
        self.check_status("rbac", "CLIENTE consulta zonas", self.request("GET", "/api/v1/zonas", token=cliente), 200)
        self.check_status("rbac", "CLIENTE consulta vehiculos", self.request("GET", "/api/vehiculos", token=cliente), 200)
        self.check_status("rbac", "CLIENTE no administra asignaciones", self.request("GET", "/api/v1/asignaciones-vehiculos", token=cliente), 403)
        self.check_status("rbac", "CLIENTE no opera tickets", self.request("GET", "/api/v1/tickets", token=cliente), 403)
        self.check_status("rbac", "RECAUDADOR opera tickets", self.request("GET", "/api/v1/tickets", token=recauda), 200)
        self.check_status("rbac", "CLIENTE no lee auditoria", self.request("GET", "/api/v1/audit", token=cliente), 403)
        self.check_status(
            "rbac",
            "CLIENTE no falsifica auditoria",
            self.request(
                "POST",
                "/api/v1/audit",
                token=cliente,
                json_body={
                    "servicio": "ms-usuarios",
                    "accion": "DELETE",
                    "entidad": "USUARIO",
                    "ip": "127.0.0.1",
                    "mac": "AA:BB:CC:DD:EE:FF",
                },
            ),
            403,
        )
        if cliente and self.logins.get("cliente"):
            own_id = self.logins["cliente"]["idUsuario"]
            other_id = self.logins["root"]["idUsuario"]
            self.check_status(
                "rbac", "CLIENTE consulta flota propia", self.request("GET", f"/api/v1/propietarios/{own_id}/vehiculos", token=cliente), 200
            )
            self.check_status(
                "rbac", "CLIENTE no consulta flota ajena", self.request("GET", f"/api/v1/propietarios/{other_id}/vehiculos", token=cliente), 403
            )

        print("\n=== Validacion negativa y entradas hostiles ===")
        self.check_status(
            "validation",
            "CLIENTE no crea zona",
            self.request(
                "POST", "/api/v1/zonas", token=cliente,
                json_body={"nombre": "QA NO CREAR", "descripcion": "x", "tipo": "REGULAR", "capacidad": 1},
            ),
            403,
        )
        self.check_status(
            "validation",
            "zona con capacidad cero",
            self.request(
                "POST", "/api/v1/zonas", token=admin,
                json_body={"nombre": "QA INVALIDA", "descripcion": "x", "tipo": "REGULAR", "capacidad": 0},
            ),
            400,
        )
        invalid_auto = {
            "tipo": "Auto",
            "datos": {
                "placa": "../../etc/passwd", "marca": "Toyota", "modelo": "RAV4",
                "color": "Negro", "anio": 2024, "clasificacion": "Gasolina",
                "numeroPuertas": 4, "capacidadMaletero": 500,
            },
        }
        self.check_status("validation", "placa hostil", self.request("POST", "/api/vehiculos", token=admin, json_body=invalid_auto), 400)
        extra_field_auto = json.loads(json.dumps(invalid_auto))
        extra_field_auto["datos"]["placa"] = "QAA-9001"
        extra_field_auto["esAdmin"] = True
        self.check_status("validation", "mass assignment vehiculo", self.request("POST", "/api/vehiculos", token=admin, json_body=extra_field_auto), 400)
        self.check_status("validation", "asignacion vacia", self.request("POST", "/api/v1/asignaciones-vehiculos", token=admin, json_body={}), 400)
        self.check_status("validation", "ticket vacio", self.request("POST", "/api/v1/tickets", token=recauda, json_body={}), 400)
        self.check_status(
            "validation",
            "auditoria con IP y MAC invalidas",
            self.request(
                "POST", "/api/v1/audit", token=admin,
                json_body={"servicio": "usuarios", "accion": "UPSERT", "entidad": "usuario", "ip": "::1", "mac": "x"},
            ),
            400,
        )
        self.check_status(
            "validation", "UUID invalido", self.request("GET", "/api/v1/usuarios/no-es-uuid", token=root), 400
        )
        missing_id = str(uuid.uuid4())
        self.check_status(
            "validation", "UUID inexistente", self.request("GET", f"/api/v1/usuarios/{missing_id}", token=root), 404
        )
        self.check_status(
            "validation",
            "payload mayor a 8 MB",
            self.request(
                "POST", "/api/v1/auth/login",
                raw_body=b'{"username":"' + (b"A" * (8 * 1024 * 1024 + 1024)) + b'","password":"x"}',
                headers={"Content-Type": "application/json"},
            ),
            413,
        )
        self.check_status("validation", "metodo TRACE", self.request("TRACE", "/api/v1/usuarios", token=root), {404, 405})

        print("\n=== CORS y cabeceras defensivas ===")
        protected = self.request("GET", "/api/v1/zonas", token=root)
        lower_headers = {key.lower(): value for key, value in protected.headers.items()}
        self.check_value("headers", "X-Content-Type-Options", lower_headers.get("x-content-type-options"), "nosniff")
        self.check_value("headers", "X-Frame-Options", lower_headers.get("x-frame-options"), "DENY")
        self.check_value("headers", "Correlation ID presente", bool(lower_headers.get("x-correlation-id")), True)
        allowed_cors = self.request(
            "OPTIONS", "/api/v1/zonas",
            headers={
                "Origin": "http://localhost:5173",
                "Access-Control-Request-Method": "GET",
                "Access-Control-Request-Headers": "Authorization",
            },
        )
        allowed_headers = {key.lower(): value for key, value in allowed_cors.headers.items()}
        self.check_status("cors", "preflight origen permitido", allowed_cors, {200, 204})
        self.check_value("cors", "ACAO origen permitido", allowed_headers.get("access-control-allow-origin"), "http://localhost:5173")
        evil_cors = self.request(
            "OPTIONS", "/api/v1/zonas",
            headers={"Origin": "https://evil.example", "Access-Control-Request-Method": "GET"},
        )
        evil_headers = {key.lower(): value for key, value in evil_cors.headers.items()}
        self.check_value("cors", "origen no permitido sin ACAO", "access-control-allow-origin" in evil_headers, False)

        print("\n=== Supervivencia despues del fuzzing ===")
        for service, path in endpoints.items():
            self.check_status("resilience", f"{service} sigue respondiendo", self.request("GET", path, token=root), 200)

        if rate_limit:
            print("\n=== Rate limiting (se ejecuta al final) ===")
            limited = False
            attempts = 0
            for attempts in range(1, 121):
                response = self.request("GET", "/api/v1/zonas", token=root)
                if response.status == 429:
                    limited = True
                    break
                if response.status not in {200, 429}:
                    break
            self.check_value("gateway", "rate limit produce 429", limited, True, f"intentos={attempts}")

        return self.finish()

    def finish(self) -> int:
        passed = sum(result.passed for result in self.results)
        failed = len(self.results) - passed
        by_category: dict[str, dict[str, int]] = {}
        for result in self.results:
            bucket = by_category.setdefault(result.category, {"passed": 0, "failed": 0})
            bucket["passed" if result.passed else "failed"] += 1
        report = {
            "summary": {"total": len(self.results), "passed": passed, "failed": failed},
            "categories": by_category,
            "results": [asdict(result) for result in self.results],
        }
        print("\n=== RESUMEN ===")
        print(json.dumps(report["summary"], indent=2))
        with open("logs/qa-security-report.json", "w", encoding="utf-8") as output:
            json.dump(report, output, ensure_ascii=False, indent=2)
        print("Reporte: logs/qa-security-report.json")
        return 0 if failed == 0 else 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gateway", default="http://localhost:8000")
    parser.add_argument("--timeout", type=float, default=10.0)
    parser.add_argument("--rate-limit", action="store_true")
    args = parser.parse_args()
    return QaRunner(args.gateway, args.timeout).run(args.rate_limit)


if __name__ == "__main__":
    sys.exit(main())
