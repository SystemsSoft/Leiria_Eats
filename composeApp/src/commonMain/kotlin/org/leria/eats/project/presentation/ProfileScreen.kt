package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.UserProfile
import org.leria.eats.project.data.saveImageLocally
import org.leria.eats.project.theme.*

// ─── Função auxiliar para limpar texto para TTS ──────────────────────────────
/**
 * Prepara o texto para Text-to-Speech:
 * - Remove emojis e símbolos especiais
 * - Substitui "x1" por "uma"
 * - Normaliza espaços
 */
private fun prepareTextForTts(text: String): String =
    text
        // Remove emojis e símbolos Unicode
        .replace(Regex("[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}]"), "")
        .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "") // surrogate emoji pairs
        .replace(Regex("[\u2600-\u27FF]"), "")  // misc symbols, dingbats, arrows
        .replace(Regex("[\uFE00-\uFE0F]"), "")  // variation selectors
        // Substitui x1 por "uma"
        .replace(Regex("\\bx1\\b", RegexOption.IGNORE_CASE), "uma")
        // Normaliza espaços múltiplos
        .replace(Regex("\\s{2,}"), " ")
        .trim()

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val PDeepBg  = KomaBg
private val PCard    = KomaCard
private val PGold    = KomaGold
private val PGreen   = KomaBrandGreen
private val PText    = KomaTextPrimary
private val PMuted   = KomaTextSec

// ─── Validação de Email / Telefone ───────────────────────────────────────────
// Campo aceitava qualquer texto (ex.: "não quero te mandar") como email — sem
// confirmação de conta/pedido válida, isso quebra o fluxo de entrega.
private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+\\.[A-Za-z]{2,}$")

private fun isValidEmail(value: String): Boolean =
    value.isBlank() || emailRegex.matches(value.trim())

private fun isValidPhone(value: String): Boolean {
    if (value.isBlank()) return true
    val digits = value.filter { it.isDigit() }
    return digits.length in 9..15
}

// ─── Alergias / Estilos de Vida: seleção múltipla em vez de texto livre ──────
// Texto livre aceitava qualquer coisa (ex.: "indigna", "sal") sem estrutura que
// a IA/filtros de cardápio pudessem usar com confiança. As opções abaixo cobrem
// os casos mais comuns; "Outro" mantém um campo de texto livre para o resto.
private val allergyOptions = listOf(
    "Amendoim", "Frutos secos", "Marisco/Crustáceos", "Peixe",
    "Ovo", "Leite/Lactose", "Glúten", "Soja", "Sésamo"
)
private val lifestyleOptions = listOf(
    "Vegano", "Vegetariano", "Keto", "Sem Lactose",
    "Sem Glúten", "Halal", "Kosher", "Low Carb"
)

// Separa uma string "Amendoim, sal, Ovo" nas opções reconhecidas (com a grafia
// padronizada da lista) e no restante, tratado como texto livre em "Outro".
private fun splitProfileTags(raw: String, options: List<String>): Pair<Set<String>, String> {
    val parts = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val matched = parts.mapNotNull { part -> options.firstOrNull { it.equals(part, ignoreCase = true) } }.toSet()
    val custom = parts.filterNot { part -> options.any { it.equals(part, ignoreCase = true) } }
    return matched to custom.joinToString(", ")
}

private fun joinProfileTags(selected: Set<String>, custom: String): String {
    val customParts = custom.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return (selected.toList() + customParts).joinToString(", ")
}

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onSave: (String, String, String, List<Address>, String?, String, String) -> Unit,
    onGetLocation: ((String, Double?, Double?) -> Unit) -> Unit,
    onGetAddressFromMap: (Double, Double) -> String?,
    isMuted: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var selectedAllergies by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customAllergies by remember { mutableStateOf("") }
    var selectedLifestyles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customLifestyles by remember { mutableStateOf("") }
    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var showMapDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var addressToEdit by remember { mutableStateOf<Address?>(null) }
    var showValidationError by remember { mutableStateOf(false) }

    val emailError = showValidationError && !isValidEmail(email)
    val phoneError = showValidationError && !isValidPhone(phone)

    val scope = rememberCoroutineScope()

    LaunchedEffect(userProfile) {
        if (userProfile.name.isNotEmpty()) name = userProfile.name
        if (userProfile.email.isNotEmpty()) email = userProfile.email
        if (userProfile.phone.isNotEmpty()) phone = userProfile.phone
        photoUrl = userProfile.photoUrl
        val (allergyMatches, allergyCustom) = splitProfileTags(userProfile.allergies, allergyOptions)
        selectedAllergies = allergyMatches
        customAllergies = allergyCustom
        val (lifestyleMatches, lifestyleCustom) = splitProfileTags(userProfile.lifestyles, lifestyleOptions)
        selectedLifestyles = lifestyleMatches
        customLifestyles = lifestyleCustom
        if (userProfile.addresses.isNotEmpty()) addresses = userProfile.addresses
    }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Selecionar Foto de Perfil"
    ) { platformFile ->
        if (platformFile != null) {
            scope.launch {
                val savedPath = saveImageLocally(platformFile)
                if (savedPath.isNotEmpty()) {
                    photoUrl = savedPath
                }
            }
        }
    }

    var pendingMapCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(pendingMapCoords) {
        val coords = pendingMapCoords ?: return@LaunchedEffect
        val selectedAddress = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            onGetAddressFromMap(coords.first, coords.second)
        }
        if (selectedAddress != null) {
            addresses = addresses + Address("Novo Endereço do Mapa", selectedAddress, latitude = coords.first, longitude = coords.second)
        }
        pendingMapCoords = null
    }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                pendingMapCoords = Pair(lat, long)
                showMapDialog = false
            }
        )
    }

    if (showAddAddressDialog || addressToEdit != null) {
        AddressEntryDialog(
            address = addressToEdit,
            onDismiss = { showAddAddressDialog = false; addressToEdit = null },
            onSave = { newAddress ->
                if (addressToEdit != null) {
                    addresses = addresses.map { if (it == addressToEdit) newAddress else it }
                } else {
                    addresses = addresses + newAddress
                }
                showAddAddressDialog = false
                addressToEdit = null
            },
            onGetLocation = onGetLocation,
            onGetAddressFromMap = onGetAddressFromMap
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PDeepBg)
    ) {
        // Ambient glow top-right
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(PGold.copy(alpha = 0.05f), CircleShape)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── AI Assistant Message (only show when profile is not yet registered) ──
            if (userProfile.name.isEmpty() || userProfile.addresses.isEmpty()) {
                item {
                    ProfileAiChatBubble(
                        isMuted = isMuted,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
            }

            item {
                // ── Avatar ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            Brush.radialGradient(listOf(PGold.copy(alpha = 0.25f), Color.Transparent)),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(PGold, PGreen)),
                            CircleShape
                        )
                        .clickable { launcher.launch() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(PCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUrl.isNullOrEmpty()) {
                            val resourceData = org.leria.eats.project.data.mapPathToImageSource(photoUrl!!)
                            
                            KamelImage(
                                resource = asyncPainterResource(data = resourceData),
                                contentDescription = "Foto de Perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onLoading = { progress ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            color = PGold,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                onFailure = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PMuted,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = PMuted,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Botão de editar (overlay) — deslocado para fora da moldura circular do
                    // avatar (offset), senão a borda do avatar cortava o ícone da câmera.
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(PGold, CircleShape)
                            .border(2.dp, PDeepBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = KomaGoldOnDark, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Meu Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (name.isNotBlank()) name else "Utilizador KOMAAI",
                    fontSize = 12.sp, color = PMuted
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Section: dados ────────────────────────────────────────
                ProfileSectionLabel(label = "Dados Pessoais", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome Completo",
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    isError = emailError,
                    errorMessage = "Introduza um email válido (ex: nome@exemplo.com)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Telefone / WhatsApp",
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    isError = phoneError,
                    errorMessage = "Introduza um número de telefone válido"
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Section: Personalização Alimentar ──────────────────────
                ProfileSectionLabel(label = "Personalização Alimentar", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                ChipMultiSelectField(
                    label = "Alergias / Intolerâncias",
                    icon = Icons.Default.NoFood,
                    options = allergyOptions,
                    selected = selectedAllergies,
                    onSelectedChange = { selectedAllergies = it },
                    customValue = customAllergies,
                    onCustomValueChange = { customAllergies = it },
                    customPlaceholder = "Outra alergia/intolerância"
                )
                Spacer(modifier = Modifier.height(16.dp))
                ChipMultiSelectField(
                    label = "Estilos de Vida",
                    icon = Icons.Default.Restaurant,
                    options = lifestyleOptions,
                    selected = selectedLifestyles,
                    onSelectedChange = { selectedLifestyles = it },
                    customValue = customLifestyles,
                    onCustomValueChange = { customLifestyles = it },
                    customPlaceholder = "Outro estilo de vida"
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Section: endereços ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileSectionLabel(label = "Meus Endereços")
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PGold.copy(alpha = 0.15f))
                            .border(1.dp, PGold.copy(alpha = 0.4f), CircleShape)
                            .clickable { showAddAddressDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Endereço", tint = PGold, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (addresses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PCard)
                            .border(1.dp, PGold.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum endereço adicionado ainda.", color = PMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(addresses) { address ->
                    AddressItem(
                        address = address,
                        onEdit = { addressToEdit = it },
                        onDelete = { addresses = addresses - it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }


            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Save FAB ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showValidationError && (emailError || phoneError)) {
                Text(
                    "Corrija os campos destacados antes de guardar.",
                    color = KomaSoftRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(PGold, KomaOrangeEnd)))
                    .clickable {
                        if (isValidEmail(email) && isValidPhone(phone)) {
                            showValidationError = false
                            onSave(
                                name, email, phone, addresses, photoUrl,
                                joinProfileTags(selectedAllergies, customAllergies),
                                joinProfileTags(selectedLifestyles, customLifestyles)
                            )
                        } else {
                            showValidationError = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = KomaGoldOnDark, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Perfil ✦", color = KomaGoldOnDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionLabel(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(PGold, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PText)
    }
}

// Substitui o campo de texto livre de "Alergias"/"Estilos de Vida" por chips de
// seleção múltipla com opções padrão + "Outro" (texto livre só pro que não está
// na lista) — dado estruturado que a IA/filtros de cardápio conseguem usar.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipMultiSelectField(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    customValue: String,
    onCustomValueChange: (String) -> Unit,
    customPlaceholder: String
) {
    var showCustomField by remember(customValue) { mutableStateOf(customValue.isNotBlank()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = PMuted, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option in selected
                SelectableChip(
                    label = option,
                    selected = isSelected,
                    onClick = {
                        onSelectedChange(if (isSelected) selected - option else selected + option)
                    }
                )
            }
            SelectableChip(
                label = "Outro",
                selected = showCustomField,
                onClick = {
                    if (showCustomField) {
                        showCustomField = false
                        onCustomValueChange("")
                    } else {
                        showCustomField = true
                    }
                }
            )
        }
        if (showCustomField) {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileTextField(
                value = customValue,
                onValueChange = onCustomValueChange,
                label = customPlaceholder
            )
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) PGold.copy(alpha = 0.18f) else PCard)
            .border(
                width = 1.dp,
                color = if (selected) PGold else PGold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) PGold else PMuted
        )
        if (selected) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Close, contentDescription = "Remover $label", tint = PGold, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun AddressItem(
    address: Address,
    onEdit: (Address) -> Unit,
    onDelete: (Address) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PCard)
            .border(1.dp, PGold.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PGreen.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, PGreen.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        address.name,
                        fontWeight = FontWeight.Bold,
                        color = PText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(address.address, color = PMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }


                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PGold.copy(alpha = 0.1f))
                        .clickable { onEdit(address) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PGold, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(KomaSoftRed.copy(alpha = 0.1f))
                        .clickable { onDelete(address) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = KomaSoftRed, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEntryDialog(
    address: Address?,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit,
    onGetLocation: ((String, Double?, Double?) -> Unit) -> Unit,
    onGetAddressFromMap: (Double, Double) -> String?,
) {
    var name by remember { mutableStateOf(address?.name ?: "") }
    var addressValue by remember { mutableStateOf(address?.address ?: "") }
    var lat by remember { mutableStateOf(address?.latitude) }
    var lng by remember { mutableStateOf(address?.longitude) }
    var isLocating by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }

    var pendingMapCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(pendingMapCoords) {
        val coords = pendingMapCoords ?: return@LaunchedEffect
        val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            onGetAddressFromMap(coords.first, coords.second)
        }
        if (resolved != null) {
            addressValue = resolved
            lat = coords.first
            lng = coords.second
        }
        pendingMapCoords = null
    }

    // iOS: MapDialog uses a native UIViewController presented on top of everything,
    // so it works correctly even when called from inside an AlertDialog.
    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { mapLat, mapLong ->
                pendingMapCoords = Pair(mapLat, mapLong)
                showMapDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PCard,
        titleContentColor = PText,
        textContentColor = PMuted,
        title = {
            Text(
                if (address == null) "Adicionar Endereço" else "Editar Endereço",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome (ex: Casa)"
                )
                Spacer(modifier = Modifier.height(10.dp))
                ProfileTextField(
                    value = addressValue,
                    onValueChange = { addressValue = it },                    label = "Endereço",
                    trailingContent = {
                        Row {
                            // Ícones sem rótulo visível — tooltip explica a ação ao tocar e
                            // segurar (ou passar o mouse, no desktop), sem precisar de texto
                            // fixo que não caberia no espaço compacto do campo.
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Usar a minha localização atual") } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (!isLocating) PGold.copy(alpha = 0.15f) else PCard)
                                        .clickable(enabled = !isLocating) {
                                            isLocating = true
                                            onGetLocation { foundAddress, foundLat, foundLng ->
                                                isLocating = false
                                                if (foundAddress.isNotEmpty()) {
                                                    addressValue = foundAddress
                                                    lat = foundLat
                                                    lng = foundLng
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLocating)
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PGold, strokeWidth = 2.dp)
                                    else
                                        Icon(Icons.Default.Home, contentDescription = "Usar a minha localização atual", tint = PGold, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Selecionar no mapa") } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PGreen.copy(alpha = 0.15f))
                                        .clickable { showMapDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Selecionar no mapa", tint = PGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(PGold, KomaOrangeEnd)))
                    .clickable {
                        onSave(Address(
                            name = name.ifEmpty { "Endereço Sem Nome" },
                            address = addressValue,
                            isDefault = false,
                            latitude = lat,
                            longitude = lng
                        ))
                    }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("Guardar", color = KomaGoldOnDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PMuted)
            }
        }
    )
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PCard)
            .border(1.dp, if (isError) KomaSoftRed.copy(alpha = 0.6f) else PGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = PMuted, modifier = Modifier.size(18.dp)) } },
            trailingIcon = trailingContent,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = isError,
            supportingText = if (isError && errorMessage != null) {
                { Text(errorMessage, color = KomaSoftRed, fontSize = 11.sp) }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedTextColor = PText,
                unfocusedTextColor = PText,
                cursorColor = PGold,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLabelColor = PGold,
                unfocusedLabelColor = PMuted,
                errorContainerColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                errorTextColor = PText,
                errorLabelColor = KomaSoftRed,
                errorSupportingTextColor = KomaSoftRed
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProfileAiChatBubble(
    isMuted: Boolean = false,
    modifier: Modifier = Modifier,
    tts: org.leria.eats.project.voice.TextToSpeechService = org.koin.compose.koinInject()
) {
    val fullMessage = buildString {
        append("Para começar a pedir, preencha o seu nome,email, telefone e adicione pelo menos um endereço de entrega.\n\n")
        append("É rápido, simples e só precisa de fazer uma vez!")
    }

    val displayedText = remember { mutableStateOf("") }

    // Typewriter animation (sem TTS: esta mensagem é proativa, não vem de interação
    // por voz — a voz da IA fica restrita ao microfone/ligação de voz)
    LaunchedEffect(Unit) {
        // Typewriter effect
        for (i in fullMessage.indices) {
            displayedText.value = fullMessage.substring(0, i + 1)
            kotlinx.coroutines.delay(8) // Faster animation
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        PGold.copy(alpha = 0.15f),
                        PGreen.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        PGold.copy(alpha = 0.4f),
                        PGreen.copy(alpha = 0.3f)
                    )
                ),
                RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PGold.copy(alpha = 0.3f), PGreen.copy(alpha = 0.2f))
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, PGold.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✨",
                    fontSize = 22.sp
                )
            }

            // Message content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "KOMA AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PGold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    displayedText.value,
                    fontSize = 13.sp,
                    color = PText.copy(alpha = 0.95f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
expect fun MapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
)