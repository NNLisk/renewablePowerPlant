// SCALA PROJECT | RENEWABLE POWERPLANT

// Contributors:
// Niko Lausto            || 001890439
// Emilija Kurtinaityte   || 002340690
// Adelina Stan           || 002288891

// AI statement: No AI tools 


// If build tools won't work, compiles with:
// scalac src/main/scala/Main.scala src/main/scala/menu/Menus.scala 
// and runs with:
// scala src/main/scala/Main.scala


import menus.Menus

object Main {
  def main(args: Array[String]): Unit = {
    

    // the main probably should only call to display menu
    Menus.showMainMenu()

  }
}

