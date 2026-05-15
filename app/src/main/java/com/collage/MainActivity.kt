package com.collage // Make sure this matches your actual package name at the top!

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.espresso.base.Default
import coil.compose.AsyncImage
import com.collage.Settings
import com.google.androidgamesdk.gametextinput.Settings
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily

private val Unit.Settings: ImageVector
    get() {
        TODO()
    }

// 1. DATA MODEL
data class ImageState(
    val uri: Uri? = null
)

class MainActivity : ComponentActivity() {
    // Inside your MainActivity class
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. Create the Toggle State
            var isDarkMode by remember { mutableStateOf(true) }

            // 2. Wrap the theme around the toggle
            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // Surface will now automatically pick the right background color
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the toggle to your screen so we can change it with a button
                    EditorScreen(isDarkMode = isDarkMode, onThemeToggle = { isDarkMode = !isDarkMode })
                }
            }
        }
    }
}@Composable
fun EditorScreen(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    // TOOLS
    // Add these with your other variables
    val fontOptions = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace, FontFamily.Cursive)
    var selectedFont by remember { mutableStateOf(FontFamily.Default) }
// We already have overlayTextColor, we'll just use the same presetColors list from earlier
    var overlayText by remember { mutableStateOf("TAP TO EDIT") }
    var overlayTextColor by remember { mutableStateOf(Color.White) }
    var textOffset by remember { mutableStateOf(Offset(200f, 200f)) }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    // STATE
    // The list of colors you want to offer
    val presetColors = listOf(
        Color.White, Color.Black, Color(0xFFF44336),
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFEB3B)
    )
// The variable that remembers the selection
    var collageBgColor by remember { mutableStateOf(Color.White) }
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
    // This makes the gap between photos smaller in Light Mode for a 'cleaner' look
    val activeSpacing = if (isDarkMode) spacing.dp else (spacing * 0.5f).dp

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(scrollState) // THIS allows you to swipe down
    ) {
        Text(
            "CUSTOM EDITOR",
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        // LAYOUT SELECTOR BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            IconButton(onClick = {  showSettings = true }) {
                IconButton(onClick = { showSettings = true }) {
                    Text("⚙️", fontSize = 24.sp)
                }
            }
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
                .aspectRatio(1f)
                .background(collageBgColor) // <--- THIS LINKS IT
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
            Text(
                text = overlayText,
                color = overlayTextColor,
                fontFamily = selectedFont, // <--- Added comma
                style = MaterialTheme.typography.headlineSmall, // <--- Added comma
                modifier = Modifier
                    .offset { IntOffset(textOffset.x.toInt(), textOffset.y.toInt()) }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            textOffset += pan
                        }
                    }
            )
        }

        // CONTROLS PANEL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Overall outer padding
            verticalArrangement = Arrangement.spacedBy(16.dp) // <--- THIS is the magic line
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "CHOOSE BACKGROUND",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presetColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (collageBgColor == color) 3.dp else 1.dp,
                                color = if (collageBgColor == color) Color.Blue else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { collageBgColor = color } // The magic happens here
                    )
                }
            }
            OutlinedTextField(
                value = overlayText,
                onValueChange = { overlayText = it },
                label = { Text("Edit Overlay Text") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            // TEXT COLOR PICKER
            Text("TEXT COLOR", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(color, CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { overlayTextColor = color }
                    )
                }
            }
// FONT STYLE PICKER
            Text("FONT STYLE", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { selectedFont = FontFamily.Serif }) { Text("Serif") }
                Button(onClick = { selectedFont = FontFamily.Monospace }) { Text("Mono") }
                Button(onClick = { selectedFont = FontFamily.Cursive }) { Text("Cursive") }
            }
            Text(
                    "RADIUS: ${radius.toInt()}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium, // <--- Add this style here
            )
            Slider(value = radius, onValueChange = { radius = it }, valueRange = 0f..100f)

            Text(
                "SPACING: ${spacing.toInt()}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium, // <--- And here
            )
            Slider(value = spacing, onValueChange = { spacing = it }, valueRange = 0f..50f)
            Spacer(modifier = Modifier.height(8.dp))
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
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Done") }
            },
            title = { Text("Settings") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                }
            }
        )
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
                contentScale = ContentScale.Fit, // This ensures the photo fills the rectangle
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        rotationZ = rotation
                    )
            )
        } else {
            Text("+", color = Color.Gray, fontSize = 30.sp)
        }
    }
}




fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "Collage_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CollageMaker")
    }

    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )

    uri?.let {
        context.contentResolver.openOutputStream(it).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream!!)
        }
        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
    }
}