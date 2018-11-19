package uk.ac.ncl.openlab.ongoingness

data class GenericResponse(
        val code: Int,
        val errors: Boolean,
        val message: String,
        val payload: String
)

/**
 * Data class to override links response, parse links directly to an array of strings.
 */
data class LinkResponse(
        val code: Int,
        val error: Boolean,
        val message: String,
        val payload: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinkResponse

        if (code != other.code) return false
        if (error != other.error) return false
        if (message != other.message) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + error.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
