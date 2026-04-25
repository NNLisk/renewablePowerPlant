package menus

// menu inspo from https://stackoverflow.com/questions/44461625/commandline-menu-loop-scala

import scala.annotation.tailrec
import data.{ApiCall, dataProcessing, powerOutputObservation}


object Menus {

    @tailrec
    def showMainMenu(): Unit = {

        println("""
        .------ Welcome to a renewable energy plant system ------.
        |                                                         |
        > 1. Display energy output and statistics                 |
        > 2. Display the control menu                             |
        > 3. Refresh datafiles (past 5 months)                    |
        > 3. Exit                                                 |
        |                                                         |
        '---------------------------------------------------------'
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" => showEnergyMetricsMenu1() ; showMainMenu()
            case "2" => /* CALL CONTROL PANEL SUB MENUY */ ; showMainMenu()
            case "3" => ApiCall.fetchPreviousFiveMonths(); showMainMenu()
            case "0" => println("Shutting down")
            case _ => showMainMenu()
        }
    }

    // i figured if we decide to have multiple nested submenus, they can be called just ControlPanelMenu2 etc

    @tailrec
    def showControlPanelMenu1(): Unit = {

        println("""
            menu here
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" =>
            case "2" =>
            case "3" =>
            case _ => showControlPanelMenu1()
        }
    }

    @tailrec
    def showEnergyMetricsMenu1(): Unit = {

        println("""
        .-------------------- Energy Source ---------------------.
        |                                                         |
        > 1. Wind power data                                      |
        > 2. Hydro power data                                     |
        > 3. Nuclear power data                                   |
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
              showFilterMenu(dataProcessing.pullFromCsv("data/nuclear.csv"))
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
        > 1. By Hour (last 24h)                                   |
        > 2. By Day (last month)                                  |
        > 3. By week (last month)                                 |
        > 4. By Month (last 6 months)                             |
        > 5. Get a specific day                                   |
        > 6. Get a specific week                                  |
        > 7. Get a specific month                                 |
        > 0. Go back                                              |
        |                                                         |
        '---------------------------------------------------------'
        """)
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
  // in cases 5-7 uses will need toenter specific date
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
    @tailrec
    def showActionMenu(filteredData: List[powerOutputObservation], originalObs: List[powerOutputObservation]): Unit = {
      println(s"\nFound ${filteredData.size} records. What would you like to do?")
      println("1. Print Raw Data")
      println("2. Sort by Energy Output (Ascending)")
      println("3. Sort by Energy Output (Descending)")
      println("4. Show Statistical Analysis (Mean, Median, etc.)")
      println("0. Back to Filters")

      scala.io.StdIn.readLine("Select: ").trim match {
        case "1" =>
          dataProcessing.printObservations(filteredData)
          showActionMenu(filteredData, originalObs)
        case "2" =>
          showActionMenu(filteredData.sortBy(_.outputKw), originalObs)

        case "3" =>
          showActionMenu(filteredData.sortBy(_.outputKw)(Ordering[Double].reverse), originalObs)

        /*case "4" =>
          performAnalysis(filteredData)
          showActionMenu(filteredData, originalObs)*/

        case "0" => showFilterMenu(originalObs)
        case _   => showActionMenu(filteredData, originalObs)
      }
    }


}