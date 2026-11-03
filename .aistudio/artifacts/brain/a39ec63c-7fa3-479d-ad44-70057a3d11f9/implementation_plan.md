# FoodWIse — Plan de Implementación de Arquitectura y Agente Basado en Utilidad

FoodWIse es una aplicación Android nativa desarrollada con Kotlin y Jetpack Compose diseñada para ayudar a los hogares a reducir el desperdicio de alimentos. El núcleo de la aplicación es un **agente inteligente basado exclusivamente en utilidad** que determina en todo momento qué alimento del hogar requiere atención prioritaria y qué acción recomendar (alertar o proponer una receta) para evitar su descarte.

---

## Decisiones Críticas y Confirmaciones del Usuario

> [!IMPORTANT]
> A partir de las aclaraciones confirmadas por el usuario, el proyecto se estructura bajo las siguientes premisas firmes:

- **Persistencia de Datos**: Base de datos local robusta con **Room (SQLite)** sin depender de servicios en la nube ni requerir inicios de sesión obligatorios, garantizando privacidad, inmediatez y funcionamiento sin conexión.
- **Inventario Inicial Precargado**: Se incluye una despensa doméstica de prueba representativa (lácteos, verduras frescas, carnes, legumbres y despensa seca) con fechas de vencimiento escalonadas (vencidos, hoy, próximos 2 a 5 días, y largo plazo) para permitir la evaluación inmediata del agente y sus recomendaciones.
- **Integración con TheMealDB y Normalización Bilingüe**: Integración con la API real de TheMealDB mediante Retrofit/Moshi, complementada con un diccionario y normalizador bilingüe (español ↔ inglés) de ingredientes para consultar recetas reales automáticamente a partir de alimentos registrados en español.

---

## 1. Visión General y Concepto Central

### Declaración Académica del Agente
> **“El agente decide, en cada momento, qué alimento del hogar requiere atención primero y qué acción recomendar sobre él —alertar o proponer una receta— para evitar que se desperdicie”.**

El sistema opera bajo el paradigma de **Agente Basado en Utilidad** (Utility-Based Agent):
- No es un agente de búsqueda de caminos (no utiliza BFS, DFS, Dijkstra o A* porque no navega un espacio de estados en busca de una secuencia de acciones).
- No es un agente con modelo de Machine Learning entrenado (la primera versión utiliza una función de utilidad formal, determinista, paramétrica y reproducible).
- El agente percibe un entorno parcialmente observable (el inventario registrado por el usuario, que puede diferir del estado físico microscópico real de los alimentos) y dinámico (modificado continuamente por compras, consumos y descartes).

---

## 2. Experiencia de Usuario y Diseño Visual

### Identidad Visual y Estilo
- **Paleta de Colores**: Verde esmeralda sostenible y salvia orgánico como colores primarios (`#1B5E20`, `#2E7D32`, `#4CAF50`), combinados con fondos neutros cálidos (`#F9FBE7`, `#F1F8E9`, superficies `#FFFFFF` y `#121212` en modo oscuro).
- **Semántica de Alertas y Prioridades**:
  - **Urgencia Crítica / Alta**: Rojo coral / Carmín (`#D32F2F`) para productos vencidos o con vencimiento en menos de 24-48 horas.
  - **Urgencia Media**: Ámbar / Mostaza cálido (`#F57C00`) para productos con vencimiento en 3 a 5 días y perecibilidad moderada.
  - **Urgencia Baja / Estable**: Verde bosque suave (`#388E3C`) para productos seguros o no perecederos.
- **Tipografía y Jerarquía**: Tipografía legible y moderna con escalas semánticas claras (`HeadlineMedium`, `TitleLarge`, `BodyLarge`, `LabelMedium`) y etiquetas de estado con insignias de alto contraste.
- **Navegación**: Barra de navegación estándar Material 3 en la parte inferior con 5 accesos principales y un menú superior/lateral complementario:
  1. **Inicio (Dashboard)**: Métricas clave, alerta principal del agente, alimento prioritario actual con recomendación activa y acceso rápido.
  2. **Inventario**: Lista completa filtrable (por estado, categoría, ubicación: refrigerador, congelador, despensa), buscador y formulario de registro/edición.
  3. **Prioridades**: Ranking calculado por la función de utilidad con desglose matemático transparente de factores.
  4. **Recetas**: Consulta de TheMealDB basada en el ingrediente prioritario, compatibilidad porcentual con la despensa actual, lista de ingredientes disponibles vs. faltantes.
  5. **Historial**: Registro inmutable de movimientos (altas, consumos parciales, consumos totales y descartes con motivos).
  6. **Evaluación**: Banco de 15 escenarios de prueba reproducibles para verificar la consistencia y precisión del agente.
  7. **Configuración**: Ajuste de ponderaciones de la función de utilidad ($w_1, w_2, w_3$), umbrales de prioridad y botón de restauración de despensa de demostración.

---

## 3. Decisiones de Producto y Función de Utilidad

### Formulación Matemática de la Función de Utilidad
La utilidad (urgencia de atención) de cada alimento $i$ se define como:

$$U(i) = w_1 \cdot P_{\text{vencimiento}}(i) + w_2 \cdot P_{\text{perecibilidad}}(i) + w_3 \cdot P_{\text{cantidad}}(i)$$

Sujeto a:
- $w_1 + w_2 + w_3 = 1$ (Valores predeterminados: $w_1 = 0.55$, $w_2 = 0.30$, $w_3 = 0.15$).
- **Proximidad del Vencimiento ($P_{\text{vencimiento}} \in [0, 1]$)**:
  - Si ya está vencido: $1.0$ (urgencia máxima de descarte o revisión inmediata).
  - Si vence hoy (0 días): $0.95$.
  - Si vence en $d$ días ($1 \le d \le 14$): $1.0 - \frac{d}{15}$.
  - Si vence en más de 14 días: valor residual decreciente hacia $0.05$.
  - Si la fecha es desconocida: $0.40$ (identificado explícitamente como dato con incertidumbre).
- **Perecibilidad ($P_{\text{perecibilidad}} \in [0, 1]$)**:
  - Clasificada por categorías estandarizadas:
    - Muy alta ($1.0$): Mariscos, pescados frescos, carnes crudas picadas, lácteos abiertos.
    - Alta ($0.80$): Carnes rojas/aves, verduras de hoja, bayas, yogur.
    - Media ($0.50$): Frutas de pulpa firme, quesos duros, huevos, tubérculos.
    - Baja ($0.20$): Panadería envasada, mermeladas, condimentos refrigerados.
    - Muy baja ($0.05$): Granos secos, arroz, pastas, enlatados, legumbres.
- **Impacto de Cantidad ($P_{\text{cantidad}} \in [0, 1]$)**:
  - Normalizada de forma sigmoide o lineal acotada para evitar que una gran cantidad anule el factor de vencimiento.
- **Regla de Desempate**: En caso de empate numérico idéntico en $U$, la prioridad se otorga al alimento con menor tiempo restante hasta el vencimiento, y secundariamente al de mayor perecibilidad.

### Determinación de la Acción
1. **Alerta Crítica / Descarte**: Si el alimento está vencido ($d < 0$).
2. **Proponer Receta de Aprovechamiento**: Si el alimento tiene urgencia Alta/Media ($U \ge 0.50$), quedan entre 0 y 5 días y es compatible con categorías culinarias.
3. **Sugerir Uso Próximo**: Si el producto requiere consumo pero no se localizan recetas adecuadas en la API o faltan la mayoría de los acompañamientos.
4. **Estado Seguro / Monitoreo**: Si la urgencia es Baja ($U < 0.50$).

---

## 4. Arquitectura Técnica y Estrategia de Datos

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                              │
│  DashboardScreen │ InventoryScreen │ RecipeScreen │ EvaluationScreen    │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Observe StateFlow / Send Events
┌────────────────────────────────────▼───────────────────────────────────┐
│                           FoodWiseViewModel                            │
│           - uiState: StateFlow<FoodWiseUiState>                        │
│           - Gestión de filtros, formularios, acciones de usuario       │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
┌──────────────────▼───────────────┐  ┌──────────────▼───────────────────┐
│     UtilityDecisionAgent         │  │       TheMealDbRepository        │
│  - Calcula U(i) determinista     │  │  - Retrofit + Moshi              │
│  - Ranking y desempates          │  │  - Diccionario Bilingüe ES-EN    │
│  - Acción recomendada            │  │  - Comparador de ingredientes    │
└──────────────────┬───────────────┘  └──────────────┬───────────────────┘
                   │                                 │
┌──────────────────▼─────────────────────────────────▼───────────────────┐
│                         FoodWiseRepository                             │
│                  Abstracción de operaciones y flujo                    │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Coroutines / Flow
┌────────────────────────────────────▼───────────────────────────────────┐
│                    Room Local Database (SQLite)                        │
│  Entities: FoodItemEntity, InventoryMovementEntity, AgentConfigEntity  │
└────────────────────────────────────────────────────────────────────────┘
```

### Entidades de Datos (Room)
1. **`FoodItemEntity`**:
   - `id: Long` (Auto-generado)
   - `name: String` (Nombre en español)
   - `nameEn: String?` (Traducción mapeada para búsqueda)
   - `quantity: Double`
   - `unit: String` (kg, g, unidades, l, ml, etc.)
   - `expirationDate: Long?` (Epoch millis o nulo si desconocida)
   - `acquisitionDate: Long`
   - `category: String` (Lácteos, Carnes, Vegetales, Frutas, Despensa, etc.)
   - `location: String` (Nevera, Congelador, Despensa)
   - `perishabilityLevel: String` (MUY_ALTA, ALTA, MEDIA, BAJA, MUY_BAJA)
   - `notes: String?`
   - `isArchived: Boolean`

2. **`InventoryMovementEntity`**:
   - `id: Long`
   - `foodItemId: Long`
   - `foodItemName: String`
   - `movementType: String` (CREACION, CONSUMO_PARCIAL, CONSUMO_TOTAL, DESCARTE)
   - `quantity: Double`
   - `unit: String`
   - `timestamp: Long`
   - `reason: String?`

3. **`AgentConfigEntity`**:
   - `wExpiration: Double` (0.55)
   - `wPerishability: Double` (0.30)
   - `wQuantity: Double` (0.15)
   - `highPriorityThreshold: Double` (0.70)
   - `mediumPriorityThreshold: Double` (0.45)

### Servicio TheMealDB e Interacción de Recetas
- **Endpoints de la API oficial**:
  - `GET api/json/v1/1/filter.php?i={ingredient}`: Búsqueda rápida por ingrediente principal.
  - `GET api/json/v1/1/lookup.php?i={mealId}`: Detalle completo de la receta, ingredientes, medidas e instrucciones.
- **Comparador de Compatibilidad**:
  - Extrae la lista de ingredientes de la receta (de `strIngredient1` a `strIngredient20`).
  - Normaliza cadenas (elimina tildes, puntuación, singular/plural).
  - Cruza los ingredientes con el inventario actual activo del hogar.
  - Clasifica cada ingrediente como: **Disponible en casa** o **Faltante**.
  - Calcula el índice de disponibilidad: $\frac{\text{Ingredientes Disponibles}}{\text{Ingredientes Totales Requeridos}} \times 100\%$.

### Módulo de Evaluación del Agente (15 Escenarios de Prueba)
Una pantalla interactiva dedicada que ejecuta de manera instantánea y reproducible los siguientes casos:
1. Alimento vencido hace 2 días (Debe ser prioridad máxima / alerta de descarte).
2. Alimento que vence hoy con perecibilidad alta (Puntuación crítica inmediata).
3. Alimento que vence mañana vs. gran cantidad que vence en 4 días (Verificación de ponderación de pesos).
4. Empate en fecha de vencimiento: desempate por perecibilidad (Yogur vs. Manzana).
5. Empate estricto de valores: desempate determinista por menor tiempo restante.
6. Alimento sin fecha de vencimiento (Manejo explícito de incertidumbre).
7. Alimentos no perecederos en gran cantidad (Arroz, legumbres secas).
8. Inventario vacío (Comportamiento controlado y mensaje explicativo).
9. Múltiples productos lácteos con diferentes fechas de caducidad.
10. Carne cruda que vence en 2 días vs. Queso curado que vence en 2 días.
11. Consumo parcial que reduce cantidad pero mantiene proximidad.
12. Descarte de producto vencido y actualización del ranking en tiempo real.
13. Consulta de TheMealDB con alimento no existente en la base culinaria (Manejo de respuesta vacía).
14. Consulta de TheMealDB con ingrediente principal común (Pollo / Tomate) y cálculo de ingredientes faltantes.
15. Verificación de invariancia matemática: a igualdad de otros factores, menor tiempo siempre produce mayor o igual urgencia.

---

## 5. Fases de Construcción y Verificación

1. **Configuración de Gradle & Manifiesto**: Habilitar permisos de Internet, dependencias activas de Room, Retrofit, Moshi y Coil.
2. **Capa de Dominio y Agente de Utilidad**: Implementar `UtilityDecisionAgent.kt`, modelos de datos, cálculo determinista y motor de desempates con pruebas unitarias.
3. **Capa de Datos Local (Room)**: Implementar DAOs, Database, repositorios y precarga de despensa de demostración.
4. **Capa de Red (TheMealDB)**: Implementar clientes Retrofit, interfaz de API, deserializadores y normalizador bilingüe de ingredientes.
5. **Capa de Presentación (Jetpack Compose M3)**:
   - Configuración de tema verde sostenible (`Theme.kt`, `Color.kt`).
   - `DashboardScreen`: Alertas del agente, resumen numérico y recomendación activa.
   - `InventoryScreen`: Lista reactiva, filtros, creación, edición, consumo y descarte.
   - `PrioritiesScreen`: Ranking de urgencia con desglose explicativo de la función de utilidad.
   - `RecipesScreen`: Detalle de recetas recomendadas con ingredientes disponibles y faltantes.
   - `HistoryScreen`: Trazabilidad completa de consumos y mermas.
   - `EvaluationScreen`: Banco de 15 pruebas reproducibles con porcentajes de éxito y métricas explicadas.
   - `SettingsScreen`: Calibrador de pesos del agente de utilidad.
6. **Compilación y Validación**: Ejecutar `compile_applet` para verificar la ausencia total de errores de compilación y validar el comportamiento reactivo.
