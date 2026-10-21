package com.example.mobiilisaasovellus.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mobiilisaasovellus.ui.theme.AppTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.round

// data class 7 päivän sääennusteelle
data class DailyForecast(
    val dayName: String,
    val condition: String,
    val icon: ImageVector,
    val maxTemp: Int,
    val minTemp: Int,
    val precipitation: Float
)

// data class nykyiselle säälle
data class WeatherDetail(
    val label: String,
    val value: String,
    val icon: ImageVector,
    // tuulikuvakkeen rotation muuttuu tuulensuunnan mukaan
    val rotation: Float = 0f
)

// tekstit ja kuvakkeet nykyiselle säälle
data class CurrentWeatherState(
    val locationName: String = "Haetaan sijaintia...",
    val temperature: String = "--°C",
    val condition: String = "Ladataan...",
    val humidity: String = "--%",
    val windSpeed: String = "-- m/s",
    val windDirection: Float = 0f,
    val icon: ImageVector = Icons.Default.WbSunny,
    val forecastList: List<DailyForecast> = emptyList()
)

// data class nykyiselle säälle sää apista
data class OpenMeteoCurrent(
    val temperature_2m: Double?,
    val relative_humidity_2m: Double?,
    val weather_code: Int?,
    val wind_speed_10m: Double?,
    val wind_direction_10m: Double?
)

// data class 7 päivän sääennusteelle apista
data class OpenMeteoDaily(
    val time: List<String>?,
    val weather_code: List<Int>?,
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?,
    val precipitation_sum: List<Double>?
)

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoCurrent?,
    val daily: OpenMeteoDaily?
)

// geocoding API data class
data class GeocodingLocation(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?
)

data class GeocodingResponse(
    val results: List<GeocodingLocation>?
)

// hae säätiedot
interface ApiService {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

// hae sijainti nimen perusteella
interface GeocodingApiService {
    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "fi",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}

object GeocodingRetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: GeocodingApiService = retrofit.create(GeocodingApiService::class.java)
}

// viikonpäivän nimet
fun formatDayLabel(index: Int, dateString: String?): String {
    val locale = Locale.forLanguageTag("fi-FI")
    val todayCal = Calendar.getInstance()
    todayCal.set(Calendar.HOUR_OF_DAY, 0)
    todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0)
    todayCal.set(Calendar.MILLISECOND, 0)

    val targetCal = Calendar.getInstance()
    var parsed = false
    if (!dateString.isNullOrEmpty()) {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val date = inputFormat.parse(dateString)
            if (date != null) {
                targetCal.time = date
                targetCal.set(Calendar.HOUR_OF_DAY, 0)
                targetCal.set(Calendar.MINUTE, 0)
                targetCal.set(Calendar.SECOND, 0)
                targetCal.set(Calendar.MILLISECOND, 0)
                parsed = true
            }
        } catch (_: Exception) { }
    }

    if (!parsed) {
        targetCal.time = todayCal.time
        targetCal.add(Calendar.DAY_OF_YEAR, index)
    }

    val diffDays = ((targetCal.timeInMillis - todayCal.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

    return when (diffDays) {
        0 -> "Tänään"
        1 -> "Huomenna"
        else -> {
            val dayNameFormat = SimpleDateFormat("EEEE", locale)
            val dayName = dayNameFormat.format(targetCal.time)
            dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }
}

// yhdistä sääkoodit tekstiin ja kuvakkeisiin
fun mapWeatherCode(code: Int?): Pair<String, ImageVector> {
    return when (code) {
        0 -> Pair("Selkeää", Icons.Default.WbSunny)
        1, 2 -> Pair("Melko selkeää", Icons.Default.WbSunny)
        3 -> Pair("Pilvistä", Icons.Default.Cloud)
        45, 48 -> Pair("Sumua", Icons.Default.Dehaze)
        51, 53, 55 -> Pair("Tihkusadetta", Icons.Default.WaterDrop)
        61, 63, 65 -> Pair("Sadetta", Icons.Default.WaterDrop)
        71, 73, 75, 77 -> Pair("Lumisadetta", Icons.Default.AcUnit)
        80, 81, 82 -> Pair("Sadekuuroja", Icons.Default.WaterDrop)
        95, 96, 99 -> Pair("Ukkosta", Icons.Default.Thunderstorm)
        else -> Pair("Puolipilvistä", Icons.Default.Cloud)
    }
}

// päänäkymä
@Composable
fun WeatherScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var selectedBottomTab by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodingLocation>>(emptyList()) }
    var isCurrentLocationFavorited by remember { mutableStateOf(false) }

    var userLocation by remember { mutableStateOf<Location?>(null) }
    var weatherState by remember { mutableStateOf(CurrentWeatherState()) }

    // hae säätiedot
    fun fetchWeatherForCoordinates(lat: Double, lng: Double, locationName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getWeather(latitude = lat, longitude = lng)
                val current = response.current
                val (conditionText, icon) = mapWeatherCode(current?.weather_code)

                val dailyList = mutableListOf<DailyForecast>()

                // lisää 7 päivän sääennusteeseen päivän tiedot
                response.daily?.let { daily ->
                    val times = daily.time ?: emptyList()
                    for (i in times.indices) {
                        val dayLabel = formatDayLabel(i, times.getOrNull(i))
                        val code = daily.weather_code?.getOrNull(i)
                        val (dayCondition, dayIcon) = mapWeatherCode(code)
                        val maxT = daily.temperature_2m_max?.getOrNull(i)?.toInt() ?: 0
                        val minT = daily.temperature_2m_min?.getOrNull(i)?.toInt() ?: 0
                        val precip = daily.precipitation_sum?.getOrNull(i)?.toFloat() ?: 0f

                        dailyList.add(
                            DailyForecast(
                                dayName = dayLabel,
                                condition = dayCondition,
                                icon = dayIcon,
                                maxTemp = maxT,
                                minTemp = minT,
                                precipitation = precip
                            )
                        )
                    }
                }

                val windSpeedTemp = current?.wind_speed_10m ?: 0.0
                val windSpeedMetersPerSecond = windSpeedTemp / 3.6
                val roundedWindSpeed = round(windSpeedMetersPerSecond * 10) / 10

                // nykyinen sää
                withContext(Dispatchers.Main) {
                    weatherState = CurrentWeatherState(
                        locationName = locationName,
                        temperature = "${current?.temperature_2m?.toInt() ?: 0}°C",
                        condition = conditionText,
                        humidity = "${current?.relative_humidity_2m?.toInt() ?: 0}%",
                        windSpeed = "$roundedWindSpeed m/s",
                        windDirection = current?.wind_direction_10m?.toFloat() ?: 0f,
                        icon = icon,
                        forecastList = if (dailyList.isNotEmpty()) dailyList else mockForecastList()
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    weatherState = weatherState.copy(
                        locationName = locationName,
                        condition = "Virhe säätietojen haussa"
                    )
                }
            }
        }
    }

    // geocoding haku
    LaunchedEffect(searchQuery) {
        // 300ms viive välttääkseen turhia api pyyntöjä
        if (searchQuery.trim().length >= 2) {
            delay(300L)
            try {
                val response = GeocodingRetrofitClient.api.searchLocations(name = searchQuery.trim())
                searchResults = response.results ?: emptyList()
            } catch (e: Exception) {
                searchResults = emptyList()
            }
        } else {
            searchResults = emptyList()
        }
    }

    val onLocationSelected: (GeocodingLocation) -> Unit = { location ->
        val displayName = buildString {
            append(location.name)
            if (!location.admin1.isNullOrEmpty()) {
                append(", ${location.admin1}")
            } else if (!location.country.isNullOrEmpty()) {
                append(", ${location.country}")
            }
        }
        searchQuery = ""
        searchResults = emptyList()
        fetchWeatherForCoordinates(location.latitude, location.longitude, displayName)
    }

    // hae sijainnin paikan nimi
    fun getCityNameAndFetchWeather(lat: Double, lng: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            var cityName = "Nykyinen sijainti"
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        val city = addresses.firstOrNull()?.locality
                            ?: addresses.firstOrNull()?.subAdminArea
                            ?: addresses.firstOrNull()?.adminArea
                        if (!city.isNullOrEmpty()) {
                            cityName = city
                        }
                        fetchWeatherForCoordinates(lat, lng, cityName)
                    }
                    return@launch
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val city = addresses?.firstOrNull()?.locality
                        ?: addresses?.firstOrNull()?.subAdminArea
                        ?: addresses?.firstOrNull()?.adminArea
                    if (!city.isNullOrEmpty()) {
                        cityName = city
                    }
                }
            } catch (e: Exception) {
                // fallback oletusnimeen
            }
            fetchWeatherForCoordinates(lat, lng, cityName)
        }
    }

    // hae käyttäjän viimeisin tunnettu sijainti
    fun fetchLastKnownLocation() {
        // onko lupa sijaintiin
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        userLocation = location
                        getCityNameAndFetchWeather(location.latitude, location.longitude)
                    } else {
                        val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        fusedLocationClient.getCurrentLocation(priority, CancellationTokenSource().token)
                            .addOnSuccessListener { currentLocation: Location? ->
                                if (currentLocation != null) {
                                    userLocation = currentLocation
                                    getCityNameAndFetchWeather(currentLocation.latitude, currentLocation.longitude)
                                } else {
                                    weatherState = weatherState.copy(locationName = "Sijaintia ei saatavilla")
                                }
                            }
                            .addOnFailureListener {
                                weatherState = weatherState.copy(locationName = "Sijainnin haku epäonnistui")
                            }
                    }
                }
                .addOnFailureListener {
                    weatherState = weatherState.copy(locationName = "Sijainnin haku epäonnistui")
                }
        } else {
            weatherState = weatherState.copy(locationName = "Ei sijaintilupaa")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLastKnownLocation()
        } else {
            weatherState = weatherState.copy(locationName = "Sijaintilupa evätty")
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchLastKnownLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            WeatherTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onFavoritesClick = { /* */ },
                searchResults = searchResults,
                onLocationSelected = onLocationSelected
            )
        },
        bottomBar = {
            WeatherBottomNavigationBar(
                selectedTab = selectedBottomTab,
                onTabSelected = { selectedBottomTab = it },
                onFavoriteCurrentLocationClick = {
                    isCurrentLocationFavorited = !isCurrentLocationFavorited
                },
                isFavorited = isCurrentLocationFavorited
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sääkortti
            CurrentWeatherCard(
                location = weatherState.locationName,
                temperature = weatherState.temperature,
                condition = weatherState.condition,
                icon = weatherState.icon,
                details = listOf(
                    WeatherDetail("Kosteus", weatherState.humidity, Icons.Default.Opacity),
                    WeatherDetail("Tuuli", weatherState.windSpeed, Icons.Filled.South, weatherState.windDirection)
                )
            )

            // 7 päivän sääennuste
            Forecast7DaySection(
                forecastList = if (weatherState.forecastList.isNotEmpty()) weatherState.forecastList else mockForecastList()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// yläpalkki
@Composable
fun WeatherTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFavoritesClick: () -> Unit,
    searchResults: List<GeocodingLocation>,
    onLocationSelected: (GeocodingLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Etsi kaupunkia...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Etsi kaupunkia"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            searchResults.firstOrNull()?.let { onLocationSelected(it) }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                IconButton(
                    onClick = onFavoritesClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Suosikit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        searchResults.forEach { location ->
                            val subText = buildString {
                                if (!location.admin1.isNullOrEmpty()) append(location.admin1)
                                if (!location.country.isNullOrEmpty()) {
                                    if (isNotEmpty()) append(", ")
                                    append(location.country)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLocationSelected(location) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = location.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (subText.isNotEmpty()) {
                                        Text(
                                            text = subText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// nykyisen sään kortti
@Composable
fun CurrentWeatherCard(
    location: String,
    temperature: String,
    condition: String,
    icon: ImageVector = Icons.Default.WbSunny,
    details: List<WeatherDetail>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = condition,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = temperature,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayLarge
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = condition,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                details.forEach { detail ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = detail.icon,
                            contentDescription = detail.label,
                            modifier = Modifier
                                .size(24.dp)
                                // tuulen suunta
                                .rotate(detail.rotation)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = detail.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = detail.value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 7 päivän ennuste
@Composable
fun Forecast7DaySection(
    forecastList: List<DailyForecast>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "7 päivän ennuste",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            forecastList.forEachIndexed { index, forecast ->
                ForecastRowItem(forecast = forecast)
                if (index < forecastList.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}

// 7 päivän ennusteen rivi
@Composable
fun ForecastRowItem(
    forecast: DailyForecast,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = forecast.icon,
                contentDescription = forecast.condition,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.surface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = forecast.dayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = forecast.condition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (forecast.precipitation > 0) {
            Row(
                modifier = Modifier.weight(0.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${forecast.precipitation} mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(0.6f))
        }

        Row(
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${forecast.maxTemp}°",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${forecast.minTemp}°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// alapalkki
@Composable
fun WeatherBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onFavoriteCurrentLocationClick: () -> Unit,
    isFavorited: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Kartta"
                )
            },
            label = { Text("Kartta") }
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Sää"
                )
            },
            label = { Text("Sää") }
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = {
                onFavoriteCurrentLocationClick()
            },
            icon = {
                Icon(
                    imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Suosikki",
                    tint = if (isFavorited) Color(0xFFF8383E) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            label = { Text("Suosikki") }
        )
    }
}

// placeholder sääennusteelle
private fun mockForecastList(): List<DailyForecast> {
    return listOf(
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f),
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f),
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f),
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f),
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f),
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f),
        DailyForecast("---", "---", Icons.AutoMirrored.Filled.ShortText, 0, 0, 0f)
    )
}

@Preview(showBackground = true)
@Composable
fun WeatherScreenPreview() {
    AppTheme {
        WeatherScreen()
    }
}
