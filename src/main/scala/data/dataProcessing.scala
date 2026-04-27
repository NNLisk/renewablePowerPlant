package data

import data.ApiCall.{parseUserDate, userFmt}

import java.io.{BufferedReader, File, FileReader}
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.temporal.IsoFields
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object dataProcessing {
  private val userFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  //private val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  //fetches saved API data from csv
  def pullFromCsv(filePath: String): List[powerOutputObservation] = {
    val file = new File(filePath)
    if (!file.exists()) {
      println(s"  File not found: $filePath")
      return List.empty
    }
    val result=ListBuffer[powerOutputObservation]()
    val reader=new BufferedReader(new FileReader(file))

    try {
      reader.readLine() // skip header
      var line = reader.readLine()
      while (line!=null) {
        if (line.trim.nonEmpty) {
          val cols=line.trim.split(";")
          try {
            val time = LocalDateTime.ofInstant(Instant.parse(cols(1).trim), java.time.ZoneOffset.UTC)
            
            // changed this because it had trouble with the Z in datetime
            // val time = LocalDateTime.parse(cols(1).trim, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            
            result+=powerOutputObservation(
              dataset= cols(0).toInt,
              startTime = time,
              endTime= time, //for now assume end time is same as start for this data structure
              outputKw= cols(3).trim.toDouble
            )
          } catch {
            case e: Exception => println(s"  Skipping bad row: $line — ${e.getMessage}")
          }
        }
        line = reader.readLine()
      }
    } catch {
      case e: Exception => println(s"  Error reading CSV: ${e.getMessage}")
    } finally {
      reader.close()
    }
    result.toList
  }

//the function below will be used for menu options that ask user to enter specific date
  def askUserForDate(): Either[String, LocalDateTime] = {
    val raw = scala.io.StdIn.readLine("  Enter date (DD/MM/YYYY): ").trim
    if (raw.isEmpty) Left("No date entered.")
    else {
      try {
        // parse as LocalDate first, then convert to LocalDateTime at start of day
        val date=LocalDate.parse(raw, userFmt)
        Right(date.atStartOfDay())
      } catch {
        case _: DateTimeParseException =>
          Left(s"Invalid format '$raw'. Use DD/MM/YYYY (e.g., 15/04/2024).")
      }
    }
  }
//higher order filtering functions, which take a list and predicate. they will return filtered list from scala collections
val filterLast24h: List[powerOutputObservation] => List[powerOutputObservation] = obs => {
  if (obs.isEmpty) Nil
  else {
    val mostRecent = obs.map(_.startTime).maxBy(_.toEpochSecond(java.time.ZoneOffset.UTC))
    obs.filter(_.startTime.isAfter(mostRecent.minusHours(24)))
  }
}
  val filterLastMonth: List[powerOutputObservation] => List[powerOutputObservation] =
  obs => obs.filter(_.startTime.isAfter(LocalDateTime.now().minusMonths(1)))

  val filterLast6Months: List[powerOutputObservation] => List[powerOutputObservation] =
  obs => obs.filter(_.startTime.isAfter(LocalDateTime.now().minusMonths(6)))

  //first choose the date then apply the resulting function to a dataset (currying)
  val filterByDay: LocalDateTime => List[powerOutputObservation] => List[powerOutputObservation] =
    date => obs => obs.filter { o =>
      o.startTime.toLocalDate.isEqual(date.toLocalDate)
    }

  val filterByWeek: LocalDateTime => List[powerOutputObservation] => List[powerOutputObservation] =
    date => obs => {
      val targetWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
      val targetYear = date.get(IsoFields.WEEK_BASED_YEAR)
      obs.filter { o =>
        o.startTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == targetWeek &&
        o.startTime.get(IsoFields.WEEK_BASED_YEAR) == targetYear
      }
    }

  val filterByMonth: LocalDateTime => List[powerOutputObservation] => List[powerOutputObservation] =
    date => obs => obs.filter { o =>
      o.startTime.getMonth == date.getMonth && o.startTime.getYear == date.getYear
    }
  //functions for printing the filtered data
  def printObservations(obs: List[powerOutputObservation]): Unit = {
    if (obs.isEmpty) {
      println("\n  No data found for that period.")
    } else {
      println(f"\n  ${"Start time"}%-25s ${"End time"}%-25s ${"kW"}%10s")
      println("  " + "-" * 63)
      printRows(obs)
      println(s"\n  Total: ${obs.size} row(s)")
    }
  }

  // =============== function for printing the analysis ===============
  def printAnalysisCard(filteredData: List[powerOutputObservation]): Unit = {
    val width = 57
    val innerWidth = width - 4
    val lines = Metrics.summarize(filteredData)
    val indent = "        "

    println("        .---------------- Statistical Analysis ----------------.")
    println("        |                                                       |")

    lines.foreach { line =>
      val clipped = if (line.length > innerWidth) line.take(innerWidth) else line
      val padded = ("%-" + innerWidth + "s").format(clipped)
      println(s"$indent| $padded |")
    }

    println("        |                                                       |")
    println("        '-------------------------------------------------------'")
  }

  @tailrec
  private def printRows(obs: List[powerOutputObservation]): Unit = obs match {
    case Nil => ()
    case head :: tail =>
      println(f"  ${head.startTime.toString}%-25s ${head.endTime.toString}%-25s ${head.outputKw}%10.2f")
      printRows(tail)
  }




}
