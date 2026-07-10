# Seven Wonders 360° View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tapping a wonder card downloads (and caches) that wonder's 360° equirectangular image to internal storage and opens an immersive 360° viewer pannable by touch and gyroscope.

**Architecture:** A `panoramaUrl` is added to each wonder. The adapter exposes an `onWonderClick` callback (no I/O in the adapter). `SevenWondersActivity` owns a `PanoramaRepository` that downloads via OkHttp and caches to `filesDir/wonders360/`, shows a progress dialog, then launches `Panorama360Activity`. The viewer is a self-contained OpenGL ES 2.0 sphere renderer (equirectangular texture) driven by touch drag and the rotation-vector sensor. No third-party 360° library.

**Tech Stack:** Kotlin, OkHttp 5 (existing), Hilt (existing OkHttpClient provider), coroutines via `lifecycleScope` (transitive from lifecycle-runtime-ktx), OpenGL ES 2.0 (`GLSurfaceView`), `SensorManager`. Tests: JUnit4 + OkHttp MockWebServer.

---

## File Structure

- `app/src/main/java/com/tf/gpsmapcamera/model/WonderItem.kt` — add `panoramaUrl` field (modify)
- `app/src/main/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepository.kt` — download + cache, pure of Android UI (create)
- `app/src/main/java/com/tf/gpsmapcamera/adapter/SevenWondersAdapter.kt` — add `onWonderClick` callback (modify)
- `app/src/main/java/com/tf/gpsmapcamera/ui/activities/SevenWondersActivity.kt` — wire click → download → launch viewer (modify)
- `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/SphereMesh.kt` — pure sphere-geometry + camera-direction math (create)
- `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Renderer.kt` — GLSurfaceView.Renderer (create)
- `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Activity.kt` — host GLSurfaceView, sensor + touch glue (create)
- `app/src/main/res/layout/activity_panorama360.xml` — viewer layout (create)
- `app/src/main/res/layout/dialog_download_progress.xml` — progress dialog layout (create)
- `app/src/main/AndroidManifest.xml` — register `Panorama360Activity` (modify)
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — add MockWebServer test dep (modify)
- `app/src/test/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepositoryTest.kt` — (create)
- `app/src/test/java/com/tf/gpsmapcamera/ui/panorama/SphereMeshTest.kt` — (create)

---

## Task 1: Add `panoramaUrl` to the wonder model and data

**Files:**
- Modify: `app/src/main/java/com/tf/gpsmapcamera/model/WonderItem.kt`
- Modify: `app/src/main/java/com/tf/gpsmapcamera/ui/activities/SevenWondersActivity.kt:44-55`

- [ ] **Step 1: Add the field to the model**

Edit `WonderItem.kt` so the `Wonder` data class is:

```kotlin
data class Wonder(
    val title: String,
    val location: String,
    val description: String,
    val imageRes: Int,
    val panoramaUrl: String
) : WonderItem()
```

- [ ] **Step 2: Supply a URL for each wonder**

In `SevenWondersActivity.setupRecyclerView()` add a `panoramaUrl` to each `WonderItem.Wonder(...)`. Placeholders below are real, publicly-hosted equirectangular test panoramas from Google's Photo Sphere sample set — replace with production URLs when available. The Ad line is unchanged.

```kotlin
list.add(WonderItem.Wonder("Great Wall", "China", "An ancient series of walls and fortifications, more than 13,000 miles in length.", R.drawable.wonders_5, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
list.add(WonderItem.Ad(null)) // Ad inserted after Great Wall
list.add(WonderItem.Wonder("Petra", "Jordan", "The famous 'Rose City' carved directly into vibrant red and pink sandstone cliffs.", R.drawable.wonders_2, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
list.add(WonderItem.Wonder("Christ the Redeemer", "Brazil", "An Art Deco statue of Jesus Christ in Rio de Janeiro, standing 98 feet tall.", R.drawable.wonders_4, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
list.add(WonderItem.Wonder("Machu Picchu", "Peru", "The 15th-century Inca citadel located in the Eastern Cordillera of southern Peru.", R.drawable.wonders_6, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
list.add(WonderItem.Wonder("Chichen Itza", "Mexico", "A massive step pyramid known as El Castillo, a hub of the Maya empire.", R.drawable.wonders_1, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
list.add(WonderItem.Wonder("Colosseum", "Italy", "The largest ancient amphitheatre ever built, symbol of Roman engineering.", R.drawable.wonders_7, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
list.add(WonderItem.Wonder("Taj Mahal", "India", "An immense mausoleum of white marble, built in Agra by Mughal emperor Shah Jahan.", R.drawable.wonders_3, "https://raw.githubusercontent.com/googlevr/vr-view/master/examples/coral.jpg"))
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the adapter still compiles; it ignores the new field for now).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/tf/gpsmapcamera/model/WonderItem.kt app/src/main/java/com/tf/gpsmapcamera/ui/activities/SevenWondersActivity.kt
git commit -m "feat: add panoramaUrl to wonder model and data"
```

---

## Task 2: Add MockWebServer test dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts:176-179`

- [ ] **Step 1: Add version + library to the catalog**

In `gradle/libs.versions.toml`, under `[versions]` (near the other okhttp versions) the `okhttp = "5.4.0"` already exists. Under `[libraries]` add:

```toml
mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
```

- [ ] **Step 2: Add to app test dependencies**

In `app/build.gradle.kts`, in the `dependencies { }` block alongside the other `testImplementation` lines:

```kotlin
testImplementation(libs.mockwebserver)
```

- [ ] **Step 3: Verify it resolves**

Run: `./gradlew :app:dependencies --configuration testDebugRuntimeClasspath -q`
Expected: output contains `com.squareup.okhttp3:mockwebserver:5.4.0` with no resolution error.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add mockwebserver test dependency"
```

---

## Task 3: PanoramaRepository (cache key + download/cache) — TDD

`PanoramaRepository` is decoupled from Android `Context`: it takes an `OkHttpClient` and a `cacheDir: File`, so it is unit-testable on the JVM. `cacheKey(title)` is a pure slug function.

**Files:**
- Create: `app/src/main/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepository.kt`
- Test: `app/src/test/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepositoryTest.kt`:

```kotlin
package com.tf.gpsmapcamera.data.panorama

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PanoramaRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var cacheDir: File
    private lateinit var repo: PanoramaRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        cacheDir = Files.createTempDirectory("wonders360-test").toFile()
        repo = PanoramaRepository(OkHttpClient(), cacheDir)
    }

    @After
    fun tearDown() {
        server.shutdown()
        cacheDir.deleteRecursively()
    }

    @Test
    fun cacheKey_slugifiesTitle() {
        assertEquals("great_wall", repo.cacheKey("Great Wall"))
        assertEquals("chichen_itza", repo.cacheKey("Chichen Itza"))
        assertEquals("christ_the_redeemer", repo.cacheKey("Christ the Redeemer"))
    }

    @Test
    fun getOrDownload_downloadsAndSavesFile() = runBlocking {
        val body = Buffer().write(byteArrayOf(1, 2, 3, 4, 5))
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = repo.getOrDownload("Great Wall", server.url("/pano.jpg").toString())

        assertTrue(result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertEquals(5L, file.length())
        assertEquals("great_wall.jpg", file.name)
    }

    @Test
    fun getOrDownload_returnsCachedFileWithoutSecondRequest() = runBlocking {
        val body = Buffer().write(byteArrayOf(9, 9, 9))
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val first = repo.getOrDownload("Petra", server.url("/p.jpg").toString()).getOrThrow()
        // No second response enqueued — a second network call would fail/hang.
        val second = repo.getOrDownload("Petra", server.url("/p.jpg").toString()).getOrThrow()

        assertEquals(first.absolutePath, second.absolutePath)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun getOrDownload_failsAndLeavesNoPartialFile() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = repo.getOrDownload("Taj Mahal", server.url("/missing.jpg").toString())

        assertTrue(result.isFailure)
        assertFalse(File(File(cacheDir, "wonders360"), "taj_mahal.jpg").exists())
        assertFalse(File(File(cacheDir, "wonders360"), "taj_mahal.jpg.tmp").exists())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.tf.gpsmapcamera.data.panorama.PanoramaRepositoryTest"`
Expected: FAIL — compilation error, `PanoramaRepository` is unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepository.kt`:

```kotlin
package com.tf.gpsmapcamera.data.panorama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Downloads and caches 360° equirectangular panorama images to [cacheDir].
 * Pure of Android UI so it can be unit-tested on the JVM.
 */
class PanoramaRepository @Inject constructor(
    private val client: OkHttpClient,
    private val cacheDir: File
) {

    private val panoDir: File
        get() = File(cacheDir, "wonders360").apply { mkdirs() }

    /** Stable filesystem-safe slug for a wonder title, e.g. "Great Wall" -> "great_wall". */
    fun cacheKey(title: String): String =
        title.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    fun cachedFile(title: String): File = File(panoDir, "${cacheKey(title)}.jpg")

    /**
     * Returns the cached file if present, otherwise downloads [url] and saves it.
     * Writes to a .tmp file and renames on success so a partial download is never
     * treated as cached.
     */
    suspend fun getOrDownload(title: String, url: String): Result<File> =
        withContext(Dispatchers.IO) {
            val dest = cachedFile(title)
            if (dest.exists() && dest.length() > 0) {
                return@withContext Result.success(dest)
            }
            val tmp = File(dest.parentFile, "${dest.name}.tmp")
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    val bodyStream = response.body?.byteStream()
                        ?: throw IOException("Empty body")
                    tmp.outputStream().use { out -> bodyStream.copyTo(out) }
                }
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                Result.success(dest)
            } catch (e: Exception) {
                tmp.delete()
                dest.delete()
                Result.failure(e)
            }
        }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.tf.gpsmapcamera.data.panorama.PanoramaRepositoryTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepository.kt app/src/test/java/com/tf/gpsmapcamera/data/panorama/PanoramaRepositoryTest.kt
git commit -m "feat: add PanoramaRepository with download/cache (TDD)"
```

---

## Task 4: Sphere geometry + camera direction math — TDD

Pure math extracted from the renderer so it is unit-testable. `buildSphere` produces interleaved-free position/uv/index arrays; `directionFromYawPitch` produces a look-at target.

**Files:**
- Create: `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/SphereMesh.kt`
- Test: `app/src/test/java/com/tf/gpsmapcamera/ui/panorama/SphereMeshTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/tf/gpsmapcamera/ui/panorama/SphereMeshTest.kt`:

```kotlin
package com.tf.gpsmapcamera.ui.panorama

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class SphereMeshTest {

    @Test
    fun buildSphere_hasExpectedVertexAndIndexCounts() {
        val stacks = 20
        val slices = 40
        val mesh = buildSphere(radius = 2f, stacks = stacks, slices = slices)

        val vertexCount = (stacks + 1) * (slices + 1)
        assertEquals(vertexCount * 3, mesh.positions.size)
        assertEquals(vertexCount * 2, mesh.texCoords.size)
        assertEquals(stacks * slices * 6, mesh.indices.size)
    }

    @Test
    fun buildSphere_texCoordsAreNormalized() {
        val mesh = buildSphere(radius = 1f, stacks = 8, slices = 8)
        for (uv in mesh.texCoords) {
            assertTrue("uv out of range: $uv", uv in -0.0001f..1.0001f)
        }
    }

    @Test
    fun directionFromYawPitch_zeroLooksDownNegativeZ() {
        val dir = directionFromYawPitch(yaw = 0f, pitch = 0f)
        assertEquals(0f, dir[0], 1e-4f)
        assertEquals(0f, dir[1], 1e-4f)
        assertEquals(-1f, dir[2], 1e-4f)
    }

    @Test
    fun directionFromYawPitch_pitchUpIncreasesY() {
        val dir = directionFromYawPitch(yaw = 0f, pitch = (PI / 4).toFloat())
        assertTrue(dir[1] > 0f)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.tf.gpsmapcamera.ui.panorama.SphereMeshTest"`
Expected: FAIL — `buildSphere` / `directionFromYawPitch` unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/SphereMesh.kt`:

```kotlin
package com.tf.gpsmapcamera.ui.panorama

import kotlin.math.cos
import kotlin.math.sin

/** Plain geometry holder for a UV sphere used as the panorama projection surface. */
data class SphereMesh(
    val positions: FloatArray,
    val texCoords: FloatArray,
    val indices: ShortArray
)

/**
 * Generates a UV sphere. The equirectangular texture maps directly: u around the
 * sphere (longitude), v top-to-bottom (latitude).
 */
fun buildSphere(radius: Float, stacks: Int, slices: Int): SphereMesh {
    val positions = ArrayList<Float>((stacks + 1) * (slices + 1) * 3)
    val texCoords = ArrayList<Float>((stacks + 1) * (slices + 1) * 2)
    val indices = ArrayList<Short>(stacks * slices * 6)

    for (i in 0..stacks) {
        val v = i.toFloat() / stacks
        val theta = v * Math.PI.toFloat()            // 0..PI (top to bottom)
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        for (j in 0..slices) {
            val u = j.toFloat() / slices
            val phi = u * 2f * Math.PI.toFloat()      // 0..2PI around
            val sinPhi = sin(phi)
            val cosPhi = cos(phi)

            positions.add(radius * sinTheta * cosPhi)
            positions.add(radius * cosTheta)
            positions.add(radius * sinTheta * sinPhi)

            texCoords.add(u)
            texCoords.add(v)
        }
    }

    val stride = slices + 1
    for (i in 0 until stacks) {
        for (j in 0 until slices) {
            val a = (i * stride + j).toShort()
            val b = (i * stride + j + 1).toShort()
            val c = ((i + 1) * stride + j).toShort()
            val d = ((i + 1) * stride + j + 1).toShort()
            indices.add(a); indices.add(c); indices.add(b)
            indices.add(b); indices.add(c); indices.add(d)
        }
    }

    return SphereMesh(
        positions = positions.toFloatArray(),
        texCoords = texCoords.toFloatArray(),
        indices = indices.toShortArray()
    )
}

/** Unit look-at direction for a given yaw (around Y) and pitch (up/down). */
fun directionFromYawPitch(yaw: Float, pitch: Float): FloatArray {
    val cosPitch = cos(pitch)
    return floatArrayOf(
        cosPitch * sin(yaw),
        sin(pitch),
        -cosPitch * cos(yaw)
    )
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.tf.gpsmapcamera.ui.panorama.SphereMeshTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/tf/gpsmapcamera/ui/panorama/SphereMesh.kt app/src/test/java/com/tf/gpsmapcamera/ui/panorama/SphereMeshTest.kt
git commit -m "feat: add sphere geometry and camera direction math (TDD)"
```

---

## Task 5: Panorama360Renderer (OpenGL ES 2.0)

GLSurfaceView.Renderer that uploads the bitmap as a texture onto the sphere and renders from a yaw/pitch camera at the sphere's center. No external library. Visual correctness is verified manually in Task 8.

**Files:**
- Create: `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Renderer.kt`

- [ ] **Step 1: Write the renderer**

Create `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Renderer.kt`:

```kotlin
package com.tf.gpsmapcamera.ui.panorama

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Panorama360Renderer(private val bitmap: Bitmap) : GLSurfaceView.Renderer {

    @Volatile var yaw: Float = 0f
    @Volatile var pitch: Float = 0f

    private val mesh = buildSphere(radius = 2f, stacks = 40, slices = 80)

    private lateinit var positionBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    private var program = 0
    private var aPositionLoc = 0
    private var aTexCoordLoc = 0
    private var uMvpLoc = 0
    private var uTextureLoc = 0
    private var textureId = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val mvp = FloatArray(16)

    private val vertexShader = """
        uniform mat4 uMvp;
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            vTexCoord = aTexCoord;
            gl_Position = uMvp * aPosition;
        }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        uniform sampler2D uTexture;
        varying vec2 vTexCoord;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // Camera sits inside the sphere; render back faces (disable culling).
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        positionBuffer = mesh.positions.toFloatBuffer()
        texCoordBuffer = mesh.texCoords.toFloatBuffer()
        indexBuffer = mesh.indices.toShortBuffer()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uMvpLoc = GLES20.glGetUniformLocation(program, "uMvp")
        uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture")

        textureId = uploadTexture(bitmap)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projection, 0, 75f, ratio, 0.1f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val dir = directionFromYawPitch(yaw, pitch)
        Matrix.setLookAtM(
            view, 0,
            0f, 0f, 0f,
            dir[0], dir[1], dir[2],
            0f, 1f, 0f
        )
        Matrix.multiplyMM(mvp, 0, projection, 0, view, 0)
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvp, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, positionBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, mesh.indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer
        )

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
    }

    private fun uploadTexture(bmp: Bitmap): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        return ids[0]
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }

    private fun FloatArray.toFloatBuffer(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply { put(this@toFloatBuffer); position(0) }

    private fun ShortArray.toShortBuffer(): ShortBuffer =
        ByteBuffer.allocateDirect(size * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer().apply { put(this@toShortBuffer); position(0) }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Renderer.kt
git commit -m "feat: add OpenGL ES 360 panorama renderer"
```

---

## Task 6: Progress dialog + viewer layouts

**Files:**
- Create: `app/src/main/res/layout/dialog_download_progress.xml`
- Create: `app/src/main/res/layout/activity_panorama360.xml`

- [ ] **Step 1: Create the progress dialog layout**

Create `app/src/main/res/layout/dialog_download_progress.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:padding="24dp">

    <ProgressBar
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        style="?android:attr/progressBarStyle" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="20dp"
        android:text="@string/loading_360_view"
        android:textSize="16sp" />
</LinearLayout>
```

- [ ] **Step 2: Add the string**

In `app/src/main/res/values/strings.xml`, add:

```xml
<string name="loading_360_view">Loading 360° view…</string>
<string name="error_360_view">Couldn\'t load 360° view</string>
```

- [ ] **Step 3: Create the viewer layout**

Create `app/src/main/res/layout/activity_panorama360.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/black">

    <android.opengl.GLSurfaceView
        android:id="@+id/gl_surface"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <ImageView
        android:id="@+id/btn_close"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_margin="16dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/error_360_view"
        android:src="@android:drawable/ic_menu_close_clear_cancel"
        app:tint="@android:color/white"
        xmlns:app="http://schemas.android.com/apk/res-auto" />
</FrameLayout>
```

- [ ] **Step 4: Build to verify resources compile**

Run: `./gradlew :app:processDebugResources`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/dialog_download_progress.xml app/src/main/res/layout/activity_panorama360.xml app/src/main/res/values/strings.xml
git commit -m "feat: add progress dialog and panorama viewer layouts"
```

---

## Task 7: Panorama360Activity (sensor + touch glue) and manifest registration

Loads the bitmap from the file path passed via intent, hosts the GLSurfaceView, and drives yaw/pitch from touch drag and the rotation-vector sensor.

**Files:**
- Create: `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Activity.kt`
- Modify: `app/src/main/AndroidManifest.xml:48-51` (add new activity near `SevenWondersActivity`)

- [ ] **Step 1: Create the activity**

Create `app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Activity.kt`:

```kotlin
package com.tf.gpsmapcamera.ui.panorama

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tf.gpsmapcamera.R
import kotlin.math.PI

class Panorama360Activity : AppCompatActivity(), SensorEventListener {

    private lateinit var glSurfaceView: GLSurfaceView
    private var renderer: Panorama360Renderer? = null

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    // Touch state
    private var previousX = 0f
    private var previousY = 0f

    // Gyro baseline so device orientation is relative to where the user started.
    private var haveSensorBaseline = false
    private var baseAzimuth = 0f
    private var basePitch = 0f
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    companion object {
        private const val EXTRA_FILE_PATH = "extra_file_path"
        private const val TOUCH_SENSITIVITY = 0.0025f

        fun newIntent(context: Context, filePath: String): Intent =
            Intent(context, Panorama360Activity::class.java)
                .putExtra(EXTRA_FILE_PATH, filePath)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama360)

        val path = intent.getStringExtra(EXTRA_FILE_PATH)
        val bitmap = path?.let { BitmapFactory.decodeFile(it) }
        if (bitmap == null) {
            Toast.makeText(this, R.string.error_360_view, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        glSurfaceView = findViewById(R.id.gl_surface)
        glSurfaceView.setEGLContextClientVersion(2)
        renderer = Panorama360Renderer(bitmap).also {
            glSurfaceView.setRenderer(it)
        }

        findViewById<ImageView>(R.id.btn_close).setOnClickListener { finish() }

        glSurfaceView.setOnTouchListener { _, event -> handleTouch(event); true }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    private fun handleTouch(event: MotionEvent) {
        val r = renderer ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                val dy = event.y - previousY
                r.yaw -= dx * TOUCH_SENSITIVITY
                r.pitch = (r.pitch + dy * TOUCH_SENSITIVITY)
                    .coerceIn((-PI / 2 + 0.01).toFloat(), (PI / 2 - 0.01).toFloat())
                previousX = event.x
                previousY = event.y
                glSurfaceView.requestRender()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        haveSensorBaseline = false
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        glSurfaceView.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val r = renderer ?: return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val azimuth = orientation[0]   // around Z
        val devicePitch = -orientation[1]
        if (!haveSensorBaseline) {
            baseAzimuth = azimuth
            basePitch = devicePitch
            haveSensorBaseline = true
            return
        }
        // Apply gyro as a delta on top of touch yaw/pitch.
        r.yaw += (azimuth - baseAzimuth)
        r.pitch = (r.pitch + (devicePitch - basePitch))
            .coerceIn((-PI / 2 + 0.01).toFloat(), (PI / 2 - 0.01).toFloat())
        baseAzimuth = azimuth
        basePitch = devicePitch
        glSurfaceView.requestRender()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
```

Note: `GLSurfaceView` default render mode is continuous, which works fine; `requestRender()` calls are harmless. (If you switch to `RENDERMODE_WHEN_DIRTY` for battery, the `requestRender()` calls above already cover updates.)

- [ ] **Step 2: Register the activity in the manifest**

In `app/src/main/AndroidManifest.xml`, add directly after the `SevenWondersActivity` entry (after line 51):

```xml
<activity
    android:name=".ui.panorama.Panorama360Activity"
    android:exported="false"
    android:screenOrientation="portrait" />
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/tf/gpsmapcamera/ui/panorama/Panorama360Activity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add Panorama360Activity with touch and gyroscope control"
```

---

## Task 8: Wire the adapter click → download → launch viewer

Adapter gains an `onWonderClick` callback (UI-only). Activity creates a `PanoramaRepository` (using the Hilt-injected `OkHttpClient` and `filesDir`), and on click downloads then launches the viewer with a progress dialog.

**Files:**
- Modify: `app/src/main/java/com/tf/gpsmapcamera/adapter/SevenWondersAdapter.kt`
- Modify: `app/src/main/java/com/tf/gpsmapcamera/ui/activities/SevenWondersActivity.kt`

- [ ] **Step 1: Add the callback to the adapter constructor**

In `SevenWondersAdapter.kt`, change the constructor to:

```kotlin
class SevenWondersAdapter(
    private val adMobManager: AdMobManager,
    private val onWonderClick: (WonderItem.Wonder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
```

- [ ] **Step 2: Invoke the callback on item click**

In `SevenWondersAdapter.WonderViewHolder.bind`, after the existing field assignments, add:

```kotlin
fun bind(wonder: WonderItem.Wonder) {
    ivWonder.setImageResource(wonder.imageRes)
    tvTitle.text = wonder.title
    tvLocation.text = wonder.location
    tvDescription.text = wonder.description
    itemView.setOnClickListener { onWonderClick(wonder) }
}
```

- [ ] **Step 3: Inject OkHttpClient and build the repository in the activity**

In `SevenWondersActivity.kt`, add imports and fields:

```kotlin
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.tf.gpsmapcamera.data.panorama.PanoramaRepository
import com.tf.gpsmapcamera.model.WonderItem
import com.tf.gpsmapcamera.ui.panorama.Panorama360Activity
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
```

Add an injected client and a lazily-built repository and an in-flight guard:

```kotlin
@Inject
lateinit var okHttpClient: OkHttpClient

private val panoramaRepository by lazy {
    PanoramaRepository(okHttpClient, filesDir)
}

private var downloadInProgress = false
```

- [ ] **Step 4: Pass the callback when creating the adapter**

In `setupRecyclerView()`, change adapter construction to:

```kotlin
adapter = SevenWondersAdapter(adMobManager) { wonder -> openPanorama(wonder) }
```

- [ ] **Step 5: Add the download-and-launch method**

Add to `SevenWondersActivity`:

```kotlin
private fun openPanorama(wonder: WonderItem.Wonder) {
    if (downloadInProgress) return
    downloadInProgress = true

    val dialog = AlertDialog.Builder(this)
        .setView(LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null))
        .setCancelable(false)
        .create()
    dialog.show()

    lifecycleScope.launch {
        val result = panoramaRepository.getOrDownload(wonder.title, wonder.panoramaUrl)
        dialog.dismiss()
        downloadInProgress = false
        result.onSuccess { file ->
            startActivity(Panorama360Activity.newIntent(this@SevenWondersActivity, file.absolutePath))
        }.onFailure {
            Toast.makeText(this@SevenWondersActivity, R.string.error_360_view, Toast.LENGTH_SHORT).show()
        }
    }
}
```

- [ ] **Step 6: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (all PanoramaRepository and SphereMesh tests green).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/tf/gpsmapcamera/adapter/SevenWondersAdapter.kt app/src/main/java/com/tf/gpsmapcamera/ui/activities/SevenWondersActivity.kt
git commit -m "feat: wire wonder click to download and open 360 viewer"
```

---

## Task 9: Manual verification

- [ ] **Step 1: Install the debug build**

Run: `./gradlew :app:installDebug`
Expected: BUILD SUCCESSFUL, app installs.

- [ ] **Step 2: Verify behavior on device/emulator**

1. Open the Seven Wonders screen.
2. Tap a wonder card → progress dialog appears → 360° viewer opens.
3. Drag on screen → view pans horizontally and vertically.
4. Physically rotate the device (real device) → view follows gyroscope.
5. Tap close → returns to list.
6. Tap the same wonder again → opens instantly (cached, no network call).
7. Turn off network, tap an uncached wonder → toast "Couldn't load 360° view", no crash.
8. Confirm files saved under `filesDir/wonders360/` (e.g. via `adb shell run-as com.tf.gpsmapcamera ls files/wonders360`).

- [ ] **Step 3: Final commit (if any cleanup needed)**

```bash
git add -A
git commit -m "chore: seven wonders 360 view manual-test cleanup"
```

---

## Self-Review Notes

- **Spec coverage:** model URL (Task 1), download+cache to internal storage (Task 3), whole-card click (Task 8), progress + error handling (Tasks 6/8), OpenGL sphere viewer with touch+gyro (Tasks 4/5/7), manifest registration (Task 7), no external 360 library (confirmed). All spec sections covered.
- **Types consistent:** `PanoramaRepository(OkHttpClient, File)`, `getOrDownload(title, url): Result<File>`, `cacheKey`, `buildSphere`/`SphereMesh`/`directionFromYawPitch`, `Panorama360Renderer(Bitmap)` with `yaw`/`pitch`, `Panorama360Activity.newIntent(context, filePath)`, `onWonderClick: (WonderItem.Wonder) -> Unit` — names match across tasks.
- **Open item:** real production 360° URLs to replace the placeholder in Task 1.
```
