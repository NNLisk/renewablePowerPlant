package menus

// menu inspo from https://stackoverflow.com/questions/44461625/commandline-menu-loop-scala

import scala.annotation.tailrec
import data.{Alerts, ApiCall, dataProcessing, powerOutputObservation}
import java.time.{LocalDateTime}

object Menus {


    // all menus here follow the same method, tailrecursive functions
    // selections call the other menu functions, errors call the same menu
    // recursively after displaying the error message to make sure it doesn't
    // crash at any point. Choice is handled via pattern matching.

    @tailrec
    def showMainMenu(): Unit = {

        println("""
        .------ Welcome to a renewable energy plant system ------.
        |                                                         |
        > 1. View energy output and statistics                    |
        > 2. View the control menu                                |
        > 3. Refresh datafiles (past 5 months)                    |
        > 0. Exit                                                 |
        |                                                         |
        '---------------------------------------------------------'
        """)

        // show menus based on choice

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" => showEnergyMetricsMenu1() ; showMainMenu()
            case "2" => showControlPanelMenu1(); showMainMenu()
            case "3" => ApiCall.fetchPreviousFiveMonths(); showMainMenu()
            case "0" => println("Shutting down")
            case _ => showMainMenu()
        }
    }

    // i figured if we decide to have multiple nested submenus, they can be called just ControlPanelMenu2 etc

    @tailrec
    def showControlPanelMenu1(): Unit = {

        println("""
        .-------------------- Control Panel --------------------.
        |                                                       |
        > 1. Adjust solar panel angle                           |
        > 2. Set wind turbine direction                         |
        > 3. View system alerts                                 |
        > 4. Simulate equipment fault                           |
        > 5. Run check for aging components                     |
        > 6. Reset all alerts                                   |
        > 0. Go back                                            |
        |                                                       |
        '-------------------------------------------------------'
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            // handle solar panel angle adjustment
            case "1" =>
              val angleRaw = scala.io.StdIn.readLine("  Enter panel angle (0-90): ").trim
              // parse user input safely (option, some, none)
              angleRaw.toDoubleOption match {
                case Some(angle) if angle >= 0.0 && angle <= 90.0 =>
                  println(s"  [OK] Solar panel angle adjusted to ${f"$angle%.1f"} degrees")
                case Some(_) => // numeric value but out of range
                  println("  Error: Angle must be between 0 and 90.")
                case None => // non-numeric input
                  println("  Error: Please enter a valid number.")
              }
              showControlPanelMenu1()

            // handle wind turbine direction adjustment
            case "2" =>
              val directionRaw = scala.io.StdIn.readLine("  Enter turbine direction (0-359): ").trim
              // parse user input safely (option, some, none)
              directionRaw.toDoubleOption match {
                case Some(direction) if direction >= 0.0 && direction < 360.0 =>
                  println(s"  [OK] Wind turbine direction set to ${f"$direction%.1f"} degrees")
                case Some(_) => // numeric value but out of range
                  println("  Error: Direction must be between 0 and 359.")
                case None => // non-numeric input
                  println("  Error: Please enter a valid number.")
              }
              showControlPanelMenu1()

            // refresh and display alerts
            case "3" =>
              // ApiCall.fetchPreviousFiveMonths(); // refresh data to ensure updated alerts (we can only check stored data instead...)
              Alerts.refreshDataAlerts() // generate alerts
              Alerts.checkAlerts() // display alerts
              showControlPanelMenu1()

            // create "dummy" fault simulation
            case "4" =>
              println("  Select source to fault:")
              println("  1. Solar")
              println("  2. Wind")
              println("  3. Hydro")

              scala.io.StdIn.readLine("  Select: ").trim match {
                case "1" => Alerts.triggerFault("Solar")
                case "2" => Alerts.triggerFault("Wind")
                case "3" => Alerts.triggerFault("Hydro")
                case _ => println("  Invalid selection.")
              }
              showControlPanelMenu1()
            case "5" =>
              Alerts.checkAgeingStatus()
              showControlPanelMenu1()
            // ---------------------------------clear all active alerts (maybe change to clear specific alert)---------------------------------
            case "6" =>
              Alerts.resetAlerts()
              showControlPanelMenu1()

            // return to main menu
            case "0" => ()
            case _ => showControlPanelMenu1() // invalid input (redisplay menu)
        }
    }

    @tailrec
    def showEnergyMetricsMenu1(): Unit = {

        println("""
        .-------------------- Energy Source ---------------------.
        |                                                         |
        > 1. Wind power data                                      |
        > 2. Hydro power data                                     |
        > 3. Solar power data                                     |
        > 0. Go back                                              |
        |                                                         |
        '---------------------------------------------------------'
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" => {
              showFilterMenu(dataProcessing.pullFromCsv("data/wind.csv"))
              showEnergyMetricsMenu1() // come back after filter menu exits

            }
            case "2" =>  {
              showFilterMenu(dataProcessing.pullFromCsv("data/hydro.csv"))
              showEnergyMetricsMenu1()
            }
            case "3" => {
              showFilterMenu(dataProcessing.pullFromCsv("data/solar.csv"))
              showEnergyMetricsMenu1()
            }
            case "0" =>()
            case _ => showEnergyMetricsMenu1()
        }
    }
    // obs - the list that comes from pullFromCsv
    @tailrec
    def showFilterMenu(obs: List[powerOutputObservation]): Unit= {
        println("""
        .-------------------- Filter period ---------------------.
        |                                                         |
        > 1. By hour (last 24h)                                   |
        > 2. By day (last month)                                  |
        > 3. By week (last month)                                 |
        > 4. By month (last 6 months)                             |
        > 5. Get a specific day                                   |
        > 6. Get a specific week                                  |
        > 7. Get a specific month                                 |
        > 0. Go back                                              |
        |                                                         |
        '---------------------------------------------------------'
        """)

        // println(obs)
        
        val selection=scala.io.StdIn.readLine("Select: ").trim
        val filtered: Option[List[powerOutputObservation]] = selection match {
          case "1" =>
            Some(dataProcessing.filterLast24h(obs))
          case "2" =>
            Some(dataProcessing.filterLastMonth(obs))
          case "3" =>
            Some(dataProcessing.filterLastMonth(obs))
          case "4" =>
            Some(dataProcessing.filterLast6Months(obs))
  // in cases 5-7 uses will need to enter specific date
          case "5" | "6" | "7" =>
            dataProcessing.askUserForDate() match {
              case Left(err) =>
                println(s"Error: $err")
                None
              case Right(date) => selection match {
                case "5" => Some(dataProcessing.filterByDay(date)(obs))
                case "6" => Some(dataProcessing.filterByWeek(date)(obs))
                case "7" => Some(dataProcessing.filterByMonth(date)(obs))
                case _ => None
              }
            }
          case "0" => None             // exits back to showEnergyMetricsMenu1
          case _ =>{
            println("Invalid choice")
            None
          }
        }
      // If we filtered list is not none, then move on to action Menu
        filtered match {
          case Some(data) if data.nonEmpty =>
            showActionMenu(data, obs)
          case Some(_) => println("\nNo data found for that period.")
            showFilterMenu(obs)
          case None =>
            if (selection!="0") showFilterMenu(obs)
            else()
        }
      }

    // actions after data found
    @tailrec
    def showActionMenu(filteredData: List[powerOutputObservation], originalObs: List[powerOutputObservation]): Unit = {
      println(s"\nFound ${filteredData.size} records. What would you like to do?")
      println("1. Print raw data")
      println("2. Sort by energy output (Ascending)")
      println("3. Sort by energy output (Descending)")
      println("4. Show Statistical Analysis (Mean, Median, etc.)")
      println("0. Back to Filters")

      scala.io.StdIn.readLine("Select: ").trim match {
        case "1" =>
          dataProcessing.printObservations(filteredData)
          showActionMenu(filteredData, originalObs)
        case "2" =>
          val sorted=filteredData.sortBy(_.outputKw)
          println("\nData sorted: low to high.")
          println("Select '1' to view the sorted data.")
          showActionMenu(sorted, originalObs)

        case "3" =>
          val sorted = filteredData.sortBy(_.outputKw)(Ordering[Double].reverse)
          println("\nData sorted: high to low.")
          println("Select '1' to view the sorted data.")
          showActionMenu(sorted, originalObs)
        case "4" =>
          dataProcessing.printAnalysisCard(filteredData)
          showActionMenu(filteredData, originalObs)

        case "0" => showFilterMenu(originalObs)
        case _   => showActionMenu(filteredData, originalObs)
      }
    }

}