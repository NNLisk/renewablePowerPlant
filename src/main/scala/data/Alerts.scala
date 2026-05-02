package data

object Alerts {
  private var activeAlerts: List[String] = List.empty

  private val monitoredSources: List[(String, String)] = List(
    ("Wind", "data/wind.csv"),
    ("Hydro", "data/hydro.csv"),
    ("Nuclear", "data/solar.csv")
  )

  // add a new alert only if it is not already active
  private def addAlert(message: String): Boolean = {
    if (activeAlerts.contains(message)) false // duplicates are not added
    else {
      activeAlerts = activeAlerts :+ message
      true
    }
  }

  // creates a new alert
  def triggerFault(source: String): Unit = {
    val message = s"[FAULT] $source equipment malfunction detected." // standard format
    val added = addAlert(message)

    if (added) println(s"  [WARN] Alert triggered: $message")
    else println(s"  [WARN] Alert already active: $message") //duplicate
  }

  // ---------------------------------clear all active alerts (maybe change to clear specific alert)---------------------------------
  def resetAlerts(): Unit = {
    activeAlerts = List.empty
    println("  [OK] All alerts cleared.")
  }

  // display alerts
  def checkAlerts(): Unit = {
    if (activeAlerts.isEmpty) {
      println("  [OK] No active alerts.")
    } else {
      println(s"  [WARN] ${activeAlerts.size} active alert(s):")
      activeAlerts.foreach(alert => println(s"    - $alert"))
    }
  }

  // alert generation logic (compare recent output to historical baseline => trigger alert if lower)
  def refreshDataAlerts(): Unit = {
    monitoredSources.foreach { case (source, path) => 
      val observations = dataProcessing.pullFromCsv(path) // observations is of type List[powerOutputObservation] ---- FUNCTOR
      if (observations.nonEmpty) checkLowOutput(source, observations) // actual comparison
    }
  }

  // core logic for comparing recent output to historical baseline
  def checkLowOutput(source: String, observations: List[powerOutputObservation]): Unit = {
    val allOutputs = observations.map(_.outputKw) // historical outputs (baseline); FUNCTOR - List.map transforms each observation into a list of outputs
    val recentOutputs = dataProcessing.filterLast24h(observations).map(_.outputKw) // recent outputs; FUNCTOR

    val maybeAlert: Option[String] = // potential alert message
      Metrics.mean(allOutputs).flatMap { baselineAvg =>
        Metrics.mean(recentOutputs).flatMap { recentAvg =>
          Metrics
            .range(allOutputs)
            .filter(spread => recentAvg < (baselineAvg - (spread * 0.25))) // trigger if recent avg is more than 25% lower than the baseline
            .map { _ =>
              val messageAvg = f"$recentAvg%.1f"
              val baseline = f"$baselineAvg%.1f"
              s"[ALERT] $source output suspiciously low: recent avg $messageAvg kW (baseline $baseline kW)"
            }
        }
      }

    maybeAlert.foreach(addAlert)
  }
  def checkAgeingStatus(): Unit = {
    monitoredSources.foreach { case (source, path) =>
      val observations = dataProcessing.pullFromCsv(path) // observations is of type List[powerOutputObservation] ---- FUNCTOR
      if (observations.nonEmpty) {
        val outputs = observations.map(_.outputKw) // FUNCTOR

        // Count zero outputs, which would show power plant component failure
        val zeroCount = outputs.count(_ == 0.0)
        val zeroThreshold = outputs.size * 0.1 // 10% of data is zero
        if (zeroCount > zeroThreshold) {
          addAlert(s"[CRITICAL] $source ageing detected: $zeroCount zero-readings recorded. Replacement recommended.")
        }
        // Check performance trend. Compares old and new data, and looks for downward trend
        val (oldData, newData) = outputs.splitAt(outputs.size / 2)
        val ageingAlert = for {
          oldAvg <- Metrics.mean(oldData)
          newAvg <- Metrics.mean(newData)
          if newAvg < (oldAvg * 0.85) // Trigger if current production is < 85% of historical
        } yield s"[SUGGESTION] $source efficiency dropped to ${f"${(newAvg/oldAvg)*100}%.1f"}%%. Consider maintenance."

        ageingAlert.foreach(addAlert)
      }
    }
    checkAlerts() // Show results immediately
  }
}
