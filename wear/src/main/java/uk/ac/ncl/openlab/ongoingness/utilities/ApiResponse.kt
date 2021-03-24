package uk.ac.ncl.openlab.ongoingness.utilities

/**
 * Classes into which the server responses are parsed into.
 *
 * @author Luis Carvalho, Daniel Welsh
 */

/**
 * Generic Response from the server.
 *
 * @param code response status code.
 * @param errors true if errors happened.
 * @param message string message of the response.
 * @param payload string payload of the response.
 */
data class GenericResponse(
        val code: Int,
        val errors: Boolean,
        val message: String,
        val payload: String
)
