# FoodWIse — Gestor Inteligente de Desperdicio Alimentario Doméstico

**FoodWIse** es una aplicación móvil nativa para Android (Kotlin + Jetpack Compose) diseñada para abordar el problema del desperdicio de alimentos en los hogares mediante la gestión activa del inventario doméstico, la priorización matemática basada en utilidad y la recomendación oportuna de recetas culinarias de aprovechamiento.

---

## 1. Decisión Central del Agente Inteligente

> **“El agente decide, en cada momento, qué alimento del hogar requiere atención primero y qué acción recomendar sobre él —alertar o proponer una receta— para evitar que se desperdicie”.**

Toda la arquitectura y lógica del sistema responde a esta definición fundacional.

---

## 2. Clasificación y Justificación Académica: Agente Basado en Utilidad

FoodWIse se clasifica formal y académicamente como un **Agente Basado en Utilidad** (Utility-Based Agent).

### ¿Por qué basado en utilidad y no en objetivos o búsqueda?
1. **Factores en competencia**: En una despensa real, los criterios compiten continuamente: un yogur perecedero que vence mañana vs. una cantidad grande (4 kg) de tomates que vencen en tres días. Un sistema puramente binario ("vencido / no vencido") o basado en metas estáticas no permite discriminar cuál producto genera mayor riesgo neto de desperdicio.
2. **Escala continua de urgencia**: El agente calcula una puntuación cuantitativa $U(i) \in [0.0, 1.0]$ que mide el grado relativo de urgencia y permite generar un orden total estricto sobre el inventario.
3. **Diferencia frente a algoritmos de búsqueda**: El agente no explora un grafo de estados espaciales con algoritmos como BFS, DFS, Dijkstra o A*, ya que no planifica una secuencia de transiciones hacia un estado meta. La consulta a la API de TheMealDB es una consulta a una fuente externa de información, no una búsqueda de caminos de IA.
4. **Entorno parcialmente observable y dinámico**: El agente solo percibe los datos registrados por el usuario en el inventario local; desconoce el estado microbiológico invisible de los alimentos. El inventario cambia en tiempo real con las compras, consumos y descartes.

---

## 3. Arquitectura en Tres Capas Separadas

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose M3 UI                           │
│  Dashboard │ Inventario │ Prioridades │ Recetas │ Historial │ Pruebas  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Eventos / StateFlow
┌────────────────────────────────────▼───────────────────────────────────┐
│                           FoodWiseViewModel                            │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
┌──────────────────▼───────────────┐  ┌──────────────▼───────────────────┐
│     CAPA 1: Decisión del Agente  │  │   CAPA 2: Ejecución de la Acción │
│        (Función de Utilidad)     │  │          (TheMealDB API)         │
│  - Cálculo determinista U(i)     │  │  - Consultas REST con Retrofit   │
│  - Desempates lexicográficos     │  │  - Normalizador Bilingüe ES-EN   │
│  - Axioma de monotonicidad       │  │  - Comparador de ingredientes    │
└──────────────────┬───────────────┘  └──────────────┬───────────────────┘
                   │                                 │
┌──────────────────▼─────────────────────────────────▼───────────────────┐
│              CAPA 3: Persistencia y Trazabilidad Inmutable            │
│                 Room Database (SQLite) / FoodWiseDao                   │
│   • food_items      • inventory_movements      • agent_config          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Función de Utilidad y Formulación Matemática

La utilidad (urgencia de atención) para un alimento $i$ se define como:

$$U(i) = w_1 \cdot P_{\text{vencimiento}}(i) + w_2 \cdot P_{\text{perecibilidad}}(i) + w_3 \cdot P_{\text{cantidad}}(i)$$

### Restricciones y Ponderaciones:
- $\sum_{j=1}^3 w_j = 1.0$ (Pesos calibrables predeterminados: $w_1 = 0.55$, $w_2 = 0.30$, $w_3 = 0.15$).
- **$P_{\text{vencimiento}} \in [0.0, 1.0]$**:
  - Alimento vencido ($d < 0$): $1.00$ (máxima urgencia de descarte o revisión).
  - Vence hoy ($d = 0$): $0.95$.
  - Vence en $1 \le d \le 14$ días: $1.00 - (d / 15.0)$.
  - Vence en $> 14$ días: decaimiento residual hacia $0.05$.
  - **Fecha desconocida**: $0.40$ (identificado explícitamente en la interfaz como dato con incertidumbre).
- **$P_{\text{perecibilidad}} \in [0.0, 1.0]$**:
  - `MUY_ALTA` ($1.00$): Mariscos, pescados crudos, lácteos abiertos.
  - `ALTA` ($0.80$): Carnes rojas, pollo fresco, verduras de hoja, yogur.
  - `MEDIA` ($0.50$): Frutas firmes, huevos, quesos curados, tubérculos.
  - `BAJA` ($0.20$): Panadería envasada, mermeladas, condimentos.
  - `MUY_BAJA` ($0.05$): Arroz, pastas secas, legumbres, enlatados.
- **$P_{\text{cantidad}} \in [0.0, 1.0]$**: Función normalizada acotada que previene que cantidades masivas anulen el factor de caducidad.
- **Regla de Desempate Determinista**: Si dos productos obtienen el mismo puntaje $U$, el desempate se resuelve por menor tiempo restante, luego por mayor perecibilidad y finalmente por ID primario.

### Acciones Recomendadas:
- `ALERTA_DESCARTE`: Si el alimento está vencido.
- `PROPONER_RECETA`: Si la prioridad es Alta ($U \ge 0.70$) y quedan entre 0 y 4 días.
- `SUGERIR_USO_PROXIMO`: Si la prioridad es Media o Alta pero no hay recetas viables.
- `MONITOREO_ESTABLE`: Si la prioridad es Baja ($U < 0.45$).

---

## 5. Integración con TheMealDB y Normalizador Bilingüe

- **Endpoints consumidos**:
  - `api/json/v1/1/filter.php?i={ingredient}`: Búsqueda rápida por ingrediente principal.
  - `api/json/v1/1/lookup.php?i={mealId}`: Extracción de ingredientes 1 a 20, medidas e instrucciones completas.
- **Normalizador Bilingüe**: Diccionario determinista que traduce nombres culinarios en español (p. ej. *Pechuga de Pollo* $\rightarrow$ *chicken*, *Tomates* $\rightarrow$ *tomato*, *Arroz* $\rightarrow$ *rice*) y gestiona tildes, plurales y mayúsculas.
- **Comparador de Despensa**: Clasifica los ingredientes de la receta en **Disponibles** (verdes) y **Faltantes** (ámbar), indicando el porcentaje de preparación posible y alertando si la receta aprovecha otros alimentos en riesgo.

---

## 6. Módulo de Evaluación y Pruebas Verificables

La aplicación incluye una pantalla interactiva con **15 escenarios de prueba académicos reproducibles**:
1. Alimento vencido hace 2 días (Alerta de descarte).
2. Alimento que vence hoy (Acción inmediata de receta).
3. Vencimiento inminente vs. mayor cantidad (Precedencia de tiempo).
4. Empate de caducidad resuelto por perecibilidad (Yogur vs. Manzanas).
5. Desempate determinista con orden invertido.
6. Manejo explícito de fecha desconocida (Incertidumbre).
7. Alimentos no perecederos en gran cantidad (Arroz).
8. Manejo de inventario vacío.
9. Monotonicidad en misma categoría de lácteos.
10. Carne fresca vs. queso curado con mismo vencimiento.
11. Invarianza matemática: menor tiempo nunca genera menor urgencia.
12. Sensibilidad a variaciones en los pesos $w_1, w_2, w_3$.
13. Normalizador bilingüe de ingredientes.
14. Discriminación entre insumos disponibles y faltantes.
15. Segmentación de umbrales continuo-discreto (Alta, Media, Baja).

---

## 7. Transparencia Científica

- **Sin Machine Learning Simulado**: La versión 1.0 no utiliza modelos de ML fingidos ni modifica automáticamente los pesos a partir del historial. Los pesos son ajustados exclusivamente por el usuario desde la pantalla de Configuración.
- **Sin Afirmaciones de Impacto No Probadas**: La aplicación no inventa porcentajes ficticios de "alimentos salvados". Las estadísticas provienen estrictamente de los registros reales de consumo y descarte.
