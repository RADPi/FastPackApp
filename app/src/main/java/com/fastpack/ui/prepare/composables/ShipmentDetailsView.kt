package com.fastpack.ui.prepare.composables

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Para items en LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage // Para cargar imágenes desde URL
import coil.request.ImageRequest
import com.fastpack.BuildConfig
import com.fastpack.data.model.ShipmentResponse
import com.fastpack.data.model.ShippingItem // Asegúrate que esta clase esté accesible
import com.fastpack.ui.prepare.PrepareViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.fastpack.R
import java.util.Date
import kotlin.text.format

// Función para crear URI temporal
fun Context.createImageUri(): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        .format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(cacheDir, "images") // Usar cacheDir
    if (!storageDir.exists()) storageDir.mkdirs()
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
    return FileProvider.getUriForFile(
        this,
        "${BuildConfig.APPLICATION_ID}.provider", // Asegúrate que coincida con tu AndroidManifest
        imageFile
    )
}

@Composable
fun ShipmentDetailsView(
    shipment: ShipmentResponse,
    photoUri: Uri?, // URI de la foto tomada para preview
    isUploading: Boolean,
    onTakePhotoClick: (Uri) -> Unit,
    onConfirmPhotoClick: () -> Unit,
    onRetakePhotoClick: () -> Unit,
    onCancelPhotoClick: () -> Unit,
    viewModel: PrepareViewModel
) {
    val context = LocalContext.current
    var tempUriForCamera: Uri? =
        null // Para almacenar la URI antes de que la cámara la llene

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempUriForCamera?.let { uri ->
                    Log.d("ShipmentDetailsView", "Foto tomada con éxito: $uri")
                    onTakePhotoClick(uri) // Notifica al ViewModel con la URI llena
                }
            } else {
                Log.d("ShipmentDetailsView", "Toma de foto cancelada o fallida.")
                tempUriForCamera = null // Limpia si falla
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // Centra el título si es un item
    ) {
        item { // Título
            Text(
                text = "Envío ID: ${shipment.id}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            shipment.shippedItemsPhoto?.let { photoInfo ->
                Text(
                    text = "Foto del paquete ya registrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AsyncImage(
                    model = photoInfo.url,
                    contentDescription = "Foto del paquete",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit,
//                    placeholder = painterResource(id = R.drawable.placeholder_image), // Opcional: Reemplaza con tu placeholder
//                    error = painterResource(id = R.drawable.error_image) // Opcional: Reemplaza con tu imagen de error
                )
            }
        }

        if (shipment.shippingItems.isNullOrEmpty()) {
            item {
                Text("No se encontraron artículos para este envío.")
            }
        } else {
            items(shipment.shippingItems) { item -> // Itera sobre la lista de shippingItems
                ShippingItemCard(item = item)
            }
        }

        // Sección de Foto del Paquete
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Foto del Paquete",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (photoUri != null) {
                // Mostrar preview de la foto y botones de confirmación/rehacer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Vista previa de la foto del paquete",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // Ajusta la altura según necesites
                            .padding(bottom = 16.dp)
                            .background(Color.Gray),
                        contentScale = ContentScale.Fit
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onConfirmPhotoClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Guardar")
                            Spacer(Modifier.width(4.dp))
                            Text("Guardar")
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = onRetakePhotoClick,
                                enabled = !isUploading
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_redo_24),
                                    "Rehacer"
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Rehacer")
                            }
                            if (isUploading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CircularProgressIndicator()
                            }
                            OutlinedButton(
                                onClick = onCancelPhotoClick,
                                enabled = !isUploading,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_no_photography_24),
                                    contentDescription = "Cancelar"
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Cancelar")
                            }
                        }
                    }
                }
            } else {
                // Botón para tomar la foto (solo si no hay una URL de foto existente en el shipment)
                if (shipment.shippedItemsPhoto?.url.isNullOrEmpty()) {
                    Button(
                        onClick = {
                            val newUri = context.createImageUri()
                            tempUriForCamera = newUri
                            cameraLauncher.launch(newUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                            contentDescription = "Cancelar"
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Tomar Foto del Paquete")
                    }
                }
            }
            if (isUploading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Subiendo y guardando foto...",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ShippingItemCard(item: ShippingItem) {
    var showHighResImageDialog by remember { mutableStateOf(false) }
    var highResImageUrl by remember { mutableStateOf<String?>(null) }

    Log.i("ShippingItemCard", "item: $item")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { // Hacer la card clickeable
                item.picture?.let { picId ->
                    highResImageUrl =
                        "http://http2.mlstatic.com/D_${picId}-O.jpg" // URL para imagen grande
                    showHighResImageDialog = true
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Descripción (ancho completo, tamaño moderado)
            Text(
                text = item.description ?: "Sin descripción",
                style = MaterialTheme.typography.titleMedium, // Moderado, puede ocupar varias líneas
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fila para imagen y detalles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top // Alinear al tope para que los textos no se centren con la imagen si tienen alturas diferentes
            ) {
                // 2. Imagen (izquierda, 40% del ancho)
                val thumbnailUrl: String? = item.picture?.let { picId ->
                    Log.i("ShippingItemCard", "picId: $picId")
                    "http://http2.mlstatic.com/D_${picId}-I.jpg" // URL para miniatura
                }

                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            .crossfade(true)
                            // .placeholder(R.drawable.placeholder_image) // Opcional
                            // .error(R.drawable.error_image) // Opcional
                            .build(),
                        contentDescription = "Imagen de ${item.description}",
                        modifier = Modifier
                            .weight(0.4f) // 40% del ancho de la Row
                            .aspectRatio(1f) // Para mantener la imagen cuadrada, ajusta si es necesario
                            .background(Color.LightGray), // Fondo mientras carga o si falla
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box( // Espacio reservado si no hay imagen
                        modifier = Modifier
                            .weight(0.4f)
                            .aspectRatio(1f)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S/I") // Sin Imagen
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 3. Grupo de textos (derecha)
                Column(modifier = Modifier.weight(0.6f)) { // 60% del ancho de la Row
                    // Modelo (solo si variation_name existe)
                    if (!item.variationName.isNullOrBlank()) {
                        Text(
                            text = "Modelo: ${item.variationName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Seller Custom Field (idem, si existe y lo quieres mostrar)
                    // if (!item.sellerCustomField.isNullOrBlank()) {
                    // Text(
                    // text = "Ref: ${item.sellerCustomField}",
                    // style = MaterialTheme.typography.bodySmall
                    // )
                    // Spacer(modifier = Modifier.height(4.dp))
                    // }

                    // Cantidad (resaltado si es más de uno)
                    Text(
                        text = "Cantidad: ${item.quantity ?: "N/D"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if ((item.quantity
                                ?: 0) > 1
                        ) FontWeight.Bold else FontWeight.Normal,
                        color = if ((item.quantity
                                ?: 0) > 1
                        ) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            }
        }
    }

    // Popup para la imagen en alta definición
    if (showHighResImageDialog && highResImageUrl != null) {
        Dialog(onDismissRequest = { showHighResImageDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(highResImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen en alta resolución de ${item.description}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit // Fit para ver la imagen completa
                    )
                    // Botón para cerrar (opcional, ya que onDismissRequest funciona)
                    IconButton(
                        onClick = { showHighResImageDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            }
        }
    }
}