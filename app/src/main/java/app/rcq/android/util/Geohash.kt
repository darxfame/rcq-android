package app.rcq.android.util

/**
 * Minimal geohash encoder — Android port of the iOS `Geohash`. People Nearby
 * uses level-6 (~1.2km × 0.6km tile): fine enough that two users in the same
 * hash are realistically within walking distance, coarse enough that the server
 * (which only ever sees the hash, never raw coordinates) can't pinpoint anyone
 * inside it. Standard interleaved-bit-bisection; must interoperate with iOS, so
 * the algorithm is identical.
 */
object Geohash {
    private val ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray()

    fun encode(lat: Double, lon: Double, length: Int = 6): String {
        var latLow = -90.0; var latHigh = 90.0
        var lonLow = -180.0; var lonHigh = 180.0
        val hash = StringBuilder()
        var bits = 0
        var bit = 0
        var even = true
        while (hash.length < length) {
            if (even) {
                val mid = (lonLow + lonHigh) / 2
                if (lon >= mid) { bits = (bits shl 1) or 1; lonLow = mid } else { bits = bits shl 1; lonHigh = mid }
            } else {
                val mid = (latLow + latHigh) / 2
                if (lat >= mid) { bits = (bits shl 1) or 1; latLow = mid } else { bits = bits shl 1; latHigh = mid }
            }
            even = !even
            bit += 1
            if (bit == 5) { hash.append(ALPHABET[bits]); bits = 0; bit = 0 }
        }
        return hash.toString()
    }

    /** Centre + half-error of a geohash cell. */
    private fun decode(hash: String): DoubleArray? {
        var latLow = -90.0; var latHigh = 90.0
        var lonLow = -180.0; var lonHigh = 180.0
        var even = true
        for (ch in hash) {
            val idx = ALPHABET.indexOf(ch)
            if (idx < 0) return null
            for (b in 4 downTo 0) {
                val v = (idx shr b) and 1
                if (even) {
                    val mid = (lonLow + lonHigh) / 2
                    if (v == 1) lonLow = mid else lonHigh = mid
                } else {
                    val mid = (latLow + latHigh) / 2
                    if (v == 1) latLow = mid else latHigh = mid
                }
                even = !even
            }
        }
        return doubleArrayOf((latLow + latHigh) / 2, (lonLow + lonHigh) / 2, (latHigh - latLow) / 2, (lonHigh - lonLow) / 2)
    }

    /** The cell plus its 8 neighbours (self first), so two users either side of
     *  a tile boundary still discover each other. */
    fun selfAndNeighbours(hash: String): List<String> {
        val dec = decode(hash) ?: return listOf(hash)
        val (lat, lon, latErr, lonErr) = dec
        val dLat = latErr * 2; val dLon = lonErr * 2
        val offsets = listOf(
            0.0 to 0.0, 0.0 to -dLon, 0.0 to dLon, -dLat to 0.0, dLat to 0.0,
            -dLat to -dLon, -dLat to dLon, dLat to -dLon, dLat to dLon,
        )
        val seen = LinkedHashSet<String>()
        for ((latOff, lonOff) in offsets) seen.add(encode(lat + latOff, lon + lonOff, hash.length))
        return seen.toList()
    }

    private operator fun DoubleArray.component1() = this[0]
    private operator fun DoubleArray.component2() = this[1]
    private operator fun DoubleArray.component3() = this[2]
    private operator fun DoubleArray.component4() = this[3]
}
