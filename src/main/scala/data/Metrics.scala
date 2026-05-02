
package data

object Metrics {

	private val toOutputs: List[powerOutputObservation] => List[Double] =
		observations => observations.map(_.outputKw) // FUNCTOR

    // option (&some/none) to handle missing values without throwing exceptions
	val mean: List[Double] => Option[Double] = values =>
		if (values.isEmpty) None else Some(values.sum / values.size)

	val median: List[Double] => Option[Double] = values =>
		if (values.isEmpty) None
		else {
			val sorted = values.sorted
			val n = sorted.size
			if (n % 2 == 1) Some(sorted(n / 2))
			else {
				val upper = n / 2
				val lower = upper - 1
				Some((sorted(lower) + sorted(upper)) / 2.0)
			}
		}
    // mode is calculated by grouping values and counting frequencies (return none if all values are unique)
	val mode: List[Double] => Option[Double] = values =>
		if (values.isEmpty) None
		else {
			val frequencies = values.groupBy(identity).view.mapValues(_.size).toMap
			val highestFrequency = frequencies.values.max
			if (highestFrequency <= 1) None
			else Some(frequencies.collect { case (value, count) if count == highestFrequency => value }.min)
		}
    
	val range: List[Double] => Option[Double] = values =>
		if (values.isEmpty) None else Some(values.max - values.min)

	val midrange: List[Double] => Option[Double] = values =>
		if (values.isEmpty) None else Some((values.max + values.min) / 2.0)

	val summarize: List[powerOutputObservation] => List[String] = observations => {
		val outputs = toOutputs(observations)

		if (outputs.isEmpty) {
			List("No values available for analysis.")
		} else {
			val format: Double => String = value => f"$value%.2f"
			val modeText = mode(outputs).map(format).getOrElse("No mode (all values occur once)") // FUNCTOR - Option.map transforms the present value if it exists

			List(
				s"Count: ${outputs.size}",
				s"Mean: ${mean(outputs).map(format).getOrElse("N/A")}", // FUNCTOR - Option.map formats the computed result when it exists
				s"Median: ${median(outputs).map(format).getOrElse("N/A")}", // FUNCTOR - Option.map preserves the optional result while changing its type
				s"Mode: $modeText",
				s"Range: ${range(outputs).map(format).getOrElse("N/A")}", // FUNCTOR - Option.map applies formatting only when a range exists
				s"Midrange: ${midrange(outputs).map(format).getOrElse("N/A")}" // FUNCTOR - Option.map keeps absence/presence intact
			)
		}
	}
}