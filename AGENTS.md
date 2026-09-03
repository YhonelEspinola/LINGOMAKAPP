# AGENTS.md — LINGOMAKAPP: Eliminar el indicador de nivel de combustible estimado

Instrucción de trabajo para el agente embebido en Android Studio. Se verificó cada archivo y línea
citada contra el código real antes de escribir esto.

---

## Decisión y motivo (para que quede documentado, no lo cuestiones)

Se decidió **eliminar por completo** la funcionalidad que calculaba y mostraba el nivel estimado de
combustible (galones + % del tanque + color de criticidad rojo/amarillo/verde) tanto en el card del
listado de Maquinaria como en la ficha de detalle. Motivo: las máquinas reales no dan una lectura
exacta de combustible (es un indicador analógico de aguja/rayitas), y un cálculo con decimales de
precisión a partir de esa fuente se desincroniza con la realidad con el paso del tiempo — el error se
acumula y nunca se autocorrige.

**Lo que SÍ se mantiene, no lo toques:** el cálculo de consumo/rendimiento
(`SuministroRepository.calcularConsumoGlsHora`, lo que se muestra como "Rendimiento (últimos 30
días)" en la ficha de máquina). Eso sigue siendo útil y correcto — solo se elimina el indicador de
**nivel** (cuánto combustible queda en el tanque), no el de **consumo** (cuánto gasta por hora).

---

## Archivos a modificar (8 confirmados, ninguno más debería tener referencias)

### 1. `data/repository/SuministroRepository.kt`
- Elimina el `data class NivelCombustibleEstimado` (línea ~8).
- Elimina la función `suspend fun calcularNivelEstimadoCombustible(uidMaquinaria: String): NivelCombustibleEstimado?` (línea ~49-80).
- No toques `calcularConsumoGlsHora` ni ninguna otra función de este archivo.

### 2. `ui/maquinaria/MaquinariaViewModel.kt`
Elimina (línea ~30-34, ~51-63):
- `_nivelCombustible` / `nivelCombustible` (LiveData)
- `_nivelesCombustible` / `nivelesCombustible` (LiveData)
- `fun cargarNivelCombustible(uidMaquinaria: String)`
- Cualquier otra función que llame a `calcularNivelEstimadoCombustible` o popule `_nivelesCombustible`.

### 3. `ui/maquinaria/MaquinariaAdapter.kt`
Elimina (línea ~18-21, ~62-77):
- La propiedad `nivelesCombustible` y `fun actualizarNivelesCombustible(...)`.
- El bloque completo en `bind()`/`onBindViewHolder` que arma `binding.tvNivelCombustible.text` y le
  aplica color según criticidad.
- **Después de esto, elimina también el `TextView` `tvNivelCombustible` de `item_maquinaria.xml`**
  (busca su bloque completo, incluyendo cualquier ícono/label que lo acompañe, ej. "⛽").

### 4. `ui/maquinaria/DetalleMaquinariaFragment.kt`
Elimina (línea ~61-81):
- El observer de `viewModel.nivelCombustible`.
- El bloque que arma `binding.tvNivelCombustibleDetalle.text` y su color.
- La llamada `viewModel.cargarNivelCombustible(uid)`.
- **Después, elimina también el `TextView` `tvNivelCombustibleDetalle`** (y cualquier label como
  "Nivel de combustible estimado" que lo acompañe) de `fragment_detalle_maquinaria.xml`, en la
  sección "Suministro y Repostaje" donde vive hoy.

### 5. `ui/maquinaria/MaquinariaAdminPagerAdapter.kt`
Elimina (línea ~81-86, ~138):
- `fun updateNivelesCombustible(...)`.
- La propiedad `nivelesCombustibleMap`.
- La llamada `maqAdapter.actualizarNivelesCombustible(nivelesCombustibleMap)` dentro del bind de la
  página de Maquinaria.

### 6. `ui/maquinaria/MaquinariaFragment.kt`
Elimina (línea ~95):
- El observer `maquinariaViewModel.nivelesCombustible.observe(viewLifecycleOwner) { ... }` completo.

### 7. `res/layout/item_maquinaria.xml`
Elimina el `TextView` `tvNivelCombustible` y cualquier vista hermana que solo exista para acompañarlo
(ícono, separador). Verifica que el layout quede visualmente coherente después de quitarlo (ajusta
márgenes si el elemento de abajo quedaba pegado a este).

### 8. `res/layout/fragment_detalle_maquinaria.xml`
Elimina el `TextView` `tvNivelCombustibleDetalle` y su label. Igual que en el punto anterior, revisa
que la sección "Suministro y Repostaje" quede bien después de quitarlo (probablemente solo queda
"Último Repostaje" y "Capacidad Tanque" ahí, que sí se mantienen).

---

## Verificación final obligatoria (Tarea 1)

Después de los 8 cambios, busca en **todo el proyecto** (no solo estos 8 archivos) cualquier
referencia restante a: `NivelCombustibleEstimado`, `nivelCombustible`, `nivelesCombustible`,
`calcularNivelEstimadoCombustible`, `tvNivelCombustible`, `tvNivelCombustibleDetalle`. Si encuentras
alguna que no esté en esta lista, avísame antes de decidir qué hacer con ella — no la borres a
ciegas.

Compila y corre en el emulador: confirma que el card de Maquinaria y la ficha de detalle ya no
muestran nada de nivel de combustible, pero que "Rendimiento (últimos 30 días)" (consumo) sigue
apareciendo normal, sin cambios.

---

## Reglas de alcance (Tarea 1)

1. No toques `calcularConsumoGlsHora` ni el texto de "Rendimiento" — eso se mantiene intacto.
2. No borres `SuministroEntity`, `SuministroRepository` completo, ni la sección "Suministro y
   Repostaje" — solo el sub-elemento de nivel dentro de ella.
3. Si algo de esto ya no existe en tu copia del código (por ejemplo si ya se había tocado antes),
   dilo explícitamente en vez de asumir que sigue igual.
4. Compila antes de reportar como terminado.

---

## TAREA 2 — Bug: los chips de filtro no se pueden deslizar horizontalmente

**Síntoma:** en varios filtros con chips que no caben en una sola pantalla, no se puede deslizar el
dedo para ver las opciones que quedan fuera de la vista — se quedan fijas.

**Diagnóstico (probable, confírmalo antes de corregir):** a diferencia del bug ya resuelto en
`SelectorFechasView` (que usaba un `ViewPager2` anidado), acá el contenedor es un
`HorizontalScrollView` normal envolviendo un `ChipGroup`. Es un problema conocido de Android: los
`Chip` de Material reclaman el toque agresivamente (por su ripple/estado), lo que puede impedir que
el `HorizontalScrollView` detecte a tiempo que el usuario quiere deslizar en vez de tocar un chip
puntual.

**Archivos afectados (9 `HorizontalScrollView` + `ChipGroup`, confirmados):**
- `fragment_alertas.xml` — `cgCategorias` (línea ~157) y `cgSubtipos`/`scrollSubtipos` (línea ~208)
  — **este último cambia de tipo de control en la Tarea 3, revisa esa tarea primero.**
- `item_inventario_page.xml` — `chipGroupStock` (línea ~107) y `chipGroupEstadoInventario`
  (línea ~120)
- `fragment_movimientos_global.xml` — `chipGroupTipo` (línea ~73) — **ojo:** en este archivo el
  `ChipGroup` NO está envuelto en `HorizontalScrollView` (son solo 3 chips, entran sin scroll) — no
  necesita este fix, ya está bien así.
- `item_mantenimiento_page.xml` — `chipGroupTipo` (línea ~103) y `chipGroupEstado` (línea ~117) —
  **este último cambia a dropdown en la Tarea 3 en la pestaña "En cola", revisa esa tarea primero.**
- `item_maquinaria_admin_page.xml` — `chipGroupEstadoMaquinaria` (línea ~151, pasa a dropdown en
  Tarea 3) y `chipGroupCategoriaMaquinaria` (línea ~164, pasa a dropdown en Tarea 3)
- `fragment_usuarios.xml` — `chipGroupRol` (línea ~100) y `chipGroupEstado` (línea ~156)

**Antes de corregir:** reproduce el bug en el emulador en al menos uno de estos (ej. Usuarios, que
tiene 3 chips y no cambia a dropdown en la Tarea 3) y confirma si el problema es exactamente el que
se describe arriba.

**Corrección propuesta, si se confirma:** crea una clase reutilizable
`utils/SwipeableHorizontalScrollView.kt` que extienda `android.widget.HorizontalScrollView` y
sobrescriba `onInterceptTouchEvent` con el mismo patrón ya usado en
`SelectorFechasView.onInterceptTouchEvent` (dejar pasar el `ACTION_DOWN` al hijo, pero interceptar en
`ACTION_MOVE` si el arrastre horizontal supera el touch slop y es más horizontal que vertical).
Reemplaza la etiqueta `<HorizontalScrollView>` por
`<com.lingomak.lingomakapp.utils.SwipeableHorizontalScrollView>` en los 7 archivos de arriba que sí
lo necesitan (los que quedan como chips después de la Tarea 3). No dupliques la lógica de
interceptación en cada archivo — un solo componente reutilizable para los 7.

---

## TAREA 3 — Regla: filtro con más de 3 opciones (incluyendo "Todos") → dropdown, si no → chips

Convierte estos filtros de chips a dropdown (mismo estilo `TextInputLayout` con
`ExposedDropdownMenu` que ya se usa en "Filtrar por Usuario" de Mantenimiento — cópiale el patrón
exacto):

| Archivo | ChipGroup a convertir | Opciones actuales |
|---|---|---|
| `item_mantenimiento_page.xml` / `MantenimientoPagerAdapter.kt` | `chipGroupEstado`, **solo en la pestaña "En cola" (position 0)** — la de Historial (position 1) se queda como chips, tiene 3 opciones | TODOS, PENDIENTE, EN_PROCESO, VENCIDO |
| `item_maquinaria_admin_page.xml` / `MaquinariaAdminPagerAdapter.kt` | `chipGroupEstadoMaquinaria` | TODOS, OPERATIVA, EN_MANTENIMIENTO, INACTIVA |
| `item_maquinaria_admin_page.xml` / `MaquinariaAdminPagerAdapter.kt` | `chipGroupCategoriaMaquinaria` | TODAS + categorías dinámicas (conviértelo aunque hoy tenga pocas — puede crecer) |
| `item_inventario_page.xml` / `InventarioPagerAdapter.kt` | `chipGroupStock` | TODOS, CON STOCK, BAJO STOCK, SIN STOCK |
| `fragment_alertas.xml` / `AlertasFragment.kt` | `cgCategorias` | Todas, Mantenimiento, Stock, Movimientos |
| `fragment_alertas.xml` / `AlertasFragment.kt` | `cgSubtipos` | **Conviértelo siempre a dropdown**, aunque a veces tenga solo 3 opciones (cuando la categoría es "Movimientos") — es el mismo filtro cambiando de tamaño según la categoría elegida arriba, y se ve inconsistente que a veces sea chips y a veces dropdown para el mismo control. Trátalo como una excepción explícita a la regla de "3 o menos = chips". |

**No toques estos, ya cumplen la regla (3 opciones, se quedan como chips):**
`chipGroupTipo` de Mantenimiento, `chipGroupEstado` de Historial en Mantenimiento,
`chipGroupEstadoInventario`, `chipGroupTipo` de Movimientos, `chipGroupRol` y `chipGroupEstado` de
Usuarios.

Al convertir cada uno: mantén la misma lógica de filtrado que ya existe (mismo `when`/comparación de
strings), solo cambia el control de entrada. No cambies ningún nombre de variable de filtro
(`filterEstadoMaq`, `filterCriticidadInv`, etc.) — otros bloques de código ya los usan.

---

## TAREA 4 — Quitar gráfica repetitiva en Estadísticas: "Salidas con OM vs Sueltas"

En `EstadisticasPagerAdapter.kt`, elimina la página con `tvTituloPagina.text = "Salidas con OM vs
Sueltas"` (línea ~202) — queda inmediatamente después de "Salidas: Consumo interno vs distribución
externa" (línea ~188) y se decidió que es repetitiva con esa.

En `MovimientosEstadisticasViewModel.kt`, elimina:
- El bloque `// PAGINA 7: OM vs Sueltas` y sus dos variables `salidasConOM`/`salidasSinOM`
  (línea ~153-154).
- El campo `omVsSueltas: Pair<Int, Int>` de la data class `EstadisticasData` (línea ~225) y su
  asignación en el `value = EstadisticasData(...)` (línea ~193).

**Importante:** al quitar esta página, las páginas que venían después (Costos Mantenimiento, Top 5
máquinas menor consumo, Rendimiento por Operario) recorren una posición hacia atrás en el
`ViewPager2`. Revisa `getItemCount()` y cualquier lógica que referencie una página por índice
numérico fijo (no por nombre) en `EstadisticasPagerAdapter.kt`, para que no queden desalineadas.

---

## TAREA 5 — Verificar si `InventarioFragment.kt` (versión admin) es código muerto

**Hallazgo a confirmar, no lo borres a ciegas:** `fragment_inventario.xml` lo usan dos clases
distintas — `InventarioFragment.kt` y `InventarioOpFragment.kt`. `InventarioOpFragment` sí se
instancia (desde `DashboardOperarioActivity.kt`, `HomeOperarioFragment.kt`,
`EscaneoQRFragment.kt` — confirmado, está vivo). `InventarioFragment.kt`, en cambio, **no aparece
instanciada en ningún lugar del proyecto para el flujo de admin** — el admin real usa
`InventarioContainerFragment` (con tabs Inventario/Movimientos), no esta clase.

Busca en todo el proyecto cualquier referencia a `InventarioFragment()` (sin el sufijo `Op`). Si
confirmas que no se usa en ningún lado, bórrala junto con cualquier código exclusivo de esa clase que
no comparta con `InventarioOpFragment` — pero **no borres `fragment_inventario.xml`**, ese layout sí
lo sigue usando `InventarioOpFragment`. Dime qué encontraste antes de borrar nada, por si me equivoco
y sí se usa desde algún lugar que no vi.

---

## Reglas de alcance — aplican a las Tareas 2 a 5 también

1. Trabaja en el orden numerado (Tarea 2, 3, 4, 5), confirmando cada una antes de pasar a la
   siguiente.
2. Para la Tarea 2, no apliques el fix sin antes reproducir el bug y confirmar la causa.
3. Para la Tarea 5, no borres nada sin confirmarme primero qué encontraste.
4. Compila y prueba en el emulador cada tarea. Si algo no compila, dilo — no asumas que está bien.
5. Si encuentras algo fuera de estas 5 tareas, repórtalo aparte.
