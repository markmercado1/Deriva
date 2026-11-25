# Deriva 0.1.0 — carpeta de entrega

Autor: Emerson Mercado · Repositorio: <https://github.com/markmercado1/Deriva>

Deriva es una aplicación de escritorio para Windows (Java 21 + JavaFX + SQLite) que registra qué aplicación o sitio está en primer plano, detecta episodios de cambios rápidos y permite fijar objetivos diarios. Todos los datos quedan en el equipo del usuario.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `Deriva-0.1.0-Setup.exe` | Instalador con asistente «Siguiente > Siguiente» (6 pasos), sin permisos de administrador. |
| `Deriva-0.1.0-portable.zip` | Versión portable: extraer y abrir `Deriva.exe`. Incluye la carpeta `extension`. |
| `Deriva-extension-chromium.zip` | Extensión para Chrome/Brave (también viene dentro de la portable y del instalador). |
| `Deriva - codigo fuente.docx` / `.pdf` | Código fuente completo, en blanco y negro. |
| `Deriva - para ejecutar.docx` / `.pdf` | Cómo acceder al repositorio, descargarlo, instalarlo y abrir la app, con capturas. |
| `Deriva - manual de uso.docx` / `.pdf` | Manual de uso detallado con capturas anotadas. |

## Descarga directa

- Instalador: <https://github.com/markmercado1/Deriva/raw/main/entrega/Deriva-0.1.0-Setup.exe>
- Versión portable: <https://github.com/markmercado1/Deriva/raw/main/entrega/Deriva-0.1.0-portable.zip>
- Extensión: <https://github.com/markmercado1/Deriva/raw/main/entrega/Deriva-extension-chromium.zip>

## Uso rápido

1. Instalar: abrir `Deriva-0.1.0-Setup.exe` y pulsar «Siguiente >» hasta «Finalizar». Si Windows muestra SmartScreen, «Más información» › «Ejecutar de todos modos» (el programa no está firmado).
2. O bien portable: extraer el `.zip` y abrir `Deriva.exe`.
3. En la app, pulsar «Iniciar captura».
4. Para registrar sitios web: Ajustes › Extensión del navegador (guía en el manual, apartado 5.5.4).

Los registros se guardan en `%LOCALAPPDATA%\Deriva`; desinstalar no los borra.

## Reconstruir

```powershell
mvn "-Dmaven.repo.local=$PWD\work\m2" --batch-mode --no-transfer-progress clean verify
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Build-Package.ps1
```

El paquete queda en `target\dist`. Pruebas y límites conocidos: `docs/PRUEBAS.md` y `README.md`.
