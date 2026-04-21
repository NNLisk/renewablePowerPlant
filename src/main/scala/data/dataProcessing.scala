package data

import data.ApiCall.{parseUserDate, userFmt}

import java.io.{BufferedReader, File, FileReader}
import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.temporal.IsoFields
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object dataProcessing {
  private val userFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  /*def parseUserDate(raw: String): Either[String, LocalDateTime] = {
    val trimmed = raw.trim
    trimmed match {
      case "" =>
        Left("No date entered. Please use format 'DD/MM/YYYY HH:mm'.")
      case s =>
        try Right(LocalDateTime.parse(s, userFmt))
        catch {
          case _: DateTimeParseException=>
            Left(
              s"Date format is invalid'$s'.\n" +
                "Please enter dates in format 'DD/MM/YYYY HH:mm'.\n"

            )
        }
    }
  }
  def askUserForPeriod(): Either[String, (LocalDateTime, LocalDateTime)] = {
    val startRaw = scala.io.StdIn.readLine("  Enter start date (DD/MM/YYYY HH:mm): ")
    val endRaw   = scala.io.StdIn.readLine("  Enter end date   (DD/MM/YYYY HH:mm): ")
    // with pattern match check Either results
    (parseUserDate(startRaw), parseUserDate(endRaw)) match {
      case (Left(err), _) => Left(s"Start date error: $err")
      case (_, Left(err)) => Left(s"End date error: $err")
      case (Right(start), Right(end)) =>
        if (!start.isBefore(end))
          Left("Start date must be before end date.")
        else
          Right((start, end))
    }
  }*/
  //fetches saved API data from csv
  def pullFromCsv(filePath: String): List[powerOutputObservation] = {
    val file = new File(filePath)
    if (!file.exists()) {
      println(s"  File not found: $filePath")
      return List.empty
    }
    val result=ListBuffer[powerOutputObservation]()
    val reader=new BufferedReader(new FileReader(file))
    //val fmt=DateTimeFormatter.ISO_LOCAL_DATE_TIME

    try {
      reader.readLine() // skip header
      var line = reader.readLine()
      while (line!=null) {
        if (line.trim.nonEmpty) {
          val cols=line.trim.split(";")
          try {
            result+=powerOutputObservation(
              dataset= cols(0).toInt,
              startTime= LocalDateTime.parse(cols(1), fmt),
              endTime= LocalDateTime.parse(cols(2), fmt),
              outputKw= cols(3).toDouble
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
    raw match {
      case ""=>Left("No date entered. Please use format 'DD/MM/YYYY'.")
      case s=>
        try Right(LocalDateTime.parse(s, userFmt).withHour(0).withMinute(0))
        catch {
          case _: DateTimeParseException => Left(
              s"Invalid date '$s'.\n" + "Please use format 'DD/MM/YYYY', example: '15/04/2024'."
            )
        }
    }
  }
//higher order filtering functions, which take a list and predicate. they will return filtered list from scala collections
  val filterLast24h: List[powerOutputObservation] => List[powerOutputObservation] =
  obs => obs.filter(_.startTime.isAfter(LocalDateTime.now().minusHours(24)))

  val filterLastMonth: List[powerOutputObservation] => List[powerOutputObservation] =
  obs => obs.filter(_.startTime.isAfter(LocalDateTime.now().minusMonths(1)))

  val filterLast6Months: List[powerOutputObservation] => List[powerOutputObservation] =
  obs => obs.filter(_.startTime.isAfter(LocalDateTime.now().minusMonths(6)))

  val filterByDay: LocalDateTime => List[powerOutputObservation] => List[powerOutputObservation] =
    date => obs => obs.filter { o =>
      o.startTime.getDayOfMonth== date.getDayOfMonth &&
      o.startTime.getMonthValue== date.getMonthValue &&
      o.startTime.getYear== date.getYear
    }

  val filterByWeek: LocalDateTime => List[powerOutputObservation] => List[powerOutputObservation] =
    date => obs => obs.filter { o =>
      o.startTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)==date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) &&
      o.startTime.get(IsoFields.WEEK_BASED_YEAR)==date.get(IsoFields.WEEK_BASED_YEAR)
    }

  val filterByMonth: LocalDateTime => List[powerOutputObservation] => List[powerOutputObservation] =
    date => obs => obs.filter { o =>
      o.startTime.getMonthValue==date.getMonthValue &&
      o.startTime.getYear==date.getYear
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

  @tailrec
  private def printRows(obs: List[powerOutputObservation]): Unit = obs match {
    case Nil => ()
    case head :: tail =>
      println(f"  ${head.startTime.toString}%-25s ${head.endTime.toString}%-25s ${head.outputKw}%10.2f")
      printRows(tail)
  }




}
