package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {

    /**
     * Busca recetas que incluyan un ingrediente principal en inglés.
     * Ejemplo: api/json/v1/1/filter.php?i=chicken
     */
    @GET("api/json/v1/1/filter.php")
    suspend fun searchMealsByIngredient(
        @Query("i") ingredient: String
    ): MealFilterResponse

    /**
     * Consulta los detalles completos de una receta por su identificador.
     * Ejemplo: api/json/v1/1/lookup.php?i=52772
     */
    @GET("api/json/v1/1/lookup.php")
    suspend fun getMealDetails(
        @Query("i") mealId: String
    ): MealLookupResponse
}
