package io.github.morgaroth.fresztok.weather.actors

import us.bleibinha.spray.json.macros.lazyy.json

@json case class Coordinates(lon: Double, lat: Double)

@json case class Main(temp: Double)

@json case class OpenWeather(name: String, id: Long, coord: Coordinates, main: Main)
