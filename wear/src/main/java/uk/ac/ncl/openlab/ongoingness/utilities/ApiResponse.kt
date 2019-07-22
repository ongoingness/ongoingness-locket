package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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

/**
 * {
"links": [],
"era": "past",
"emotions": [],
"_id": "5c583da8d7cc1a00117c5e19",
"path": "5c58338f8a727300111ef4c2_20190204132703.jpg",
"mimetype": "image/jpeg",
"user": "5c58338f8a727300111ef4c2",
"locket": "temp",
"createdAt": "2019-02-04T13:27:04.241Z",
"updatedAt": "2019-02-04T13:27:04.241Z",
"__v": 0
}
 */
data class Media(
        val _id: String,
        val links: Array<String>,
        val era: String,
        val emotions: Array<String>,
        val path: String,
        val mimetype: String,
        val user: String,
        val locket: String,
        val createdAt: String,
        val updatedAt: String,
        val __v: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Media

        if (!links.contentEquals(other.links)) return false
        if (era != other.era) return false
        if (!emotions.contentEquals(other.emotions)) return false
        if (_id != other._id) return false
        if (path != other.path) return false
        if (mimetype != other.mimetype) return false
        if (user != other.user) return false
        if (locket != other.locket) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (__v != other.__v) return false

        return true
    }

    override fun hashCode(): Int {
        var result = links.contentHashCode()
        result = 31 * result + era.hashCode()
        result = 31 * result + emotions.contentHashCode()
        result = 31 * result + _id.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + mimetype.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + locket.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + __v
        return result
    }
}

data class MediaResponse(
    val code: Int,
    val errors: Boolean,
    val message: String,
    val payload: Array<Media>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaResponse

        if (code != other.code) return false
        if (errors != other.errors) return false
        if (message != other.message) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + errors.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
