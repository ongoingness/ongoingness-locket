package uk.ac.ncl.openlab.ongoingness.controllers

/**
 * States of a controller.
 *
 * @author Luis Carvalho
 */
enum class ControllerState{
    STANDBY,
    READY,
    ACTIVE,
    OFF,
    CHARGING,
    PULLING_DATA,
    UNKNOWN,
}