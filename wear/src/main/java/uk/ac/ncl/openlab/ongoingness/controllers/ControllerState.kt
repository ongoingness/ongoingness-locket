package uk.ac.ncl.openlab.ongoingness.controllers

enum class ControllerState{
    STANDBY,
    READY,
    ACTIVE,
    OFF,
    CHARGING,
    PULLING_DATA,
    UNKNOWN,
}