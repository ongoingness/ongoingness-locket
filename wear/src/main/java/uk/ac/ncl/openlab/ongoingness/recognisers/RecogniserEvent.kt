package uk.ac.ncl.openlab.ongoingness.recognisers

/**
 * Types of event sent by the recognisers.
 *
 * @author Luis Carvalho
 */
enum class RecogniserEvent{
    STARTED,
    STOPPED,

    UP,
    DOWN,
    TOWARDS,
    AWAY,
    UNKNOWN,
    LONG_PRESS,
    TAP,

    ROTATE_UP,
    ROTATE_DOWN,

    ROTATE_LEFT,
    ROTATE_RIGHT,
    AWAY_LEFT,
    AWAY_RIGHT,

    AWAY_TOWARDS,
}