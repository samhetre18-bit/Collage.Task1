package com.collage // Make sure this matches your actual package name at the top!

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// 1. DATA MODEL
data class ImageState(
    val uri: Uri? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    EditorScreen()
                }
            }
        }
    }
}

@Composable
fun EditorScreen() {
    // TOOLS
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    // STATE
    var gridCount by remember { mutableIntStateOf(4) }
    var radius by remember { mutableFloatStateOf(0f) }
    var spacing by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // This list resets when the grid layout changes
    val images = remember(gridCount) {
        mutableStateListOf<ImageState>().apply {
            repeat(gridCount) { add(ImageState()) }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && selectedIndex != -1) {
            images[selectedIndex] = images[selectedIndex].copy(uri = uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        Text(
            "CUSTOM EDITOR",
            color = Color.White,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        // LAYOUT SELECTOR BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2, 3, 4, 5).forEach { count ->
                Button(
                    onClick = { gridCount = count },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gridCount == count) Color(0xFF3D5AFE) else Color.DarkGray
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("${count} GRID", fontSize = 10.sp)
                }
            }
        }

        // THE COLLAGE AREA (The part that gets saved)
        // THE COLLAGE FRAME
        // THE COLLAGE BOX - This is the parent container
        // THE COLLAGE BOX - This is the parent container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Keep it a perfect square collage
                .padding(spacing.dp)
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }
        ) {
            when (gridCount) {
                // 1) 2 Images: 2 tall rectangular grids side-by-side
                2 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        ImageCell(images[0], radius) { selectedIndex = 0; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        ImageCell(images[1], radius) { selectedIndex = 1; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    }
                }

                // 2) 3 Images: 1 tall rectangle (left) and 2 squares (right)
                3 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        ImageCell(images[0], radius) { selectedIndex = 0; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        Box(Modifier.weight(1f)) { ImageCell(images[1], radius) { selectedIndex = 1; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        Box(Modifier.weight(1f)) { ImageCell(images[2], radius) { selectedIndex = 2; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                    }
                }

                // 4 Images: Standard 2x2 square grid
                4 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        Box(Modifier.weight(1f)) { ImageCell(images[0], radius) { selectedIndex = 0; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        Box(Modifier.weight(1f)) { ImageCell(images[1], radius) { selectedIndex = 1; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        Box(Modifier.weight(1f)) { ImageCell(images[2], radius) { selectedIndex = 2; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        Box(Modifier.weight(1f)) { ImageCell(images[3], radius) { selectedIndex = 3; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                    }
                }

                // 5) 5 Images: 2 on left (rectangles) and 3 on right (rectangles)
                5 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        Box(Modifier.weight(1f)) { ImageCell(images[0], radius) { selectedIndex = 0; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        Box(Modifier.weight(1f)) { ImageCell(images[1], radius) { selectedIndex = 1; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        Box(Modifier.weight(1f)) { ImageCell(images[2], radius) { selectedIndex = 2; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        Box(Modifier.weight(1f)) { ImageCell(images[3], radius) { selectedIndex = 3; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                        Box(Modifier.weight(1f)) { ImageCell(images[4], radius) { selectedIndex = 4; launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                    }
                }
            }
        }

        // CONTROLS PANEL
        Column(
            modifier = Modifier
                .background(Color(0xFF252525))
                .padding(20.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            Text("RADIUS: ${radius.toInt()}", color = Color.White)
            Slider(value = radius, onValueChange = { radius = it }, valueRange = 0f..100f)

            Text("SPACING: ${spacing.toInt()}", color = Color.White)
            Slider(value = spacing, onValueChange = { spacing = it }, valueRange = 0f..50f)

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        saveBitmapToGallery(context, bitmap)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                Text("SAVE TO GALLERY")
            }
        }
    }
}


@Composable
fun ImageCell(state: ImageState, radius: Float, onImageClick: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize() // CHANGE THIS: Fill the rectangular space provided by the parent
            .clip(RoundedCornerShape(radius.dp))
            .background(Color(0xFF333333))
            .clickable { onImageClick() }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    scale *= zoom
                    offset += pan
                    rotation += rotate
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (state.uri != null) {
            AsyncImage(
                model = state.uri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        rotationZ = rotation
                    ),
                contentScale = ContentScale.Crop // This ensures the photo fills the rectangle
            )
        } else {
            Text("+", color = Color.Gray, fontSize = 30.sp)
        }
    }
}




fun saveBitmapToGallery(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    val filename = "Collage_${System.currentTimeMillis()}.jpg"
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CollageMaker")
    }

    val uri = context.contentResolver.insert(
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )

    uri?.let {
        context.contentResolver.openOutputStream(it).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream!!)
        }
        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
    }
}