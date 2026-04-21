package data

// scala has no native http library :/ so we use the java library
// https://docs.oracle.com/en/java/javase/12/docs/api/java.net.http/java/net/http/HttpClient.html
import config.Config

import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.URI
import config.Config

import java.io.{BufferedReader, BufferedWriter, FileReader, FileWriter}
import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.collection.mutable.ListBuffer
import java.io.File
// API call object:
// - currying fetch functions for fingrid api calls, different datasets
// csv and dataobject functions

object ApiCall {

    val fingridurl: String = "https://data.fingrid.fi/api/datasets"
    val apiKey: String = Config.fingridApiKey
    val client = HttpClient.newHttpClient()

    private val userFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val apiFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    private val csvFmt  = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    val fetch: String => String => String => HttpResponse[String] = url => endpoint => options => {
        println(s"Fetching from $url/$endpoint?$options")

        val request = HttpRequest.newBuilder()
            .uri(URI.create(s"$url/$endpoint?$options"))
            .header("x-api-key", apiKey)
            .GET()
            .build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    // curried functions to fetch a specific dataset data from fingrid

    def fetchFromFinGrid() = fetch(fingridurl)

    def fetchWindRealTime() = fetchFromFinGrid("181/data")("format=csv")
    def fetchHydroRealTime() = fetchFromFinGrid("191/data")("format=csv")
    def fetchNuclearRealTime() = fetchFromFinGrid("188/data")("format=csv")

    def fetchWind15Min() = fetchFromFinGrid("75/data")("format=csv")

    def fetchWindWithOptions(startTime: LocalDateTime, endTime: LocalDateTime): HttpResponse[String] = {
        fetchFromFinGrid(s"181/data")(s"format=csv&startTime=${startTime}&endTime=${endTime}")
    }

    def fetchHydroWithOptions(startTime: LocalDateTime, endTime: LocalDateTime): HttpResponse[String] = {
        fetchFromFinGrid(s"191/data")(s"format=csv&startTime=${startTime}&endTime=${endTime}")
    }

    def fetchNuclearWithOptions(startTime: LocalDateTime, endTime: LocalDateTime): HttpResponse[String] = {
        fetchFromFinGrid(s"188/data")(s"format=csv&startTime=${startTime}&endTime=${endTime}")
    }

    def showWindRealTime(): Unit = {
        val data: HttpResponse[String] = fetchWindRealTime
        println(data.body())
    }

    def showNuclearRealTime(): Unit = {
        val data: HttpResponse[String] = fetchNuclearRealTime
        println(data.body())
    }

    def showHydroRealTime(): Unit = {
        val data: HttpResponse[String] = fetchHydroRealTime
        println(data.body())
    }


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
    //imperative file handling functions. First one parses csv
    def parseCsv (csv: String): List[powerOutputObservation] = {
        val lines=csv.split("\n")
        val result=ListBuffer[powerOutputObservation]()
        for (line <- lines.tail) { // .tail skips the header row
            if (line.trim.nonEmpty) {
                val cols = line.trim.split(",")
                try {
                    result += powerOutputObservation(
                        dataset= cols(0).toInt,
                        startTime=LocalDateTime.parse(cols(1).trim, csvFmt),
                        endTime=LocalDateTime.parse(cols(2).trim, csvFmt),
                        outputKw=cols(3).toDouble
                    )
                } catch {
                    case e: Exception => println(s"  Skipping bad row: $line — ${e.getMessage}")
                }
            }
        }

        result.toList
    }
    //writes fetched API data into csv
    def writeIntoCsv(observations: List[powerOutputObservation], filePath: String): Unit = {
        val file=new File(filePath)
        val exists=file.exists()
        // make sure the folder exists
        file.getParentFile match {
            case null=>()
            case parent => parent.mkdirs()
        }
        val writer = new BufferedWriter(new FileWriter(file, true)) // append mode
        try {
            if (!exists) {
                writer.write("datasetId,startTime,endTime,value")
                writer.newLine()
            }
            for (obs <- observations) {
                writer.write(s"${obs.dataset};${obs.startTime};${obs.endTime};${obs.outputKw}")
                writer.newLine()
            }
            println(s"  Saved ${observations.size} rows to $filePath")
        } catch {
            case e: Exception => println(s"  Error writing CSV: ${e.getMessage}")
        } finally {
            writer.close()
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


