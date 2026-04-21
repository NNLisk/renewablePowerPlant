package data

case class powerOutputObservation(
    dataset: Int,
    startTime: java.time.LocalDateTime,
    endTime: java.time.LocalDateTime,
    outputKw: Double
    )

