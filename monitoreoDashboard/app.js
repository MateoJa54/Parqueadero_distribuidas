/* ============================================================
 * Dashboard de monitoreo de espacios (solo lectura, vía SSE)
 * ============================================================
 * El navegador se suscribe con EventSource al canal SSE del
 * microservicio de zonas. No envía nada: solo escucha y pinta.
 *
 *   snapshot           -> foto completa al conectarse
 *   espacio-creado     -> se agregó un espacio nuevo
 *   espacio-actualizado-> cambió el estado de un espacio
 *                         (incluye los cambios que dispara un ticket)
 * ============================================================ */

// URL directa al microservicio de zonas (EventSource no envía token,
// por eso NO pasa por Kong). Cámbiala si zonas corre en otro host/puerto.
const SSE_URL = "http://localhost:8080/api/v1/sse/espacios";

// Estado local: idEspacio -> objeto espacio (EspacioRespondeDto).
const espacios = new Map();

// ---- Referencias del DOM ----
const $zonas = document.getElementById("zonasContainer");
const $vacio = document.getElementById("vacio");
const $status = document.getElementById("statusConexion");
const $dot = document.getElementById("dotConexion");
const $lastUpdate = document.getElementById("lastUpdate");
const $apiUrl = document.getElementById("apiUrl");
const $numTotal = document.getElementById("numTotal");
const $numDisp = document.getElementById("numDisponible");
const $numOcup = document.getElementById("numOcupado");
const $numOtros = document.getElementById("numOtros");

$apiUrl.textContent = SSE_URL;

// ---- Utilidades de presentación ----
const CLASE_ESTADO = {
  DISPONIBLE: "bg-disponible",
  OCUPADO: "bg-ocupado",
  RESERVADO: "bg-reservado",
  MANTENIMIENTO: "bg-mantenimiento",
};

const ICONO_TIPO = {
  MOTO: "🏍️",
  AUTO: "🚗",
  BUSETA: "🚐",
};

function marcarConexion(activa) {
  if (activa) {
    $status.textContent = "Conectado";
    $dot.className = "dot dot-on";
  } else {
    $status.textContent = "Sin conexión (reintentando…)";
    $dot.className = "dot dot-off";
  }
}

function tocarReloj() {
  $lastUpdate.textContent = new Date().toLocaleTimeString("es-EC");
}

// ---- Renderizado completo agrupado por zona ----
function render() {
  const total = espacios.size;
  $vacio.style.display = total === 0 ? "" : "none";

  // Contadores para el resumen.
  let disp = 0, ocup = 0, otros = 0;

  // Agrupar por zona (nombre + id).
  const porZona = new Map();
  for (const esp of espacios.values()) {
    const clave = esp.idZona || "sin-zona";
    if (!porZona.has(clave)) {
      porZona.set(clave, { nombre: esp.nombreZona || "Sin zona", lista: [] });
    }
    porZona.get(clave).lista.push(esp);

    if (esp.estado === "DISPONIBLE") disp++;
    else if (esp.estado === "OCUPADO") ocup++;
    else otros++;
  }

  $numTotal.textContent = total;
  $numDisp.textContent = disp;
  $numOcup.textContent = ocup;
  $numOtros.textContent = otros;

  // Reconstruir el contenedor de zonas.
  $zonas.querySelectorAll(".zona-bloque").forEach((n) => n.remove());

  const zonasOrdenadas = [...porZona.values()].sort((a, b) =>
    a.nombre.localeCompare(b.nombre)
  );

  for (const zona of zonasOrdenadas) {
    const bloque = document.createElement("section");
    bloque.className = "zona-bloque";

    const libres = zona.lista.filter((e) => e.estado === "DISPONIBLE").length;
    bloque.innerHTML = `
      <div class="flex items-center justify-between mb-3">
        <h2 class="zona-titulo">📍 ${zona.nombre}</h2>
        <span class="zona-badge">${libres}/${zona.lista.length} libres</span>
      </div>
    `;

    const grid = document.createElement("div");
    grid.className =
      "grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3";

    zona.lista
      .sort((a, b) => (a.codigo || "").localeCompare(b.codigo || ""))
      .forEach((esp) => grid.appendChild(tarjeta(esp)));

    bloque.appendChild(grid);
    $zonas.appendChild(bloque);
  }
}

function tarjeta(esp) {
  const card = document.createElement("div");
  card.id = "esp-" + esp.id;
  const clase = CLASE_ESTADO[esp.estado] || "bg-mantenimiento";
  card.className =
    "espacio-card " + clase + (esp.activo === false ? " espacio-inactivo" : "");

  card.innerHTML = `
    <span class="espacio-codigo">${ICONO_TIPO[esp.tipo] || "🅿️"} ${esp.codigo || "—"}</span>
    <span class="espacio-meta">${esp.tipo || ""}${esp.descripcion ? " · " + esp.descripcion : ""}</span>
    <span class="espacio-estado">${esp.estado}</span>
  `;
  return card;
}

// Aplica un cambio puntual y anima la tarjeta afectada.
function aplicarCambio(esp) {
  espacios.set(esp.id, esp);
  render();
  const card = document.getElementById("esp-" + esp.id);
  if (card) {
    card.classList.remove("flash");
    void card.offsetWidth; // reinicia la animación
    card.classList.add("flash");
  }
  tocarReloj();
}

// ---- Conexión SSE ----
function conectar() {
  const source = new EventSource(SSE_URL);

  source.onopen = () => marcarConexion(true);

  source.onerror = () => {
    marcarConexion(false);
    // EventSource reintenta solo; si el navegador lo cerró, reabrimos.
    if (source.readyState === EventSource.CLOSED) {
      setTimeout(conectar, 3000);
    }
  };

  // Foto inicial: lista completa de espacios.
  source.addEventListener("snapshot", (e) => {
    espacios.clear();
    const lista = JSON.parse(e.data);
    lista.forEach((esp) => espacios.set(esp.id, esp));
    render();
    tocarReloj();
  });

  // Nuevo espacio creado en zonas.
  source.addEventListener("espacio-creado", (e) => {
    aplicarCambio(JSON.parse(e.data));
  });

  // Cambio de estado (incluye el que dispara emitir/pagar/anular un ticket).
  source.addEventListener("espacio-actualizado", (e) => {
    aplicarCambio(JSON.parse(e.data));
  });
}

conectar();
