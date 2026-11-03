package com.example.data.network

import java.text.Normalizer
import java.util.Locale

object IngredientTranslator {

    private val spanishToEnglishMap: Map<String, String> = mapOf(
        // Carnes y aves
        "pollo" to "chicken",
        "pechuga de pollo" to "chicken",
        "muslo de pollo" to "chicken",
        "carne" to "beef",
        "carne picada" to "minced beef",
        "res" to "beef",
        "ternera" to "beef",
        "cerdo" to "pork",
        "lomo" to "pork",
        "tocino" to "bacon",
        "panceta" to "bacon",
        "jamon" to "ham",
        "pavo" to "turkey",
        "cordero" to "lamb",

        // Pescados y mariscos
        "pescado" to "fish",
        "atun" to "tuna",
        "salmon" to "salmon",
        "bacalao" to "cod",
        "camaron" to "prawns",
        "camarones" to "prawns",
        "gamba" to "prawns",
        "gambas" to "prawns",

        // Lácteos y derivados
        "leche" to "milk",
        "leche entera" to "milk",
        "leche descremada" to "milk",
        "yogur" to "yogurt",
        "yogur natural" to "yogurt",
        "yogur griego" to "yogurt",
        "queso" to "cheese",
        "queso curado" to "cheese",
        "queso rallado" to "cheese",
        "queso parmesano" to "parmesan",
        "queso mozzarella" to "mozzarella",
        "queso cheddar" to "cheddar",
        "mantequilla" to "butter",
        "crema" to "cream",
        "nata" to "cream",
        "huevo" to "egg",
        "huevos" to "egg",

        // Verduras y hortalizas
        "tomate" to "tomato",
        "tomates" to "tomato",
        "cebolla" to "onion",
        "cebollas" to "onion",
        "ajo" to "garlic",
        "ajos" to "garlic",
        "papa" to "potato",
        "papas" to "potato",
        "patata" to "potato",
        "patatas" to "potato",
        "zanahoria" to "carrot",
        "zanahorias" to "carrot",
        "espinaca" to "spinach",
        "espinacas" to "spinach",
        "lechuga" to "lettuce",
        "pimiento" to "pepper",
        "pimientos" to "pepper",
        "champiñon" to "mushrooms",
        "champiñones" to "mushrooms",
        "setas" to "mushrooms",
        "calabacin" to "zucchini",
        "pepino" to "cucumber",
        "brocoli" to "broccoli",
        "apio" to "celery",
        "aguacate" to "avocado",
        "palta" to "avocado",

        // Granos, pastas y legumbres
        "arroz" to "rice",
        "arroz blanco" to "rice",
        "pasta" to "pasta",
        "fideos" to "noodles",
        "espaguetis" to "spaghetti",
        "macarrones" to "macaroni",
        "harina" to "flour",
        "pan" to "bread",
        "lentejas" to "lentils",
        "garbanzos" to "chickpeas",
        "frijoles" to "beans",
        "alubias" to "beans",
        "avena" to "oats",

        // Frutas
        "manzana" to "apple",
        "manzanas" to "apple",
        "platano" to "banana",
        "banana" to "banana",
        "naranja" to "orange",
        "naranjas" to "orange",
        "limon" to "lemon",
        "limones" to "lemon",
        "fresa" to "strawberries",
        "fresas" to "strawberries"
    )

    /**
     * Normaliza un texto removiendo acentos, caracteres especiales y mayúsculas.
     */
    fun normalize(text: String): String {
        val trimmed = text.trim().lowercase(Locale.ROOT)
        val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    /**
     * Traduce un nombre en español al término en inglés compatible con TheMealDB.
     * Si no se encuentra mapeo directo, busca si el texto contiene alguna clave conocida.
     * Si falla, retorna el término normalizado original.
     */
    fun translateToEnglish(spanishName: String): String {
        val clean = normalize(spanishName)

        // 1. Coincidencia exacta
        spanishToEnglishMap[clean]?.let { return it }

        // 2. Coincidencia por subcadena
        for ((esKey, enValue) in spanishToEnglishMap) {
            if (clean.contains(esKey) || esKey.contains(clean)) {
                return enValue
            }
        }

        // 3. Fallback: devolver el término limpio
        return clean
    }

    /**
     * Comprueba si un ingrediente de receta en inglés coincide con algún alimento del inventario del usuario.
     */
    fun matchesInventory(recipeIngredientEn: String, userFoodName: String, userFoodNameEn: String?): Boolean {
        val cleanRecipe = normalize(recipeIngredientEn)
        val cleanUserEs = normalize(userFoodName)
        val translated = userFoodNameEn?.let { normalize(it) } ?: translateToEnglish(cleanUserEs)

        if (cleanRecipe.isEmpty()) return false

        // Coincidencia con la traducción al inglés
        if (cleanRecipe == translated || cleanRecipe.contains(translated) || translated.contains(cleanRecipe)) {
            return true
        }

        // Coincidencia directa con el nombre en español (por ejemplo "pasta", "arroz", "tuna")
        if (cleanRecipe == cleanUserEs || cleanRecipe.contains(cleanUserEs) || cleanUserEs.contains(cleanRecipe)) {
            return true
        }

        return false
    }
}
