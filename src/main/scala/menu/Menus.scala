package menus

// menu inspo from https://stackoverflow.com/questions/44461625/commandline-menu-loop-scala

import scala.annotation.tailrec
import data.ApiCall


object Menus {

    @tailrec
    def showMainMenu(): Unit = {

        println("""
        .------ Welcome to a renewabkle energy plant system ------.
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
        > 1. Display real time wind data                          |
        > 2. Display the control menu                             |
        > 3. Exit                                                 |
        |                                                         |
        '---------------------------------------------------------'
        """)

        scala.io.StdIn.readLine("Select: ").trim match {
            case "1" => ApiCall.showWindRealTime()
            case "2" => 
            case "3" => 
            case _ => showEnergyMetricsMenu1()
        }
    }
}