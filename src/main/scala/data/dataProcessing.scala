package data

import data.ApiCall.{parseUserDate, userFmt}

import java.io.{BufferedReader, File, FileReader}
import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.collection.mutable.ListBuffer

object dataProcessing {
  def parseUserDate(raw: String): Either[String, LocalDateTime] = {
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
  }
  //fetches data from csv
  def pullFromCsv(filePath: String): List[powerOutputObservation] = {
    val file = new File(filePath)
    if (!file.exists()) {
      println(s"  File not found: $filePath")
      return List.empty
    }
    val result=ListBuffer[powerOutputObservation]()
    val reader=new BufferedReader(new FileReader(file))
    val fmt=DateTimeFormatter.ISO_LOCAL_DATE_TIME

    try {
      reader.readLine() // skip header
      var line = reader.readLine()
      while (line != null) {
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

}
