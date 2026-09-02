# AGENTS.md — LINGOMAKAPP: Diseño de campos, bugs de Bitácora de Uso, orientación

Instrucción de trabajo para el agente embebido en Android Studio. Todo lo citado (archivo + línea)
fue verificado en el código real antes de escribirse.

---

## TAREA 1 — Unificar todos los campos de texto al estilo TextInputLayout

Ya existe el patrón que se quiere generalizar: `TextInputLayout` con el label integrado en el borde
del campo (visible hoy en 12 archivos, ej. el campo "Horómetro Final" de
`fragment_finalizar_mantenimiento.xml`). El patrón viejo a reemplazar es label en un `TextView`
aparte arriba + `EditText` con `android:background="@drawable/bg_edit_text"`.

**Archivos confirmados con el patrón viejo, a convertir (21 en total):**
`fragment_inventario.xml`, `fragment_editar_mantenimiento.xml`, `fragment_inventario_container.xml`,
`fragment_cambiar_password.xml`, `fragment_registrar_salida_op.xml`, `auth_login.xml`,
`fragment_registrar_movimiento_global.xml`, `fragment_finalizar_mantenimiento.xml` (solo sus campos
que no sean ya TextInputLayout — audita cuáles le faltan), `fragment_mantenimiento.xml`,
`fragment_agregar_usuario.xml`, `fragment_programar_mantenimiento.xml`, `fragment_usuarios.xml`,
`fragment_operario_maquinaria.xml`, `fragment_editar_movimiento.xml`, `fragment_maquinaria.xml`,
`fragment_editar_repuesto.xml`, `dialog_seleccionar_insumo.xml`, `fragment_configuracion.xml`,
`fragment_maquinaria_admin.xml`, `fragment_editar_usuario.xml`.

**Patrón de reemplazo** (usa como referencia exacta cualquier campo ya bueno, ej. `etHorometroFinal`
en `fragment_finalizar_mantenimiento.xml`):
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Nombre del campo">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etMismoIdQueTeniaAntes"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="..." />
</com.google.android.material.textfield.TextInputLayout>
```
Reglas al convertir:
- **Mantén el mismo `android:id`** que tenía el `EditText` original — no lo cambies, para no romper
  ninguna referencia en el Kotlin.
- Si el campo original tenía `android:hint="Ej: 3500"` (un ejemplo, no un label), decide si ese texto
  pasa a ser el `android:hint` del `TextInputLayout` (el label) o si debe ir como
  `app:helperText` — no los dejes duplicados ni los pierdas.
- Campos de fecha que ya están bloqueados (`focusable="false"`, abren un `DatePickerDialog`) deben
  seguir bloqueados igual dentro del nuevo `TextInputLayout` — no los vuelvas editables por accidente.
- Si algún `EditText` ya tiene un `DecimalDigitsInputFilter` o cualquier `InputFilter` aplicado en el
  Kotlin, verifica que lo sigas aplicando al `TextInputEditText` nuevo (el id no cambia, así que en
  teoría no requiere tocar el Kotlin, pero confírmalo).
- Trabaja archivo por archivo, compila después de cada uno — son 21 archivos, no lo hagas todo de
  una pasada sin verificar.

---

## TAREA 2 — Bug: filtros de Bitácora de Uso se comportan mal después de ver un detalle

**Diagnóstico (probable, a confirmar antes de corregir):** `MaquinariaAdminPagerAdapter.kt` llama
`notifyDataSetChanged()` en 7 métodos (`updateBitacora`, `updateMaquinarias`,
`updateNivelesCombustible`, `updateCatalogoMaquinas`, `updateCatalogoOperarios`,
`updateCategorias`, `updateSearch`) — todos enganchados a observadores de LiveData en
`MaquinariaFragment.kt` (líneas 92-108). Al volver de `DetalleBitacoraFragment`, esos observadores
se vuelven a disparar (comportamiento normal de LiveData al recuperar el ciclo de vida STARTED), lo
que dispara `notifyDataSetChanged()` sobre un `RecyclerView` de 2 posiciones que vive anidado dentro
de un `ViewPager2` (`binding.viewPagerMaquinaria`) — eso puede hacer que se reconstruya el
`PageViewHolder` de una posición sin que el estado de `expandedFilters` (un `Map` en el propio
adapter) quede sincronizado con las vistas reales, causando que el botón de toggle termine
controlando la vista equivocada.

**Antes de corregir:** reproduce el bug en el emulador con breakpoints o logs en
`PageViewHolder.bind()` y en `actualizarUIPorExpansion()` — confirma si el `PageViewHolder` se está
recreando (se llama `onCreateViewHolder` de nuevo) al volver del detalle, o si es el mismo objeto
reutilizado con datos desincronizados. Dime cuál de los dos es antes de aplicar la corrección.

**Corrección más probable, aplícala si se confirma lo anterior:** reemplaza los 7
`notifyDataSetChanged()` por actualizaciones puntuales:
```kotlin
fun updateBitacora(list: List<BitacoraUsoModel>) {
    allBitacora = list
    notifyItemChanged(1) // solo la página de Bitácora, no las 2
}
```
(y el equivalente `notifyItemChanged(0)` para lo que solo afecta a la página de Maquinaria). Esto es
además buena práctica de RecyclerView en general — `notifyDataSetChanged()` nunca debería ser la
opción por defecto cuando se sabe qué posición cambió.

---

## TAREA 3 — Bug: el carrusel de rango de fechas no se puede deslizar

**Causa confirmada:** `SelectorFechasView.kt` tiene su propio `ViewPager2` interno con 3 páginas
(`SelectorAdapter.getItemCount() = 3`, línea ~107 — el dato está bien, si sale mal en pantalla no es
por falta de páginas). El problema es que este `ViewPager2` ahora vive **anidado dentro de otro
`ViewPager2`** (el de las pestañas Maquinaria/Bitácora en `MaquinariaFragment.kt`). Es el conflicto
clásico de "ViewPager2 dentro de ViewPager2": el de afuera intercepta el gesto de arrastre horizontal
antes de que el de adentro pueda reaccionar.

**Corrección:** en `SelectorFechasView.kt`, sobre el `viewPager` interno (línea 23), agrega un
listener de touch que le pida a su padre que no le robe el gesto mientras el usuario arrastra:
```kotlin
viewPager.getChildAt(0).setOnTouchListener { v, event ->
    when (event.action) {
        android.view.MotionEvent.ACTION_DOWN -> parent.requestDisallowInterceptTouchEvent(true)
        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
            parent.requestDisallowInterceptTouchEvent(false)
    }
    v.performClick()
    false
}
```
(el `viewPager.getChildAt(0)` es el `RecyclerView` interno que usa `ViewPager2` — confirma que esa
es la forma correcta de acceder a él en la versión de `viewpager2` que usa el proyecto; si hay una
forma más directa en esa versión, iúsala). Prueba después en el emulador que se puede deslizar entre
las 3 páginas (chips 7D/30D/12S/6M/1A, navegación con flechas, selector custom) sin que se trabe.

---

## TAREA 4 — Bloquear orientación horizontal en toda la app

**Confirmado:** ninguna de las 3 actividades reales de la app tiene `android:screenOrientation`
definido en `AndroidManifest.xml` (la única que sí lo tiene, `UCropActivity`, es de una librería de
terceros — no la toques, ya está bien).

Agrega `android:screenOrientation="portrait"` a estas 3 actividades en `AndroidManifest.xml`:
- `.ui.auth.LoginActivity`
- `.ui.dashboard.DashboardAdminActivity`
- `.ui.dashboard.DashboardOperarioActivity`

No hace falta tocar ningún layout ni Kotlin — es un cambio de manifest únicamente. Verifica en el
emulador que al girar el dispositivo físico/virtual la app se mantiene en vertical en las 3.

---

## Reglas de alcance — qué NO hacer

1. En la Tarea 1, no cambies ningún `android:id` — rompería referencias en Kotlin.
2. En la Tarea 2, no apliques la corrección sin antes confirmar la causa real con breakpoints/logs —
   dime qué encontraste primero.
3. En la Tarea 3, no elimines ni reemplaces el `ViewPager2` interno de `SelectorFechasView` por otro
   componente — el fix es de manejo de touch, no de arquitectura.
4. Compila y prueba cada tarea en el emulador antes de pasar a la siguiente, en el orden numerado.
5. Si encuentras algo fuera de estas 4 tareas, repórtalo aparte, no lo corrijas de paso.
