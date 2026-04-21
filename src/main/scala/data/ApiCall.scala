package data

// scala has no native http library :/ so we use the java library
// https://docs.oracle.com/en/java/javase/12/docs/api/java.net.http/java/net/http/HttpClient.html
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.URI
import config.Config

// API call object:
// - currying fetch functions for fingrid api calls, different datasets
// csv and dataobject functions

object ApiCall {

    val fingridurl: String = "https://data.fingrid.fi/api/datasets"
    val apiKey: String = Config.fingridApiKey
    val client = HttpClient.newHttpClient()


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

    val fetchFromFinGrid = fetch(fingridurl)
    val fetchWindRealTime = fetchFromFinGrid("181/data")("format=csv")
    val fetchHydroRealTime = fetchFromFinGrid("191/data")("format=csv")
    val fetchNuclearRealTime = fetchFromFinGrid("188/data")("format=csv")
    val fetchWind15Min = fetchFromFinGrid("75/data")("format=csv")

    def fetchWithOptions(dataset: String, startTime: String, endTime: String): Response[String] = {
        fetchFromFinGrid(s"${dataset}/data")(s"format=csv&startTime=${startTime}&endTime=${endTime}")
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

    def writeIntoCsv() = {
        
    }

    def pullFromCsv() = {
        
    }
}