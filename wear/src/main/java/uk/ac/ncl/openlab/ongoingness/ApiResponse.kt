package uk.ac.ncl.openlab.ongoingness

data class GenericResponse(
        val code: Int,
        val errors: Boolean,
        val message: String,
        val payload: String
)
