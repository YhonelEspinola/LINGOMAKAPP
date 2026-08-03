# AGENTS.md — LINGOMAKAPP: Coherencia visual de listados y filtros

Este documento es la instrucción de trabajo para el agente de IA embebido en Android Studio.
Todo lo que dice aquí fue verificado leyendo el código real del proyecto antes de escribirlo — no son
suposiciones. Cuando se dice "confirmado en código", significa que se citó el archivo y la línea exacta.

## Contexto del proyecto

- App Android nativa, Kotlin, arquitectura MVVM, Room + Firestore (offline-first).
- Los módulos con listados usan uno de estos dos patrones de "contenedor + pestañas":
  - **Patrón A (ViewPager2 + Fragments separados)** — no se usa actualmente en los módulos de este documento.
  - **Patrón B (ViewPager2 + un solo `RecyclerView.Adapter` que genera cada pestaña como un "page")** —
    este es el patrón real y vigente. Ejemplos: `MantenimientoPagerAdapter.kt` (pestañas EN COLA/HISTORIAL)
    y `MaquinariaAdminPagerAdapter.kt` (pestañas MAQUINARIA/BITÁCORA DE USO). Cada "page" es un
    `ViewHolder` que infla `item_mantenimiento_page.xml` o `item_maquinaria_admin_page.xml` respectivamente.
- Regla de alcance: si vas a tocar un módulo, sigue el Patrón B — no reintroduzcas fragments separados
  por pestaña, ya se intentó antes y generó inconsistencias (duplicación de FABs, botones sin conectar).

## Regla base — ningún cambio "de estilo nuevo": esto es reordenar y unificar lo que ya existe

Antes de escribir una sola línea, un hallazgo importante para no malgastar esfuerzo:
**no existen dos estilos visuales distintos de dropdown o de chip en el proyecto.** Tanto
`item_mantenimiento_page.xml` como `item_maquinaria_admin_page.xml` usan el mismo
`style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"` para
los dropdowns. Lo que hace que Bitácora de Uso "se vea mejor" no es un componente distinto — es el
**orden**: ahí los dropdowns (Máquina, Operario) aparecen antes que los chips, mientras que en
Mantenimiento los chips aparecen antes que el dropdown (Usuario). Ese orden invertido en Mantenimiento
es justamente lo que hay que corregir (ver Tarea 3).

Sí hay una inconsistencia real de **chips** (ver Tarea 4), pero tampoco es "dos estilos": es que el
mismo estilo bueno (`R.layout.layout_chip_choice`, con `selector_chip_choice` y `selector_chip_text`)
se usa en algunos chip groups y en otros no, incluso dentro del mismo módulo.

---

## TAREA 1 — Todos los filtros deben ser colapsables

Estado actual (confirmado):
- `item_mantenimiento_page.xml` y `item_maquinaria_admin_page.xml` ya son colapsables (header
  "FILTROS Y RANGO" + chevron + `layoutFiltrosExpandible` con `visibility="gone"` por defecto).
- **`fragment_usuarios.xml` no tiene filtros de ningún tipo** (ni colapsables ni fijos) — es solo un
  `RecyclerView` a pantalla completa. Si el módulo de Usuarios debe tener filtro (ej. por rol o por
  estado activo/inactivo), agrégalo siguiendo el mismo patrón de header colapsable de los otros dos
  archivos citados arriba. Si no necesita filtros, está bien que no los tenga — pero sí falta el
  buscador (ver Tarea 7).

## TAREA 2 — Barra de búsqueda fuera de los filtros colapsables, encima del selector de pestañas

Estado actual (confirmado):
- `fragment_mantenimiento.xml` y `fragment_maquinaria_admin.xml` ya cumplen esto: tienen un
  `Toolbar` con un `EditText` de búsqueda (`etBuscarMantenimiento` / `etBuscarMaquinaria`) ANTES del
  `TabLayout`, en el contenedor — no dentro de cada pestaña.
- **`fragment_inventario_container.xml` NO tiene barra de búsqueda.** El `TabLayout` está directo en
  la raíz, sin ningún `Toolbar`/`EditText` encima.
- En su lugar, la búsqueda vive **duplicada** dentro de cada pestaña por separado:
  - `fragment_inventario.xml` línea 9: `EditText id="etBuscar"`
  - `fragment_movimientos_global.xml` línea 23: `EditText id="etBuscar"` (mismo id, archivo distinto)

Esto es una regresión respecto al patrón ya usado en Mantenimiento y Maquinaria. Corrección:
1. Mueve el `EditText` de búsqueda a `fragment_inventario_container.xml`, replicando exactamente
   la estructura de `fragment_maquinaria_admin.xml` (Toolbar con `LinearLayout` + `EditText`,
   `app:layout_constraintTop_toTopOf="parent"`, y el `TabLayout` constreñido debajo de ese Toolbar).
2. Elimina los dos `EditText etBuscar` duplicados de `fragment_inventario.xml` y
   `fragment_movimientos_global.xml`.
3. En el Kotlin del contenedor de Inventario, cablea el texto del buscador para que filtre la
   pestaña visible actualmente (revisa cómo lo hace `MaquinariaFragment.kt` con
   `etBuscarMaquinaria` hacia `pagerAdapter.updateSearch(...)` y replica el mismo mecanismo).

## TAREA 3 — Orden jerárquico al expandir filtros: fechas → dropdowns → chips

Estado actual (confirmado, orden real por archivo):

| Archivo | Orden actual | ¿Correcto? |
|---|---|---|
| `item_maquinaria_admin_page.xml` (Bitácora) | fecha → dropdowns (Máquina, Operario) → *(sin chips en esa pestaña)* | Sí, es la referencia |
| `item_maquinaria_admin_page.xml` (Maquinaria) | chips (Estado, Categoría) → *(sin dropdown ni fecha en esa pestaña)* | No aplica comparación, no tiene los 3 tipos |
| `item_mantenimiento_page.xml` | fecha → **chips (Tipo, Estado)** → **dropdown (Usuario)** | **No — orden invertido, corregir** |

Corrección en `item_mantenimiento_page.xml`: mueve el bloque
`TextInputLayout id="tilFiltroUsuario"` (dropdown de Usuario) para que quede **inmediatamente
después** del bloque `layoutFiltroFecha`, y antes de los dos `HorizontalScrollView` que contienen
`chipGroupTipo` y `chipGroupEstado`. No cambies ningún id ni la lógica de filtrado en el Kotlin
(`MantenimientoPagerAdapter.kt`), es un cambio de orden visual únicamente.

Cuando un módulo tenga los 3 tipos de filtro a la vez, el orden final debe quedar:
1. Selector de fechas (con su etiqueta dinámica "Últimos 30 días" arriba)
2. Listas desplegables (dropdowns)
3. Chips

## TAREA 4 — Mismo estilo de dropdown y de chip en todos los módulos

Dropdowns: ya son consistentes (ver "Regla base" arriba) — no se necesita ningún cambio.

Chips: **sí hay una inconsistencia real**, y está incluso dentro del mismo archivo. Confirmado en
`MantenimientoPagerAdapter.kt`:
- El chip group `chipGroupEstado` (dinámico) se construye con `createChip(...)`, que infla
  `R.layout.layout_chip_choice` — este es el estilo "bueno" (fondo/texto con
  `selector_chip_choice` / `selector_chip_text`, sin borde).
- Pero los chips de `chipGroupTipo` (Todos/Preventivo/Correctivo) están escritos **directo en el XML**
  de `item_mantenimiento_page.xml` con `style="@style/Widget.MaterialComponents.Chip.Choice"` a secas,
  sin el selector de color custom — se ven con el estilo default de Material, no con el estilo de marca.

Corrección: convierte los 3 chips estáticos de `chipGroupTipo` (`chipTipoTodos`,
`chipTipoPreventivo`, `chipTipoCorrectivo`) en chips generados dinámicamente igual que
`chipGroupEstado`, usando `createChip(...)` / `R.layout.layout_chip_choice`, en vez de declararlos
fijos en el XML. Audita también cualquier otro `ChipGroup` del proyecto (ej. en Inventario,
Movimientos, filtros de estado de Maquinaria) para confirmar que todos usan
`R.layout.layout_chip_choice` — si encuentras alguno con `style="@style/Widget.MaterialComponents.Chip.Choice"`
puesto directo, conviértelo al mismo patrón.

## TAREA 5 — Bug de filtros en Bitácora de Uso (diagnosticar antes de asumir la causa)

Descripción del síntoma tal como lo reporta el usuario: al presionar el header de filtros en la
pestaña Bitácora de Uso, "los filtros no aparecen y en su lugar se esconde/muestra la card del
listado" — un comportamiento visualmente incorrecto.

**Auditoría hecha:** se revisó `MaquinariaAdminPagerAdapter.kt` completo (la clase que maneja ambas
pestañas). La lógica de expansión (`expandedFilters` por posición, `actualizarUIPorExpansion`,
el listener de `btnToggleFiltros`) está estructuralmente correcta a primera lectura — no se encontró
una causa obvia de que tocar el header mueva la visibilidad del `RecyclerView` en vez de la del
`layoutFiltrosExpandible`. Posibles causas a investigar con el proyecto corriendo (breakpoints o
logs, no se puede diagnosticar más por lectura estática):

1. Revisa si `getItemViewType()` está sobrescrito en `MaquinariaAdminPagerAdapter`. Ahora mismo NO lo
   está — las dos pestañas (Maquinaria y Bitácora) usan el mismo `viewType` por defecto (0). Aunque el
   código de `bind()` parece re-configurar todo en cada llamada, prueba a agregar
   `override fun getItemViewType(position: Int) = position` para eliminar cualquier posibilidad de que
   el RecyclerView interno del ViewPager2 esté reciclando y confundiendo el `ViewHolder` de una pestaña
   con el de la otra al hacer swipe rápido.
2. Verifica en tiempo real (con el layout inspector) si al pulsar el header, es realmente
   `layoutFiltrosExpandible` el que cambia de `GONE` a `VISIBLE`, o si por error está afectando a
   `rvContenido` — compara los ids exactos en el debugger, no solo por lectura del XML.
3. Confirma que `item_maquinaria_admin_page.xml` no tiene ningún `id` duplicado con otro layout que
   pudiera colisionar en el binding generado.

No implementes una solución sin antes reproducir el bug y confirmar la causa real — este es
justamente el tipo de bug que ya nos costó tiempo antes por asumir en vez de verificar.

## TAREA 6 — Buscador y filtros anclados en la cabecera (no deben desaparecer al hacer scroll)

Estado actual (confirmado):
- `fragment_mantenimiento.xml` y `fragment_maquinaria_admin.xml` YA implementan esto correctamente:
  el `Toolbar` (buscador) y el `TabLayout` están constreñidos directamente al `ConstraintLayout` raíz,
  fuera del `ViewPager2` — nunca se mueven, solo el contenido de la lista hace scroll debajo.
- Los **filtros colapsables** (dentro de cada page) SÍ viven dentro del área que hace scroll junto
  con la lista — eso es intencional y correcto (es "colapsable", no "fijo"); lo que debe quedar fijo
  es solo el buscador y el selector de pestañas, no el bloque de filtros expandido.
- Una vez corregida la Tarea 2 (mover el buscador de Inventario al contenedor), Inventario/Movimientos
  quedará con el mismo comportamiento anclado automáticamente, porque copiará la misma estructura.

No hay trabajo adicional en esta tarea más allá de lo ya pedido en la Tarea 2.

## TAREA 7 — Vista de Usuarios no aprovecha el ancho de pantalla

Estado actual (confirmado): la sospecha inicial de "padding excesivo" **no se confirma numéricamente**.
`fragment_usuarios.xml` tiene `android:padding="16dp"` en las 4 direcciones sobre el `ConstraintLayout`
raíz — eso da un inset total de 16dp por lado. En comparación, en Mantenimiento el inset real es
mayor (16dp del contenedor de la página + 12dp de `layout_marginHorizontal` que trae la propia card
en `item_mantenimiento.xml` = 28dp por lado). Es decir, Usuarios tiene MENOS padding lateral que
Mantenimiento, no más.

La causa más probable de que "se vea raro" es otra, y sí está confirmada: **`fragment_usuarios.xml`
es la única vista de listado del proyecto sin buscador ni cabecera anclada** — el `RecyclerView` empieza
pegado arriba de la pantalla sin ningún `Toolbar`, a diferencia de todos los demás módulos. Además su
FAB (`fabAgregarUsuario`) es un `FloatingActionButton` circular simple con solo ícono, mientras que en
el resto del proyecto se usa `ExtendedFloatingActionButton` con texto (ver Tarea 8, mismo patrón de
inconsistencia).

Corrección:
1. Dale a `fragment_usuarios.xml` la misma estructura de cabecera que `fragment_maquinaria_admin.xml`
   (Toolbar con buscador). Usuarios no tiene pestañas, así que no necesita `TabLayout`/`ViewPager2` —
   pero sí el `Toolbar` con `EditText` de búsqueda, ancho igual al resto.
2. Cambia `fabAgregarUsuario` de `FloatingActionButton` a `ExtendedFloatingActionButton` con texto
   "+ REGISTRAR USUARIO", mismo estilo que se pide en la Tarea 8 para los demás módulos.
3. Confirma visualmente en el emulador, después del cambio, si las cards de `item_usuario.xml` ya se
   ven consistentes con las de otros módulos — si sigue viéndose "cortado", compara medida por medida
   contra `item_maquinaria.xml` antes de tocar el padding a ciegas.

## TAREA 8 — Botones de registro en Inventario / Movimientos

Estado actual confirmado en `fragment_movimientos_global.xml` (líneas ~158-172):
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnRegistrar"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:layout_margin="16dp"
    android:text="REGISTRAR"
    app:icon="@drawable/qr"
    ...
    app:layout_constraintBottom_toBottomOf="parent" />
```
Es un `MaterialButton` de **ancho completo** (`match_parent`), no un botón flotante — esa es la causa
exacta de por qué no se ve como el resto de la app. Todos los demás FABs "REGISTRAR" del proyecto
(`fabAgregarMaquinaria`, `fabAgregarRepuesto`, `fabAccionMaquinaria`) son
`ExtendedFloatingActionButton` con `layout_width="wrap_content"`, forma de píldora, y posicionados
solo en la esquina inferior derecha con `layout_margin="16dp"`.

Corrección exacta:
1. En `fragment_movimientos_global.xml`: cambia `btnRegistrar` de `MaterialButton` a
   `ExtendedFloatingActionButton`, `layout_width="wrap_content"`, mismo patrón visual que
   `fabAgregarRepuesto` en `fragment_inventario.xml` (líneas 195-207) — cornerRadius de píldora
   (no le pongas `app:cornerRadius` explícito, el `ExtendedFloatingActionButton` ya la trae por
   defecto). **Ojo:** este botón hoy dispara el flujo de escaneo QR (`app:icon="@drawable/qr"`) — al
   convertirlo a FAB, mantén exactamente la misma acción en el `setOnClickListener`, solo cambia
   apariencia e ícono.
2. Cambia el texto de `btnRegistrar` de `"REGISTRAR"` a `"+ REGISTRAR MOVIMIENTO"`, y el ícono de
   `@drawable/qr` a `@android:drawable/ic_input_add` (el mismo "+" que usan los otros FABs) — si el
   ícono de QR es importante para indicar la función de escaneo, dilo en el chat antes de quitarlo,
   podría ser mejor un ícono compuesto o un botón secundario aparte para el QR en vez de perder esa
   indicación visual.
3. En `fragment_inventario.xml`, cambia el texto de `fabAgregarRepuesto` de `"REGISTRAR"` a
   `"+ REGISTRAR REPUESTO"` (línea 200).

## TAREA 9 — Bug: el FAB de Maquinaria muestra el texto incorrecto al entrar por primera vez

Causa confirmada en `MaquinariaFragment.kt` (líneas 56-81, función `setupViewPager()`):
- El XML `fragment_maquinaria_admin.xml` (línea 81) define el texto por defecto del FAB como
  `"REGISTRAR"`.
- El texto correcto ("REGISTRAR ACTIVO" en pestaña 0, "REGISTRAR USO" en pestaña 1) solo se asigna
  dentro de `registerOnPageChangeCallback { onPageSelected(...) }` — y ese callback de ViewPager2
  **no se dispara automáticamente al cargar la pestaña inicial**, solo cuando el usuario cambia de
  página. Por eso el FAB muestra el texto default del XML ("REGISTRAR") hasta que el usuario desliza
  una vez y regresa.

Corrección: en `setupViewPager()`, después de registrar el `OnPageChangeCallback` (o justo después
del bloque `.attach()` del `TabLayoutMediator`), llama manualmente una vez a la misma lógica que fija
el texto para la posición inicial:
```kotlin
binding.fabAccionMaquinaria.text = "REGISTRAR ACTIVO" // posición inicial = 0
```
o, mejor, extrae el cuerpo de `onPageSelected` a una función privada `actualizarFabPorPagina(position: Int)`
y llámala tanto desde el callback como una vez manualmente con `position = 0` al final de
`setupViewPager()`. Esto evita duplicar la lógica y that cualquier cambio futuro a ese texto solo se
edite en un lugar.

---

## Reglas de alcance — qué NO hacer (aplican a todas las tareas de arriba)

Estas nacen de bugs reales que ya tuvimos con otro agente en este mismo proyecto. No son opcionales.

1. **No borres ninguna vista o archivo sin buscar todas sus referencias primero** — un layout puede
   estar compartido por más de un Fragment/Adapter.
2. **No reutilices una pantalla entre roles distintos ocultando partes por condición** — si algo debe
   verse distinto para admin y operario, evalúa si ya existen fragments/layouts separados para cada
   uno antes de mezclar lógica condicional en uno solo.
3. **Antes de cambiar una ruta de navegación, busca todos los puntos de entrada** — menú lateral,
   cards de acceso rápido del home, y cualquier fallback de otra pantalla (ej. un botón "cerrar" que
   regresa a esta).
4. **Ningún botón visible sin su acción conectada.** Si tocas un XML con un botón, verifica en el
   mismo cambio que tiene su `setOnClickListener` correspondiente en el Kotlin.
5. **No dupliques controles entre un contenedor y sus pestañas hijas** — antes de agregar un FAB o
   botón nuevo, confirma si ya existe uno equivalente en otro nivel.
6. **Las migraciones de Room deben ser no destructivas** si tocas cualquier entidad — no uses
   `fallbackToDestructiveMigration()`.
7. **No toques código fuera del alcance de estas 9 tareas.** Si encuentras otro bug al auditar,
   repórtalo aparte antes de corregirlo.
8. **No reportes ninguna tarea como terminada sin compilar y probar en el emulador.** Si algo no
   compila, dilo explícitamente en vez de asumir que el cambio es correcto.
9. **Trabaja tarea por tarea, en el orden numerado de este documento**, y confirma cada una (con
   captura o descripción de lo verificado) antes de pasar a la siguiente.
