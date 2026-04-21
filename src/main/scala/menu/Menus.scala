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
        > 3. Exit                                                 |
        |                                                         |
        '---------------------------------------------------------'
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" => showEnergyMetricsMenu1() ; showMainMenu()
            case "2" => /* CALL CONTROL PANEL SUB MENUY */ ; showMainMenu()
            case "3" => println("Shutting down")
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
        .-------------------- ENERGY METRICS ---------------------.
        |                                                         |
        > 1. Display real time wind power data                    |
        > 2. Display real time hydro power data                   |
        > 3. Display real time nuclear power data                 |
        > 0. Go back                                              |
        |                                                         |
        '---------------------------------------------------------'
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" => {
              val observations=dataProcessing.pullFromCsv("data/wind.csv")
              showFilterMenu(observations)
              showEnergyMetricsMenu1() // come back after filter menu exits

            }
            case "2" =>  {
              val observations=dataProcessing.pullFromCsv("data/hydro.csv")
              showFilterMenu(observations)
              showEnergyMetricsMenu1()
            }
            case "3" => {
              val observations=dataProcessing.pullFromCsv("data/nuclear.csv")
              showFilterMenu(observations)
              showEnergyMetricsMenu1()
            }
            case "0" => showMainMenu()
            case _ => showEnergyMetricsMenu1()
        }
    }
  // obs - the list that comes from pullFromCsv
    @tailrec
    def showFilterMenu(obs: List[powerOutputObservation]): Unit= {
        println("""
        .-------------------- ENERGY METRICS ---------------------.
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
        scala.io.StdIn.readLine("Select: ").trim match {
          case "1" =>
            dataProcessing.printObservations(dataProcessing.filterLast24h(obs))
            showFilterMenu(obs)
          case "2" =>
            dataProcessing.printObservations(dataProcessing.filterLastMonth(obs))
            showFilterMenu(obs)
          case "3" =>
            dataProcessing.printObservations(dataProcessing.filterLastMonth(obs))
            showFilterMenu(obs)
          case "4" =>
            dataProcessing.printObservations(dataProcessing.filterLast6Months(obs))
            showFilterMenu(obs)
  // in cases 5-7 Either handles bad user input
          case "5" =>
            dataProcessing.askUserForDate() match {
              case Left(err)   => println(s"\n  Error: $err")
              case Right(date) => dataProcessing.printObservations(dataProcessing.filterByDay(date)(obs))
        }
    showFilterMenu(obs)

    case "6" =>
    dataProcessing.askUserForDate() match {
      case Left(err)   => println(s"\n  Error: $err")
      case Right(date) => dataProcessing.printObservations(dataProcessing.filterByWeek(date)(obs))
    }
    showFilterMenu(obs)

    case "7" =>
    dataProcessing.askUserForDate() match {
      case Left(err)   => println(s"\n  Error: $err")
      case Right(date) => dataProcessing.printObservations(dataProcessing.filterByMonth(date)(obs))
    }
    showFilterMenu(obs)

    case "0" => ()             // exits back to showEnergyMetricsMenu1
    case _ => showFilterMenu(obs)
    }
  }


}